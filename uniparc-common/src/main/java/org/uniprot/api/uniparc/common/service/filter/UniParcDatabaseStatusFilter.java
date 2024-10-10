package org.uniprot.api.uniparc.common.service.filter;

import java.util.Objects;
import java.util.function.BiFunction;

import org.uniprot.core.uniparc.UniParcCrossReference;

/**
 * @author sahmad
 * @created 11/08/2020
 */
public class UniParcDatabaseStatusFilter
        implements BiFunction<UniParcCrossReference, Boolean, Boolean> {

    @Override
    public Boolean apply(UniParcCrossReference xref, Boolean isActive) {
        return Objects.isNull(isActive) || Objects.equals(isActive, xref.isActive());
    }
}
