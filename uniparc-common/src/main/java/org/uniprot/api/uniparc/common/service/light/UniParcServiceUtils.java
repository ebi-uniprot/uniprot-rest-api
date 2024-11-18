package org.uniprot.api.uniparc.common.service.light;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.uniprot.core.util.Utils;

public class UniParcServiceUtils {
    private UniParcServiceUtils() {}

    public static List<String> csvToList(String csv) {
        List<String> list = new ArrayList<>();
        if (Utils.notNullNotEmpty(csv)) {
            list = Arrays.stream(csv.split(",")).map(String::trim).toList();
        }
        return list;
    }
}
