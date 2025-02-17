package org.uniprot.api.uniparc.common.service.filter;

import org.uniprot.core.uniparc.UniParcCrossReference;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * @author sahmad
 * @created 11/08/2020
 */
public class UniParcDatabaseIdFilter
        implements BiFunction<UniParcCrossReference, String, Boolean> {

    @Override
    public Boolean apply(UniParcCrossReference xref, String id) {
        return Objects.isNull(id) || Objects.equals(id, xref.getId());
    }
}
