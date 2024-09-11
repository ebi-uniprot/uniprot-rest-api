package org.uniprot.api.uniparc.common.service.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Objects;

import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.util.Utils;

public class UniParcServiceUtils {
    private UniParcServiceUtils() {}

    public static boolean filterByDatabases(UniParcCrossReference xref, List<String> databases) {
        if (Utils.nullOrEmpty(databases)) {
            return true;
        }

        return Objects.nonNull(xref.getDatabase())
                && databases.contains(xref.getDatabase().getDisplayName().toLowerCase());
    }

    public static boolean filterByTaxonomyIds(
            UniParcCrossReference xref, List<String> taxonomyIds) {
        if (Utils.nullOrEmpty(taxonomyIds)) {
            return true;
        }

        return Objects.nonNull(xref.getOrganism())
                && taxonomyIds.contains(String.valueOf(xref.getOrganism().getTaxonId()));
    }

    public static boolean filterByStatus(UniParcCrossReference xref, Boolean isActive) {
        if (isActive == null) {
            return true;
        }
        return Objects.nonNull(xref.getDatabase()) && Objects.equals(isActive, xref.isActive());
    }

    public static List<String> csvToList(String csv) {
        List<String> list = new ArrayList<>();
        if (Utils.notNullNotEmpty(csv)) {
            list = Arrays.stream(csv.split(",")).map(String::trim).toList();
        }
        return list;
    }
}
