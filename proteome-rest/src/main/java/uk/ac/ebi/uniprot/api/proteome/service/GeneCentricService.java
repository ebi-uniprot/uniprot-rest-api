package uk.ac.ebi.uniprot.api.proteome.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrRequest;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import uk.ac.ebi.uniprot.api.proteome.request.GeneCentricRequest;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.common.Utils;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.search.document.proteome.GeneCentricDocument;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;

import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;

/**
 * @author jluo
 * @date: 30 Apr 2019
 */
@Service
public class GeneCentricService {
    private GeneCentricQueryRepository repository;
    private final GeneCentricEntryConverter converter;
    private final GeneCentricSortClause solrSortClause;
    private GeneCentricFacetConfig facetConfig;

    @Autowired
    public GeneCentricService(GeneCentricQueryRepository repository,
                              GeneCentricFacetConfig facetConfig,
                              GeneCentricSortClause solrSortClause) {
        this.repository = repository;
        this.converter = new GeneCentricEntryConverter();
        this.facetConfig = facetConfig;
        this.solrSortClause = solrSortClause;
    }

    public QueryResult<?> search(GeneCentricRequest request, MessageConverterContext<CanonicalProtein> context) {
        MediaType contentType = context.getContentType();
        SolrRequest solrRequest = createQuery(request);

        QueryResult<GeneCentricDocument> results = repository
                .searchPage(solrRequest, request.getCursor(), request.getSize());
        if (request.hasFacets()) {
            context.setFacets(results.getFacets());
        }

        if (contentType.equals(LIST_MEDIA_TYPE)) {
            List<String> accList = results.getContent().stream().map(GeneCentricDocument::getDocumentId)
                    .collect(Collectors.toList());
            context.setEntityIds(results.getContent().stream().map(GeneCentricDocument::getDocumentId));
            return QueryResult.of(accList, results.getPage(), results.getFacets());
        } else {
            List<CanonicalProtein> converted = results.getContent().stream().map(converter).filter(Utils::nonNull)
                    .collect(Collectors.toList());

            QueryResult<CanonicalProtein> queryResult = QueryResult
                    .of(converted, results.getPage(), results.getFacets());
            context.setEntities(converted.stream());
            return queryResult;
        }
    }

    public CanonicalProtein getByAccession(String accession) {
        SolrRequest solrRequest = SolrRequest.builder()
                .query(GeneCentricField.Search.accession.name() + ":" + accession)
                .build();
        try {
            return repository
                    .getEntry(solrRequest)
                    .map(converter)
                    .orElseThrow(() -> new ResourceNotFoundException("{search.not.found}"));

        } catch (ResourceNotFoundException e) {
            throw e;
        } catch (Exception e) {
            String message = "Could not fetch entry";
            throw new ServiceException(message, e);
        }
    }

    private SolrRequest createQuery(GeneCentricRequest request) {
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
