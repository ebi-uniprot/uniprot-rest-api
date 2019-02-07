package uk.ac.ebi.uniprot.uniprotkb.service.filters;

import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.domain.uniprot.builder.UniProtEntryBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.comment.Comment;
import uk.ac.ebi.uniprot.domain.uniprot.feature.Feature;
import uk.ac.ebi.uniprot.domain.uniprot.xdb.UniProtDBCrossReference;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class EntryFilters {
	private static final String ALL = "all";


	public static UniProtEntry filterEntry(UniProtEntry entry, Map<String, List<String>> filterParams) {
		if((filterParams !=null ) && !filterParams.isEmpty()) {
			UniProtEntryBuilder.ActiveEntryBuilder builder = new UniProtEntryBuilder().from(entry);
			for (FilterComponentType component : FilterComponentType.values()) {
				if (!filterParams.containsKey(component.name().toLowerCase())) {
					remove(builder, component);
				} else if (component == FilterComponentType.COMMENT) {
					List<String> values = filterParams.get(component.name().toLowerCase());
					Predicate<Comment> filter = createCommentFilter(values);
					List<Comment> comments = entry.getComments();
					comments.removeIf(comment -> !filter.test(comment));
					builder.comments(comments);
				} else if (component == FilterComponentType.FEATURE) {
					List<String> values = filterParams.get(component.name().toLowerCase());
					Predicate<Feature> filter = createFeatureFilter(values);
					List<Feature> features = entry.getFeatures();
					features.removeIf(feature -> !filter.test(feature));
					builder.features(features);
				} else if (component == FilterComponentType.XREF) {
					List<String> values = filterParams.get(component.name().toLowerCase());
					Predicate<UniProtDBCrossReference> filter = createDbReferenceFilter(values);
					List<UniProtDBCrossReference> crossReferences = entry.getDatabaseCrossReferences();
					crossReferences.removeIf(xref -> !filter.test(xref));
					builder.databaseCrossReferences(crossReferences);
				}
			}
			return builder.build();
		}else {
			return entry;
		}
	}
	
	
	private static Predicate<UniProtDBCrossReference> createDbReferenceFilter(List<String> values) {
		return v -> createXrefPredicate(v, values);
	}

	private static boolean createXrefPredicate(UniProtDBCrossReference v, List<String> values) {
		if (values.contains(ALL)) {
			return true;
		}
		return values.contains(v.getDatabaseType().getName().toLowerCase());
	}

	private static Predicate<Feature> createFeatureFilter(List<String> values) {
		return (Feature v) -> createFeaturePredicate(v, values);
	}

	private static boolean createFeaturePredicate(Feature v, List<String> values) {
		if (values.contains(ALL)) {
			return true;
		}
		return values.contains(v.getType().name().toLowerCase());
	}

	private static Predicate<Comment> createCommentFilter(List<String> values) {
		return (Comment v) -> createCommentPredicate(v, values);
	}

	private static boolean createCommentPredicate(Comment v, List<String> values) {
		if (values.contains(ALL)) {
			return true;
		}
		return values.contains(v.getCommentType().name().toLowerCase());
	}

	private static void remove(UniProtEntryBuilder.ActiveEntryBuilder builder, FilterComponentType type) {
		switch (type) {
		case PROTEIN_EXISTENCE:
			builder.proteinExistence(null);
			break;
		case SECONDARY_ACCESSION:
			builder.secondaryAccessions(null);
			break;
		case PROTEIN_NAME:
			builder.proteinDescription(null);
			break;
		case GENE:
			builder.genes(null);
			break;
		case ORGANISM:
			builder.organism(null);
			break;
		case ORGANISM_HOST:
			builder.organismHosts(null);
			break;
		case GENE_LOCATION:
			builder.geneLocations(null);
			break;
		case COMMENT:
			builder.comments(null);
			break;
		case KEYWORD:
			builder.keywords(null);
			break;
		case FEATURE:
			builder.features(null);
			break;
		case SEQUENCE:
			builder.sequence(null);
			break;
		case XREF:
			builder.databaseCrossReferences(null);
			break;
		case REFERENCE:
			builder.references(null);
			break;
		default:
			break;
		}
	}

}
