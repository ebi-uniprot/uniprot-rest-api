package org.uniprot.api.common.repository.search;

import static org.junit.jupiter.api.Assertions.*;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicReference;

import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.client.solrj.impl.NoOpResponseParser;
import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sun.net.httpserver.HttpServer;

class SolrJsonRequestTransportTest {
    @Test
    void largeQueriesAndFiltersAreSentInBodyWithShortUrl() throws Exception {
        AtomicReference<String> method = new AtomicReference<>();
        AtomicReference<String> uri = new AtomicReference<>();
        AtomicReference<String> body = new AtomicReference<>();
        HttpServer server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/solr/uniprot/select",
                exchange -> {
                    method.set(exchange.getRequestMethod());
                    uri.set(exchange.getRequestURI().toString());
                    body.set(
                            new String(
                                    exchange.getRequestBody().readAllBytes(),
                                    StandardCharsets.UTF_8));
                    byte[] response = "{}".getBytes(StandardCharsets.UTF_8);
                    exchange.getResponseHeaders().set("Content-Type", "application/json");
                    exchange.sendResponseHeaders(200, response.length);
                    exchange.getResponseBody().write(response);
                    exchange.close();
                });
        server.start();
        try (Http2SolrClient client =
                new Http2SolrClient.Builder(
                                "http://127.0.0.1:"
                                        + server.getAddress().getPort()
                                        + "/solr/uniprot")
                        .useHttp1_1(true)
                        .withResponseParser(new NoOpResponseParser("json"))
                        .build()) {
            String query = "accession:P12345 OR ".repeat(2000) + "accession:P00001";
            String filter = "{!terms f=accession_id}" + "P12345,".repeat(2000) + "P00001";
            var request =
                    new SolrRequestConverter()
                            .toJsonQueryRequest(
                                    SolrRequest.builder()
                                            .query(query)
                                            .filterQuery(filter)
                                            .filterQuery("reviewed:true")
                                            .rows(10)
                                            .build());

            client.request(request);

            assertEquals("POST", method.get());
            assertTrue(uri.get().length() < 1024, uri.get());
            var params = new ObjectMapper().readTree(body.get()).get("params");
            assertEquals(query, params.get("q").get(0).asText());
            assertEquals(filter, params.get("fq").get(0).asText());
            assertEquals("reviewed:true", params.get("fq").get(1).asText());
        } finally {
            server.stop(0);
        }
    }
}
