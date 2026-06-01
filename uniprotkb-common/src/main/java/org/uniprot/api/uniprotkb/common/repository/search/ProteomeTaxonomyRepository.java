package org.uniprot.api.uniprotkb.common.repository.search;

import java.util.Locale;
import java.util.Optional;

import org.apache.solr.client.solrj.SolrClient;
import org.springframework.stereotype.Repository;
import org.uniprot.api.common.repository.search.SolrQueryRepository;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.common.repository.search.SolrRequestConverter;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.store.search.SolrCollection;
import org.uniprot.store.search.SolrQueryUtil;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import com.fasterxml.jackson.databind.ObjectMapper;

@Repository
public class ProteomeTaxonomyRepository extends SolrQueryRepository<ProteomeDocument> {
    private static final String UPID_FIELD = "upid";

    private final ObjectMapper objectMapper;

    public ProteomeTaxonomyRepository(
            SolrClient solrClient, SolrRequestConverter requestConverter) {
        super(solrClient, SolrCollection.proteome, ProteomeDocument.class, null, requestConverter);
        this.objectMapper = ProteomeJsonConfig.getInstance().getFullObjectMapper();
    }

    public Optional<String> findTaxonomyIdByUpId(String upId) {
        String escapedUpId =
                SolrQueryUtil.escapeSpecialCharacters(upId.strip().toUpperCase(Locale.ROOT));
        SolrRequest request =
                SolrRequest.builder().query(UPID_FIELD + ":" + escapedUpId).rows(1).build();

        return getEntry(request).flatMap(this::extractTaxonomyId);
    }

    Optional<String> extractTaxonomyId(ProteomeDocument document) {
        try {
            ProteomeEntry entry =
                    objectMapper.readValue(document.proteomeStored, ProteomeEntry.class);
            if (entry.getTaxonomy() == null) {
                return Optional.empty();
            }
            Long taxonId = entry.getTaxonomy().getTaxonId();
            if (taxonId == null || taxonId == 0) {
                return Optional.empty();
            }
            return Optional.of(String.valueOf(taxonId));
        } catch (Exception e) {
            throw new IllegalStateException(
                    "Unable to read proteome taxonomy from Solr document", e);
        }
    }
}
