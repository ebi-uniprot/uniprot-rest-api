package org.uniprot.api.uniprotkb.service;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniprotkb.repository.search.impl.TaxonomyRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.field.TaxonomyField;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;

/**
 *
 * @author jluo
 * @date: 16 Oct 2019
 *
*/
@Slf4j
@Service
public class TaxonomyServiceImpl implements TaxonomyService {
  private final BasicSearchService<TaxonomyEntry, TaxonomyDocument> basicService;
  @Autowired	
  public TaxonomyServiceImpl(TaxonomyRepository taxRepo) {
	  this.basicService = new BasicSearchService<>(taxRepo, new TaxonomyEntryConverter());
  }
 	
  @Cacheable("taxonomys")
  public TaxonomyEntry findById(long taxId) {
    return basicService.getEntity(TaxonomyField.Search.id.name(), String.valueOf(taxId));
  }
  static class TaxonomyEntryConverter implements Function<TaxonomyDocument, TaxonomyEntry> {

	    private final ObjectMapper objectMapper;

	    public TaxonomyEntryConverter() {
	        objectMapper = TaxonomyJsonConfig.getInstance().getFullObjectMapper();
	    }

	    @Override
	    public TaxonomyEntry apply(TaxonomyDocument taxonomyDocument) {
	        try {
	            return objectMapper.readValue(
	                    taxonomyDocument.getTaxonomyObj().array(), TaxonomyEntry.class);
	        } catch (Exception e) {
	        	 log.info("Error converting solr binary to TaxonomyEntry: ", e);
	        }
	        return null;
	    }
  }
}

