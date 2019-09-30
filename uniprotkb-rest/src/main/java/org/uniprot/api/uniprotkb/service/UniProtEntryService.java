package org.uniprot.api.uniprotkb.service;

import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.uniprot.api.rest.output.UniProtMediaType.TSV_MEDIA_TYPE;
import static org.uniprot.api.rest.output.UniProtMediaType.XLS_MEDIA_TYPE;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Stream;

import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.common.repository.search.QueryBoosts;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.store.StoreStreamer;
import org.uniprot.api.uniprotkb.controller.request.FieldsParser;
import org.uniprot.api.uniprotkb.controller.request.SearchRequestDTO;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtQueryBoostsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniProtTermsConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotFacetConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.uniprot.UniProtDocument;
import org.uniprot.store.search.field.UniProtField;

import com.google.common.base.Strings;

@Service
@Import(UniProtQueryBoostsConfig.class)
public class UniProtEntryService {
    private static final String ACCESSION = "accession_id";
    private final StoreStreamer<UniProtEntry> storeStreamer;
    private final UniProtEntryQueryResultsConverter resultsConverter;
    private final QueryBoosts queryBoosts;
    private final UniProtTermsConfig uniProtTermsConfig;
    private UniprotQueryRepository repository;
    private UniprotFacetConfig uniprotFacetConfig;

    public UniProtEntryService(
            UniprotQueryRepository repository,
            UniprotFacetConfig uniprotFacetConfig,
            UniProtTermsConfig uniProtTermsConfig,
            QueryBoosts uniProtKBQueryBoosts,
            UniProtKBStoreClient entryStore,
            StoreStreamer<UniProtEntry> uniProtEntryStoreStreamer) {
        this.repository = repository;
        this.uniProtTermsConfig = uniProtTermsConfig;
        this.queryBoosts = uniProtKBQueryBoosts;
        this.uniprotFacetConfig = uniprotFacetConfig;
        this.storeStreamer = uniProtEntryStoreStreamer;
        this.resultsConverter = new UniProtEntryQueryResultsConverter(entryStore);
    }

    public QueryResult<UniProtEntry> search(SearchRequestDTO request) {
        SolrRequest solrRequest = createSolrRequest(request, true);

        QueryResult<UniProtDocument> results =
                repository.searchPage(solrRequest, request.getCursor(), request.getSize());

        return resultsConverter.convertQueryResult(
                results, FieldsParser.parseForFilters(request.getFields()));
    }

    public UniProtEntry getByAccession(String accession, String fields) {
        try {
            Map<String, List<String>> filters = FieldsParser.parseForFilters(fields);
            SolrRequest solrRequest =
                    SolrRequest.builder().query(ACCESSION + ":" + accession.toUpperCase()).build();
            Optional<UniProtDocument> optionalDoc = repository.getEntry(solrRequest);
            Optional<UniProtEntry> optionalUniProtEntry =
                    optionalDoc
                            .map(doc -> resultsConverter.convertDoc(doc, filters))
                            .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

            return optionalUniProtEntry.orElseThrow(
                    () -> new ResourceNotFoundException("{search.not.found}"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get accession for: [" + accession + "]";
            throw new ServiceException(message, e);
        }
    }

    public Stream<UniProtEntry> stream(SearchRequestDTO request, MediaType contentType) {
        SolrRequest solrRequest = createSolrRequest(request, false);
        boolean defaultFieldsOnly =
                FieldsParser.isDefaultFilters(FieldsParser.parseForFilters(request.getFields()));
        if (defaultFieldsOnly
                && (contentType.equals(APPLICATION_JSON)
                        || contentType.equals(TSV_MEDIA_TYPE)
                        || contentType.equals(XLS_MEDIA_TYPE))) {
            return storeStreamer.defaultFieldStream(solrRequest);
        } else {
            return storeStreamer.idsToStoreStream(solrRequest);
        }
    }

    public Stream<String> streamIds(SearchRequestDTO request) {
        SolrRequest solrRequest = createSolrRequest(request, false);
        return storeStreamer.idsStream(solrRequest);
    }

    private SolrRequest createSolrRequest(SearchRequestDTO request, boolean includeFacets) {
        SolrRequest.SolrRequestBuilder requestBuilder = SolrRequest.builder();
        requestBuilder.queryBoosts(queryBoosts);

        String requestedQuery = request.getQuery();

        if (needsToFilterIsoform(request)) {
            requestBuilder.filterQuery(UniProtField.Search.is_isoform.name() + ":" + false);
        }

        if (request.isShowMatchedFields()) {
            requestBuilder.termQuery(requestedQuery);
            uniProtTermsConfig.getFields().forEach(requestBuilder::termField);
        }

        requestBuilder.query(requestedQuery);
        requestBuilder.sort(getUniProtSort(request.getSort()));

        if (includeFacets && request.hasFacets()) {
            requestBuilder.facets(request.getFacetList());
            requestBuilder.facetConfig(uniprotFacetConfig);
        }

        return requestBuilder.build();
    }

    private Sort getUniProtSort(String sortStr) {
        if (Strings.isNullOrEmpty(sortStr)) {
            return UniProtSortUtil.createDefaultSort();
        } else {
            return UniProtSortUtil.createSort(sortStr);
        }
    }

    /**
     * This method verify if we need to add isoform filter query to remove isoform entries
     *
     * <p>if does not have id fields (we can not filter isoforms when querying for IDS) AND has
     * includeIsoform params in the request URL Then we analyze the includeIsoform request
     * parameter. IMPORTANT: Implementing this way, query search has precedence over isoform request
     * parameter
     *
     * @return true if we need to add isoform filter query
     */
    private boolean needsToFilterIsoform(SearchRequestDTO request) {
        boolean hasIdFieldTerms =
                SolrQueryUtil.hasFieldTerms(
                        request.getQuery(),
                        UniProtField.Search.accession_id.name(),
                        UniProtField.Search.mnemonic.name(),
                        UniProtField.Search.is_isoform.name());

        if (!hasIdFieldTerms) {
            return !request.isIncludeIsoform();
        } else {
            return false;
        }
    }
}
