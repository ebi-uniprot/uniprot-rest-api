package org.uniprot.api.uniparc.common.service.filter;

import java.util.List;
import java.util.function.BiFunction;

import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 11/08/2020
 */
public class UniParcDatabaseFilter
        implements BiFunction<UniParcCrossReference, List<String>, Boolean> {

    @Override
    public Boolean apply(UniParcCrossReference xref, List<String> databases) {
        return Utils.nullOrEmpty(databases)
                || (Utils.notNull(xref.getDatabase())
                        && databases.contains(xref.getDatabase().getDisplayName().toLowerCase()));
    }
}
