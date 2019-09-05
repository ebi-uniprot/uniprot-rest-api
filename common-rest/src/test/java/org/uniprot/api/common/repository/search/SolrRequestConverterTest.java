package org.uniprot.api.common.repository.search;

import org.apache.solr.client.solrj.SolrQuery;
import org.junit.jupiter.api.*;
import org.springframework.data.domain.Sort;
import org.springframework.data.solr.core.query.*;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.facet.FakeFacetConfig;

import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Created 17/06/19
 *
 * @author Edd
 */
class SolrRequestConverterTest {
    private SolrRequestConverter converter;

    @BeforeEach
    void setup() {
        this.converter = new SolrRequestConverter();
    }

    @Nested
    @DisplayName("Testing SolrQuery creation")
    class SolrQueryConverterTest {
        @Test
        void canCreateSolrQueryWithFacetsAndTermFields() {
            // given
            String queryString = "query";
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
            Query.Operator defaultQueryOperator = Query.Operator.OR;
            SolrRequest request = SolrRequest.builder()
                    .query(queryString)
                    .facet(facet1)
                    .facet(facet2)
                    .termQuery(queryString)
                    .facetConfig(facetConfig)
                    .termField(termField1)
                    .termField(termField2)
                    .filterQuery(filterQuery1)
                    .filterQuery(filterQuery2)
                    .sort(new Sort(Sort.Direction.ASC, sortField1).and(new Sort(Sort.Direction.DESC, sortField2)))
                    .defaultQueryOperator(defaultQueryOperator)
                    .build();

            // when
            SolrQuery solrQuery = converter.toSolrQuery(request);

            // then
            assertThat(solrQuery.getQuery(), is(queryString));
            assertThat(solrQuery.getFacetFields(), arrayContainingInAnyOrder(facet1));
            assertThat(solrQuery.getParams("facet.interval"), arrayContainingInAnyOrder(facet2));
            assertThat(solrQuery
                               .getParams("f.length.facet.interval.set"), arrayContainingInAnyOrder("[1,200]", "[201,400]", "[401,600]", "[601,800]", "[801,*]"));
            assertThat(solrQuery.getFacetLimit(), is(facetLimit));
            assertThat(solrQuery.getFacetMinCount(), is(facetMinCount));
            assertThat(solrQuery.getTermsFields(), arrayContainingInAnyOrder(termField1, termField2));
            assertThat(solrQuery.getTermsMinCount(), is(1));
            assertThat(solrQuery.getFilterQueries(), arrayContainingInAnyOrder(filterQuery1, filterQuery2));
            assertThat(solrQuery.getSorts(),
                       contains(new SolrQuery.SortClause(sortField1, SolrQuery.ORDER.asc),
                                new SolrQuery.SortClause(sortField2, SolrQuery.ORDER.desc)));
            assertThat(solrQuery.get("q.op"), is(defaultQueryOperator.asQueryStringRepresentation()));
        }

        @Test
        void requestingTermFieldsWithoutTermQueryCausesException() {
            SolrRequest request = SolrRequest.builder().query("too long").termField("field 1")
                    .build();
            assertThrows(InvalidRequestException.class, () -> converter.toSolrQuery(request));
        }

        @Test
        void tooLongQueryWithTermsRequestCausesException() {
            SolrRequest request = SolrRequest.builder().query("too long").termQuery("too long").termField("field 1")
                    .build();
            assertThrows(InvalidRequestException.class, () -> converter.toSolrQuery(request));
        }

        @Test
        void queryBoostsWithPlaceholderIsReplacedCorrectly() {
            // given
            SolrRequest request = SolrRequest.builder().query("value1 value2").queryBoosts(
                    QueryBoosts.builder()
                            .defaultSearchBoost("field1:{query}^3")
                            .defaultSearchBoost("field2:value3^2")
                            .build()
            ).build();

            // when
            SolrQuery solrQuery = converter.toSolrQuery(request);

            // then
            assertThat(solrQuery.getParams("bq"), is(arrayContainingInAnyOrder("field1:(value1 value2)^3", "field2:value3^2")));
            assertThat(solrQuery.get("boost"), is(nullValue()));
        }

