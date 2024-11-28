package org.uniprot.api.async.download.model.request.mapto;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.uniprot.api.rest.request.UniProtKBRequestUtil;
import org.uniprot.store.search.SolrCollection;

import static org.junit.jupiter.api.Assertions.*;

class UniParcToUniProtKBMapDownloadRequestTest {
    private UniParcToUniProtKBMapDownloadRequest request;

    @BeforeEach
    void setUp() {
        request = new UniParcToUniProtKBMapDownloadRequest();
    }

    @Test
    void testInitialFieldValues() {
        // Default values should be null or false
        assertNull(request.getFormat());
        assertNull(request.getFields());
        assertNull(request.getDownloadJobId());
        assertFalse(request.isForce());
    }

    @Test
    void testSetAndGetFormat() {
        // Set the format
        String format = "xml";
        request.setFormat(format);
        String expectedFormat = UniProtKBRequestUtil.parseFormat(format);
        assertEquals(expectedFormat, request.getFormat());
    }

    @Test
    void testSetAndGetFields() {
        // Set and get fields
        String fields = "xref_full_name, protein_name";
        request.setFields(fields);
        assertEquals(fields, request.getFields());
    }

    @Test
    void testSetAndGetDownloadJobId() {
        String downloadJobId = "12345";
        request.setDownloadJobId(downloadJobId);
        assertEquals(downloadJobId, request.getDownloadJobId());
    }

    @Test
    void testSetAndIsForce() {
        request.setForce(true);
        assertTrue(request.isForce());
    }

    @Test
    void testGetFromMethod() {
        assertEquals(SolrCollection.uniparc.name(), request.getFrom());
    }

    @Test
    void testGetToMethod() {
        assertEquals(SolrCollection.uniprot.name(), request.getTo());
    }
}
