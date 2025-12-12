package org.uniprot.api.uniparc.common.service.filter;

import static org.uniprot.core.uniparc.UniParcCrossReference.PROPERTY_SOURCES;

import java.util.List;
import java.util.Objects;
import java.util.function.BiPredicate;

import org.uniprot.core.Property;
import org.uniprot.core.uniparc.ProteomeIdComponent;
import org.uniprot.core.uniparc.UniParcCrossReference;
import org.uniprot.core.util.Utils;

public class UniParcProteomeIdFilter implements BiPredicate<UniParcCrossReference, String> {

    @Override
    public boolean test(UniParcCrossReference xref, String proteomeId) {
        return Objects.isNull(proteomeId)
                || hasSourceProteome(xref, proteomeId)
                || hasProteomeId(xref, proteomeId);
    }

    private boolean hasSourceProteome(UniParcCrossReference xref, String proteomeId) {
        return Utils.notNullNotEmpty(xref.getProperties())
                && xref.getProperties().stream()
                        .filter(p -> p.getKey().equals(PROPERTY_SOURCES))
                        .map(Property::getValue)
                        .anyMatch(v -> v.toLowerCase().contains(proteomeId.toLowerCase()));
    }

    private boolean hasProteomeId(UniParcCrossReference xref, String proteomeId) {
        List<ProteomeIdComponent> proteomeIdComponents = xref.getProteomeIdComponents();
        return proteomeIdComponents.stream()
                .map(ProteomeIdComponent::getProteomeId)
                .anyMatch(proteomeId::equalsIgnoreCase);
    }
}
