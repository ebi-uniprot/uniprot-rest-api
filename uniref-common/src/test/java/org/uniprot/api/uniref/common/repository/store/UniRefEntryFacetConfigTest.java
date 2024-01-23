package org.uniprot.api.uniref.common.repository.store;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.jetbrains.annotations.NotNull;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.impl.UniRefEntryLightBuilder;

/**
 * @author lgonzales
 * @since 28/08/2020
 */
class UniRefEntryFacetConfigTest {

    @Test
    void getFacetsEmptyMembers() {
        UniRefEntryLight entryLight = new UniRefEntryLightBuilder().build();
        List<Facet> result =
                UniRefEntryFacetConfig.getFacets(
                        entryLight.getMembers(), "member_id_type,uniprot_member_id_type");
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void getFacetsSuccess() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        List<Facet> result =
                UniRefEntryFacetConfig.getFacets(
                        entryLight.getMembers(), "member_id_type,uniprot_member_id_type");
        assertNotNull(result);
        assertEquals(2, result.size());
        Facet memberType = result.get(0);
        assertEquals("Member Types", memberType.getLabel());
        assertEquals("member_id_type", memberType.getName());
        assertNotNull(memberType.getValues());
        assertEquals(2, memberType.getValues().size());
        FacetItem item = memberType.getValues().get(1);
        assertEquals("UniParc", item.getLabel());
        assertEquals("uniparc", item.getValue());
        assertEquals(2, item.getCount());

        memberType = result.get(1);
        assertEquals("UniProtKB Member Types", memberType.getLabel());
        assertEquals("uniprot_member_id_type", memberType.getName());
        assertNotNull(memberType.getValues());
        assertEquals(2, memberType.getValues().size());
        item = memberType.getValues().get(1);
        assertEquals("UniProtKB Reviewed (Swiss-Prot)", item.getLabel());
        assertEquals("uniprotkb_reviewed_swissprot", item.getValue());
        assertEquals(2, item.getCount());
    }

    @Test
    void applyMemberFilterInvalidValueReturnEmptyMembers() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        String query =
                UniRefEntryFacetConfig.UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName() + ":invalid";
        List<String> result =
                UniRefEntryFacetConfig.applyFacetFilters(entryLight.getMembers(), query);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @Test
    void applyMemberFilterUniParc() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        String query =
                UniRefEntryFacetConfig.UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName() + ":uniparc";
        List<String> result =
                UniRefEntryFacetConfig.applyFacetFilters(entryLight.getMembers(), query);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("300,3", result.get(0));
        assertEquals("301,3", result.get(1));
    }

    @Test
    void applyMemberFilterUniProtKb() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        String query =
                UniRefEntryFacetConfig.UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName()
                        + ":uniprotkb_id";
        List<String> result =
                UniRefEntryFacetConfig.applyFacetFilters(entryLight.getMembers(), query);
        assertNotNull(result);
        assertEquals(4, result.size());
        assertEquals("000,0", result.get(0));
        assertEquals("001,0", result.get(1));
        assertEquals("100,1", result.get(2));
        assertEquals("101,1", result.get(3));
    }

    @Test
    void applyUniProtMemberFilterSwissProt() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        String query =
                UniRefEntryFacetConfig.UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName()
                        + ":uniprotkb_reviewed_swissprot";
        List<String> result =
                UniRefEntryFacetConfig.applyFacetFilters(entryLight.getMembers(), query);
        assertNotNull(result);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("000,0", result.get(0));
        assertEquals("001,0", result.get(1));
    }

    @Test
    void applyUniProtMemberFilterTrembl() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        String query =
                UniRefEntryFacetConfig.UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName()
                        + ":uniprotkb_unreviewed_trembl";
        List<String> result =
                UniRefEntryFacetConfig.applyFacetFilters(entryLight.getMembers(), query);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("100,1", result.get(0));
        assertEquals("101,1", result.get(1));
    }

    @Test
    void applyMemberFilterUniProtKbAndUniProtMemberSwissProt() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        String query =
                UniRefEntryFacetConfig.UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName()
                        + ":uniprotkb_id AND "
                        + UniRefEntryFacetConfig.UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE
                                .getFacetName()
                        + ":uniprotkb_reviewed_swissprot";
        List<String> result =
                UniRefEntryFacetConfig.applyFacetFilters(entryLight.getMembers(), query);
        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals("000,0", result.get(0));
        assertEquals("001,0", result.get(1));
    }

    @Test
    void getFacetNamesReturnSuccess() {
        UniRefEntryFacetConfig facetConfig = new UniRefEntryFacetConfig();
        Collection<String> result = facetConfig.getFacetNames();
        assertNotNull(result);
        assertEquals(2, result.size());
        assertTrue(result.contains("member_id_type"));
        assertTrue(result.contains("uniprot_member_id_type"));
    }

    @Test
    void getFacetPropertyMapReturnNull() {
        UniRefEntryFacetConfig facetConfig = new UniRefEntryFacetConfig();
        Map<String, FacetProperty> result = facetConfig.getFacetPropertyMap();
        assertNull(result);
    }

    @Test
    void getUniProtMemberIdTypeFacetsSuccess() {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        List<Facet> result =
                UniRefEntryFacetConfig.getFacets(entryLight.getMembers(), "uniprot_member_id_type");
        assertNotNull(result);
        assertEquals(1, result.size());
        Facet memberType = result.get(0);
        assertEquals("UniProtKB Member Types", memberType.getLabel());
        assertEquals("uniprot_member_id_type", memberType.getName());
        assertNotNull(memberType.getValues());
        assertEquals(2, memberType.getValues().size());
        FacetItem item = memberType.getValues().get(1);
        assertEquals("UniProtKB Reviewed (Swiss-Prot)", item.getLabel());
        assertEquals("uniprotkb_reviewed_swissprot", item.getValue());
        assertEquals(2, item.getCount());
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = " ")
    void getFacetsEmptySuccess(String facetNames) {
        UniRefEntryLight entryLight = getUniRefEntryLight();
        List<Facet> result = UniRefEntryFacetConfig.getFacets(entryLight.getMembers(), facetNames);
        assertNotNull(result);
        assertTrue(result.isEmpty());
    }

    @NotNull
    private UniRefEntryLight getUniRefEntryLight() {
        return new UniRefEntryLightBuilder()
                .membersAdd("000,0")
                .membersAdd("001,0")
                .membersAdd("100,1")
                .membersAdd("101,1")
                .membersAdd("300,3")
                .membersAdd("301,3")
                .build();
    }
}
