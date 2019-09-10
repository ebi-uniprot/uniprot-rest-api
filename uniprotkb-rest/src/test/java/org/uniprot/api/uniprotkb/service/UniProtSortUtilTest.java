package org.uniprot.api.uniprotkb.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import org.uniprot.store.search.field.UniProtField;

import java.util.Iterator;

import static org.junit.jupiter.api.Assertions.*;

/**
 *  Unit Test class to validate UniProtSortUtil class behaviour
 *
 * @author lgonzales
 */
class UniProtSortUtilTest {

    @Test
    void testCreateSingleAccessionSortAsc() {
        Sort sort = UniProtSortUtil.createSort("accession asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertEquals(sortIterator.hasNext(),true);
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertEquals(sortIterator.hasNext(),false);
    }

    @Test
    void testCreateSingleMnemonicSortDescAlsoAddAccessionAsc() {
        Sort sort = UniProtSortUtil.createSort("mnemonic desc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertEquals(sortIterator.hasNext(),true);
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.mnemonic.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);


        assertEquals(sortIterator.hasNext(),true);
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertEquals(sortIterator.hasNext(),false);
    }

    @Test
    void testCreateCompositeAccessionSortAscAndGeneDesc() {
        Sort sort = UniProtSortUtil.createSort("accession desc,gene asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertEquals(sortIterator.hasNext(),true);
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertEquals(sortIterator.hasNext(),true);
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.gene.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertEquals(sortIterator.hasNext(),false);
    }

    @Test
    void testCreateCompositeMnemonicSortDescAlsoAddAccessionAsc() {
        Sort sort = UniProtSortUtil.createSort("organism asc,mass desc , accession asc");
        assertNotNull(sort);

        Iterator<Sort.Order> sortIterator = sort.iterator();
        assertNotNull(sortIterator);

        assertEquals(sortIterator.hasNext(),true);
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.organism.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);


        assertEquals(sortIterator.hasNext(),true);
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.mass.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertEquals(sortIterator.hasNext(),true);
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertEquals(sortIterator.hasNext(),false);
    }

    @Test
    void testCreateDefaultSortWithScore() {
        Sort defaultSort = UniProtSortUtil.createDefaultSort();
        assertNotNull(defaultSort);

        Iterator<Sort.Order> sortIterator = defaultSort.iterator();
        assertNotNull(sortIterator);

        assertTrue(sortIterator.hasNext());
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), "score");
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.annotation_score.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertTrue(sortIterator.hasNext());
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertFalse(sortIterator.hasNext());
    }
}