        @Test
        void defaultQueryBoostsAndFunctionsCreatedCorrectly() {
            // given
            SolrRequest request = SolrRequest.builder().query("value1 value2").queryBoosts(
                    QueryBoosts.builder()
                            .defaultSearchBoost("field1:value3^3")
                            .defaultSearchBoost("field2:value4^2")
                            .defaultSearchBoostFunctions("f1,f2")
                            .build()
            ).build();

            // when
            SolrQuery solrQuery = converter.toSolrQuery(request);

            // then
            assertThat(solrQuery.getParams("bq"), is(arrayContainingInAnyOrder("field1:value3^3", "field2:value4^2")));
            assertThat(solrQuery.get("boost"), is("f1,f2"));
        }

        @Test
        void advancedSearchQueryBoostsAndFunctionsCreatedCorrectly() {
            // given
            SolrRequest request = SolrRequest.builder().query("field1:value1 value2").queryBoosts(
                    QueryBoosts.builder()
                            .advancedSearchBoost("field1:value3^3")
                            .advancedSearchBoost("field2:value4^2")
                            .advancedSearchBoostFunctions("f1,f2")
                            .build()
            ).build();

            // when
            SolrQuery solrQuery = converter.toSolrQuery(request);

            // then
            assertThat(solrQuery.getParams("bq"), is(arrayContainingInAnyOrder("field1:value3^3", "field2:value4^2")));
            assertThat(solrQuery.get("boost"), is("f1,f2"));
        }
    }

    @Nested
    @DisplayName("Testing Spring Query creation")
    class QueryConverterTest {
        @Test
        void canCreateFacetQuery() {
            // given
            String queryString = "my query";
            FakeFacetConfig facetConfig = new FakeFacetConfig();
            int facetLimit = 100;
            int facetMinCount = 10;
            facetConfig.setLimit(facetLimit);
            facetConfig.setMincount(facetMinCount);
            String facet1 = "fragment";
            String facet2 = "reviewed";
            String filterQuery1 = "filter query 1";
            String filterQuery2 = "filter query 2";
            Sort sorts = new Sort(Sort.Direction.ASC, "sortField1").and(new Sort(Sort.Direction.DESC, "sortField2"));
            Query.Operator defaultQueryOperator = Query.Operator.OR;
            SolrRequest request = SolrRequest.builder()
                    .query(queryString)
                    .facet(facet1)
                    .facet(facet2)
                    .facetConfig(facetConfig)
                    .filterQuery(filterQuery1)
                    .filterQuery(filterQuery2)
                    .sort(sorts)
                    .defaultQueryOperator(defaultQueryOperator)
                    .build();

            // when
            Query facetQuery = converter.toQuery(request);

            // then
            assertThat(((SimpleStringCriteria) facetQuery.getCriteria()).getQueryString(), is(queryString));
            FacetOptions facetOptions = ((SimpleFacetQuery) facetQuery).getFacetOptions();
            assertThat(facetOptions.getFacetLimit(), is(facetLimit));
            assertThat(facetOptions.getFacetMinCount(), is(facetMinCount));
            assertThat(facetOptions.getFacetOnFields().stream().map(Field::getName)
                               .collect(Collectors.toList()), containsInAnyOrder(facet1, facet2));
            assertThat(facetQuery.getFilterQueries().stream()
                               .map(SolrDataQuery::getCriteria)
                               .map(c -> ((SimpleStringCriteria) c).getQueryString())
                               .collect(Collectors.toList()),
                       containsInAnyOrder(filterQuery1, filterQuery2));
            assertThat(facetQuery.getSort(), is(sorts));
            assertThat(facetQuery.getDefaultOperator(), is(defaultQueryOperator));
        }
    }

    @Test
    void notSupportedIntervalQueryQuery() {
        String queryString = "my query";
        FakeFacetConfig facetConfig = new FakeFacetConfig();
        int facetLimit = 100;
        int facetMinCount = 10;
        facetConfig.setLimit(facetLimit);
        facetConfig.setMincount(facetMinCount);
        String facet1 = "length";
        String facet2 = "reviewed";
        SolrRequest request = SolrRequest.builder()
                .query(queryString)
                .facet(facet1)
                .facet(facet2)
                .facetConfig(facetConfig)
                .build();

        // when
        Assertions.assertThrows(UnsupportedOperationException.class, () -> {
            converter.toQuery(request);
        });
    }
}