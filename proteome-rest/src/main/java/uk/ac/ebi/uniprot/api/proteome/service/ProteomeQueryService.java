package uk.ac.ebi.uniprot.api.proteome.service;

import static uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType.LIST_MEDIA_TYPE;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;

import uk.ac.ebi.uniprot.api.common.exception.ResourceNotFoundException;
import uk.ac.ebi.uniprot.api.common.exception.ServiceException;
import uk.ac.ebi.uniprot.api.common.repository.search.QueryResult;
import uk.ac.ebi.uniprot.api.common.repository.search.SolrQueryBuilder;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeQueryRepository;
import uk.ac.ebi.uniprot.api.proteome.request.ProteomeRequest;
import uk.ac.ebi.uniprot.api.rest.output.context.MessageConverterContext;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;
import uk.ac.ebi.uniprot.search.field.ProteomeField;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
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
		this.proteomeConverter =new  ProteomeEntryConverter();
		this.solrSortClause = solrSortClause;
	}

	
	  public QueryResult<?> search(ProteomeRequest request, MessageConverterContext<ProteomeEntry> context) {
	        MediaType contentType = context.getContentType();
			SimpleQuery simpleQuery = createQuery(request);

			QueryResult<ProteomeDocument> results = repository.searchPage(simpleQuery, request.getCursor(),
					request.getSize());
	        if (request.hasFacets()) {
	            context.setFacets(results.getFacets());
	        }

	        if (contentType.equals(LIST_MEDIA_TYPE)) {
	            List<String> accList = results.getContent().stream().map(doc -> doc.upid).collect(Collectors.toList());
	            context.setEntityIds(results.getContent().stream().map(doc -> doc.upid));
	            return QueryResult.of(accList, results.getPage(), results.getFacets());
	        } else {
	        	List<ProteomeEntry> converted = results.getContent().stream().map(proteomeConverter).filter(val -> val != null)
	    				.collect(Collectors.toList());
	        	
	            QueryResult<ProteomeEntry> queryResult =QueryResult.of(converted, results.getPage(), results.getFacets());
	            context.setEntities(converted.stream());
	            return queryResult;
	        }
	    }

	
	public QueryResult<ProteomeEntry> search(ProteomeRequest request) {
		SimpleQuery simpleQuery = createQuery(request);

		QueryResult<ProteomeDocument> results = repository.searchPage(simpleQuery, request.getCursor(),
				request.getSize());
		List<ProteomeEntry> converted = results.getContent().stream().map(proteomeConverter).filter(val -> val != null)
				.collect(Collectors.toList());
		return QueryResult.of(converted, results.getPage(), results.getFacets());
	}

	public ProteomeEntry getByUPId(String upid) {
		SimpleQuery simpleQuery = new SimpleQuery(
				Criteria.where(ProteomeField.Search.upid.name()).is(upid.toUpperCase()));
		try {
		Optional<ProteomeDocument> optionalDoc = repository.getEntry(simpleQuery);
		if(optionalDoc.isPresent()) {
			ProteomeEntry entry = proteomeConverter.apply(optionalDoc.get());
			if(entry ==null) {
				 String message = "Could not convert Proteome entry from document for: [" + upid + "]";
				 throw new ServiceException(message);
			}
			else
				return entry;
		}else {
			throw new ResourceNotFoundException("{search.not.found}");
		}
		
		}catch (ResourceNotFoundException e) {
			throw e;
		} catch (Exception e) {
            String message = "Could not get upid for: [" + upid + "]";
            throw new ServiceException(message, e);
        }
	}

	private SimpleQuery createQuery(ProteomeRequest request) {
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
