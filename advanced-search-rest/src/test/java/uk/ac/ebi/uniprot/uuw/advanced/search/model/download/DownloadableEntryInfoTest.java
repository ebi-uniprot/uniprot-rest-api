package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EntryInfo;

class DownloadableEntryInfoTest {
	@Test
	void testFields() {
		List<String> fields =  DownloadableEntryInfo.FIELDS;
		List<String> expected =Arrays.asList(new String [] {
				"reviewed", "version",
				"date_create", "date_mod"
		});
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	
	@Test
	void testGetData() {
		EntryInfo info = new EntryInfo("Swiss-Prot",  "2005-05-24", "2018-09-12",  119);
		DownloadableEntryInfo downloadable = new DownloadableEntryInfo(info);
		Map<String, String> result = downloadable.getData();
		assertEquals(4, result.size());
		verify("reviewed", "reviewed", result);
		verify("version", "119", result);
		verify("date_create", "2005-05-24", result);
		verify("date_mod", "2018-09-12", result);
	}
	private void verify(String field, String expected, Map<String, String> result) {
		assertEquals(expected, result.get(field));
	}
}
