package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.TaxNode;

public class DownloadableLineage implements Downloadable {
	private final List<TaxNode> lineage;
	public static final List<String> FIELDS = 
			Arrays.asList(new String[] { "tl:all", "tl:class", "tl:cohort",
					"tl:family", "tl:forma", "tl:genus", "tl:infraclass", "tl:infraorder", "tl:kingdom", "tl:order",
					"tl:parvorder", "tl:phylum", "tl:species", "tl:species_group", "tl:species_subgroup", "tl:subclass",
					"tl:subcohort", "tl:subfamily", "tl:subgenus", "tl:subkingdom", "tl:suborder", "tl:subphylum",
					"tl:subspecies", "tl:subtribe", "tl:superclass", "tl:superfamily", "tl:superkingdom", "tl:superorder",
					"tl:superphylum", "tl:tribe", "tl:varietas" });

	public DownloadableLineage(List<TaxNode> lineage) {
		if (lineage == null) {
			this.lineage = Collections.emptyList();
		} else {
			this.lineage = Collections.unmodifiableList(lineage);
		}

	}

	@Override
	public Map<String, String> getData() {
		if(lineage.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> map = new HashMap<>();
		map.put("tl:all", getAll());
		FIELDS.stream().skip(1).forEach(val -> addToMap(val, map));
		return map;
	}

	private void addToMap(String field, Map<String, String> map) {
		Optional<TaxNode> node = getValue(field);
		if (node.isPresent()) {
			map.put(field, node.get().getName());
		}
	}

	private String getAll() {

		return lineage.stream().map(val -> val.getName()).collect(Collectors.joining(", "));
	}

	private Optional<TaxNode> getValue(String field) {
		String type = field.substring(3);
		return lineage.stream().filter(val -> val.getRank().equalsIgnoreCase(type)).findFirst();
	}
}
