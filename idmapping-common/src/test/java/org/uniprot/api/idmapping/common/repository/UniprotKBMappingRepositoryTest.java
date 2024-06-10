package org.uniprot.api.idmapping.common.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.core.uniprotkb.DeletedReason;
import org.uniprot.core.uniprotkb.InactiveReasonType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;

class UniprotKBMappingRepositoryTest {

    @Test
    void getDeletedEntrySuccess() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("accession_id", "I8FBX0");
        solrDocument.put("id", "INACTIVE_DROME");
        solrDocument.put("active", false);
        solrDocument.put("inactive_reason", "DELETED:SWISSPROT_DELETION");
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(solrDocument);

        UniProtKBEntry result = repository.getDeletedEntry("I8FBX0");
        assertNotNull(result);
        assertEquals("I8FBX0", result.getPrimaryAccession().getValue());
        assertEquals("INACTIVE_DROME", result.getUniProtkbId().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, result.getEntryType());
        assertEquals(
                InactiveReasonType.DELETED, result.getInactiveReason().getInactiveReasonType());
        assertEquals(
                DeletedReason.SWISSPROT_DELETION, result.getInactiveReason().getDeletedReason());
    }

    @Test
    void getDeletedEntryForIncorrectDeletedEntryThrowsException() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("accession_id", "P21802");
        solrDocument.put("active", true);
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(solrDocument);

        assertThrows(QueryRetrievalException.class, () -> repository.getDeletedEntry("P21802"));
    }

    @Test
    void getDeletedEntryThrowsSolrException() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new IOException("SolrError"));

        assertThrows(QueryRetrievalException.class, () -> repository.getDeletedEntry("P21802"));
    }

    @Test
    void getDeletedEntryForMergedMappedEntrySuccess() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        SolrDocument mergedDocument = new SolrDocument();
        mergedDocument.put("accession_id", "A0A2T1ATI5");
        mergedDocument.put("id", "INACTIVE_DROME");
        mergedDocument.put("active", false);
        mergedDocument.put("inactive_reason", "MERGED:A0A0D5V897");
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.eq("A0A2T1ATI5")))
                .thenReturn(mergedDocument);

        SolrDocument deletedDocument = new SolrDocument();
        deletedDocument.put("accession_id", "A0A0D5V897");
        deletedDocument.put("id", "INACTIVE_A0A0D5V897");
        deletedDocument.put("active", false);
        deletedDocument.put("inactive_reason", "DELETED:PROTEOME_REDUNDANCY");
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.eq("A0A0D5V897")))
                .thenReturn(deletedDocument);

        UniProtKBEntry result = repository.getDeletedEntry("A0A2T1ATI5");
        assertNotNull(result);
        assertEquals("A0A0D5V897", result.getPrimaryAccession().getValue());
        assertEquals("INACTIVE_A0A0D5V897", result.getUniProtkbId().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, result.getEntryType());
        assertEquals(
                InactiveReasonType.DELETED, result.getInactiveReason().getInactiveReasonType());
        assertEquals(
                DeletedReason.PROTEOME_REDUNDANCY, result.getInactiveReason().getDeletedReason());
    }
}
