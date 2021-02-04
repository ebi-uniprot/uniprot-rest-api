package org.uniprot.api.support.data.configure.service;

import static org.junit.jupiter.api.Assertions.*;

import java.util.List;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.uniprot.api.support.data.configure.response.SolrJsonQuery;

/** @author lgonzales */
class SolrQueryConverterTest {

    private static final Logger logger = LoggerFactory.getLogger(SolrQueryConverterTest.class);

    @Test
    void convertAllDocumentsQuery() {
        Query allDocumentsQuery = getQueryFromString("*:*");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("matchAllDocsQuery", jsonQuery.getType());
        assertEquals("*", jsonQuery.getField());
        assertEquals("*", jsonQuery.getValue());
    }

    @Test
    void convertWildcardQuery() {
        Query allDocumentsQuery = getQueryFromString("cc_active_sites:*");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("wildcardQuery", jsonQuery.getType());
        assertEquals("cc_active_sites", jsonQuery.getField());
        assertEquals("*", jsonQuery.getValue());
    }

    @Test
    void convertMiddleWildcardQuery() {
        Query allDocumentsQuery = getQueryFromString("ec:4.7.*.1");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("wildcardQuery", jsonQuery.getType());
        assertEquals("ec", jsonQuery.getField());
        assertEquals("4.7.*.1", jsonQuery.getValue());
    }

    @Test
    void convertDefaultFieldQuery() {
        Query allDocumentsQuery = getQueryFromString("foo");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("", jsonQuery.getField());
        assertEquals("termQuery", jsonQuery.getType());
        assertEquals("foo", jsonQuery.getValue());
    }

    @Test
    void convertNotDefaultFieldQuery() {
        Query allDocumentsQuery = getQueryFromString("NOT foo");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertNotNull(jsonQuery.getBooleanQuery());
        List<SolrJsonQuery> solrJsonQueries = jsonQuery.getBooleanQuery();
        assertEquals(1, solrJsonQueries.size());

        SolrJsonQuery booleanQueryItem = solrJsonQueries.get(0);
        assertNotNull(booleanQueryItem);
        assertEquals("NOT", booleanQueryItem.getQueryOperator());
        assertEquals("termQuery", booleanQueryItem.getType());
        assertEquals("foo", booleanQueryItem.getValue());
        assertEquals("", booleanQueryItem.getField());
    }

    @Test
    void convertSimpleTermQuery() {
        Query allDocumentsQuery = getQueryFromString("(id:blah)");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("termQuery", jsonQuery.getType());
        assertEquals("blah", jsonQuery.getValue());
        assertEquals("id", jsonQuery.getField());
    }

    @Test
    void convertNotTermQuery() {
        Query allDocumentsQuery = getQueryFromString("NOT (id:blah)");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertNotNull(jsonQuery.getBooleanQuery());
        assertEquals("booleanQuery", jsonQuery.getType());

        List<SolrJsonQuery> solrJsonQueries = jsonQuery.getBooleanQuery();
        assertEquals(1, solrJsonQueries.size());

        SolrJsonQuery booleanQueryItem = solrJsonQueries.get(0);
        assertNotNull(booleanQueryItem);
        assertEquals("termQuery", booleanQueryItem.getType());
        assertEquals("NOT", booleanQueryItem.getQueryOperator());
        assertEquals("id", booleanQueryItem.getField());
        assertEquals("blah", booleanQueryItem.getValue());
    }

    @Test
    void convertPhraseTermQuery() {
        Query allDocumentsQuery = getQueryFromString("organism_name:\"homo sapiens\"");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("phraseQuery", jsonQuery.getType());
        assertEquals("organism_name", jsonQuery.getField());
        assertEquals("homo sapiens", jsonQuery.getValue());
    }

    @Test
    void convertRangeTermQuery() {
        Query allDocumentsQuery = getQueryFromString("(created:[2018-03-04 TO 2018-03-08])");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("rangeQuery", jsonQuery.getType());
        assertEquals("created", jsonQuery.getField());
        assertEquals("2018-03-04", jsonQuery.getFrom());
        assertTrue(jsonQuery.getFromInclude());
        assertEquals("2018-03-08", jsonQuery.getTo());
        assertTrue(jsonQuery.getToInclude());
    }

