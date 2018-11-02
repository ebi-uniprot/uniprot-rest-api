package uk.ac.ebi.uniprot.uuw.advanced.search.service;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Sort;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;

import java.util.Iterator;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

/**
 *  Unit Test class to validate UniProtSortUtil class behaviour
 *
 * @author lgonzales
 */
class UniProtSortUtilTest {

    @Test
    void testCreateSingleAccessionSortAsc() {
        Optional<Sort> singleSort = UniProtSortUtil.createSort("accession asc");

        assertEquals(singleSort.isPresent(),true);
        Sort sort = singleSort.get();
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
        Optional<Sort> singleSort = UniProtSortUtil.createSort("mnemonic desc");

        assertEquals(singleSort.isPresent(),true);
        Sort sort = singleSort.get();
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
        Optional<Sort> singleSort = UniProtSortUtil.createSort("accession desc,gene asc");

        assertEquals(singleSort.isPresent(),true);
        Sort sort = singleSort.get();
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
        Optional<Sort> singleSort = UniProtSortUtil.createSort("organism asc,mass desc , accession asc");

        assertEquals(singleSort.isPresent(),true);
        Sort sort = singleSort.get();
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
    void testCreateDefaultSort() {
        Sort defaultSort = UniProtSortUtil.createDefaultSort();
        assertNotNull(defaultSort);

        Iterator<Sort.Order> sortIterator = defaultSort.iterator();
        assertNotNull(sortIterator);

        assertEquals(sortIterator.hasNext(),true);
        Sort.Order order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.annotation_score.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.DESC);

        assertEquals(sortIterator.hasNext(),true);
        order = sortIterator.next();
        assertEquals(order.getProperty(), UniProtField.Sort.accession.getSolrFieldName());
        assertEquals(order.getDirection(), Sort.Direction.ASC);

        assertEquals(sortIterator.hasNext(),false);
    }

}