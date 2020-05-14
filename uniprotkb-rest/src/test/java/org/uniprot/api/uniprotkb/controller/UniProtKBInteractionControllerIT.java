package org.uniprot.api.uniprotkb.controller;

import static org.hamcrest.collection.IsIterableContainingInOrder.contains;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.http.MediaType.APPLICATION_XML_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import java.util.HashMap;

import lombok.extern.slf4j.Slf4j;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.extension.RegisterExtension;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.client.AutoConfigureWebClient;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.uniprot.api.uniprotkb.UniProtKBREST;
import org.uniprot.api.uniprotkb.repository.DataStoreTestConfig;
import org.uniprot.api.uniprotkb.repository.search.impl.UniprotQueryRepository;
import org.uniprot.api.uniprotkb.repository.store.UniProtKBStoreClient;
import org.uniprot.core.uniprotkb.UniProtKBEntry;
import org.uniprot.core.uniprotkb.comment.InteractionComment;
import org.uniprot.core.uniprotkb.comment.impl.InteractantBuilder;
import org.uniprot.core.uniprotkb.comment.impl.InteractionBuilder;
import org.uniprot.core.uniprotkb.comment.impl.InteractionCommentBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBEntryBuilder;
import org.uniprot.cv.chebi.ChebiRepo;
import org.uniprot.cv.ec.ECRepo;
import org.uniprot.store.datastore.voldemort.uniprot.VoldemortInMemoryUniprotEntryStore;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.indexer.uniprot.mockers.GoRelationsRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.PathwayRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.TaxonomyRepoMocker;
import org.uniprot.store.indexer.uniprot.mockers.UniProtEntryMocker;
import org.uniprot.store.indexer.uniprotkb.converter.UniProtEntryConverter;
import org.uniprot.store.indexer.uniprotkb.processor.InactiveEntryConverter;
import org.uniprot.store.search.SolrCollection;

/**
 * Created 06/05/2020
 *
 * @author Edd
 */
