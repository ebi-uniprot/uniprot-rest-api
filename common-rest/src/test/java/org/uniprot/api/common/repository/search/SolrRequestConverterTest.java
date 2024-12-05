package org.uniprot.api.common.repository.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.io.IOException;
import java.util.*;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.InvalidRequestException;

import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.extern.slf4j.Slf4j;
import voldemort.utils.StringOutputStream;

/**
 * Created 17/06/19
 *
 * @author Edd
 */
@Slf4j
class SolrRequestConverterTest {
    private SolrRequestConverter converter;

    @BeforeEach
    void setup() {
        this.converter = new SolrRequestConverter();
    }

    @Nested
    @DisplayName("Testing SolrQuery creation")
    class SolrQueryConverterTest {
        ObjectMapper mapper = new ObjectMapper();

        @Test
        void canCreateSolrQueryWithFacetsAndTermFields() throws IOException {
            // given
            String queryString = "query";
            int rows = 25;
            SolrFacetRequest facet1 =
                    SolrFacetRequest.builder().name("fragment").minCount(10).limit(2).build();
            Map<String, String> interval = new HashMap<>();
            interval.put("1", "[1,200]");
            interval.put("2", "[201,400]");
            interval.put("3", "[401,600]");
            interval.put("4", "[601,800]");
            interval.put("5", "[801,*]");
            SolrFacetRequest facet2 =
                    SolrFacetRequest.builder().name("length").interval(interval).build();

            String termField1 = "term field 1";
            String termField2 = "term field 2";
            String filterQuery1 = "filter query 1";
            String filterQuery2 = "filter query 2";
            String sortField1 = "sortField1";
            String sortField2 = "sortField2";
            QueryOperator defaultQueryOperator = QueryOperator.OR;
            SolrRequest request =
                    SolrRequest.builder()
                            .query(queryString)
                            .rows(rows)
                            .facet(facet1)
                            .facet(facet2)
                            .termQuery(queryString)
                            .termField(termField1)
                            .termField(termField2)
                            .filterQuery(filterQuery1)
                            .filterQuery(filterQuery2)
                            .sort(SolrQuery.SortClause.asc(sortField1))
                            .sort(SolrQuery.SortClause.desc(sortField2))
                            .defaultQueryOperator(defaultQueryOperator)
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);

            // then
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            List<String> filters = Arrays.asList(queryParams.getParams("fq"));
            assertThat(filters, containsInAnyOrder(filterQuery1, filterQuery2));
            assertThat(queryParams.get("q"), is(queryString));
            assertThat(queryParams.get("rows"), is(String.valueOf(rows)));

            assertThat(queryParams.get("defType"), is("edismax"));
            assertThat(queryParams.get("distrib"), is("true"));
            assertThat(queryParams.get("terms"), is("true"));
            assertThat(queryParams.get("terms.mincount"), is("1"));
            assertThat(queryParams.get("terms.list"), is("query"));
            assertThat(queryParams.get("q.op"), is(defaultQueryOperator.name()));
            List<String> termFields = Arrays.asList(queryParams.getParams("terms.fl"));
            assertThat(termFields, containsInAnyOrder("term field 1", "term field 2"));

            // Facets and sort must be in the json request.
            StringOutputStream out = new StringOutputStream();
            solrQuery.getContentWriter("").write(out);
            String jsonRequest = out.getString();
            log.debug("request: {}", jsonRequest);
            assertNotNull(jsonRequest);

            Map result = mapper.readValue(jsonRequest, Map.class);
            assertNotNull(result);
            assertThat(result.get("sort"), is(sortField1 + " asc," + sortField2 + " desc"));

            Map<String, Object> facets = (Map<String, Object>) result.get("facet");
            assertThat(facets.keySet(), containsInAnyOrder(facet1.getName(), facet2.getName()));

            Map<String, Object> fragment = (Map<String, Object>) facets.get("fragment");
            assertThat(fragment.get("type"), is("terms"));
            assertThat(fragment.get("field"), is("fragment"));
            assertThat(fragment.get("mincount"), is(10));
            assertThat(fragment.get("limit"), is(2));
            assertThat(fragment.get("refine"), is(true));

            Map<String, Object> lengthFacet = (Map<String, Object>) facets.get("length");
            assertThat(lengthFacet.get("type"), is("range"));
            assertThat(lengthFacet.get("field"), is("length"));
            assertThat(lengthFacet.get("refine"), is("true"));
            List ranges = (List) lengthFacet.get("ranges");
            assertNotNull(ranges);
            assertThat(((Map) ranges.get(0)).get("range"), is("[1,200]"));
            assertThat(((Map) ranges.get(1)).get("range"), is("[201,400]"));
            assertThat(((Map) ranges.get(2)).get("range"), is("[401,600]"));
            assertThat(((Map) ranges.get(3)).get("range"), is("[601,800]"));
            assertThat(((Map) ranges.get(4)).get("range"), is("[801,*]"));
        }

