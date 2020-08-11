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
public class UniParcDatabaseFilter implements BiFunction<UniParcEntry, List<String>, UniParcEntry> {

    @Override
    public UniParcEntry apply(UniParcEntry uniParcEntry, List<String> databases) {
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(uniParcEntry);
        List<UniParcCrossReference> xrefs = uniParcEntry.getUniParcCrossReferences();
        if (Utils.notNullNotEmpty(xrefs) && Utils.notNullNotEmpty(databases)) {
            List<UniParcCrossReference> filteredRefs =
                    xrefs.stream()
                            .filter(xref -> Objects.nonNull(xref.getDatabase()))
                            .filter(xref -> databases.contains(xref.getDatabase().getDisplayName()))
                            .collect(Collectors.toList());
            builder.uniParcCrossReferencesSet(filteredRefs);
        }
        return builder.build();
    }
}
