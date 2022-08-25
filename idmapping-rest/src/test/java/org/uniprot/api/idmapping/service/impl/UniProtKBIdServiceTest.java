package org.uniprot.api.idmapping.service.impl;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.common.SolrDocument;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.uniprot.api.common.exception.InvalidRequestException;
import org.uniprot.api.common.repository.search.QueryRetrievalException;
import org.uniprot.api.idmapping.model.IdMappingStringPair;
import org.uniprot.api.idmapping.model.UniProtKBEntryPair;
import org.uniprot.api.idmapping.repository.UniprotKBMappingRepository;
import org.uniprot.core.uniprotkb.InactiveReasonType;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.UniProtKBEntryType;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;

class UniProtKBIdServiceTest {

    @Test
    void convertToPair() {
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(null);
        UniProtKBIdService idService =
                new UniProtKBIdService(
                        null, null, null, null, repository, null, null, null, null, null);
        IdMappingStringPair idPair = new IdMappingStringPair("P21802", "P21802");
        Map<String, UniProtKBEntry> idEntryMap = new HashMap<>();
        UniProtKBEntry entry =
                new UniProtKBEntryBuilder("P21802", "FGFR2_HUMAN", UniProtKBEntryType.SWISSPROT)
                        .build();
        idEntryMap.put("P21802", entry);

        UniProtKBEntryPair result = idService.convertToPair(idPair, idEntryMap);
        assertNotNull(result);
        assertEquals("P21802", result.getFrom());
        assertEquals(entry, result.getTo());
    }

    @Test
    void convertToPairForDeletedEntry() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("accession_id", "I8FBX0");
        solrDocument.put("id", "INACTIVE_DROME");
        solrDocument.put("active", false);
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(solrDocument);

        UniProtKBIdService idService =
                new UniProtKBIdService(
                        null, null, null, null, repository, null, null, null, null, null);
        IdMappingStringPair idPair = new IdMappingStringPair("I8FBX0", "I8FBX0");
        Map<String, UniProtKBEntry> idEntryMap = new HashMap<>();
        UniProtKBEntryPair result = idService.convertToPair(idPair, idEntryMap);
        assertNotNull(result);
        assertEquals("I8FBX0", result.getFrom());
        assertNotNull(result.getTo());
        assertEquals("I8FBX0", result.getTo().getPrimaryAccession().getValue());
        assertEquals("INACTIVE_DROME", result.getTo().getUniProtkbId().getValue());
        assertEquals(UniProtKBEntryType.INACTIVE, result.getTo().getEntryType());
        assertEquals(
                InactiveReasonType.DELETED,
                result.getTo().getInactiveReason().getInactiveReasonType());
    }

    @Test
    void convertToPairForIncorrectDeletedEntryThrowsException() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        SolrDocument solrDocument = new SolrDocument();
        solrDocument.put("accession_id", "P21802");
        solrDocument.put("active", true);
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenReturn(solrDocument);

        UniProtKBIdService idService =
                new UniProtKBIdService(
                        null, null, null, null, repository, null, null, null, null, null);
        IdMappingStringPair idPair = new IdMappingStringPair("P21802", "P21802");
        Map<String, UniProtKBEntry> idEntryMap = new HashMap<>();
        assertThrows(
                QueryRetrievalException.class, () -> idService.convertToPair(idPair, idEntryMap));
    }

    @Test
    void convertToPairForSolrThrowsException() throws Exception {
        SolrClient solrClient = Mockito.mock(SolrClient.class);
        UniprotKBMappingRepository repository = new UniprotKBMappingRepository(solrClient);
        Mockito.when(solrClient.getById(Mockito.anyString(), Mockito.anyString()))
                .thenThrow(new IOException("SolrError"));

        UniProtKBIdService idService =
                new UniProtKBIdService(
                        null, null, null, null, repository, null, null, null, null, null);
        IdMappingStringPair idPair = new IdMappingStringPair("P21802", "P21802");
        Map<String, UniProtKBEntry> idEntryMap = new HashMap<>();
        assertThrows(
                QueryRetrievalException.class, () -> idService.convertToPair(idPair, idEntryMap));
    }

    @Test
    void isLineageAllowedFoundLineage() {
        SolrClient solrClient = Mockito.mock(SolrClient.class);

        UniProtKBIdService idService =
                new UniProtKBIdService(null, null, null, null, null, null, null, null, null, null);
        assertTrue(idService.isLineageAllowed("lineage"));
    }

    @Test
    void isLineageAllowedFoundLineageIds() {
        UniProtKBIdService idService =
                new UniProtKBIdService(null, null, null, null, null, null, null, null, null, null);
        assertTrue(idService.isLineageAllowed("lineage_ids"));
    }

    @Test
    void isLineageAllowedNotFound() {
        SolrClient solrClient = Mockito.mock(SolrClient.class);

        UniProtKBIdService idService =
                new UniProtKBIdService(null, null, null, null, null, null, null, null, null, null);
        assertFalse(idService.isLineageAllowed("accession"));
    }

    @Test
    void validateSubSequenceRequestValid() {
        UniProtKBIdService idService =
                new UniProtKBIdService(null, null, null, null, null, null, null, null, null, null);
        IdMappingStringPair id1 = IdMappingStringPair.builder().from("P21802[10-20]").build();
        IdMappingStringPair id2 = IdMappingStringPair.builder().from("P12345[50-60]").build();
        List<IdMappingStringPair> mappedIds = List.of(id1, id2);
        assertDoesNotThrow(() -> idService.validateSubSequenceRequest(mappedIds, true));
    }

    @Test
    void validateSubSequenceRequestThownExceptionIfNotAllSubSequenceFrom() {
        UniProtKBIdService idService =
                new UniProtKBIdService(null, null, null, null, null, null, null, null, null, null);
        IdMappingStringPair id1 = IdMappingStringPair.builder().from("P21802[10-20]").build();
        IdMappingStringPair id2 = IdMappingStringPair.builder().from("P12345").build();
        IdMappingStringPair id3 = IdMappingStringPair.builder().from("P05067").build();
        List<IdMappingStringPair> mappedIds = List.of(id1, id2, id3);
        InvalidRequestException exception =
                assertThrows(
                        InvalidRequestException.class,
                        () -> idService.validateSubSequenceRequest(mappedIds, true));
        assertNotNull(exception);
        assertEquals(
                "Unable to compute fasta subsequence for IDs: P12345,P05067. Expected format is accession[begin-end], for example:Q00001[10-20]",
                exception.getMessage());
    }

    @Test
    void validateSubSequenceRequestDoNotValidateFalseSubSequence() {
        UniProtKBIdService idService =
                new UniProtKBIdService(null, null, null, null, null, null, null, null, null, null);
        IdMappingStringPair id1 = IdMappingStringPair.builder().from("P21802[10-20]").build();
        IdMappingStringPair id2 = IdMappingStringPair.builder().from("P12345").build();
        List<IdMappingStringPair> mappedIds = List.of(id1, id2);
        assertDoesNotThrow(() -> idService.validateSubSequenceRequest(mappedIds, false));
    }
}
