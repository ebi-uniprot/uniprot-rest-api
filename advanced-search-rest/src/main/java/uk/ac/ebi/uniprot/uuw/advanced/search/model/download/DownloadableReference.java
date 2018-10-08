package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.ReferenceConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.citation.Reference;

public class DownloadableReference implements Downloadable {
	public static final List<String> FIELDS = Arrays.asList("pm_id");
	private final List<Reference> references;

	public DownloadableReference(List<Reference> references) {
		if (references == null) {
			this.references = Collections.emptyList();
		} else {
			this.references = Collections.unmodifiableList(references);
		}
	}

	@Override
	public Map<String, String> map() {
		if (references.isEmpty()) {
			return Collections.emptyMap();
		}
		String result = references.stream().map(val -> val.getCitation()).filter(val -> val.getDbReferences() != null)
				.flatMap(val -> val.getDbReferences().stream())
				.filter(val -> val.getType().equals(ReferenceConverter.PUB_MED)).map(val -> val.getId())
				.collect(Collectors.joining("; "));
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), result);
		return map;
	}
	public static  boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));
		
	}
}
