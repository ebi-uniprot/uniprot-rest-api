package org.uniprot.api.uniprotkb.controller;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.http.*;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Created 01/10/2021
 *
 * @author Edd
 */
class VarnishCachingTest {
    @ParameterizedTest
    @ValueSource(
            strings = {
                "silly|hit",
                "(brca)|hit",
                "((length:[10 TO 100]))|hit",
                "((length:[10 TO%20100]))|hit",
                "((length:[10%20TO 100]))|hit",
                "\"ATP synthase subunit\"|hit"
            })
    void sendRequestToVarnish(String value) {
        String[] inputValues = value.split("\\|");
        String query = inputValues[0];
        String cachedStatus = inputValues[1];

        RestTemplate restTemplate = new RestTemplate();

        String url =
                "http://hx-rke-wp-webadmin-02-worker-11.caas.ebi.ac.uk:30091/uniprot/beta/api/uniprotkb/search?query={query}";

        HttpHeaders headers = new HttpHeaders();
        headers.set("Accept", MediaType.APPLICATION_JSON_VALUE);
        HttpEntity<?> entity = new HttpEntity<>(headers);

        try {
            ResponseEntity<String> response =
                    restTemplate.exchange(url, HttpMethod.GET, entity, String.class, query);

            int statusCode = response.getStatusCode().value();
            assertThat("Status Code must be 200 OK", statusCode, is((200)));

            String xCacheHeader = response.getHeaders().get("x-cache").get(0);
            assertThat(xCacheHeader, matchesPattern(".*" + cachedStatus + ".*"));

            List<String> totalRecordsList = response.getHeaders().get("x-total-records");
            if(totalRecordsList.size() > 0){
                String resultsCount = totalRecordsList.get(0);
                System.out.println(
                        String.format(
                                "query=%s, status=%s, x-cache=%s, results=%s",
                                query, statusCode, cachedStatus, resultsCount));
            }

        } catch (Exception e) {
            System.err.println("Query failed: " + query);
            e.printStackTrace();
            assertThat(true, is(false));
        }
    }
}
