package uk.ac.ebi.uniprot.uniprotkb.output.model;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.GeneLocation;

class DownloadableEncodedTest {
	@Test
	void testFields() {
		List<String> fields =  EntryEncodedMap.FIELDS;
		List<String> expected =Arrays.asList(new String [] {
				"gene_location"
		});
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	
	@Test
	void testGetDataEmpty() {
		EntryEncodedMap dl = new EntryEncodedMap(null);
		assertTrue(dl.attributeValues().isEmpty());
	}
	
	@Test
	void testGetData() {
		List<GeneLocation> glocations = new ArrayList<>();
		 glocations.add(new GeneLocation("Mitochondrion", Collections.emptyList(), ""));
		 glocations.add(new GeneLocation("Plasmid", Collections.emptyList(), "mlp1"));
		EntryEncodedMap dl = new EntryEncodedMap(glocations);
		Map<String, String> result = dl.attributeValues();
		assertEquals(1, result.size());
		String value = result.get(EntryEncodedMap.FIELDS.get(0));
		assertNotNull(value);
		assertEquals("Mitochondrion; Plasmid mlp1", value);
	}

}
