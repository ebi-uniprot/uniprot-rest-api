package org.uniprot.api.rest.request;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;

class UniProtKBRequestUtilTest {

    private static final String ACCESSION_ID = "accession_id";
    private static final String IS_ISOFORM = "is_isoform";

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

    @Test
    void doesNotNeedToFilterIsoformForAccessionIdQueries() {
        String query = ACCESSION_ID + ":P21802";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, false);
        assertFalse(result);
    }

    @Test
    void doesNotNeedToFilterIsoformForIsIsoformQueries() {
        String query = IS_ISOFORM + ":true";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, false);
        assertFalse(result);
    }

    @Test
    void doesNotNeedToFilterIsoformForIsoformDefaultSearch() {
        String query = "P21802-2";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, false);
        assertFalse(result);
    }

    @Test
    void doesNotNeedToFilterIsoformWhenIncludeIsoformTrue() {
        String query = ACCESSION_ID + ":P21802";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, true);
        assertFalse(result);
    }

    @Test
    void doesNotNeedToFilterIsoformForDefaultSearchAndIncludeIsoformTrue() {
        String query = "queryValue";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, true);
        assertFalse(result);
    }

    @Test
    void needToFilterIsoformForDefaultSearchAndIncludeIsoformFalse() {
        String query = "queryValue";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, false);
        assertTrue(result);
    }

    @Test
    void needToFilterIsoformForGeneSearchAndIncludeIsoformFalse() {
        String query = "gene:geneValue";
        boolean result =
                UniProtKBRequestUtil.needsToFilterIsoform(ACCESSION_ID, IS_ISOFORM, query, false);
        assertTrue(result);
    }
}
