package uk.ac.ebi.uniprot.uniprotkb.output.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.TaxNode;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryLineageMapTest {

	@Test
	void testFields() {
		List<String> fields = EntryLineageMap.FIELDS;
		List<String> expected = Arrays.asList(new String[] {"lineage", "tl:all", "tl:class", "tl:cohort",
				"tl:family", "tl:forma", "tl:genus", "tl:infraclass", "tl:infraorder", "tl:kingdom", "tl:order",
				"tl:parvorder", "tl:phylum", "tl:species", "tl:species_group", "tl:species_subgroup", "tl:subclass",
				"tl:subcohort", "tl:subfamily", "tl:subgenus", "tl:subkingdom", "tl:suborder", "tl:subphylum",
				"tl:subspecies", "tl:subtribe", "tl:superclass", "tl:superfamily", "tl:superkingdom", "tl:superorder",
				"tl:superphylum", "tl:tribe", "tl:varietas" });
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	@Test
	void testGetDataEmpty() {
		EntryLineageMap dl = new EntryLineageMap(null);
		Map<String, String> result = dl.attributeValues();
		assertTrue(result.isEmpty());
		
	}
	
	@Test
	void testGetData() {
		List<TaxNode> lineage =new ArrayList<>();
		lineage.add(new  TaxNode(2759, "Eukaryota", "superkingdom", false));
		
		lineage.add(new  TaxNode(33208, "Metazoa", "kingdom", false));
		lineage.add(new  TaxNode(7711, "Chordata", "phylum", false));
		lineage.add(new  TaxNode(40674, "Mammalia", "class", false));
		lineage.add(new  TaxNode(9443, "Primates", "order", false));
		lineage.add(new  TaxNode(314295, "Hominoidea", "superfamily", true));
		lineage.add(new  TaxNode(9604, "Hominidae", "family", false));
		lineage.add(new  TaxNode(207598, "Homininae", "subfamily", true));
		lineage.add(new  TaxNode(9605, "Homo", "genus", false));
		lineage.add(new  TaxNode(9606, "Homo sapiens", "species", true));
		EntryLineageMap dl = new EntryLineageMap(lineage);
		Map<String, String> result = dl.attributeValues();
		assertEquals(12, result.size());
		verify(result, "tl:all",
				"Eukaryota, Metazoa, Chordata, Mammalia, Primates, Hominoidea, Hominidae, Homininae, Homo, Homo sapiens");
		verify(result, "tl:superkingdom", "Eukaryota");
		verify(result, "tl:kingdom", "Metazoa");
		verify(result, "tl:phylum", "Chordata");
		verify(result, "tl:class", "Mammalia");
		verify(result, "tl:order", "Primates");
		verify(result, "tl:superfamily", "Hominoidea");
		verify(result, "tl:family", "Hominidae");
		verify(result, "tl:subfamily", "Homininae");
		verify(result, "tl:genus", "Homo");
		verify(result, "tl:species", "Homo sapiens");
	
		
	}
	private  void verify(Map<String, String> result, String field, String expected ) {
		String value = result.get(field);
		assertEquals(expected, value);
	}
}
