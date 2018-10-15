package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Organism;

public class DownloadableOrganismHost implements Downloadable {
	public static final List<String> FIELDS = Arrays.asList("organism_host");
	private final List<Organism> organismHost;

	public DownloadableOrganismHost(List<Organism> organismHost) {
		if (organismHost == null) {
			this.organismHost = Collections.emptyList();
		} else {
			this.organismHost = Collections.unmodifiableList(organismHost);
		}
	}

	public static boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));

	}

	@Override
	public Map<String, String> map() {
		if (organismHost.isEmpty())
			return Collections.emptyMap();
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), organismHost.stream().map(this::getOrganismName).collect(Collectors.joining("; ")));
		return map;
	}

	private String getOrganismName(Organism organism) {
		StringBuilder sb = new StringBuilder();
		sb.append(DownloadableUtil.convertOrganism(organism));
		sb.append(" [TaxID: ").append("" + organism.getTaxonomy()).append("]");
		return sb.toString();
	}
}
