package org.uniprot.api.uniprotkb.service;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.util.Iterator;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Sort;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.uniprot.api.rest.search.AbstractSolrSortClause;

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
        Sort sort = uniProtSolrSortClause.getSort("accession asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(uniProtSolrSortClause.getSolrSortFieldName("accession"), order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateSingleMnemonicSortDescAlsoAddAccessionAsc() {
        Sort sort = uniProtSolrSortClause.getSort("mnemonic desc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(uniProtSolrSortClause.getSolrSortFieldName("mnemonic"), order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(uniProtSolrSortClause.getSolrSortFieldName("accession"), order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateCompositeAccessionSortAscAndGeneDesc() {
        Sort sort = uniProtSolrSortClause.getSort("accession desc,gene asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(uniProtSolrSortClause.getSolrSortFieldName("accession"), order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(uniProtSolrSortClause.getSolrSortFieldName("gene"), order.getProperty());
        assertEquals(Sort.Direction.ASC, order.getDirection());

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateCompositeMnemonicSortDescAlsoAddAccessionAsc() {
        Sort sort = uniProtSolrSortClause.getSort("organism_name asc,mass desc , accession asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(
                order.getProperty(), uniProtSolrSortClause.getSolrSortFieldName("organism_name"));
        assertEquals(Sort.Direction.ASC, order.getDirection());

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(uniProtSolrSortClause.getSolrSortFieldName("mass"), order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), uniProtSolrSortClause.getSolrSortFieldName("accession"));
        assertEquals(Sort.Direction.ASC, order.getDirection());

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void testCreateDefaultSortWithScore() {
        Sort defaultSort = uniProtSolrSortClause.getSort("");
        assertNotNull(defaultSort);

        Iterator<Sort.Order> sortIterator = defaultSort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(AbstractSolrSortClause.SCORE, order.getProperty());
        assertEquals(Sort.Direction.DESC, order.getDirection());

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(
                order.getProperty(),
                uniProtSolrSortClause.getSolrSortFieldName("annotation_score"));
        assertEquals(Sort.Direction.DESC, order.getDirection());

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), uniProtSolrSortClause.getSolrSortFieldName("accession"));
        assertEquals(Sort.Direction.ASC, order.getDirection());

        assertFalse(sortIterator.hasNext());
    }

    @Test
    void canGetSortFieldWhenExists() {
        assertThat(uniProtSolrSortClause.getSolrSortFieldName("accession"), is("accession_id"));
    }

    @Test
    void gettingSortFieldWhenDoesntExistCausesException() {
        assertThrows(Exception.class, () -> uniProtSolrSortClause.getSolrSortFieldName("XXXX"));
    }
}
