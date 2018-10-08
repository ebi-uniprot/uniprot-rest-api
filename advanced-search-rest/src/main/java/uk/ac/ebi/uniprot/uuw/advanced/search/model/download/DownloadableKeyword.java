package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Keyword;

public class DownloadableKeyword implements Downloadable {
	private final List<Keyword> keywords;
	public static final List<String> FIELDS = 
			Arrays.asList(
					"keyword", "keywordId"
			);
	public DownloadableKeyword(List<Keyword> keywords) {
		if(keywords ==null) {
			this.keywords = Collections.emptyList();
		}else {
			this.keywords = Collections.unmodifiableList(keywords);
		}
	}
	@Override
	public Map<String, String> map() {
		Map<String, String> map = new HashMap<>();
		
		return map;
	}

	public static  boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));
	}

}
