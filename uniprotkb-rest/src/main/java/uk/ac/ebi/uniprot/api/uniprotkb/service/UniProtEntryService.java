package uk.ac.ebi.uniprot.api.uniprotkb.service;

import com.google.common.base.Strings;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.api.common.repository.store.StreamRequest;
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

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

@Service
public class UniProtEntryService {
    private static final String ACCESSION = "accession_id";
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final DefaultSearchHandler defaultSearchHandler;
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;

    public UniProtEntryService(UniprotQueryRepository repository,
                               UniprotFacetConfig uniprotFacetConfig,
                               UniProtStoreClient entryStore,
                               StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer,
                               DefaultSearchHandler defaultSearchHandler) {
        this.repository = repository;
        this.defaultSearchHandler = defaultSearchHandler;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore);
    }

    public QueryResult<UniProtEntry> search(SearchRequestDTO request) {
        SimpleQuery simpleQuery = createQuery(request);

        QueryResult<UniProtDocument> results = repository
                .searchPage(simpleQuery, request.getCursor(), request.getSize());

        return resultsConverter.convertQueryResult(results, FieldsParser.parseForFilters(request.getFields()));
    }

    public UniProtEntry getByAccession(String accession, String fields) {
        try {
            UniProtEntry result = null;
            Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
            SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION).is(accession.toUpperCase()));
            Optional<UniProtDocument> optionalDoc = repository.getEntry(simpleQuery);
            Optional<UniProtEntry> optionalUniProtEntry = optionalDoc
                    .map(doc -> resultsConverter.convertDoc(doc, filters))
                    .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
            if(optionalUniProtEntry.isPresent()){
                result = optionalUniProtEntry.get();
                if (isInactiveAndMergedEntry(result)) {
                    String mergedAccession = result.getInactiveReason().getMergeDemergeTo().get(0);
                    result = getByAccession(mergedAccession, fields);
                }
            }else{
                throw new ResourceNotFoundException("{search.not.found}");
            }
            return result;
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<UniProtEntry> stream(SearchRequestDTO request, MediaType contentType) {
        StreamRequest streamRequest = getStreamRequest(request);
        boolean defaultFieldsOnly = FieldsParser
                .isDefaultFilters(FieldsParser.parseForFilters(request.getFields()));
        if (defaultFieldsOnly && (contentType.equals(APPLICATION_JSON) || contentType
                .equals(TSV_MEDIA_TYPE) || contentType.equals(XLS_MEDIA_TYPE))) {
            return storeStreamer.defaultFieldStream(streamRequest);
        } else {
            return storeStreamer.idsToStoreStream(streamRequest);
        }
    }

    public Stream<String> streamIds(SearchRequestDTO request) {
        StreamRequest streamRequest = getStreamRequest(request);
        return storeStreamer.idsStream(streamRequest);
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

    private StreamRequest getStreamRequest(SearchRequestDTO request) {
        StreamRequest.StreamRequestBuilder streamBuilder =  StreamRequest.builder();

        if (needFilterIsoform(request)) {
            streamBuilder.filterQuery(UniProtField.Search.is_isoform.name() + ":" + false);
        }

        boolean hasScore = false;
        String requestedQuery = request.getQuery();
        if(defaultSearchHandler.hasDefaultSearch(requestedQuery)){
            requestedQuery = defaultSearchHandler.optimiseDefaultSearch(requestedQuery);
            hasScore = true;
            streamBuilder.defaultQueryOperator(Query.Operator.OR.toString());
        }
        streamBuilder.query(requestedQuery);
        streamBuilder.sort(getUniProtSort(request.getSort(),hasScore));

        return streamBuilder.build();
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
