package uk.ac.ebi.uniprot.uniprotkb.output.model;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EntryInfo;
import uk.ac.ebi.uniprot.rest.output.model.NamedValueMap;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class EntryInfoMap implements NamedValueMap {
	public static final List<String> FIELDS = Arrays.asList("reviewed", "version", "date_create", "date_mod");
	private final EntryInfo info;

	public EntryInfoMap(EntryInfo info) {
		this.info = info;
	}

	@Override
	public Map<String, String> attributeValues() {
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), info.getType().equals("Swiss-Prot") ? "reviewed" : "unreviewed");
		map.put(FIELDS.get(1), "" + info.getVersion());
		map.put(FIELDS.get(2), info.getCreated());
		map.put(FIELDS.get(3), info.getModified());
		return map;
	}
	public static  boolean contains(List<String> fields) {
		return fields.stream().anyMatch(FIELDS::contains);
	}
}
