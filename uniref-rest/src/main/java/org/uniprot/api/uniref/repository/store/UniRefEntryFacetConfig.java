package org.uniprot.api.uniref.repository.store;

import static org.uniprot.api.common.repository.search.facet.FacetUtils.getCleanFacetValue;
import static org.uniprot.store.search.SolrQueryUtil.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.*;
import org.uniprot.core.uniref.UniRefEntryLight;
import org.uniprot.core.uniref.UniRefMemberIdType;
import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 26/08/2020
 */
@Component
public class UniRefEntryFacetConfig extends FacetConfig {

    enum UniRefEntryFacet {
        MEMBER_ID_TYPE("member_id_type", "Member Types"),
        UNIPROT_MEMBER_ID_TYPE("uniprot_member_id_type", "UniProtKB Member Types");

        private final String label;
        private final String facetName;

        UniRefEntryFacet(String facetName, String label) {
            this.facetName = facetName;
            this.label = label;
        }

        public String getLabel() {
            return label;
        }

        public String getFacetName() {
            return facetName;
        }
    }

    @Override
    public Collection<String> getFacetNames() {
        return Arrays.stream(UniRefEntryFacet.values())
                .map(UniRefEntryFacet::getFacetName)
                .collect(Collectors.toList());
    }

    @Override
    public Map<String, FacetProperty> getFacetPropertyMap() {
        return null;
    }

    static List<String> applyFacetFilters(List<String> members, String filter) {
        List<String> result = new ArrayList<>(members);
        if (Utils.notNullNotEmpty(filter)) {
            if (hasFieldTerms(filter, UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName())) {
                String filterValue =
                        getTermValue(filter, UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName());
                result.removeIf(memberValue -> filterMemberTypeValue(filterValue, memberValue));
            }
            if (hasFieldTerms(filter, UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName())) {
                String filterValue =
                        getTermValue(
                                filter, UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName());
                result.removeIf(
                        memberValue -> filterUniProtMemberTypeValue(filterValue, memberValue));
            }
        }
        return result.stream().map(memberId -> memberId.split(",")[0]).collect(Collectors.toList());
    }

    static List<Facet> getFacets(UniRefEntryLight uniRefEntryLight) {
        List<Facet> results = new ArrayList<>();
        Facet memberType =
                Facet.builder()
                        .label(UniRefEntryFacet.MEMBER_ID_TYPE.getLabel())
                        .name(UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName())
                        .allowMultipleSelection(false)
                        .values(getMemberTypeValues(uniRefEntryLight.getMembers()))
                        .build();
        if (Utils.notNullNotEmpty(memberType.getValues())) {
            results.add(memberType);
        }

        Facet uniProtMemberType =
                Facet.builder()
                        .label(UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getLabel())
                        .name(UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName())
                        .allowMultipleSelection(false)
                        .values(getUniProtMemberTypeValues(uniRefEntryLight.getMembers()))
                        .build();
        if (Utils.notNullNotEmpty(uniProtMemberType.getValues())) {
            results.add(uniProtMemberType);
        }

        return results;
    }

    private static boolean filterMemberTypeValue(String filterValue, String memberValue) {
        String typeOrder = memberValue.split(",")[1];
        UniRefMemberIdType memberType = UniRefMemberIdType.fromDisplayOrder(typeOrder);

        String cleanUniProt = getCleanFacetValue(UniRefMemberIdType.UNIPROTKB.getDisplayName());
        String cleanUniParc = getCleanFacetValue(UniRefMemberIdType.UNIPARC.getDisplayName());
        if (cleanUniProt.equals(filterValue.trim())) {
            return !(memberType.equals(UniRefMemberIdType.UNIPROTKB_SWISSPROT)
                    || memberType.equals(UniRefMemberIdType.UNIPROTKB_TREMBL));
        } else if (cleanUniParc.equals(filterValue.trim())) {
            return !memberType.equals(UniRefMemberIdType.UNIPARC);
        } else {
            return true;
        }
    }

    private static boolean filterUniProtMemberTypeValue(String filterValue, String memberValue) {
        String typeOrder = memberValue.split(",")[1];
        UniRefMemberIdType memberType = UniRefMemberIdType.fromDisplayOrder(typeOrder);

        String cleanFilterValue = getCleanFacetValue(memberType.getDisplayName());
        return !cleanFilterValue.equalsIgnoreCase(filterValue.trim());
    }

    private static List<FacetItem> getUniProtMemberTypeValues(List<String> members) {
        Map<String, Long> mappedItems =
                members.stream()
                        .map(member -> member.split(",")[1])
                        .map(UniRefMemberIdType::fromDisplayOrder)
                        .filter(memberType -> memberType != UniRefMemberIdType.UNIPARC)
                        .map(UniRefMemberIdType::getDisplayName)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return FacetUtils.buildFacetItems(mappedItems);
    }

    private static List<FacetItem> getMemberTypeValues(List<String> members) {
        Map<String, Long> mappedItems =
                members.stream()
                        .map(member -> member.split(",")[1])
                        .map(UniRefEntryFacetConfig::mapTremblAndSwissProtToUniProt)
                        .map(UniRefMemberIdType::fromDisplayOrder)
                        .map(UniRefMemberIdType::getDisplayName)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return FacetUtils.buildFacetItems(mappedItems);
    }

    private static String mapTremblAndSwissProtToUniProt(String id) {
        int order = Integer.parseInt(id);
        if (order == UniRefMemberIdType.UNIPROTKB_SWISSPROT.getDisplayOrder()
                || order == UniRefMemberIdType.UNIPROTKB_TREMBL.getDisplayOrder()) {
            order = UniRefMemberIdType.UNIPROTKB.getDisplayOrder();
        }
        return String.valueOf(order);
    }
}
