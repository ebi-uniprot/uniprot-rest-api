package org.uniprot.api.uniparc.common.service.request;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UniParcBasicRequestTest {

    @Test
    void setQuery_whenNoProteomeComponent() {
        UniParcBasicRequest uniParcBasicRequest = new UniParcBasicRequest();
        String someQuery = "someQuery";
        uniParcBasicRequest.setQuery(someQuery);
        String query = uniParcBasicRequest.getQuery();
        assertSame(someQuery, query);
    }

    @Test
    void setQuery_whenProteomeComponent() {
        UniParcBasicRequest uniParcBasicRequest = new UniParcBasicRequest();
        String someQuery = "proteome:UP12345 AND proteomecomponent:chromosome";
        uniParcBasicRequest.setQuery(someQuery);
        String query = uniParcBasicRequest.getQuery();
        assertEquals("+proteome:UP12345 +proteomecomponent:\"UP12345 chromosome\"", query);
    }

    @Test
    void setQuery_whenProteomeComponentWithBrackets() {
        UniParcBasicRequest uniParcBasicRequest = new UniParcBasicRequest();
        String someQuery = "(proteome:UP12345) AND (proteomecomponent:chromosome)";
        uniParcBasicRequest.setQuery(someQuery);
        String query = uniParcBasicRequest.getQuery();
        assertEquals("+proteome:UP12345 +proteomecomponent:\"UP12345 chromosome\"", query);
    }
}