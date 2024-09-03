package org.uniprot.api.uniparc.common.service.filter;

import java.util.List;
import java.util.function.BiFunction;

import org.uniprot.core.uniparc.UniParcCrossReference;

/**
 * @author lgonzales
 * @since 14/08/2020
 */
public class UniParcCrossReferenceTaxonomyFilter
        implements BiFunction<UniParcCrossReference, List<String>, Boolean> {

    @Override
    public Boolean apply(UniParcCrossReference xref, List<String> taxonomyIds) {
        return taxonomyIds.isEmpty()
                || taxonomyIds.contains(String.valueOf(xref.getOrganism().getTaxonId()));
    }
}