        @Test
        void entryRequestDoesNotSetDefaults() {
            // given
            String queryString = "query";
            SolrRequest request = SolrRequest.builder().query(queryString).build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request, true);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("q"), is(queryString));
            assertThat(queryParams.get("df"), is(nullValue()));
            assertThat(queryParams.get("defType"), is(nullValue()));
        }

        @Test
        void requestingTermFieldsWithoutTermQueryCausesException() {
            SolrRequest request =
                    SolrRequest.builder().query("too long").termField("field 1").build();
            assertThrows(
                    InvalidRequestException.class, () -> converter.toJsonQueryRequest(request));
        }

        @Test
        void doNotAddTermParametersForMultiWordQueryTermRequest() {
            SolrRequest request =
                    SolrRequest.builder()
                            .query("multi word")
                            .termQuery("multi word")
                            .termField("field 1")
                            .build();
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            assertNotNull(solrQuery);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            assertThat(queryParams.get("defType"), is("edismax"));
            assertThat(queryParams.get("distrib"), nullValue());
            assertThat(queryParams.get("terms"), nullValue());
            assertThat(queryParams.get("terms.mincount"), nullValue());
            assertThat(queryParams.get("terms.list"), nullValue());
            assertThat(queryParams.get("terms.fl"), nullValue());
        }

        @Test
        void doNotCreateQueryBoostsAndHighlightForPageSizeZero() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(0)
                            .boostFunctions("f1,f2")
                            .fieldBoost("field1:value3^3")
                            .highlightFields("highlightFields")
                            .staticBoost("field2:value3^3")
                            .queryField("field1 field2")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("bq"), is(nullValue()));
            assertThat(queryParams.get("boost"), is(nullValue()));
            assertThat(queryParams.get("hl"), is(nullValue()));
            assertThat(queryParams.get("hl.fl"), is(nullValue()));
            assertThat(queryParams.get("qf"), is("field1 field2"));
        }

        @Test
        void queryBoostsWithPlaceholderIsReplacedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(10)
                            .fieldBoost("field1:{query}^3")
                            .staticBoost("field2:value3^2")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);
            // then
            List<String> boostQuery = Arrays.asList(queryParams.getParams("bq"));
            assertThat(
                    boostQuery,
                    containsInAnyOrder(
                            "field1:(value1)^3", "field1:(value2)^3", "field2:value3^2"));
            assertThat(queryParams.get("boost"), is(nullValue()));
        }

        @Test
        void intQueryBoostWithPlaceholderAndIntQueryIsReplacedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("9606")
                            .rows(10)
                            .fieldBoost("field1=number:{query}^3")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("bq"), is("field1:(9606)^3"));
            assertThat(queryParams.get("boost"), is(nullValue()));
        }

        @Test
        void intQueryBoostWithPlaceholderAndStringQueryIsReplacedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("hello")
                            .rows(10)
                            /*                            .queryConfig(
                            SolrQueryConfig.builder()
                                    .addBoost("field1=number:{query}^3")
                                    .build())*/
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("bq"), is(nullValue()));
            assertThat(queryParams.get("boost"), is(nullValue()));
        }

        @Test
        void defaultQueryBoostsAndFunctionsCreatedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(10)
                            .staticBoost("field1:value3^3")
                            .staticBoost("field2:value4^2")
                            .boostFunctions("f1,f2")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            List<String> boostQuery = Arrays.asList(queryParams.getParams("bq"));
            assertThat(boostQuery, containsInAnyOrder("field1:value3^3", "field2:value4^2"));
            assertThat(queryParams.get("boost"), is("f1,f2"));
        }

        @Test
        void queryFieldsQFCreatedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1")
                            /*                            .queryConfig(
                            SolrQueryConfig.builder().queryFields("field1 field2").build())*/
                            .queryField("field1 field2")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("qf"), is("field1 field2"));
        }

        @Test
        void advancedSearchQueryBoostsAndFunctionsCreatedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("field1:value1 value2")
                            .rows(10)
                            .fieldBoost("field1:value3^3")
                            .fieldBoost("field2:value4^2")
                            .boostFunctions("f1,f2")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);

            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            List<String> boostQuery = Arrays.asList(queryParams.getParams("bq"));
            assertThat(boostQuery, contains("field1:value3^3", "field2:value4^2"));
            assertThat(queryParams.get("boost"), is("f1,f2"));
        }

        @Test
        void advancedSearchQueryBoostsForFieldsAndPlaceHolderCreatedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("field1:value1 value2")
                            .rows(10)
                            .fieldBoost("field1:value3^3")
                            .fieldBoost("field2:value4^2")
                            .fieldBoost("field3:{query}^4")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);

            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            List<String> boostQuery = Arrays.asList(queryParams.getParams("bq"));
            assertThat(
                    boostQuery,
                    contains("field1:value3^3", "field2:value4^2", "field3:(value2)^4"));
        }

        @Test
        void highlightsFieldsCanBeCreatedCorrectly() {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(10)
                            .highlightFields("h1,h2")
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("hl"), is("on"));
            assertThat(queryParams.get("hl.fl"), is("h1,h2"));
        }
    }
}
