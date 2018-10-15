package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Sequence;

class DownloadableSequenceTest {
	@Test
	void testFields() {
		List<String> fields =  DownloadableSequence.FIELDS;
		List<String> expected =Arrays.asList(new String [] {
				"sequence", "sequence_version",
				"mass", "length", "date_seq_mod", "fragment"
		});
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	@Test
	void testGetDataWithEmptySeq() {
		Sequence sequence = new 
				 Sequence(23, 123, 9456, "", "");
		DownloadableSequence dl = new DownloadableSequence(sequence);
		Map<String, String> result = dl.map();
		assertEquals(6, result.size());
		verify("sequence", "", result);
		verify("sequence_version", "23", result);
		verify("mass", "9456", result);
		verify("length", "123", result);
		verify("date_seq_mod", "", result);
		verify("fragment", "", result);
	}
	private void verify(String field, String expected, Map<String, String> result) {
		assertEquals(expected, result.get(field));
	}
	
	@Test
	void testGetData() {
		Sequence sequence = new 
				 Sequence(23, 123, 9456, "2005-05-24", "AFADSFADSD");
		sequence.setFragment("fragment");
		DownloadableSequence dl = new DownloadableSequence(sequence);
		Map<String, String> result = dl.map();
		assertEquals(6, result.size());
		verify("sequence", "AFADSFADSD", result);
		verify("sequence_version", "23", result);
		verify("mass", "9456", result);
		verify("length", "123", result);
		verify("date_seq_mod", "2005-05-24", result);
		verify("fragment", "fragment", result);
	}

}
