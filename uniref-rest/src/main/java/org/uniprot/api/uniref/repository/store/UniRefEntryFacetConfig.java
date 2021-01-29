package org.uniprot.api.uniref.repository.store;

import static org.uniprot.api.common.repository.search.facet.FacetUtils.getCleanFacetValue;
import static org.uniprot.store.search.SolrQueryUtil.*;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;
import org.uniprot.api.common.repository.search.facet.*;
import org.uniprot.core.uniref.UniRefMemberIdType;
import org.uniprot.core.util.Utils;

/**
 * @author lgonzales
 * @since 26/08/2020
 */
@Component
public class UniRefEntryFacetConfig extends FacetConfig {

    private static final String UNIPROTKB_FILTER_VALUE =
            getCleanFacetValue(UniRefMemberIdType.UNIPROTKB.getDisplayName());
    private static final String UNIPARC_FILTER_VALUE =
            getCleanFacetValue(UniRefMemberIdType.UNIPARC.getDisplayName());

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
        return result;
    }

    static List<Facet> getFacets(List<String> members, String facetNames) {
        List<Facet> results = new ArrayList<>();
        if (Utils.notNullNotEmpty(facetNames)
                && facetNames.contains(UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName())) {
            Facet memberType =
                    Facet.builder()
                            .label(UniRefEntryFacet.MEMBER_ID_TYPE.getLabel())
                            .name(UniRefEntryFacet.MEMBER_ID_TYPE.getFacetName())
                            .allowMultipleSelection(false)
                            .values(getMemberTypeValues(members))
                            .build();
            if (Utils.notNullNotEmpty(memberType.getValues())) {
                results.add(memberType);
            }
        }

        if (Utils.notNullNotEmpty(facetNames)
                && facetNames.contains(UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName())) {
            Facet uniProtMemberType =
                    Facet.builder()
                            .label(UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getLabel())
                            .name(UniRefEntryFacet.UNIPROT_MEMBER_ID_TYPE.getFacetName())
                            .allowMultipleSelection(false)
                            .values(getUniProtMemberTypeValues(members))
                            .build();
            if (Utils.notNullNotEmpty(uniProtMemberType.getValues())) {
                results.add(uniProtMemberType);
            }
        }

        return results;
    }

    /**
     * This method return true if member should be removed from list and false otherwise For
     * UniprotKb filter we must not remove Trembl or SwissProt
     *
     * @param filterValue requested filter value
     * @param memberValue member value with the format "databaseId,memberTypeId"
     * @return true if member should be removed from list
     */
    private static boolean filterMemberTypeValue(String filterValue, String memberValue) {
        String memberTypeId = memberValue.split(",")[1];
        UniRefMemberIdType memberType = UniRefMemberIdType.fromMemberTypeId(memberTypeId);

        boolean shouldRemove = true;
        if (UNIPROTKB_FILTER_VALUE.equalsIgnoreCase(filterValue)) {
            shouldRemove = !isMemberTypeTremblOrSwissProt(memberType);
        } else if (UNIPARC_FILTER_VALUE.equalsIgnoreCase(filterValue)) {
            shouldRemove = !memberType.equals(UniRefMemberIdType.UNIPARC);
        }
        return shouldRemove;
    }

    private static boolean isMemberTypeTremblOrSwissProt(UniRefMemberIdType memberType) {
        return memberType.equals(UniRefMemberIdType.UNIPROTKB_SWISSPROT)
                || memberType.equals(UniRefMemberIdType.UNIPROTKB_TREMBL);
    }

    private static boolean filterUniProtMemberTypeValue(String filterValue, String memberValue) {
        String memberTypeId = memberValue.split(",")[1];
        UniRefMemberIdType memberType = UniRefMemberIdType.fromMemberTypeId(memberTypeId);

        String cleanFilterValue = getCleanFacetValue(memberType.getDisplayName());
        return !cleanFilterValue.equalsIgnoreCase(filterValue.trim());
    }

    private static List<FacetItem> getUniProtMemberTypeValues(List<String> members) {
        Map<String, Long> mappedItems =
                members.stream()
                        .map(member -> member.split(",")[1])
                        .map(UniRefMemberIdType::fromMemberTypeId)
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
                        .map(UniRefMemberIdType::fromMemberTypeId)
                        .map(UniRefMemberIdType::getDisplayName)
                        .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));

        return FacetUtils.buildFacetItems(mappedItems);
    }

    private static String mapTremblAndSwissProtToUniProt(String id) {
        int memberIdTypeId = Integer.parseInt(id);
        if (memberIdTypeId == UniRefMemberIdType.UNIPROTKB_SWISSPROT.getMemberIdTypeId()
                || memberIdTypeId == UniRefMemberIdType.UNIPROTKB_TREMBL.getMemberIdTypeId()) {
            memberIdTypeId = UniRefMemberIdType.UNIPROTKB.getMemberIdTypeId();
        }
        return String.valueOf(memberIdTypeId);
    }
}
