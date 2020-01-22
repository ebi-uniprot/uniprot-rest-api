package org.uniprot.api.uniprotkb.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.uniprotkb.repository.search.impl.TaxonomyRepository;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;
import org.uniprot.store.search.domain2.UniProtSearchFields;

import java.util.function.Function;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
@Slf4j
@Service
public class TaxonomyServiceImpl extends BasicSearchService<TaxonomyDocument, TaxonomyEntry>
        implements TaxonomyService {
    @Autowired
    public TaxonomyServiceImpl(TaxonomyRepository taxRepo) {
        super(taxRepo, new TaxonomyEntryConverter());
    }

    @Cacheable("taxonomyCache")
    @Override
    public TaxonomyEntry findByUniqueId(String uniqueId) {
        return super.findByUniqueId(uniqueId);
    }

    @Override
    protected String getIdField() {
        return UniProtSearchFields.TAXONOMY.getField("id").getName();
    }

    @Override
    public TaxonomyEntry findById(long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    static class TaxonomyEntryConverter implements Function<TaxonomyDocument, TaxonomyEntry> {

        private final ObjectMapper objectMapper;

        public TaxonomyEntryConverter() {
            objectMapper = TaxonomyJsonConfig.getInstance().getFullObjectMapper();
        }

        @Override
        public TaxonomyEntry apply(TaxonomyDocument taxonomyDocument) {
            log.info("fetch taxonomy=" + taxonomyDocument.getTaxId());
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
