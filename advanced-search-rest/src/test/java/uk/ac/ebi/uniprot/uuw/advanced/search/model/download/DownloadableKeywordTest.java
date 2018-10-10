package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Keyword;

class DownloadableKeywordTest {
	@Test
	void testFields() {
		List<String> fields = DownloadableKeyword.FIELDS;
		List<String> expected = Arrays
				.asList("keyword", "keywordid");
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	@Test
	void testMapEmpty() {
		DownloadableKeyword dl = new DownloadableKeyword(null);
		Map<String, String> result = dl.map();
		assertTrue(result.isEmpty());
	}
	@Test
	void testMap() {
		List<Keyword> keywords = new ArrayList<>();
		keywords.add(new Keyword("KW-0002", create("3D-structure")));
		keywords.add(new Keyword("KW-0106", create("Calcium")));
		DownloadableKeyword dl = new DownloadableKeyword(keywords);
		Map<String, String> result = dl.map();
		assertEquals(2, result.size());
		verify("KW-0002; KW-0106", "keywordid", result );
		verify("3D-structure;Calcium", "keyword", result );
	}
	
	private void verify(String expected, String field, Map<String, String> result) {
		String evaluated = result.get(field);
		assertEquals(expected, evaluated);
	}
	private EvidencedString create(String value) {
		return new EvidencedString(value, Collections.emptyList());
	}
}
