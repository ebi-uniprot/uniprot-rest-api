package org.uniprot.api.rest.output.converter;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

import org.uniprot.core.uniprotkb.UniProtkbEntry;
import org.uniprot.core.uniprotkb.comment.Comment;
import org.uniprot.core.uniprotkb.feature.Feature;
import org.uniprot.core.uniprotkb.impl.UniProtkbEntryBuilder;
import org.uniprot.core.uniprotkb.xdb.UniProtkbCrossReference;
import org.uniprot.store.search.field.UniProtField;

public class UniProtkbEntryFilters {
    private static final String ALL = "all";

    public static UniProtkbEntry filterEntry(
            UniProtkbEntry entry, Map<String, List<String>> filterParams) {
        if ((filterParams != null) && !filterParams.isEmpty()) {
            UniProtkbEntryBuilder builder = UniProtkbEntryBuilder.from(entry);
            for (UniProtField.ResultFields component : UniProtField.ResultFields.values()) {
                if (component.isMandatoryJsonField() == false
                        && !filterParams.containsKey(component.name())) {
                    remove(builder, component);
                } else if (component == UniProtField.ResultFields.comment) {
                    List<String> values = filterParams.get(component.name().toLowerCase());
                    Predicate<Comment> filter = createCommentFilter(values);
                    List<Comment> comments = entry.getComments();
                    comments.removeIf(comment -> !filter.test(comment));
                    builder.commentsSet(comments);
                } else if (component == UniProtField.ResultFields.feature) {
                    List<String> values = filterParams.get(component.name().toLowerCase());
                    Predicate<Feature> filter = createFeatureFilter(values);
                    List<Feature> features = entry.getFeatures();
                    features.removeIf(feature -> !filter.test(feature));
                    builder.featuresSet(features);
                } else if (component == UniProtField.ResultFields.crossReference) {
                    List<String> values = filterParams.get(component.name().toLowerCase());
                    Predicate<UniProtkbCrossReference> filter = createDbReferenceFilter(values);
                    List<UniProtkbCrossReference> crossReferences =
                            entry.getUniProtkbCrossReferences();
                    crossReferences.removeIf(xref -> !filter.test(xref));
                    builder.uniProtCrossReferencesSet(crossReferences);
                }
            }
            return builder.build();
        } else {
            return entry;
        }
    }

    public static Predicate<UniProtkbCrossReference> createDbReferenceFilter(List<String> values) {
        return v -> createXrefPredicate(v, values);
    }

    private static boolean createXrefPredicate(UniProtkbCrossReference v, List<String> values) {
        if (values.contains(ALL)) {
            return true;
        }
        return values.contains(v.getDatabase().getName().toLowerCase());
    }

    public static Predicate<Feature> createFeatureFilter(List<String> values) {
        return (Feature v) -> createFeaturePredicate(v, values);
    }

    private static boolean createFeaturePredicate(Feature v, List<String> values) {
        if (values.contains(ALL)) {
            return true;
        }
        return values.contains(v.getType().name().toLowerCase());
    }

    public static Predicate<Comment> createCommentFilter(List<String> values) {
        return (Comment v) -> createCommentPredicate(v, values);
    }

    private static boolean createCommentPredicate(Comment v, List<String> values) {
        if (values.contains(ALL)) {
            return true;
        }
        return values.contains(v.getCommentType().name().toLowerCase());
    }

    private static void remove(UniProtkbEntryBuilder builder, UniProtField.ResultFields type) {
        switch (type) {
            case protein_existence:
                builder.proteinExistence(null);
                break;
            case secondary_accession:
                builder.secondaryAccessionsSet(null);
                break;
            case protein_name:
                builder.proteinDescription(null);
                break;
            case gene:
                builder.genesSet(null);
                break;
            case organism:
                builder.organism(null);
                break;
            case organism_host:
                builder.organismHostsSet(null);
                break;
            case organelle:
                builder.geneLocationsSet(null);
                break;
            case comment:
                builder.commentsSet(null);
                break;
            case keyword:
                builder.keywordsSet(null);
                break;
            case feature:
                builder.featuresSet(null);
                break;
            case sequence:
                builder.sequence(null);
                break;
            case crossReference:
                builder.uniProtCrossReferencesSet(null);
                break;
            case reference:
                builder.referencesSet(null);
                break;
            default:
                break;
        }
    }
}
