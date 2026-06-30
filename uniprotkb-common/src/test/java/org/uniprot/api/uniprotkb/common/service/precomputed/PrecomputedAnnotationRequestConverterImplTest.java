package org.uniprot.api.uniprotkb.common.service.precomputed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.query.processor.UniProtQueryProcessorConfig;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;

class PrecomputedAnnotationRequestConverterImplTest {

    @Test
    void createsSolrQueryFromResolvedTaxonomyId() {
        PrecomputedAnnotationRequestConverterImpl converter = converter();
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setTaxonomyId("9606");
        request.setSize(10);
        request.setSort("accession desc");

        SolrRequest solrRequest = converter.createSearchSolrRequest(request);

        assertEquals("taxonomy_id:9606", solrRequest.getQuery());
        assertEquals("accession uniparc taxonomy_id", solrRequest.getQueryField());
        assertEquals(10, solrRequest.getRows());
        assertEquals(10, solrRequest.getTotalRows());
        assertEquals("accession", solrRequest.getSorts().get(0).getItem());
        assertEquals(SolrQuery.ORDER.desc, solrRequest.getSorts().get(0).getOrder());
    }

    @Test
    void createsStreamSolrQueryFromResolvedTaxonomyId() {
        PrecomputedAnnotationRequestConverterImpl converter = converter();
        PrecomputedAnnotationStreamByProteomeRequest request = new PrecomputedAnnotationStreamByProteomeRequest();
        request.setTaxonomyId("9606");
        request.setSort("accession desc");

        SolrRequest solrRequest = converter.createStreamSolrRequest(request);

        assertEquals("taxonomy_id:9606", solrRequest.getQuery());
        assertEquals("accession uniparc taxonomy_id", solrRequest.getQueryField());
        assertEquals(100, solrRequest.getRows());
        assertEquals(Integer.MAX_VALUE, solrRequest.getTotalRows());
        assertEquals("accession", solrRequest.getSorts().get(0).getItem());
        assertEquals(SolrQuery.ORDER.desc, solrRequest.getSorts().get(0).getOrder());
    }

    @Test
    void ignoresOtherFieldsAndUsesResolvedTaxonomyId() {
        PrecomputedAnnotationRequestConverterImpl converter = converter();
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setAccession("p12345");
        request.setUniparc("upi0000001866");
        request.setTaxonomyId("61156");

        SolrRequest solrRequest = converter.createSearchSolrRequest(request);

        assertEquals("taxonomy_id:61156", solrRequest.getQuery());
    }

    private static PrecomputedAnnotationRequestConverterImpl converter() {
        PrecomputedAnnotationSortClause sortClause = new PrecomputedAnnotationSortClause();
        sortClause.init();
        RequestConverterConfigProperties configProperties = new RequestConverterConfigProperties();
        configProperties.setDefaultRestPageSize(25);
        return new PrecomputedAnnotationRequestConverterImpl(
                SolrQueryConfig.builder().queryFields("accession,uniparc,taxonomy_id").build(),
                sortClause,
                UniProtQueryProcessorConfig.builder().build(),
                configProperties);
    }
}
