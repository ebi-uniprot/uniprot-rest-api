package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism.OrganismName;

public class DownloadableOrganism implements Downloadable {
	public static final List<String> FIELDS = Arrays.asList(new String[] { "organism", "organism_id", "tax_id" });
	private final Organism organism;

	public DownloadableOrganism(Organism organism) {
		this.organism = organism;
	}

	@Override
	public Map<String, String> getData() {
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), getOrganismStr());
		map.put(FIELDS.get(1), "" + organism.getTaxonomy());
		map.put(FIELDS.get(2), "" + organism.getTaxonomy());

		return map;
	}

	private String getOrganismStr() {
		StringBuilder sb = new StringBuilder();
		Optional<OrganismName> scientific = organism.getNames().stream()
				.filter(val -> val.getType().equals("scientific")).findFirst();
		if (scientific.isPresent()) {
			sb.append(scientific.get().getValue());
		}
		Optional<OrganismName> common = organism.getNames().stream().filter(val -> val.getType().equals("common"))
				.findFirst();
		if (common.isPresent()) {
			sb.append(" (");
			sb.append(common.get().getValue());
			sb.append(")");
		}
		List<OrganismName> synonyms = organism.getNames().stream().filter(val -> val.getType().equals("synonym"))
				.collect(Collectors.toList());
		if (!synonyms.isEmpty()) {
			sb.append(" (").append(synonyms.stream().map(val -> val.getValue()).collect(Collectors.joining(", ")))
					.append(")");
		}
		return sb.toString();
	}

}
