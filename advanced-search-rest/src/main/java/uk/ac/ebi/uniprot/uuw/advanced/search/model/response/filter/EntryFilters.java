package uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.comment.Comment;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Feature;
import uk.ac.ebi.uniprot.dataservice.restful.response.adapter.JsonDataAdapter;

import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

public class EntryFilters {
	private static final String ALL = "all";

	public static UPEntry convertAndFilter(JsonDataAdapter<UniProtEntry, UPEntry> uniProtJsonAdaptor,
			UniProtEntry upEntry,  Map<String, List<String>> filterParams) {
		UPEntry entry  = uniProtJsonAdaptor.convertEntity(upEntry, filterParams);
		if((filterParams ==null ) || filterParams.isEmpty())
			return entry;
		filterEntry(entry, filterParams);
	
		return entry;
	}
	public static void filterEntry(UPEntry entry, Map<String, List<String>> filterParams) {
		for (FilterComponentType component : FilterComponentType.values()) {
			if (!filterParams.containsKey(component.name().toLowerCase())) {
				remove(entry, component);
			} else if (component == FilterComponentType.COMMENT) {
				List<String> values = filterParams.get(component.name().toLowerCase());
				Predicate<Comment> filter = createCommentFilter(values);
				entry.getComments().removeIf(comment -> !filter.test(comment));

			} else if (component == FilterComponentType.FEATURE) {
				List<String> values = filterParams.get(component.name().toLowerCase());
				Predicate<Feature> filter = createFeatureFilter(values);
				entry.getFeatures().removeIf(feature -> !filter.test(feature));
			} else if (component == FilterComponentType.XREF) {
				List<String> values = filterParams.get(component.name().toLowerCase());
				Predicate<DbReference> filter = createDbReferenceFilter(values);
				entry.getDbReferences().removeIf(xref -> !filter.test(xref));
			}
		}
	}
	
	
	private static Predicate<DbReference> createDbReferenceFilter(List<String> values) {
		return (DbReference v) -> createXrefPredicate(v, values);
	}

	private static boolean createXrefPredicate(DbReference v, List<String> values) {
		if (values.contains(ALL)) {
			return true;
		}
		return values.contains(v.getType().toLowerCase());
	}

	private static Predicate<Feature> createFeatureFilter(List<String> values) {
		return (Feature v) -> createFeaturePredicate(v, values);
	}

	private static boolean createFeaturePredicate(Feature v, List<String> values) {
		if (values.contains(ALL)) {
			return true;
		}
		return values.contains(v.getType().toLowerCase());
	}

	private static Predicate<Comment> createCommentFilter(List<String> values) {
		return (Comment v) -> createCommentPredicate(v, values);
	}

	private static boolean createCommentPredicate(Comment v, List<String> values) {
		if (values.contains(ALL)) {
			return true;
		}
		return values.contains(v.getType().name().toLowerCase());
	}

	private static void remove(UPEntry entry, FilterComponentType type) {
		switch (type) {
		case PROTEIN_EXISTENCE:
			entry.setProteinExistence(null);
			break;
		case SECONDARY_ACCESSION:
			entry.setSecondaryAccession(null);
			break;
		case PROTEIN_NAME:
			entry.setProtein(null);
			break;
		case LINEAGE:
			entry.setLineage(null);
			break;
		case GENE:
			entry.setGene(null);
			break;
		case ORGANISM:
			entry.setOrganism(null);
			break;
		case ORGANISM_HOST:
			entry.setOrganismHost(null);
			break;
		case GENE_LOCATION:
			entry.setGeneLocations(null);
			break;
	//	case ENTRY_INFO:
	//		entry.setInfo(null);
	//		break;
		case COMMENT:
			entry.setComments(null);
			break;
		case KEYWORD:
			entry.setKeywords(null);
			break;
		case FEATURE:
			entry.setFeatures(null);
			break;
		case SEQUENCE:
			entry.setSequence(null);
			break;
		case XREF:
			entry.setDbReferences(null);
			break;
		case REFERENCE:
			entry.setReferences(null);
			break;
		default:
			break;
		}
	}

}
