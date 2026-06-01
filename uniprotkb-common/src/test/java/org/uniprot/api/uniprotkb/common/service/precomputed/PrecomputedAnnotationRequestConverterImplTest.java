package org.uniprot.api.uniprotkb.common.service.precomputed;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrQueryConfig;
import org.uniprot.api.common.repository.search.SolrRequest;
import org.uniprot.api.rest.service.request.RequestConverterConfigProperties;

class PrecomputedAnnotationRequestConverterImplTest {

    @Test
    void createsSolrQueryFromAccessionField() {
        PrecomputedAnnotationRequestConverterImpl converter = converter();
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setAccession("p12345");
        request.setSize(10);
        request.setSort("accession desc");

        SolrRequest solrRequest = converter.createSearchSolrRequest(request);

        assertEquals("accession:P12345", solrRequest.getQuery());
        assertEquals("accession uniparc taxonomy_id", solrRequest.getQueryField());
        assertEquals(10, solrRequest.getRows());
        assertEquals(10, solrRequest.getTotalRows());
        assertEquals("accession", solrRequest.getSorts().get(0).getItem());
        assertEquals(SolrQuery.ORDER.desc, solrRequest.getSorts().get(0).getOrder());
    }

    @Test
    void createsSolrQueryFromUniparcAndTaxonomyIdFields() {
        PrecomputedAnnotationRequestConverterImpl converter = converter();
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();
        request.setUniparc("upi0000001866");
        request.setTaxonomyId("61156");

        SolrRequest solrRequest = converter.createSearchSolrRequest(request);

        assertEquals("uniparc:UPI0000001866 AND taxonomy_id:61156", solrRequest.getQuery());
    }

    @Test
    void createsSearchAllQueryWhenNoFieldsAreProvided() {
        PrecomputedAnnotationRequestConverterImpl converter = converter();
        PrecomputedAnnotationSearchByProteomeRequest request =
                new PrecomputedAnnotationSearchByProteomeRequest();

        SolrRequest solrRequest = converter.createSearchSolrRequest(request);

        assertEquals("*:*", solrRequest.getQuery());
        assertEquals(25, solrRequest.getRows());
        assertEquals(25, solrRequest.getTotalRows());
        assertEquals("accession", solrRequest.getSorts().get(0).getItem());
        assertEquals(SolrQuery.ORDER.asc, solrRequest.getSorts().get(0).getOrder());
    }

    private static PrecomputedAnnotationRequestConverterImpl converter() {
        PrecomputedAnnotationSortClause sortClause = new PrecomputedAnnotationSortClause();
        sortClause.init();
        RequestConverterConfigProperties configProperties = new RequestConverterConfigProperties();
        configProperties.setDefaultRestPageSize(25);
        return new PrecomputedAnnotationRequestConverterImpl(
                SolrQueryConfig.builder().queryFields("accession,uniparc,taxonomy_id").build(),
                sortClause,
                null,
                configProperties);
    }
}
