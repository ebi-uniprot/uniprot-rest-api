package uk.ac.ebi.uniprot.api.proteome.service;

import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeQueryRepository;
import uk.ac.ebi.uniprot.api.proteome.request.ProteomeRequest;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;

/**
 * @author jluo
 * @date: 26 Apr 2019
 */
@Service
public class ProteomeQueryService {
    private ProteomeQueryRepository repository;
    private ProteomeFacetConfig facetConfig;

    private final ProteomeEntryConverter proteomeConverter;
    private final ProteomeSortClause solrSortClause;

    public ProteomeQueryService(ProteomeQueryRepository repository, ProteomeFacetConfig facetConfig,
                                ProteomeSortClause solrSortClause) {
        this.repository = repository;
        this.facetConfig = facetConfig;
        this.proteomeConverter = new ProteomeEntryConverter();
        this.solrSortClause = solrSortClause;
    }

    public QueryResult<?> search(ProteomeRequest request, MessageConverterContext<ProteomeEntry> context) {
        MediaType contentType = context.getContentType();
        SolrRequest solrRequest = createQuery(request);

        QueryResult<ProteomeDocument> results = repository
                .searchPage(solrRequest, request.getCursor(), request.getSize());
        if (request.hasFacets()) {
            context.setFacets(results.getFacets());
        }

        if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> accList = results.getContent().stream().map(doc -> doc.upid).collect(Collectors.toList());
            context.setEntityIds(results.getContent().stream().map(doc -> doc.upid));
            return QueryResult.of(accList, results.getPage(), results.getFacets());
        } else {
            List<ProteomeEntry> converted = results.getContent().stream().map(proteomeConverter)
                    .filter(Utils::nonNull)
                    .collect(Collectors.toList());

            QueryResult<ProteomeEntry> queryResult = QueryResult.of(converted, results.getPage(), results.getFacets());
            context.setEntities(converted.stream());
            return queryResult;
        }
    }


    public QueryResult<ProteomeEntry> search(ProteomeRequest request) {
        SolrRequest solrRequest = createQuery(request);

        QueryResult<ProteomeDocument> results = repository.searchPage(solrRequest, request.getCursor(),
                                                                      request.getSize());
        List<ProteomeEntry> converted = results.getContent().stream().map(proteomeConverter).filter(Utils::nonNull)
                .collect(Collectors.toList());
        return QueryResult.of(converted, results.getPage(), results.getFacets());
    }

    public ProteomeEntry getByUPId(String upid) {
        SolrRequest solrRequest = SolrRequest.builder()
                .query(ProteomeField.Search.upid.name() + ":" + upid.toUpperCase())
                .build();

        try {
            return repository.getEntry(solrRequest)
                    .map(proteomeConverter)
                    .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));
        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not get upid for: [" + upid + "]";
            throw new ServiceException(message, e);
        }
    }

    private SolrRequest createQuery(ProteomeRequest request) {
        SolrRequest.SolrRequestBuilder builder = SolrRequest.builder();
        String requestedQuery = request.getQuery();
        builder.query(requestedQuery);
        builder.addSort(solrSortClause.getSort(request.getSort(), false));

        if (request.hasFacets()) {
            builder.facets(request.getFacetList());
            builder.facetConfig(facetConfig);
        }
        return builder.build();
    }
}
