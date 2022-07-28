package org.uniprot.api.common.repository.stream.store.uniprotkb;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrQuery;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.rest.request.StreamRequest;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.service.BasicSearchService;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.core.json.parser.taxonomy.TaxonomyJsonConfig;
import org.uniprot.core.taxonomy.TaxonomyEntry;
import org.uniprot.core.taxonomy.TaxonomyLineage;
import org.uniprot.store.config.UniProtDataType;
import org.uniprot.store.config.searchfield.common.SearchFieldConfig;
import org.uniprot.store.config.searchfield.factory.SearchFieldConfigFactory;
import org.uniprot.store.config.searchfield.model.SearchFieldItem;
import org.uniprot.store.search.document.taxonomy.TaxonomyDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * @author jluo
 * @date: 16 Oct 2019
 */
@Slf4j
@Service
public class TaxonomyLineageServiceImpl extends BasicSearchService<TaxonomyDocument, TaxonomyEntry>
        implements TaxonomyLineageService {
    private final SearchFieldConfig searchFieldConfig;

    @Autowired
    public TaxonomyLineageServiceImpl(TaxonomyLineageRepository taxRepo) {
        super(
                taxRepo,
                new TaxonomyEntryConverter(),
                new TaxonomySortClause(),
                getQueryBoosts(),
                null);
        this.searchFieldConfig =
                SearchFieldConfigFactory.getSearchFieldConfig(UniProtDataType.TAXONOMY);
    }

    @Cacheable("taxonomyCache")
    @Override
    public TaxonomyEntry findByUniqueId(String uniqueId) {
        return super.findByUniqueId(uniqueId);
    }

    @Override
    protected SearchFieldItem getIdField() {
        return searchFieldConfig.getSearchFieldItemByName("id");
    }

    @Override
    protected UniProtQueryProcessorConfig getQueryProcessorConfig() {
        return UniProtQueryProcessorConfig.builder().build();
    }

    @Override
    public TaxonomyEntry findById(long taxId) {
        return findByUniqueId(String.valueOf(taxId));
    }

    @Override
    public Map<Long, List<TaxonomyLineage>> findByIds(Set<Long> taxIds) {
        String idFieldName = getIdField().getFieldName() + ":";
        String query =
                idFieldName
                        + taxIds.stream()
                                .map(String::valueOf)
                                .collect(Collectors.joining(" OR " + idFieldName));
        StreamRequest request = new TaxonomyLineageStreamRequest(query);
        return this.stream(request)
                .collect(Collectors.toMap(TaxonomyEntry::getTaxonId, TaxonomyEntry::getLineages));
    }

    private static SolrQueryConfig getQueryBoosts() {
        return SolrQueryConfig.builder().queryFields("id").build();
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
                        taxonomyDocument.getTaxonomyObj(), TaxonomyEntry.class);
            } catch (Exception e) {
                log.info("Error converting solr binary to TaxonomyEntry: ", e);
            }
            return null;
        }
    }

    static class TaxonomyLineageStreamRequest implements StreamRequest {

        private final String query;

        TaxonomyLineageStreamRequest(String query) {
            this.query = query;
        }

        @Override
        public String getQuery() {
            return query;
        }

        @Override
        public String getFields() {
            return null;
        }

        @Override
        public String getSort() {
            return null;
        }

        @Override
        public String getDownload() {
            return null;
        }
    }

    static class TaxonomySortClause extends AbstractSolrSortClause {
        private static final String DOC_ID = "id";

        TaxonomySortClause() {
            init();
        }

        public void init() {
            addDefaultFieldOrderPair(DOC_ID, SolrQuery.ORDER.asc);
        }

        @Override
        protected String getSolrDocumentIdFieldName() {
            return DOC_ID;
        }

        @Override
        protected UniProtDataType getUniProtDataType() {
            return UniProtDataType.TAXONOMY;
        }
    }
}
