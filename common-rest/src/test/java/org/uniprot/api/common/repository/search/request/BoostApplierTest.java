package org.uniprot.api.common.repository.search.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrRequest;

class BoostApplierTest {

    private ModifiableSolrParams solrQuery;
    private SolrRequest.SolrRequestBuilder requestBuilder;

    @BeforeEach
    void setUp() {
        solrQuery = new ModifiableSolrParams();
        requestBuilder = SolrRequest.builder();
    }

    @Test
    void noBoostsAdded() {
        SolrRequest request = requestBuilder.query("cdc7").build();

        BoostApplier.addBoosts(solrQuery, request);

        assertThat(solrQuery.get("bq"), is(nullValue()));
    }

    @Test
    void staticBoostsAdded() {
        SolrRequest request = requestBuilder.query("cdc7").staticBoost("static1:9606^1.0").build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("static1:9606^1.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsOneDefaultTerm() {
        SolrRequest request = requestBuilder.query("cdc7").fieldBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0"));
    }

    @Test
    void numericFieldBoostAddedWhenQueryIsOneDefaultTerm() {
        SolrRequest request =
                requestBuilder.query("9606").fieldBoost("field1=number:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(9606)^1.0"));
    }

    @Test
    void twoBoostsAddedWhenQueryIsOneDefaultTerm() {
        SolrRequest request =
                requestBuilder
                        .query("cdc7")
                        .fieldBoost("field1:{query}^1.0")
                        .fieldBoost("field2:{query}^2.0")
                        .build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0", "field2:(cdc7)^2.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsTwoDefaultTerms() {
        SolrRequest request =
                requestBuilder.query("cdc7 brca2").fieldBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0", "field1:(brca2)^1.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsPhrasalTerms() {
        SolrRequest request =
                requestBuilder.query("\"phrasal query\"").fieldBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(\"phrasal query\")^1.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsOneDefaultTermAndOneFieldTerm() {
        SolrRequest request =
                requestBuilder.query("cdc7 reviewed:true").fieldBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0"));
    }

    @Test
    void staticAndFieldBoostsAdded() {
        SolrRequest request =
                requestBuilder
                        .query("cdc7")
                        .staticBoost("static1:9606^2.0")
                        .fieldBoost("field1:{query}^1.0")
                        .build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("static1:9606^2.0", "field1:(cdc7)^1.0"));
    }

    @Test
    void staticAndFieldBoostsAddedForComplexQuery() {
        SolrRequest request =
                requestBuilder
                        .query("cdc7 reviewed:true (other:value OR 4000 OR brca2)")
                        .staticBoost("static1:9606^2.0")
                        .fieldBoost("field1:{query}^1.0")
                        .fieldBoost("field2:{query}^2.0")
                        .fieldBoost("field3=number:{query}^3.0")
                        .build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(
                bqs,
                containsInAnyOrder(
                        "static1:9606^2.0",
                        "field1:(cdc7)^1.0",
                        "field2:(cdc7)^2.0",
                        "field1:(brca2)^1.0",
                        "field2:(brca2)^2.0",
                        "field3:(4000)^3.0"));
    }

    @Test
    void staticAndFieldBoostsAddedForComplexQueryWithQuotedDefaultQueries() {
        SolrRequest request =
                requestBuilder
                        .query("\"cdc7\" reviewed:true (other:value OR 4000 OR \"brca2 values\")")
                        .staticBoost("static1:9606^2.0")
                        .fieldBoost("field1:{query}^1.0")
                        .fieldBoost("field2:{query}^2.0")
                        .fieldBoost("field3=number:{query}^3.0")
                        .build();

        BoostApplier.addBoosts(solrQuery, request);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(
                bqs,
                containsInAnyOrder(
                        "static1:9606^2.0",
                        "field1:(\"cdc7\")^1.0",
                        "field2:(\"cdc7\")^2.0",
                        "field1:(\"brca2 values\")^1.0",
                        "field2:(\"brca2 values\")^2.0",
                        "field3:(4000)^3.0"));
    }

    @Test
    void boostFunctionAdded() {
        SolrRequest request = requestBuilder.query("cdc7").boostFunctions("function").build();

        BoostApplier.addBoosts(solrQuery, request);

        assertThat(solrQuery.get("boost"), is("function"));
    }
}
