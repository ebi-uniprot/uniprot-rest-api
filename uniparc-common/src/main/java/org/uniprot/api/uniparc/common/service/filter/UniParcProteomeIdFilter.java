package org.uniprot.api.uniparc.common.service.filter;

import java.util.Objects;
import java.util.function.BiFunction;

import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.util.Utils;

public class UniParcProteomeIdFilter implements BiFunction<UniParcCrossReference, String, Boolean> {

    @Override
    public Boolean apply(UniParcCrossReference xref, String proteomeId) {
        return Objects.isNull(proteomeId) || hasSourceProteome(xref, proteomeId);
    }

    private boolean hasSourceProteome(UniParcCrossReference xref, String proteomeId) {
        return Utils.notNullNotEmpty(xref.getProperties())
                && xref.getProperties().stream()
                        .filter(p -> p.getKey().equals("source"))
                        .anyMatch(p -> p.getValue().contains(proteomeId));
    }
}
