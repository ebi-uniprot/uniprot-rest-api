package org.uniprot.api.uniprotkb.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.util.List;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.search.AbstractSolrSortClause;
import org.uniprot.api.rest.search.SortUtils;

/**
 * Unit Test class to validate UniProtSortUtil class behaviour
 *
 * @author lgonzales
 */
@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {UniProtSolrSortClause.class})
class UniProtSolrSortClauseTest {

    @Autowired private UniProtSolrSortClause uniProtSolrSortClause;

    @Test
    void testCreateSingleAccessionSortAsc() {
        List<SolrQuery.SortClause> sorts = uniProtSolrSortClause.getSort("accession asc");

        assertThat(sorts, contains(SolrQuery.SortClause.asc(getSolrSortFieldName("accession"))));
    }

    @Test
    void testCreateSingleMnemonicSortDescAlsoAddAccessionAsc() {
        List<SolrQuery.SortClause> sorts = uniProtSolrSortClause.getSort("id desc");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.desc(getSolrSortFieldName("id")),
                        SolrQuery.SortClause.asc(getSolrSortFieldName("accession"))));
    }

    @Test
    void testCreateCompositeAccessionSortAscAndGeneDesc() {
        List<SolrQuery.SortClause> sorts = uniProtSolrSortClause.getSort("accession desc,gene asc");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.desc(getSolrSortFieldName("accession")),
                        SolrQuery.SortClause.asc(getSolrSortFieldName("gene"))));
    }

    @Test
    void testCreateCompositeMnemonicSortDescAlsoAddAccessionAsc() {
        List<SolrQuery.SortClause> sorts =
                uniProtSolrSortClause.getSort("organism_name asc,mass desc , accession asc");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.asc(getSolrSortFieldName("organism_name")),
                        SolrQuery.SortClause.desc(getSolrSortFieldName("mass")),
                        SolrQuery.SortClause.asc(getSolrSortFieldName("accession"))));
    }

    @Test
    void testCreateDefaultSortWithScore() {
        List<SolrQuery.SortClause> sorts = uniProtSolrSortClause.getSort("");

        assertThat(
                sorts,
                contains(
                        SolrQuery.SortClause.desc(AbstractSolrSortClause.SCORE),
                        SolrQuery.SortClause.desc(getSolrSortFieldName("annotation_score")),
                        SolrQuery.SortClause.asc(getSolrSortFieldName("accession"))));
    }

    @Test
    void canGetSortFieldWhenExists() {
        assertThat(getSolrSortFieldName("accession"), is("accession_id"));
    }

    @Test
    void gettingSortFieldWhenDoesntExistCausesException() {
        assertThrows(Exception.class, () -> getSolrSortFieldName("XXXX"));
    }

    private String getSolrSortFieldName(String fieldName) {
        return SortUtils.getSolrSortFieldName(
                uniProtSolrSortClause.getUniProtDataType(), fieldName);
    }
}
