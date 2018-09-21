package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EntryInfo;

public class DownloadableEntryInfo implements Downloadable {
	public static final List<String> FIELDS = 
			Arrays.asList(new String [] {
					"reviewed", "version",
					"date_create", "date_mod"
			});
	private final EntryInfo info;
	public DownloadableEntryInfo(EntryInfo info) {
		this.info= info;
	}
	@Override
	public Map<String, String> getData() {
		Map<String, String> map = new HashMap<>();
		map.put(FIELDS.get(0), info.getType().equals("Swiss-Prot")? "reviewed":"unreviewed");
		map.put(FIELDS.get(1), ""+info.getVersion());
		map.put(FIELDS.get(2), info.getCreated());
		map.put(FIELDS.get(3), info.getModified());
		return map;
	}

}