    @Test
    void convertPrefixQuery() {
        Query allDocumentsQuery = getQueryFromString("ec:4.7.*");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("prefixQuery", jsonQuery.getType());
        assertEquals("ec", jsonQuery.getField());
        assertEquals("4.7.*", jsonQuery.getValue());
    }

    @Test
    void convertAndBooleanQuery() {
        Query allDocumentsQuery =
                getQueryFromString("((length:[1 TO 10]) AND (organism_name:\"homo sapiens\"))");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("booleanQuery", jsonQuery.getType());

        assertNotNull(jsonQuery.getBooleanQuery());
        List<SolrJsonQuery> solrJsonQueries = jsonQuery.getBooleanQuery();
        assertEquals(2, solrJsonQueries.size());

        SolrJsonQuery booleanQueryItem = solrJsonQueries.get(0);
        assertNotNull(booleanQueryItem);
        assertEquals("AND", booleanQueryItem.getQueryOperator());
        assertEquals("rangeQuery", booleanQueryItem.getType());
        assertEquals("length", booleanQueryItem.getField());
        assertTrue(booleanQueryItem.getFromInclude());
        assertEquals("1", booleanQueryItem.getFrom());
        assertTrue(booleanQueryItem.getToInclude());
        assertEquals("10", booleanQueryItem.getTo());

        booleanQueryItem = solrJsonQueries.get(1);
        assertNotNull(booleanQueryItem);
        assertEquals("AND", booleanQueryItem.getQueryOperator());
        assertEquals("phraseQuery", booleanQueryItem.getType());
        assertEquals("organism_name", booleanQueryItem.getField());
        assertEquals("homo sapiens", booleanQueryItem.getValue());
    }

    @Test
    void convertOrBooleanQuery() {
        Query allDocumentsQuery = getQueryFromString("(id:blah) OR (id:foo)");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("booleanQuery", jsonQuery.getType());

        assertNotNull(jsonQuery.getBooleanQuery());
        List<SolrJsonQuery> solrJsonQueries = jsonQuery.getBooleanQuery();
        assertEquals(2, solrJsonQueries.size());

        SolrJsonQuery booleanQueryItem = solrJsonQueries.get(0);
        assertNotNull(booleanQueryItem);
        assertEquals("OR", booleanQueryItem.getQueryOperator());
        assertEquals("termQuery", booleanQueryItem.getType());
        assertEquals("blah", booleanQueryItem.getValue());
        assertEquals("id", booleanQueryItem.getField());

        booleanQueryItem = solrJsonQueries.get(1);
        assertNotNull(booleanQueryItem);
        assertEquals("termQuery", booleanQueryItem.getType());
        assertEquals("id", booleanQueryItem.getField());
        assertEquals("foo", booleanQueryItem.getValue());
        assertEquals("OR", booleanQueryItem.getQueryOperator());
    }

    @Test
    void convertNotBooleanQuery() {
        Query allDocumentsQuery = getQueryFromString("(id:blah) NOT (id:foo)");
        SolrJsonQuery jsonQuery = SolrQueryConverter.convert(allDocumentsQuery);

        assertNotNull(jsonQuery);
        assertEquals("booleanQuery", jsonQuery.getType());

        assertNotNull(jsonQuery.getBooleanQuery());
        List<SolrJsonQuery> solrJsonQueries = jsonQuery.getBooleanQuery();
        assertEquals(2, solrJsonQueries.size());

        SolrJsonQuery booleanQueryItem = solrJsonQueries.get(0);
        assertNotNull(booleanQueryItem);
        assertEquals("termQuery", booleanQueryItem.getType());
        assertEquals("id", booleanQueryItem.getField());
        assertEquals("blah", booleanQueryItem.getValue());
        assertEquals("OR", booleanQueryItem.getQueryOperator());

        booleanQueryItem = solrJsonQueries.get(1);
        assertNotNull(booleanQueryItem);
        assertEquals("termQuery", booleanQueryItem.getType());
        assertEquals("id", booleanQueryItem.getField());
        assertEquals("NOT", booleanQueryItem.getQueryOperator());
        assertEquals("foo", booleanQueryItem.getValue());
    }

    private static Query getQueryFromString(String query) {
        QueryParser qp = new QueryParser("", new WhitespaceAnalyzer());
        qp.setAllowLeadingWildcard(true);
        try {
            return qp.parse(query);
        } catch (ParseException e) {
            logger.error("Error converting query from String: ", e);
            return null;
        }
    }
}
