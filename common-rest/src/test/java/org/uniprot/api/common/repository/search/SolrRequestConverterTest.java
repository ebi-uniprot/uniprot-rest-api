package org.uniprot.api.common.repository.search;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import lombok.extern.slf4j.Slf4j;

import org.apache.solr.client.solrj.SolrQuery;
import org.apache.solr.client.solrj.request.json.JsonQueryRequest;
import org.apache.solr.common.params.SolrParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;

import voldemort.utils.StringOutputStream;

import com.fasterxml.jackson.databind.ObjectMapper;

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
            String facet1 = "fragment";
            String facet2 = "length";
            FakeFacetConfig facetConfig = new FakeFacetConfig();
            int facetLimit = 100;
            int facetMinCount = 10;
            facetConfig.setLimit(facetLimit);
            facetConfig.setMincount(facetMinCount);

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
                            .facetConfig(facetConfig)
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
            assertThat(facets.keySet(), containsInAnyOrder(facet1, facet2));

            Map<String, Object> fragment = (Map<String, Object>) facets.get(facet1);
            assertThat(fragment.get("type"), is("terms"));
            assertThat(fragment.get("field"), is(facet1));
            assertThat(fragment.get("mincount"), is(10));
            assertThat(fragment.get("limit"), is(2));
            assertThat(fragment.get("refine"), is(true));

            Map<String, Object> lengthFacet = (Map<String, Object>) facets.get(facet2);
            assertThat(lengthFacet.get("type"), is("range"));
            assertThat(lengthFacet.get("field"), is(facet2));
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
        void entryRequestDoesNotSetDefaults() throws IOException {
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
        void tooLongQueryWithTermsRequestCausesException() {
            SolrRequest request =
                    SolrRequest.builder()
                            .query("too long")
                            .termQuery("too long")
                            .termField("field 1")
                            .build();
            assertThrows(
                    InvalidRequestException.class, () -> converter.toJsonQueryRequest(request));
        }

        @Test
        void doNotCreateQueryBoostsAndFunctionsForPageSizeZero() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(0)
                            .queryConfig(
                                    SolrQueryConfig.builder()
                                            .defaultSearchBoost("field1:value3^3")
                                            .defaultSearchBoostFunctions("f1,f2")
                                            .queryFields("field1,field2")
                                            .build())
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("bq"), is(nullValue()));
            assertThat(queryParams.get("boost"), is(nullValue()));
            assertThat(queryParams.get("qf"), is("field1 field2"));
        }

        @Test
        void queryBoostsWithPlaceholderIsReplacedCorrectly() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(10)
                            .queryConfig(
                                    SolrQueryConfig.builder()
                                            .defaultSearchBoost("field1:{query}^3")
                                            .defaultSearchBoost("field2:value3^2")
                                            .build())
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);
            // then
            List<String> boostQuery = Arrays.asList(queryParams.getParams("bq"));
            assertThat(boostQuery, contains("field1:(value1 value2)^3", "field2:value3^2"));
            assertThat(queryParams.get("boost"), is(nullValue()));
        }

        @Test
        void intQueryBoostWithPlaceholderAndIntQueryIsReplacedCorrectly() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("9606")
                            .rows(10)
                            .queryConfig(
                                    SolrQueryConfig.builder()
                                            .defaultSearchBoost("field1=number:{query}^3")
                                            .build())
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
        void intQueryBoostWithPlaceholderAndStringQueryIsReplacedCorrectly() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("hello")
                            .rows(10)
                            .queryConfig(
                                    SolrQueryConfig.builder()
                                            .defaultSearchBoost("field1=number:{query}^3")
                                            .build())
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
        void defaultQueryBoostsAndFunctionsCreatedCorrectly() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1 value2")
                            .rows(10)
                            .queryConfig(
                                    SolrQueryConfig.builder()
                                            .defaultSearchBoost("field1:value3^3")
                                            .defaultSearchBoost("field2:value4^2")
                                            .defaultSearchBoostFunctions("f1,f2")
                                            .build())
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
        void queryFieldsQFCreatedCorrectly() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("value1")
                            .queryConfig(
                                    SolrQueryConfig.builder().queryFields("field1,field2").build())
                            .build();

            // when
            JsonQueryRequest solrQuery = converter.toJsonQueryRequest(request);
            SolrParams queryParams = solrQuery.getParams();
            assertNotNull(queryParams);

            // then
            assertThat(queryParams.get("qf"), is("field1 field2"));
        }

        @Test
        void advancedSearchQueryBoostsAndFunctionsCreatedCorrectly() throws IOException {
            // given
            SolrRequest request =
                    SolrRequest.builder()
                            .query("field1:value1 value2")
                            .rows(10)
                            .queryConfig(
                                    SolrQueryConfig.builder()
                                            .advancedSearchBoost("field1:value3^3")
                                            .advancedSearchBoost("field2:value4^2")
                                            .advancedSearchBoostFunctions("f1,f2")
                                            .build())
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
    }
}
