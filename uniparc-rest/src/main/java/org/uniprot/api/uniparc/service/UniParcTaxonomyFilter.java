package org.uniprot.api.uniparc.service;

import java.util.List;
import java.util.function.BiFunction;
import java.util.stream.Collectors;

import org.uniprot.core.uniparc.UniParcEntry;
import org.uniprot.core.uniparc.impl.UniParcEntryBuilder;
import org.uniprot.core.uniprotkb.taxonomy.Taxonomy;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 11/08/2020
 */
public class UniParcTaxonomyFilter implements BiFunction<UniParcEntry, List<String>, UniParcEntry> {

    @Override
    public UniParcEntry apply(UniParcEntry uniParcEntry, List<String> taxonIds) {
        UniParcEntryBuilder builder = UniParcEntryBuilder.from(uniParcEntry);
        List<Taxonomy> taxonomies = uniParcEntry.getTaxonomies();
        if (Utils.notNullNotEmpty(taxonomies) && Utils.notNullNotEmpty(taxonIds)) {
            List<Taxonomy> filteredTaxons =
                    taxonomies.stream()
                            .filter(
                                    taxonomy ->
                                            taxonIds.contains(
                                                    String.valueOf(taxonomy.getTaxonId())))
                            .collect(Collectors.toList());
            builder.taxonomiesSet(filteredTaxons);
        }
        return builder.build();
    }
}
