package uk.ac.ebi.uniprot.api.uniprotkb.service;

import com.google.common.base.Strings;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;

import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.api.common.repository.store.StreamRequest;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.api.uniprotkb.controller.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import uk.ac.ebi.uniprot.api.uniprotkb.repository.store.UniProtStoreClient;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.domain.uniprot.InactiveReasonType;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.search.DefaultSearchHandler;
import uk.ac.ebi.uniprot.search.SolrQueryUtil;
import uk.ac.ebi.uniprot.search.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.search.field.UniProtField;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.*;

@Service
public class UniProtEntryService {
    private static final String ACCESSION = "accession_id";
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final ThreadPoolTaskExecutor downloadTaskExecutor;
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final DefaultSearchHandler defaultSearchHandler;
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;

    public UniProtEntryService(UniprotQueryRepository repository,
                               UniprotFacetConfig uniprotFacetConfig,
                               UniProtStoreClient entryStore,
                               StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer,
                               ThreadPoolTaskExecutor downloadTaskExecutor,
                               DefaultSearchHandler defaultSearchHandler) {
        this.repository = repository;
        this.defaultSearchHandler = defaultSearchHandler;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.downloadTaskExecutor = downloadTaskExecutor;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore);
    }

    public QueryResult<?> search(SearchRequestDTO request, MessageConverterContext<UniProtEntry> context) {
        MediaType contentType = context.getContentType();
        SimpleQuery simpleQuery = createQuery(request);

        QueryResult<UniProtDocument> results = repository
                .searchPage(simpleQuery, request.getCursor(), request.getSize());
        if (request.hasFacets()) {
            context.setFacets(results.getFacets());
        }

        if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> accList = results.getContent().stream().map(doc -> doc.accession).collect(Collectors.toList());
            context.setEntityIds(results.getContent().stream().map(doc -> doc.accession));
            return QueryResult.of(accList, results.getPage(), results.getFacets());
        } else {
            QueryResult<UniProtEntry> queryResult = resultsConverter
                    .convertQueryResult(results, FieldsParser.parseForFilters(request.getFields()));
            context.setEntities(queryResult.getContent().stream());
            return queryResult;
        }
    }

    public void getByAccession(String accession, String fields, MessageConverterContext<UniProtEntry> context) {
        MediaType contentType = context.getContentType();
        try {
            Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION).is(accession.toUpperCase()));
            Optional<UniProtDocument> optionalDoc = repository.getEntry(simpleQuery);
            Optional<UniProtEntry> optionalUniProtEntry = optionalDoc
                    .map(doc -> resultsConverter.convertDoc(doc, filters))
                    .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
            if(optionalUniProtEntry.isPresent()){
                if (contentType.equals(LIST_MEDIA_TYPE)) {
                    context.setEntityIds(Stream.of(accession));
                } else {
                    UniProtEntry uniProtEntry = optionalUniProtEntry.get();
                    if (isInactiveAndMergedEntry(uniProtEntry)) {
                        String mergedAccession = uniProtEntry.getInactiveReason().getMergeDemergeTo().get(0);
                        getByAccession(mergedAccession, fields, context);
                    } else {
                        context.setEntities(Stream.of(uniProtEntry));
                    }
                }
            }else{
                throw new ResourceNotFoundException("{search.not.found}");
            }
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public void stream(SearchRequestDTO request, MessageConverterContext<UniProtEntry> context, ResponseBodyEmitter emitter) {
        MediaType contentType = context.getContentType();
        boolean defaultFieldsRequested = FieldsParser
                .isDefaultFilters(FieldsParser.parseForFilters(request.getFields()));
        streamEntities(request, defaultFieldsRequested, contentType, context);

        downloadTaskExecutor.execute(() -> {
            try {
                emitter.send(context, contentType);
            } catch (IOException e) {
                emitter.completeWithError(e);
            }
            emitter.complete();
        });
    }

    private SimpleQuery createQuery(SearchRequestDTO request) {
        SolrQueryBuilder builder = new SolrQueryBuilder();

        if (needFilterIsoform(request)) {
            builder.addFilterQuery(new SimpleQuery(UniProtField.Search.is_isoform.name() + ":" + false));
        }

        boolean hasScore = false;
        String requestedQuery = request.getQuery();
        if(defaultSearchHandler.hasDefaultSearch(requestedQuery)){
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            builder.defaultOperator(Query.Operator.OR);
        }
        builder.query(requestedQuery);
        builder.addSort(getUniProtSort(request.getSort(),hasScore));

        if(request.hasFacets()) {
            builder.facets(request.getFacetList());
            builder.facetConfig(uniprotFacetConfig);
        }
        return builder.build();
    }

    private void streamEntities(SearchRequestDTO request, boolean defaultFieldsOnly, MediaType contentType, MessageConverterContext<UniProtEntry> context) {
        StreamRequest.StreamRequestBuilder requestBuilder =  StreamRequest.builder();

        if (needFilterIsoform(request)) {
            requestBuilder.filterQuery(UniProtField.Search.is_isoform.name() + ":" + false);
        }

        boolean hasScore = false;
        String requestedQuery = request.getQuery();
        if(defaultSearchHandler.hasDefaultSearch(requestedQuery)){
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            requestBuilder.defaultQueryOperator(Query.Operator.OR.toString());
        }
        requestBuilder.query(requestedQuery);
        requestBuilder.sort(getUniProtSort(request.getSort(),hasScore));

        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(storeStreamer.idsStream(requestBuilder.build()));
        }

        if (defaultFieldsOnly && (contentType.equals(APPLICATION_JSON) || contentType
                .equals(TSV_MEDIA_TYPE) || contentType.equals(XLS_MEDIA_TYPE))) {
            context.setEntities(storeStreamer.defaultFieldStream(requestBuilder.build()));
        } else {
            context.setEntities(storeStreamer.idsToStoreStream(requestBuilder.build()));
        }
    }

    private Sort getUniProtSort(String sortStr,boolean hasScore) {
        if (Strings.isNullOrEmpty(sortStr)) {
            return UniProtSortUtil.createDefaultSort(hasScore);
        } else {
            return UniProtSortUtil.createSort(sortStr);
        }
    }

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * if does not have id fields (we can not filter isoforms when querying for IDS)
     * AND
     * has includeIsoform params in the request URL
     * Then we analyze the includeIsoform request parameter.
     * IMPORTANT: Implementing this way, query search has precedence over isoform request parameter
     *
     * @return true if we need to add isoform filter query
     */
    private boolean needFilterIsoform(SearchRequestDTO request){
        boolean hasIdFieldTerms = SolrQueryUtil.hasFieldTerms(request.getQuery(),
                UniProtField.Search.accession_id.name(),
                UniProtField.Search.mnemonic.name(),
                UniProtField.Search.is_isoform.name());

        if(!hasIdFieldTerms){
            return !request.isIncludeIsoform();
        }else{
            return false;
        }
    }

    private boolean isInactiveAndMergedEntry(UniProtEntry uniProtEntry) {
        return !uniProtEntry.isActive() &&
                uniProtEntry.getInactiveReason() != null &&
                uniProtEntry.getInactiveReason().getInactiveReasonType().equals(InactiveReasonType.MERGED) &&
                Utils.notEmpty(uniProtEntry.getInactiveReason().getMergeDemergeTo());
    }
}
