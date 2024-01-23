package org.uniprot.api.uniref.common.repository.store;

import static org.junit.jupiter.api.Assertions.*;
import static org.uniprot.api.uniref.common.repository.store.UniRefEntryFacetConfig.*;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import junit.framework.AssertionFailedError;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.uniprot.api.common.exception.ResourceNotFoundException;
import org.uniprot.api.common.repository.search.QueryResult;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.page.impl.CursorPage;
import org.uniprot.api.uniref.common.request.UniRefMemberRequest;
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
    void getFirstPageEntryMembersDefault25AndRepresentativeWithFacets() {
        UniRefMemberRequest request = new UniRefMemberRequest();

        request.setId(UNIREF_ID_OK);
        request.setFacets(UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName());
        QueryResult<UniRefMember> result = repository.getEntryMembers(request);
        assertNotNull(result);

        assertNotNull(result.getPage());
        CursorPage page = (CursorPage) result.getPage();
        assertEquals(0L, page.getOffset().longValue());
        assertEquals(25, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertEquals("3v5g94y9lqs", page.getEncryptedNextCursor());
        assertTrue(page.hasNextPage());

        assertNotNull(result.getFacets());
        List<Facet> facets = new ArrayList<>(result.getFacets());
        assertEquals(1, facets.size());
        Facet memberTypeFacet = facets.get(0);
        assertEquals("Member Types", memberTypeFacet.getLabel());
        assertEquals("member_id_type", memberTypeFacet.getName());
        assertNotNull(memberTypeFacet.getValues());
        assertEquals(2, memberTypeFacet.getValues().size());
        FacetItem item = memberTypeFacet.getValues().get(1);
        assertEquals("UniParc", item.getLabel());
        assertEquals("uniparc", item.getValue());
        assertEquals(9, item.getCount());

        assertNotNull(result.getContent());
        List<UniRefMember> members = result.getContent().collect(Collectors.toList());
        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertEquals(25, members.size());

        UniRefMember seedMember =
                members.stream()
                        .filter(uniRefMember -> uniRefMember.isSeed() != null)
                        .filter(UniRefMember::isSeed)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(SEED_ID, seedMember.getMemberId());

        RepresentativeMember repMember =
                members.stream()
                        .filter(uniRefMember -> uniRefMember instanceof RepresentativeMember)
                        .map(member -> (RepresentativeMember) member)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(REPRESENTATIVE_ID, repMember.getMemberId());
    }

    @Test
    void getMembersEntryMembersFilteringFacet() {
        UniRefMemberRequest request = new UniRefMemberRequest();

        request.setId(UNIREF_ID_OK);
        request.setFacets(UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName());
        request.setFacetFilter(UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName() + ":uniparc");
        QueryResult<UniRefMember> result = repository.getEntryMembers(request);
        assertNotNull(result);

        assertNotNull(result.getPage());
        CursorPage page = (CursorPage) result.getPage();
        assertEquals(0L, page.getOffset().longValue());
        assertEquals(25, page.getPageSize().intValue());
        assertEquals(9L, page.getTotalElements().longValue());
        assertFalse(page.hasNextPage());

        assertNotNull(result.getFacets());
        List<Facet> facets = new ArrayList<>(result.getFacets());
        assertEquals(1, facets.size());
        Facet memberTypeFacet = facets.get(0);
        assertEquals("Member Types", memberTypeFacet.getLabel());
        assertEquals("member_id_type", memberTypeFacet.getName());
        assertNotNull(memberTypeFacet.getValues());
        assertEquals(1, memberTypeFacet.getValues().size());
        FacetItem item = memberTypeFacet.getValues().get(0);
        assertEquals("UniParc", item.getLabel());
        assertEquals("uniparc", item.getValue());
        assertEquals(9, item.getCount());

        assertNotNull(result.getContent());
        List<UniRefMember> members = result.getContent().collect(Collectors.toList());
        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertEquals(9, members.size());

        List<UniRefMemberIdType> memberTypes =
                members.stream()
                        .map(UniRefMember::getMemberIdType)
                        .filter(type -> type.equals(UniRefMemberIdType.UNIPARC))
                        .collect(Collectors.toList());

        assertEquals(9, memberTypes.size());
    }

    @Test
    void getFirstPageEntryMembersByIdWithSize10() {
        UniRefMemberRequest request = new UniRefMemberRequest();
        request.setSize(10);
        request.setId(UNIREF_ID_OK);
        QueryResult<UniRefMember> result = repository.getEntryMembers(request);
        assertNotNull(result);

        assertNotNull(result.getPage());
        CursorPage page = (CursorPage) result.getPage();
        assertEquals(0L, page.getOffset().longValue());
        assertEquals(10, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertEquals("3sbq7rwffis", page.getEncryptedNextCursor());
        assertTrue(page.hasNextPage());

        List<UniRefMember> members = result.getContent().collect(Collectors.toList());
        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertEquals(10, members.size());

        UniRefMember seedMember =
                members.stream()
                        .filter(uniRefMember -> uniRefMember.isSeed() != null)
                        .filter(UniRefMember::isSeed)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(SEED_ID, seedMember.getMemberId());

        RepresentativeMember repMember =
                members.stream()
                        .filter(uniRefMember -> uniRefMember instanceof RepresentativeMember)
                        .map(member -> (RepresentativeMember) member)
                        .findFirst()
                        .orElseThrow(AssertionFailedError::new);
        assertEquals(REPRESENTATIVE_ID, repMember.getMemberId());

        List<String> memberIds =
                members.stream().map(UniRefMember::getMemberId).collect(Collectors.toList());

        assertTrue(memberIds.contains("P21802"));

        assertTrue(memberIds.contains("P12341"));
        assertTrue(memberIds.contains("P43211"));
        assertTrue(memberIds.contains("UniParc1"));

        assertTrue(memberIds.contains("P12342"));
        assertTrue(memberIds.contains("P43212"));
        assertTrue(memberIds.contains("UniParc2"));

        assertTrue(memberIds.contains("P12343"));
        assertTrue(memberIds.contains("P43213"));
        assertTrue(memberIds.contains("UniParc3"));
    }

    @Test
    void getSecondPageEntryMembersByIdWithSize10OnlyMembers() {
        UniRefMemberRequest request = new UniRefMemberRequest();
        request.setSize(10);
        request.setCursor("3sbq7rwffis");
        request.setId(UNIREF_ID_OK);
        QueryResult<UniRefMember> result = repository.getEntryMembers(request);
        assertNotNull(result);

        assertNotNull(result.getPage());
        CursorPage page = (CursorPage) result.getPage();
        assertEquals(10L, page.getOffset().longValue());
        assertEquals(10, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertEquals("3v3i3le3dck", page.getEncryptedNextCursor());
        assertTrue(page.hasNextPage());

        List<UniRefMember> members = result.getContent().collect(Collectors.toList());
        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertEquals(10, members.size());

        List<String> memberIds =
                members.stream().map(UniRefMember::getMemberId).collect(Collectors.toList());

        assertTrue(memberIds.contains("P12344"));
        assertTrue(memberIds.contains("P43214"));
        assertTrue(memberIds.contains("UniParc4"));

        assertTrue(memberIds.contains("P12345"));
        assertTrue(memberIds.contains("P43215"));
        assertTrue(memberIds.contains("UniParc5"));

        assertTrue(memberIds.contains("P12346"));
        assertTrue(memberIds.contains("P43216"));
        assertTrue(memberIds.contains("UniParc6"));

        assertTrue(memberIds.contains("P12347"));
    }

    @Test
    void getLastPageEntryMembersById() {
        UniRefMemberRequest request = new UniRefMemberRequest();
        request.setSize(10);
        request.setCursor("3v3i3le3dck");
        request.setId(UNIREF_ID_OK);
        QueryResult<UniRefMember> result = repository.getEntryMembers(request);
        assertNotNull(result);

        assertNotNull(result.getPage());
        CursorPage page = (CursorPage) result.getPage();
        assertEquals(20L, page.getOffset().longValue());
        assertEquals(10, page.getPageSize().intValue());
        assertEquals(28L, page.getTotalElements().longValue());
        assertFalse(page.hasNextPage());

        List<UniRefMember> members = result.getContent().collect(Collectors.toList());
        assertNotNull(members);
        assertFalse(members.isEmpty());
        assertEquals(8, members.size());

        List<String> memberIds =
                members.stream().map(UniRefMember::getMemberId).collect(Collectors.toList());

        assertTrue(memberIds.contains("P43217"));
        assertTrue(memberIds.contains("UniParc7"));

        assertTrue(memberIds.contains("P12348"));
        assertTrue(memberIds.contains("P43218"));
        assertTrue(memberIds.contains("UniParc8"));

        assertTrue(memberIds.contains("P12349"));
        assertTrue(memberIds.contains("P43219"));
        assertTrue(memberIds.contains("UniParc9"));
    }

    @Test
    void getEntryByIdWorksFine() {
        UniRefEntry entry = repository.getEntryById(UNIREF_ID_OK);
        assertNotNull(entry);
        assertEquals(UNIREF_ID_OK, entry.getId().getValue());
        assertEquals("name value", entry.getName());
        assertEquals(UniRefType.UniRef50, entry.getEntryType());
        assertNotNull(entry.getUpdated());
        assertNotNull(entry.getCommonTaxon());
        assertEquals("common taxon value", entry.getCommonTaxon().getScientificName());
        assertEquals(10L, entry.getCommonTaxon().getTaxonId());
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

        List<String> memberIds =
                entry.getMembers().stream()
                        .filter(Objects::nonNull)
                        .map(UniRefMember::getMemberId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toList());

        assertEquals(27, memberIds.size());
        assertFalse(memberIds.contains(REPRESENTATIVE_ID));
    }

    @Test
    void getEntryByIdNotFound() {
        ResourceNotFoundException error =
                assertThrows(
                        ResourceNotFoundException.class,
                        () -> repository.getEntryById("NOT_FOUND_ID"));
        assertEquals(
                "Unable to get UniRefEntry from store. ClusterId: NOT_FOUND_ID",
                error.getMessage());
    }

    @Test
    void getUniRef90ById() {
        UniRefEntry entry = repository.getEntryById(UNIREF_90_ID);
        assertNotNull(entry);
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
        UniRefEntry entry = repository.getEntryById(UNIREF_REPRESENTATIVE_AS_SEED_ID);
        assertNotNull(entry);

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
                        .representativeMember(createRepresentativeMember(REPRESENTATIVE_ID, null))
                        .membersAdd(REPRESENTATIVE_ID + ",0")
                        .seedId(SEED_ID);
        IntStream.range(1, 10)
                .forEach(
                        i -> {
                            builderOk.membersAdd("P1234" + i + ",0");
                            builderOk.membersAdd("P4321" + i + ",1");
                            builderOk.membersAdd("UniParc" + i + ",3");
                        });
        lightStoreClient.saveEntry(builderOk.build());

        UniRefEntryLight uniref90 =
                new UniRefEntryLightBuilder()
                        .entryType(UniRefType.UniRef90)
                        .id(UNIREF_90_ID)
                        .membersAdd(REPRESENTATIVE_ID + ",0")
                        .membersAdd(SEED_ID + ",0")
                        .representativeMember(createRepresentativeMember(REPRESENTATIVE_ID, null))
                        .seedId(SEED_ID)
                        .build();
        lightStoreClient.saveEntry(uniref90);

        UniRefEntryLight repreSeed =
                new UniRefEntryLightBuilder()
                        .entryType(UniRefType.UniRef100)
                        .id(UNIREF_REPRESENTATIVE_AS_SEED_ID)
                        .seedId(SEED_ID)
                        .representativeMember(createRepresentativeMember(SEED_ID, true))
                        .membersAdd(SEED_ID + ",0")
                        .build();
        lightStoreClient.saveEntry(repreSeed);
    }

    private RepresentativeMember createRepresentativeMember(String memberId, Boolean isSeed) {
        return new RepresentativeMemberBuilder()
                .memberIdType(UniRefMemberIdType.UNIPROTKB_SWISSPROT)
                .memberId(memberId)
                .accessionsAdd(new UniProtKBAccessionBuilder(memberId).build())
                .isSeed(isSeed)
                .sequence(new SequenceBuilder("AAAAAA").build())
                .build();
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
                                            .memberIdType(UniRefMemberIdType.UNIPROTKB_TREMBL)
                                            .sequence(new SequenceBuilder("BBBBB").build())
                                            .accessionsAdd(
                                                    new UniProtKBAccessionBuilder("P4321" + i)
                                                            .build())
                                            .build());

                            memberStoreClient.saveEntry(
                                    new RepresentativeMemberBuilder()
                                            .memberId("UniParc" + i)
                                            .memberIdType(UniRefMemberIdType.UNIPARC)
                                            .sequence(new SequenceBuilder("CCCCC").build())
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
