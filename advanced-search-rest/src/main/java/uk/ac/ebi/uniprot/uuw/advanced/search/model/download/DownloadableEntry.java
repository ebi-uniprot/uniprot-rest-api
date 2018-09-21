package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;

public class DownloadableEntry implements Downloadable {
	private final UPEntry entry;
	private final List<String> fields;
	public DownloadableEntry(UPEntry entry, List<String> fields) {
		this.entry =entry;
		this.fields = Collections.unmodifiableList(fields);
	}
	@Override
	public Map<String, String> getData() {
		// TODO Auto-generated method stub
		return null;
	}

	
}
