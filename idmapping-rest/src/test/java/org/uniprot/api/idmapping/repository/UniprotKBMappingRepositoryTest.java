package org.uniprot.api.idmapping.repository;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
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
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(solrDocument);

        UniProtKBEntry result = repository.getDeletedEntry("I8FBX0");
        assertNotNull(result);
        assertEquals("I8FBX0", result.getPrimaryAccession().getValue());
        assertEquals("INACTIVE_DROME", result.getUniProtkbId().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, result.getEntryType());
        assertEquals(
                InactiveReasonType.DELETED, result.getInactiveReason().getInactiveReasonType());
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
}
