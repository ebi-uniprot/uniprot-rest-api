package org.uniprot.api.uniparc.service;

import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 11/08/2020
 */
public class UniParcDatabaseStatusFilter
        implements BiFunction<UniParcEntry, Boolean, UniParcEntry> {

    @Override
    public UniParcEntry apply(UniParcEntry uniParcEntry, Boolean isActive) {
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        if (Utils.notNullNotEmpty(xrefs) && Objects.nonNull(isActive)) {
            List<UniParcCrossReference> filteredRefs =
                    xrefs.stream()
                            .filter(xref -> Objects.nonNull(xref.getDatabase()))
                            .filter(xref -> Objects.equals(isActive, xref.isActive()))
                            .collect(Collectors.toList());
            builder.uniParcCrossReferencesSet(filteredRefs);
        }
        return builder.build();
    }
}
