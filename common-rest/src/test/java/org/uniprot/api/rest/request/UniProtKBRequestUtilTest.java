package org.uniprot.api.rest.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UniProtKBRequestUtilTest {

    @Test
    void parseFormatSuccess() {
        String result = UniProtKBRequestUtil.parseFormat("json");
        assertNotNull(result);
        assertEquals("application/json", result);
    }

    @Test
    void parseFormatInvalid() {
        String result = UniProtKBRequestUtil.parseFormat("invalid");
        assertNotNull(result);
        assertEquals("invalid", result);
    }

    @Test
    void parseFormatNull() {
        String result = UniProtKBRequestUtil.parseFormat(null);
        assertNull(result);
    }

    @Test
    void parseFormatFasta() {
        String result = UniProtKBRequestUtil.parseFormat("fasta");
        assertNotNull(result);
        assertEquals("text/plain;format=fasta", result);
    }

    @Test
    void parseFormatFullFasta() {
        String result = UniProtKBRequestUtil.parseFormat("text/plain;format=fasta");
        assertNotNull(result);
        assertEquals("text/plain;format=fasta", result);
    }
}
