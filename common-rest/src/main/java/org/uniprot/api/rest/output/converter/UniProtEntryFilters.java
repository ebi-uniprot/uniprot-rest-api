package org.uniprot.api.rest.output.converter;

import org.uniprot.core.uniprot.UniProtEntry;
import org.uniprot.core.uniprot.builder.UniProtEntryBuilder;
import org.uniprot.core.uniprot.comment.Comment;
import org.uniprot.core.uniprot.feature.Feature;
import org.uniprot.core.uniprot.xdb.UniProtDBCrossReference;
import org.uniprot.store.search.field.UniProtField;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class UniProtEntryFilters {
    private static final String ALL = "all";

    public static UniProtEntry filterEntry(
            UniProtEntry entry, Map<String, List<String>> filterParams) {
        if ((filterParams != null) && !filterParams.isEmpty()) {
            UniProtEntryBuilder builder = UniProtEntryBuilder.from(entry);
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
                } else if (component == UniProtField.ResultFields.xref) {
                    List<String> values = filterParams.get(component.name().toLowerCase());
                    Predicate<UniProtDBCrossReference> filter = createDbReferenceFilter(values);
                    List<UniProtDBCrossReference> crossReferences =
                            entry.getDatabaseCrossReferences();
                    crossReferences.removeIf(xref -> !filter.test(xref));
                    builder.databaseCrossReferencesSet(crossReferences);
                }
            }
            return builder.build();
        } else {
            return entry;
        }
    }

    public static Predicate<UniProtDBCrossReference> createDbReferenceFilter(List<String> values) {
        return v -> createXrefPredicate(v, values);
    }

    private static boolean createXrefPredicate(UniProtDBCrossReference v, List<String> values) {
        if (values.contains(ALL)) {
            return true;
        }
        return values.contains(v.getDatabaseType().getName().toLowerCase());
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

    private static void remove(UniProtEntryBuilder builder, UniProtField.ResultFields type) {
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
            case xref:
                builder.databaseCrossReferencesSet(null);
                break;
            case reference:
                builder.referencesSet(null);
                break;
            default:
                break;
        }
    }
}
