package uk.ac.ebi.uniprot.api.proteome.service;

import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricQueryRepository;
import uk.ac.ebi.uniprot.api.proteome.request.GeneCentricRequest;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.search.document.proteome.GeneCentricDocument;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;

/**
 *
 * @author jluo
 * @date: 30 Apr 2019
 *
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
			SimpleQuery simpleQuery = createQuery(request);

			QueryResult<GeneCentricDocument> results = repository.searchPage(simpleQuery, request.getCursor(),
					request.getSize());
	        if (request.hasFacets()) {
	            context.setFacets(results.getFacets());
	        }

	        if (contentType.equals(LIST_MEDIA_TYPE)) {
	            List<String> accList = results.getContent().stream().map(doc -> doc.getDocumentId()).collect(Collectors.toList());
	            context.setEntityIds(results.getContent().stream().map(doc -> doc.getDocumentId()));
	            return QueryResult.of(accList, results.getPage(), results.getFacets());
	        } else {
	        	List<CanonicalProtein> converted = results.getContent().stream().map(converter).filter(val -> val != null)
	    				.collect(Collectors.toList());
	        	
	            QueryResult<CanonicalProtein> queryResult =QueryResult.of(converted, results.getPage(), results.getFacets());
	            context.setEntities(converted.stream());
	            return queryResult;
	        }
	    }

	public CanonicalProtein getByAccession(String accession) {
		SimpleQuery simpleQuery = new SimpleQuery(Criteria.where(GeneCentricField.Search.accession.name()).is(accession));
		try {
			Optional<GeneCentricDocument> optionalDoc = repository.getEntry(simpleQuery);
			if (optionalDoc.isPresent()) {

				CanonicalProtein entry = converter.apply(optionalDoc.get());
				return entry;

			} else {
				throw new ResourceNotFoundException("{search.not.found}");
			}

		} catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
			String message = "Could not fetch entry";
			throw new ServiceException(message, e);
		}
	}

	private SimpleQuery createQuery(GeneCentricRequest request) {
		SolrQueryBuilder builder = new SolrQueryBuilder();
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
