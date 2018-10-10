package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import com.google.common.base.Strings;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Keyword;

public class DownloadableKeyword implements Downloadable {
	private final List<Keyword> keywords;
	public static final List<String> FIELDS = 
			Arrays.asList(
					"keyword", "keywordid"
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
		if(keywords.isEmpty()) {
			return Collections.emptyMap();
		}
		Map<String, String> map = new HashMap<>();
		String kwValue=
		keywords.stream().map(val ->val.getValue().getValue()).collect(Collectors.joining(";"));
		map.put(FIELDS.get(0), kwValue);
		String kwIds=
		keywords.stream().map(val ->val.getKeywordId()).filter(val ->!Strings.isNullOrEmpty(val)).collect(Collectors.joining("; "));
		map.put(FIELDS.get(1), kwIds);
		return map;
	}

	public static  boolean contains(List<String> fields) {
		return fields.stream().anyMatch(val -> FIELDS.contains(val));
	}

}
