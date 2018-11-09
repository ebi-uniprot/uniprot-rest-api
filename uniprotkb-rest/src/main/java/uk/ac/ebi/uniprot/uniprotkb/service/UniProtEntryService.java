package uk.ac.ebi.uniprot.uniprotkb.service;

import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleField;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyEmitter;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.common.repository.store.StoreStreamer;
import uk.ac.ebi.uniprot.common.service.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;
import uk.ac.ebi.uniprot.dataservice.document.uniprot.UniProtDocument;
import uk.ac.ebi.uniprot.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.uniprotkb.controller.request.FieldsParser;
import uk.ac.ebi.uniprot.uniprotkb.controller.request.SearchRequestDTO;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.impl.SolrQueryBuilder;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.impl.UniprotFacetConfig;
import uk.ac.ebi.uniprot.uniprotkb.repository.search.impl.UniprotQueryRepository;
import uk.ac.ebi.uniprot.uniprotkb.repository.store.UniProtStoreClient;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static uk.ac.ebi.uniprot.rest.output.UniProtMediaType.*;

@Service
public class UniProtEntryService {
    private static final String ACCESSION = "accession_id";
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final ThreadPoolTaskExecutor downloadTaskExecutor;
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;

    public UniProtEntryService(UniprotQueryRepository repository,
                               UniprotFacetConfig uniprotFacetConfig,
                               UniProtStoreClient entryStore,
                               StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer,
                               ThreadPoolTaskExecutor downloadTaskExecutor) {
        this.repository = repository;
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
        if (request.isIncludeFacets()) {
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
            if (contentType.equals(LIST_MEDIA_TYPE)) {
                context.setEntityIds(Stream.of(accession));
            } else {
                Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
                SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(ACCESSION).is(accession.toUpperCase()));
                simpleQuery.addProjectionOnField(new SimpleField(ACCESSION));
                Optional<UniProtDocument> optionalDoc = repository.getEntry(simpleQuery);
                Optional<UniProtEntry> optionalUniProtEntry = optionalDoc
                        .map(doc -> resultsConverter.convertDoc(doc, filters))
                        .orElseThrow(() -> new ServiceException("Document found to be null"));
                UniProtEntry uniProtEntry = optionalUniProtEntry
                        .orElseThrow(() -> new ServiceException("Entry found to be null"));
                context.setEntities(Stream.of(uniProtEntry));
            }
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
        SimpleQuery simpleQuery;
        if (request.isIncludeFacets()) {
            simpleQuery = SolrQueryBuilder.of(request.getQuery(), uniprotFacetConfig).build();
        } else {
            simpleQuery = SolrQueryBuilder.of(request.getQuery()).build();
        }

        if (request.needIsoformFilterQuery()) {
            simpleQuery.addFilterQuery(new SimpleQuery(UniProtField.Search.is_isoform.name() + ":" + false));
        }
        simpleQuery.addSort(getUniProtSort(request.getSort()));

        return simpleQuery;
    }

    private void streamEntities(SearchRequestDTO request, boolean defaultFieldsOnly, MediaType contentType, MessageConverterContext<UniProtEntry> context) {
        String query = request.getQuery();
        String filterQuery = null;
        if (request.needIsoformFilterQuery()) {
            filterQuery = UniProtField.Search.is_isoform.name() + ":" + false;
        }
        Sort sort = getUniProtSort(request.getSort());
        if (contentType.equals(LIST_MEDIA_TYPE)) {
            context.setEntityIds(storeStreamer.idsStream(query, filterQuery, sort));
        }

        if (defaultFieldsOnly && (contentType.equals(APPLICATION_JSON) || contentType
                .equals(TSV_MEDIA_TYPE) || contentType.equals(XLS_MEDIA_TYPE))) {
            context.setEntities(storeStreamer.defaultFieldStream(query, filterQuery, sort));
        } else {
            context.setEntities(storeStreamer.idsToStoreStream(query, filterQuery, sort));
        }
    }

    private Sort getUniProtSort(String sortStr) {
        return UniProtSortUtil.createSort(sortStr).orElse(UniProtSortUtil.createDefaultSort());
    }
}
