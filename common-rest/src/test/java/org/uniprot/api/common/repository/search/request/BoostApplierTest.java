package org.uniprot.api.common.repository.search.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

import java.util.Arrays;
import java.util.List;

import org.apache.solr.common.params.ModifiableSolrParams;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.common.repository.search.SolrQueryConfig;

class BoostApplierTest {

    private ModifiableSolrParams solrQuery;
    private SolrQueryConfig.SolrQueryConfigBuilder queryConfigBuilder;

    @BeforeEach
    void setUp() {
        solrQuery = new ModifiableSolrParams();
        queryConfigBuilder = SolrQueryConfig.builder();
    }

    @Test
    void noBoostsAdded() {
        SolrQueryConfig config = queryConfigBuilder.build();

        BoostApplier.addBoosts(solrQuery, "cdc7", config);

        assertThat(solrQuery.get("bq"), is(nullValue()));
    }

    @Test
    void staticBoostsAdded() {
        SolrQueryConfig config = queryConfigBuilder.addBoost("static1:9606^1.0").build();

        BoostApplier.addBoosts(solrQuery, "cdc7", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("static1:9606^1.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsOneDefaultTerm() {
        SolrQueryConfig config = queryConfigBuilder.addBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, "cdc7", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0"));
    }

    @Test
    void numericFieldBoostAddedWhenQueryIsOneDefaultTerm() {
        SolrQueryConfig config = queryConfigBuilder.addBoost("field1=number:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, "9606", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(9606)^1.0"));
    }

    @Test
    void twoBoostsAddedWhenQueryIsOneDefaultTerm() {
        SolrQueryConfig config =
                queryConfigBuilder
                        .addBoost("field1:{query}^1.0")
                        .addBoost("field2:{query}^2.0")
                        .build();

        BoostApplier.addBoosts(solrQuery, "cdc7", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0", "field2:(cdc7)^2.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsTwoDefaultTerms() {
        SolrQueryConfig config = queryConfigBuilder.addBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, "cdc7 brca2", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0", "field1:(brca2)^1.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsPhrasalTerms() {
        SolrQueryConfig config = queryConfigBuilder.addBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, "\"phrasal query\"", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(\"phrasal query\")^1.0"));
    }

    @Test
    void fieldBoostsAddedWhenQueryIsOneDefaultTermAndOneFieldTerm() {
        SolrQueryConfig config = queryConfigBuilder.addBoost("field1:{query}^1.0").build();

        BoostApplier.addBoosts(solrQuery, "cdc7 reviewed:true", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("field1:(cdc7)^1.0"));
    }

    @Test
    void staticAndFieldBoostsAdded() {
        SolrQueryConfig config =
                queryConfigBuilder
                        .addBoost("static1:9606^2.0")
                        .addBoost("field1:{query}^1.0")
                        .build();

        BoostApplier.addBoosts(solrQuery, "cdc7", config);

        List<String> bqs = Arrays.asList(solrQuery.getParams("bq"));
        assertThat(bqs, containsInAnyOrder("static1:9606^2.0", "field1:(cdc7)^1.0"));
    }

    @Test
    void staticAndFieldBoostsAddedForComplexQuery() {
        SolrQueryConfig config =
                queryConfigBuilder
                        .addBoost("static1:9606^2.0")
                        .addBoost("field1:{query}^1.0")
                        .addBoost("field2:{query}^2.0")
                        .addBoost("field3=number:{query}^3.0")
                        .build();

        BoostApplier.addBoosts(
                solrQuery, "cdc7 reviewed:true (other:value OR 4000 OR brca2)", config);

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
        SolrQueryConfig config =
                queryConfigBuilder
                        .addBoost("static1:9606^2.0")
                        .addBoost("field1:{query}^1.0")
                        .addBoost("field2:{query}^2.0")
                        .addBoost("field3=number:{query}^3.0")
                        .build();

        BoostApplier.addBoosts(
                solrQuery, "\"cdc7\" reviewed:true (other:value OR 4000 OR \"brca2 values\")", config);

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
        SolrQueryConfig config = queryConfigBuilder.boostFunctions("function").build();

        BoostApplier.addBoosts(solrQuery, "cdc7", config);

        assertThat(solrQuery.get("boost"), is("function"));
    }
}