@Slf4j
@ContextConfiguration(classes = {DataStoreTestConfig.class, UniProtKBREST.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniProtKBInteractionController.class)
@AutoConfigureWebClient
@ExtendWith(value = {SpringExtension.class})
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class UniProtKBInteractionControllerIT {

    private static final String UNIPROTKB_ACCESSION_PATH = "/uniprotkb/accession/";
    private static final String ENTRY_WITH_INTERACTION_BUT_NO_ASSOCIATED_ENTRIES = "P00002";
    private static final String ENTRY_WITH_INTERACTION = "P00000";
    private static final String CROSS_REFERENCED_ASSOCIATION = "P00001";
    private static final String NON_EXISTENT_ENTRY = "P99999";
    @RegisterExtension static final DataStoreManager STORE_MANAGER = new DataStoreManager();
    private final UniProtKBEntry entryWithNoInteractions =
            UniProtEntryMocker.create(UniProtEntryMocker.Type.SP);;
    @Autowired private UniprotQueryRepository repository;
    @Autowired private UniProtKBStoreClient storeClient;
    @Autowired private MockMvc mockMvc;

    @BeforeAll
    void initUniprotKbDataStore() {
        UniProtEntryConverter uniProtEntryConverter =
                new UniProtEntryConverter(
                        TaxonomyRepoMocker.getTaxonomyRepo(),
                        GoRelationsRepoMocker.getGoRelationRepo(),
                        PathwayRepoMocker.getPathwayRepo(),
                        Mockito.mock(ChebiRepo.class),
                        Mockito.mock(ECRepo.class),
                        new HashMap<>());

        STORE_MANAGER.addDocConverter(DataStoreManager.StoreType.UNIPROT, uniProtEntryConverter);
        STORE_MANAGER.addDocConverter(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, new InactiveEntryConverter());
        STORE_MANAGER.addSolrClient(
                DataStoreManager.StoreType.INACTIVE_UNIPROT, SolrCollection.uniprot);

        storeClient =
                new UniProtKBStoreClient(
                        VoldemortInMemoryUniprotEntryStore.getInstance("avro-uniprot"));
        STORE_MANAGER.addStore(DataStoreManager.StoreType.UNIPROT, storeClient);

        STORE_MANAGER.addSolrClient(DataStoreManager.StoreType.UNIPROT, SolrCollection.uniprot);
        ReflectionTestUtils.setField(
                repository,
                "solrClient",
                STORE_MANAGER.getSolrClient(DataStoreManager.StoreType.UNIPROT));

        saveScenarios();
    }

    @Test
    void entryWithNoInteractionsCausesNoContentStatus() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ACCESSION_PATH
                                        + entryWithNoInteractions.getPrimaryAccession().getValue()
                                        + "/interactions")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print()).andExpect(status().is(HttpStatus.NO_CONTENT.value()));
    }

    @Test
    void entryWithInteractionsSucceeds() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ACCESSION_PATH + ENTRY_WITH_INTERACTION + "/interactions")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(
                        jsonPath(
                                "$.interactionMatrix[*].uniProtKBAccession",
                                contains(ENTRY_WITH_INTERACTION, CROSS_REFERENCED_ASSOCIATION)))
                .andExpect(jsonPath("$.interactionMatrix[*].uniProtKBId").exists())
                .andExpect(jsonPath("$.interactionMatrix[*].organism").exists())
                .andExpect(jsonPath("$.interactionMatrix[*].proteinExistence").exists())
                .andExpect(jsonPath("$.interactionMatrix[*].interactions.size()", contains(1)))
                .andExpect(
                        jsonPath(
                                "$.interactionMatrix[*].interactions[*].interactantOne.uniProtkbAccession",
                                contains(ENTRY_WITH_INTERACTION)))
                .andExpect(
                        jsonPath(
                                "$.interactionMatrix[*].interactions[*].interactantOne.intActId",
                                contains("EBI-00001")))
                .andExpect(
                        jsonPath(
                                "$.interactionMatrix[*].interactions[*].interactantTwo.uniProtkbAccession",
                                contains(CROSS_REFERENCED_ASSOCIATION)))
                .andExpect(
                        jsonPath(
                                "$.interactionMatrix[*].interactions[*].interactantOne.intActId",
                                contains("EBI-00001")))
                .andExpect(
                        jsonPath(
                                "$.interactionMatrix[*].interactions[*].numberOfExperiments",
                                contains(2)))
                .andExpect(jsonPath("$.interactionMatrix[*].subcellularLocations").exists());
    }

    @Test
    void entryWithInteractionsInXMLSucceeds() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ACCESSION_PATH + ENTRY_WITH_INTERACTION + "/interactions")
                                .header(ACCEPT, APPLICATION_XML_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_XML_VALUE))
                .andExpect(xpath("InteractionEntry").exists());
    }

    @Test
    void entryNotFoundCauses404() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ACCESSION_PATH + NON_EXISTENT_ENTRY + "/interactions")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print()).andExpect(status().is(HttpStatus.NOT_FOUND.value()));
    }

    @Test
    void retrievedEntryWhoseInteractionEntriesAreNotFoundCauses500() throws Exception {
        // when
        ResultActions response =
                mockMvc.perform(
                        get(UNIPROTKB_ACCESSION_PATH
                                        + ENTRY_WITH_INTERACTION_BUT_NO_ASSOCIATED_ENTRIES
                                        + "/interactions")
                                .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print()).andExpect(status().is(HttpStatus.INTERNAL_SERVER_ERROR.value()));
    }

    private void saveScenarios() {
        // ========================================================================================
        // save entry with no interactions
        STORE_MANAGER.save(DataStoreManager.StoreType.UNIPROT, entryWithNoInteractions);

        InteractionComment interactionWithSavedKBCrossReference =
                new InteractionCommentBuilder()
                        .interactionsAdd(
                                new InteractionBuilder()
                                        .interactantOne(
                                                new InteractantBuilder()
                                                        .uniProtKBAccession("P00000")
                                                        .intActId("EBI-00001")
                                                        .build())
                                        .interactantTwo(
                                                new InteractantBuilder()
                                                        .uniProtKBAccession("P00001")
                                                        .intActId("EBI-00001")
                                                        .build())
                                        .numberOfExperiments(2)
                                        .isOrganismDiffer(true)
                                        .build())
                        .build();

        // ========================================================================================
        // save a KB entry that has interaction, and also save the KB entry cross-referenced by the
        // interaction
        UniProtKBEntry entry_withInteraction =
                UniProtKBEntryBuilder.from(entryWithNoInteractions)
                        .primaryAccession(ENTRY_WITH_INTERACTION)
                        .commentsAdd(interactionWithSavedKBCrossReference)
                        .build();
        UniProtKBEntry entry_associatedToInteraction =
                UniProtKBEntryBuilder.from(entryWithNoInteractions)
                        .primaryAccession(CROSS_REFERENCED_ASSOCIATION)
                        .build();

        STORE_MANAGER.save(DataStoreManager.StoreType.UNIPROT, entry_withInteraction);
        STORE_MANAGER.save(DataStoreManager.StoreType.UNIPROT, entry_associatedToInteraction);

        // ========================================================================================
        // save KB entry that has interactions, and this interaction cross-references a KB entry
        // that has not saved
        InteractionComment interactionWithUnsavedKBCrossReference =
                new InteractionCommentBuilder()
                        .interactionsAdd(
                                new InteractionBuilder()
                                        .interactantOne(
                                                new InteractantBuilder()
                                                        .uniProtKBAccession("P00000")
                                                        .intActId("EBI-00001")
                                                        .build())
                                        .interactantTwo(
                                                new InteractantBuilder()
                                                        .uniProtKBAccession(NON_EXISTENT_ENTRY)
                                                        .intActId("EBI-00001")
                                                        .build())
                                        .numberOfExperiments(2)
                                        .isOrganismDiffer(true)
                                        .build())
                        .build();

        UniProtKBEntry entry_withInteractionButNoAssociated =
                UniProtKBEntryBuilder.from(entryWithNoInteractions)
                        .primaryAccession(ENTRY_WITH_INTERACTION_BUT_NO_ASSOCIATED_ENTRIES)
                        .commentsAdd(interactionWithUnsavedKBCrossReference)
                        .build();

        STORE_MANAGER.save(
                DataStoreManager.StoreType.UNIPROT, entry_withInteractionButNoAssociated);
    }
}
