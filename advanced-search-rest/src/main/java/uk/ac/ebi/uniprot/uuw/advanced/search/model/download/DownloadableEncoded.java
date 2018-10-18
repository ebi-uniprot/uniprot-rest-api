package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import com.google.common.base.Strings;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.GeneLocation;

import java.util.*;
import java.util.stream.Collectors;

public class DownloadableEncoded implements Downloadable {
	public static final List<String> FIELDS = Arrays.asList(new String[] { "gene_location" });
	private final List<GeneLocation> geneLocations;

	public DownloadableEncoded(List<GeneLocation> geneLocations) {
		if (geneLocations == null) {
			this.geneLocations = Collections.emptyList();
		} else {
			this.geneLocations = Collections.unmodifiableList(geneLocations);
		}
	}

	@Override
	public Map<String, String> attributeValues() {
		if (geneLocations.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), geneLocations.stream().map(this::getGeneLocation).collect(Collectors.joining("; ")));

		return map;
	}

	private String getGeneLocation(GeneLocation g) {
		StringBuilder sb = new StringBuilder();
		sb.append(g.getType());
		if (!Strings.isNullOrEmpty(g.getName())) {
			sb.append(" ").append(g.getName());
		}
		return sb.toString();
	}

	public static boolean contains(List<String> fields) {
		return fields.stream().anyMatch(FIELDS::contains);
	}
}
