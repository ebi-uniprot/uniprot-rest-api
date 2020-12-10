package org.uniprot.api.uniref.repository.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import java.time.LocalDate;
import java.util.stream.IntStream;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniref.request.UniRefIdRequest;
import org.uniprot.api.uniref.service.UniRefEntryResult;
import org.uniprot.core.cv.go.GoAspect;
import org.uniprot.core.cv.go.impl.GeneOntologyEntryBuilder;
import org.uniprot.core.impl.SequenceBuilder;
import org.uniprot.core.uniparc.impl.UniParcIdBuilder;
import org.uniprot.core.uniprotkb.impl.UniProtKBAccessionBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Organism;
import org.uniprot.core.uniprotkb.taxonomy.impl.OrganismBuilder;
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
    public static final String UNIREF_REPRESENTATIVE_AS_SEED_ID = "UniRef50_P11111";
    public static final String REPRESENTATIVE_ID = "P21802";
    public static final String SEED_ID = "P12342";
    private static final String UNIREF_90_ID = "UniRef90_P21802";
    private UniRefEntryStoreRepository repository;

    @BeforeAll
    void setup() {
        VoldemortInMemoryUniRefMemberStore memberStore =
                VoldemortInMemoryUniRefMemberStore.getInstance("uniref-member");
        UniRefMemberStoreClient memberStoreClient = new UniRefMemberStoreClient(memberStore, 5);
        createMembers(memberStoreClient);

        VoldemortInMemoryUniRefEntryLightStore lightStore =
                VoldemortInMemoryUniRefEntryLightStore.getInstance("uniref-light");
        UniRefLightStoreClient lightStoreClient = new UniRefLightStoreClient(lightStore);
        createLightEntries(lightStoreClient);

        UniRefLightStoreConfigProperties lightStoreConfig = new UniRefLightStoreConfigProperties();
        lightStoreConfig.setFetchMaxRetries(1);
        lightStoreConfig.setFetchRetryDelayMillis(100);

        UniRefMemberStoreConfigProperties memberStoreConfig =
                new UniRefMemberStoreConfigProperties();
        memberStoreConfig.setFetchMaxRetries(1);
        memberStoreConfig.setFetchRetryDelayMillis(100);
        repository =
                new UniRefEntryStoreRepository(
                        memberStoreClient, memberStoreConfig, lightStoreClient, lightStoreConfig);
        repository.defaultPageSize = 25;
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

        assertEquals(28, entry.getMemberCount().intValue()); // 27+representative
        assertEquals(27, entry.getMembers().size());

        assertEquals(REPRESENTATIVE_ID, entry.getRepresentativeMember().getMemberId());
        assertNotNull(entry.getRepresentativeMember().getSequence());
        UniRefMember seedMember =
                entry.getMembers().stream()
                        .filter(uniRefMember -> uniRefMember.isSeed() != null)
                        .filter(UniRefMember::isSeed)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(SEED_ID, seedMember.getMemberId());
    }

    @Test
    void getFirstPageEntryByIdDefault25WithCommonDataAndRepresentative() {
        UniRefIdRequest request = new UniRefIdRequest();
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);
        assertNotNull(result.getPage());
        CursorPage page = result.getPage();
        assertEquals(0L, page.getOffset().longValue());
        assertEquals(25, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertEquals("3v5g94y9lqs", page.getEncryptedNextCursor());
        assertTrue(page.hasNextPage());

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
        assertEquals(24, entry.getMembers().size());

        assertEquals(REPRESENTATIVE_ID, entry.getRepresentativeMember().getMemberId());
        UniRefMember seedMember =
                entry.getMembers().stream()
                        .filter(uniRefMember -> uniRefMember.isSeed() != null)
                        .filter(UniRefMember::isSeed)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(SEED_ID, seedMember.getMemberId());
    }

    @Test
    void getFirstPageEntryByIdWithSize10() {
        UniRefIdRequest request = new UniRefIdRequest();
        request.setSize(10);
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);
        assertNotNull(result.getPage());
        CursorPage page = result.getPage();
        assertEquals(0L, page.getOffset().longValue());
        assertEquals(10, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertEquals("3sbq7rwffis", page.getEncryptedNextCursor());
        assertTrue(page.hasNextPage());

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
        assertEquals(9, entry.getMembers().size());

        assertEquals(REPRESENTATIVE_ID, entry.getRepresentativeMember().getMemberId());
        UniRefMember seedMember =
                entry.getMembers().stream()
                        .filter(uniRefMember -> uniRefMember.isSeed() != null)
                        .filter(UniRefMember::isSeed)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(SEED_ID, seedMember.getMemberId());
    }

    @Test
    void getSecondPageEntryByIdWithSize10OnlyMembers() {
        UniRefIdRequest request = new UniRefIdRequest();
        request.setSize(10);
        request.setCursor("3sbq7rwffis");
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);

        assertNotNull(result);
        assertNotNull(result.getPage());
        CursorPage page = result.getPage();
        assertEquals(10L, page.getOffset().longValue());
        assertEquals(10, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertEquals("3v3i3le3dck", page.getEncryptedNextCursor());
        assertTrue(page.hasNextPage());

        assertNotNull(result.getEntry());

        UniRefEntry entry = result.getEntry();
        assertNotNull(entry);
        assertNull(entry.getId());
        assertNull(entry.getName());
        assertNull(entry.getEntryType());
        assertNull(entry.getUpdated());
        assertNull(entry.getCommonTaxon());
        assertNull(entry.getCommonTaxonId());
        assertTrue(entry.getGoTerms().isEmpty());
        assertNull(entry.getRepresentativeMember());
        assertNull(entry.getMemberCount());
        assertEquals(10, entry.getMembers().size());
    }

    @Test
    void getLastPageEntryById() {
        UniRefIdRequest request = new UniRefIdRequest();
        request.setSize(10);
        request.setCursor("3v3i3le3dck");
        UniRefEntryResult result = repository.getEntryById(UNIREF_ID_OK, request);
        assertNotNull(result);

        assertNotNull(result);
        assertNotNull(result.getPage());
        CursorPage page = result.getPage();
        assertEquals(20L, page.getOffset().longValue());
        assertEquals(10, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertFalse(page.hasNextPage());

        assertNotNull(result.getEntry());

        UniRefEntry entry = result.getEntry();
        assertNotNull(entry);
        assertNull(entry.getId());
        assertNull(entry.getName());
        assertNull(entry.getEntryType());
        assertNull(entry.getUpdated());
        assertNull(entry.getCommonTaxon());
        assertNull(entry.getCommonTaxonId());
        assertTrue(entry.getGoTerms().isEmpty());
        assertNull(entry.getRepresentativeMember());
        assertNull(entry.getMemberCount());
        assertEquals(8, entry.getMembers().size());
    }

    @Test
    void getNotFoundEntryLight() {
        UniRefIdRequest request = new UniRefIdRequest();
        ResourceNotFoundException error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> repository.getEntryById("NOT_FOUND_ID", request));
        assertEquals(
                "Unable to get UniRefEntry from store. ClusterId: NOT_FOUND_ID",
                error.getMessage());
    }

    @Test
    void getUniRef90ById() {
        UniRefIdRequest request = new UniRefIdRequest();
        UniRefEntryResult result = repository.getEntryById(UNIREF_90_ID, request);
        assertNotNull(result);
        assertNotNull(result.getPage());
        assertFalse(result.getPage().hasNextPage());

        UniRefEntry entry = result.getEntry();
        assertEquals(UNIREF_90_ID, entry.getId().getValue());

        assertEquals(2, entry.getMemberCount().intValue());
        assertEquals(1, entry.getMembers().size());

        assertEquals(REPRESENTATIVE_ID, entry.getRepresentativeMember().getMemberId());
        assertNull(entry.getRepresentativeMember().isSeed());
        assertNull(entry.getRepresentativeMember().getUniRef90Id());

        UniRefMember seedMember = entry.getMembers().get(0);
        assertNotNull(seedMember);

        assertEquals(SEED_ID, seedMember.getMemberId());
        assertTrue(seedMember.isSeed());
        assertNull(seedMember.getUniRef90Id());
    }

    @Test
    void getRepresentativeSeedEntryById() {
        UniRefIdRequest request = new UniRefIdRequest();
        UniRefEntryResult result =
                repository.getEntryById(UNIREF_REPRESENTATIVE_AS_SEED_ID, request);
        assertNotNull(result);

        UniRefEntry entry = result.getEntry();
        assertEquals(UNIREF_REPRESENTATIVE_AS_SEED_ID, entry.getId().getValue());

        assertEquals(1, entry.getMemberCount().intValue());
        assertEquals(0, entry.getMembers().size());

        assertEquals(SEED_ID, entry.getRepresentativeMember().getMemberId());
        assertEquals(true, entry.getRepresentativeMember().isSeed());
    }

    private void createLightEntries(UniRefLightStoreClient lightStoreClient) {
        Organism commonOrganism =
                new OrganismBuilder().taxonId(10L).scientificName("common taxon value").build();
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
                        .commonTaxon(commonOrganism)
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

        UniRefEntryLight uniref90 =
                new UniRefEntryLightBuilder()
                        .entryType(UniRefType.UniRef90)
                        .id(UNIREF_90_ID)
                        .membersAdd(REPRESENTATIVE_ID)
                        .membersAdd(SEED_ID)
                        .representativeId(REPRESENTATIVE_ID)
                        .seedId(SEED_ID)
                        .build();
        lightStoreClient.saveEntry(uniref90);

        UniRefEntryLight repreSeed =
                new UniRefEntryLightBuilder()
                        .entryType(UniRefType.UniRef100)
                        .id(UNIREF_REPRESENTATIVE_AS_SEED_ID)
                        .seedId(SEED_ID)
                        .representativeId(SEED_ID)
                        .membersAdd(SEED_ID)
                        .build();
        lightStoreClient.saveEntry(repreSeed);
    }

    private void createMembers(UniRefMemberStoreClient memberStoreClient) {
        IntStream.range(1, 10)
                .forEach(
                        i -> {
                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("P1234" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPROTKB)
                                            .sequence(new SequenceBuilder("AAAAA").build())
                                            .accessionsAdd(
                                                    new UniProtKBAccessionBuilder("P1234" + i)
                                                            .build())
                                            .build());

                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("P4321" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPROTKB)
                                            .sequence(new SequenceBuilder("AAAAA").build())
                                            .accessionsAdd(
                                                    new UniProtKBAccessionBuilder("P4321" + i)
                                                            .build())
                                            .build());

                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("UniParc" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPARC)
                                            .sequence(new SequenceBuilder("AAAAA").build())
                                            .uniparcId(new UniParcIdBuilder("UniParc" + i).build())
                                            .build());
                        });

        memberStoreClient.saveEntry(
                new RepresentativeMemberBuilder()
                        .memberId(REPRESENTATIVE_ID)
                        .memberIdType(UniRefMemberIdType.UNIPROTKB)
                        .sequence(new SequenceBuilder("AAAAA").build())
                        .accessionsAdd(new UniProtKBAccessionBuilder(REPRESENTATIVE_ID).build())
                        .build());
    }
}
