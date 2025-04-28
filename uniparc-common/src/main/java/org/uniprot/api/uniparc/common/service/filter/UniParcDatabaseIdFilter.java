package org.uniprot.api.uniparc.common.service.filter;

import java.util.Objects;
import java.util.function.BiFunction;

import org.apache.commons.lang3.StringUtils;
import org.uniprot.core.uniparc.UniParcCrossReference;

public class UniParcDatabaseIdFilter implements BiFunction<UniParcCrossReference, String, Boolean> {

    @Override
    public Boolean apply(UniParcCrossReference xref, String id) {
        return Objects.isNull(id) || StringUtils.equalsIgnoreCase(id, xref.getId());
    }
}
