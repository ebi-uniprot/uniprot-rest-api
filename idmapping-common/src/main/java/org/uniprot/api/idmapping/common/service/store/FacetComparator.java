package org.uniprot.api.idmapping.common.service.store;

import java.util.List;
import java.util.Objects;

import org.uniprot.api.common.repository.search.facet.Facet;
import org.uniprot.api.common.repository.search.facet.FacetItem;
// TODO delete this file after testing

public class FacetComparator {
    public static boolean areFacetListsEqual(List<Facet> list1, List<Facet> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            Facet f1 = list1.get(i);
            Facet f2 = list2.get(i);

            if (!areFacetsEqual(f1, f2)) {
                return false;
            }
        }
        return true;
    }

    private static boolean areFacetsEqual(Facet f1, Facet f2) {
        if (f1 == null && f2 == null) return true;
        if (f1 == null || f2 == null) return false;
        if (!Objects.equals(f1.getLabel(), f2.getLabel())) return false;
        if (!Objects.equals(f1.getName(), f2.getName())) return false;
        if (f1.isAllowMultipleSelection() != f2.isAllowMultipleSelection()) return false;

        return areFacetItemsEqual(f1.getValues(), f2.getValues());
    }

    private static boolean areFacetItemsEqual(List<FacetItem> list1, List<FacetItem> list2) {
        if (list1 == null && list2 == null) return true;
        if (list1 == null || list2 == null) return false;
        if (list1.size() != list2.size()) return false;

        for (int i = 0; i < list1.size(); i++) {
            FacetItem item1 = list1.get(i);
            FacetItem item2 = list2.get(i);

            if (!Objects.equals(item1.getLabel(), item2.getLabel())) return false;
            if (!Objects.equals(item1.getValue(), item2.getValue())) return false;
            if (!Objects.equals(item1.getCount(), item2.getCount())) return false;
        }
        return true;
    }
}
