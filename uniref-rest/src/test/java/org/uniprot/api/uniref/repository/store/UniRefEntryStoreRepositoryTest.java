package org.uniprot.api.uniref.repository.store;

import static org.junit.jupiter.api.Assertions.*;

import java.time.LocalDate;
import java.util.stream.IntStream;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.api.uniref.service.UniRefEntryResult;
import org.uniprot.core.cv.go.GoAspect;
import org.uniprot.core.cv.go.impl.GeneOntologyEntryBuilder;
import org.uniprot.core.uniparc.impl.UniParcIdBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionBuilder;
import org.uniprot.core.uniref.*;
import org.uniprot.core.uniref.impl.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;
import org.uniprot.store.datastore.voldemort.light.uniref.VoldemortInMemoryUniRefEntryLightStore;
import org.uniprot.store.datastore.voldemort.member.uniref.VoldemortInMemoryUniRefMemberStore;

/**
 * @author lgonzales
 * @since 24/07/2020
 */
@TestInstance(value = TestInstance.Lifecycle.PER_CLASS)
class UniRefEntryStoreRepositoryTest {

    private static final String UNIREF_ID_OK = "UniRef50_P21802";
    public static final String REPRESENTATIVE_ID = "P21802";
    public static final String SEED_ID = "P12342";
    private UniRefEntryStoreRepository repository;

    @BeforeAll
    void setup() {
        VoldemortInMemoryUniRefMemberStore memberStore =
                VoldemortInMemoryUniRefMemberStore.getInstance("uniref-member");
        UniRefMemberStoreClient memberStoreClient = new UniRefMemberStoreClient(memberStore);
        createMembers(memberStoreClient);

        VoldemortInMemoryUniRefEntryLightStore lightStore =
                VoldemortInMemoryUniRefEntryLightStore.getInstance("uniref-light");
        UniRefLightStoreClient lightStoreClient = new UniRefLightStoreClient(lightStore);
        createLightEntries(lightStoreClient);

        repository = new UniRefEntryStoreRepository(memberStoreClient, lightStoreClient);
    }

    @Test
    void getCompleteEntryById() {
        UniRefIdRequest request = new UniRefIdRequest();
        request.setComplete("true");
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);
        assertNull(result.getPage());
        assertNotNull(result.getEntry());

        UniRefEntry entry = result.getEntry();
        assertEquals(UNIREF_ID_OK, entry.getId().getValue());
        assertEquals("name value", entry.getName());
        assertEquals(UniRefType.UniRef50, entry.getEntryType());
        assertNotNull(entry.getUpdated());
        assertEquals("common taxon value", entry.getCommonTaxon());
        assertEquals(10L, entry.getCommonTaxonId().longValue());
        assertEquals(1, entry.getGoTerms().size());

        assertEquals(28, entry.getMemberCount().intValue());
        assertEquals(27, entry.getMembers().size());

        assertEquals(REPRESENTATIVE_ID, entry.getRepresentativeMember().getMemberId());
        UniRefMember seedMember =
                entry.getMembers().stream()
                        .filter(UniRefMember::isSeed)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(SEED_ID, seedMember.getMemberId());
    }

    @Test
    void getFirstPageEntryByIdDefault25() {
        UniRefIdRequest request = new UniRefIdRequest();
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);
    }

    @Test
    void getSecondPageEntryById() {
        UniRefIdRequest request = new UniRefIdRequest();
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);
    }

    @Test
    void getLastPageEntryById() {
        UniRefIdRequest request = new UniRefIdRequest();
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);
    }

    private void createLightEntries(UniRefLightStoreClient lightStoreClient) {
        UniRefEntryLightBuilder builderOk =
                new UniRefEntryLightBuilder()
                        .id(UNIREF_ID_OK)
                        .entryType(UniRefType.UniRef50)
                        .name("name value")
                        .updated(LocalDate.now())
                        .goTermsAdd(
                                new GeneOntologyEntryBuilder()
                                        .aspect(GoAspect.COMPONENT)
                                        .id("1")
                                        .build())
                        .commonTaxonId(10L)
                        .commonTaxon("common taxon value")
                        .memberIdTypesAdd(UniRefMemberIdType.UNIPROTKB)
                        .representativeId(REPRESENTATIVE_ID)
                        .membersAdd(REPRESENTATIVE_ID)
                        .seedId(SEED_ID);
        IntStream.range(1, 10)
                .forEach(
                        i -> {
                            builderOk.membersAdd("P1234" + i);
                            builderOk.membersAdd("P4321" + i);
                            builderOk.membersAdd("UniParc" + i);
                        });
        lightStoreClient.saveEntry(builderOk.build());

        UniRefEntryLight wrongEntry =
                new UniRefEntryLightBuilder().id("INVALID_ID").membersAdd("INVALID").build();

        lightStoreClient.saveEntry(wrongEntry);
    }

    private void createMembers(UniRefMemberStoreClient memberStoreClient) {
        IntStream.range(1, 10)
                .forEach(
                        i -> {
                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("P1234" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPROTKB)
                                            .accessionsAdd(
                                                    new UniProtKBAccessionBuilder("P1234" + i)
                                                            .build())
                                            .build());

                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("P4321" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPROTKB)
                                            .accessionsAdd(
                                                    new UniProtKBAccessionBuilder("P4321" + i)
                                                            .build())
                                            .build());

                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("UniParc" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPARC)
                                            .uniparcId(new UniParcIdBuilder("UniParc" + i).build())
                                            .build());
                        });

        memberStoreClient.saveEntry(
                new RepresentativeMemberBuilder()
                        .memberId(REPRESENTATIVE_ID)
                        .memberIdType(UniRefMemberIdType.UNIPROTKB)
                        .accessionsAdd(new UniProtKBAccessionBuilder(REPRESENTATIVE_ID).build())
                        .build());
    }
}
