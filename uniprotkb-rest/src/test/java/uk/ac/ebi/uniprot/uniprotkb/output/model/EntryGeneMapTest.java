package uk.ac.ebi.uniprot.uniprotkb.output.model;

import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Gene;

import java.util.*;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class EntryGeneMapTest {
	@Test
	void testFields() {
		List<String> fields = EntryGeneMap.FIELDS;
		List<String> expected = Arrays
				.asList(new String[] { "gene_names", "gene_primary", "gene_synonym", "gene_oln", "gene_orf" });
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}
	@Test
	void testGetDataEmpty() {
		EntryGeneMap dl = new EntryGeneMap(null);
		Map<String, String> result = dl.attributeValues();
		assertTrue(result.isEmpty());
		
	}

	@Test
	void testGetDataAll() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setName(create("name11"));
		builder.setSynonyms(create(Arrays.asList(new String[] { "syn1", "syn2" })));
		builder.setOrfNames(create(Arrays.asList(new String[] { "orf1", "orf2" })));
		builder.setOlnNames(create(Arrays.asList(new String[] { "oln1", "oln2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		Map<String, String> result = dl.attributeValues();
		assertEquals(5, result.size());
		verify("gene_names", "name11 syn1 syn2 oln1 oln2 orf1 orf2", result);
		verify("gene_primary", "name11", result);
		verify("gene_synonym", "syn1 syn2", result);
		verify("gene_oln", "oln1 oln2", result);
		verify("gene_orf", "orf1 orf2", result);
	}

	@Test
	void testGetDataMulti() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setName(create("name11"));
		builder.setSynonyms(create(Arrays.asList(new String[] { "syn1", "syn2" })));
		builder.setOrfNames(create(Arrays.asList(new String[] { "orf1", "orf2" })));
		builder.setOlnNames(create(Arrays.asList(new String[] { "oln1", "oln2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setName(create("name12"));
		builder2.setSynonyms(create(Arrays.asList(new String[] { "syn3", "syn4" })));
		genes.add(builder2.build());

		EntryGeneMap dl = new EntryGeneMap(genes);
		Map<String, String> result = dl.attributeValues();
		assertEquals(5, result.size());
		verify("gene_names", "name11 syn1 syn2 oln1 oln2 orf1 orf2; name12 syn3 syn4", result);
		verify("gene_primary", "name11; name12", result);
		verify("gene_synonym", "syn1 syn2; syn3 syn4", result);
		verify("gene_oln", "oln1 oln2; ", result);
		verify("gene_orf", "orf1 orf2; ", result);
	}

	@Test
	void testGetDataOnlyPrimarySynonym() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setName(create("name11"));
		builder.setSynonyms(create(Arrays.asList(new String[] { "syn1", "syn2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		Map<String, String> result = dl.attributeValues();
		assertEquals(5, result.size());
		verify("gene_names", "name11 syn1 syn2", result);
		verify("gene_primary", "name11", result);
		verify("gene_synonym", "syn1 syn2", result);
		verify("gene_oln", "", result);
		verify("gene_orf", "", result);
	}

	@Test
	void testGetDataOnlyPrimaryOrf() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setName(create("name11"));
		builder.setOrfNames(create(Arrays.asList(new String[] { "orf1", "orf2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		Map<String, String> result = dl.attributeValues();
		assertEquals(5, result.size());
		verify("gene_names", "name11 orf1 orf2", result);
		verify("gene_primary", "name11", result);
		verify("gene_synonym", "", result);
		verify("gene_oln", "", result);
		verify("gene_orf", "orf1 orf2", result);
	}

	@Test
	void testGetDataOnlyOln() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setOlnNames(create(Arrays.asList(new String[] { "oln1", "oln2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		Map<String, String> result = dl.attributeValues();
		assertEquals(5, result.size());
		verify("gene_names", "oln1 oln2", result);
		verify("gene_primary", "", result);
		verify("gene_synonym", "", result);
		verify("gene_oln", "oln1 oln2", result);
		verify("gene_orf", "", result);
	}

	private void verify(String field, String expected, Map<String, String> result) {
		assertEquals(expected, result.get(field));
	}

	@Test
	void testGetGeneName() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setName(create("name11"));
		builder.setSynonyms(create(Arrays.asList(new String[] { "syn1", "syn2" })));
		builder.setOrfNames(create(Arrays.asList(new String[] { "orf1", "orf2" })));
		builder.setOlnNames(create(Arrays.asList(new String[] { "oln1", "oln2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		String result = dl.getGeneName();
		String expected = "name11 syn1 syn2 oln1 oln2 orf1 orf2";
		assertEquals(expected, result);

		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setName(create("name12"));
		builder2.setSynonyms(create(Arrays.asList(new String[] { "syn3", "syn4" })));
		genes.add(builder2.build());
		dl = new EntryGeneMap(genes);
		result = dl.getGeneName();
		expected = "name11 syn1 syn2 oln1 oln2 orf1 orf2; name12 syn3 syn4";
		assertEquals(expected, result);

		Gene.Builder builder3 = Gene.newBuilder();
		builder3.setOrfNames(create(Arrays.asList(new String[] { "orf3", "orf4" })));
		genes.add(builder3.build());
		dl = new EntryGeneMap(genes);
		result = dl.getGeneName();
		expected = "name11 syn1 syn2 oln1 oln2 orf1 orf2; name12 syn3 syn4; orf3 orf4";
		assertEquals(expected, result);

		Gene.Builder builder4 = Gene.newBuilder();
		builder4.setName(create("name14"));
		builder4.setOlnNames(create(Arrays.asList(new String[] { "oln3", "oln4" })));
		genes.add(builder4.build());
		dl = new EntryGeneMap(genes);
		result = dl.getGeneName();
		expected = "name11 syn1 syn2 oln1 oln2 orf1 orf2; name12 syn3 syn4; orf3 orf4; name14 oln3 oln4";
		assertEquals(expected, result);
	}

	@Test
	void testGetPrimaryName() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setName(create("name11"));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		String result = dl.getPrimaryName();
		assertEquals("name11", result);
		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setName(create("name2"));
		genes.add(builder2.build());
		dl = new EntryGeneMap(genes);
		result = dl.getPrimaryName();
		assertEquals("name11; name2", result);

		genes = new ArrayList<>();
		genes.add(Gene.newBuilder().build());
		genes.add(Gene.newBuilder().build());
		dl = new EntryGeneMap(genes);
		result = dl.getPrimaryName();
		assertEquals("; ", result);
	}

	@Test
	void testGetSynonyms() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setSynonyms(create(Arrays.asList(new String[] { "syn1", "syn2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		String result = dl.getSynonyms();
		assertEquals("syn1 syn2", result);
		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setSynonyms(create(Arrays.asList(new String[] { "syn3", "syn4" })));
		genes.add(builder2.build());
		dl = new EntryGeneMap(genes);
		result = dl.getSynonyms();
		assertEquals("syn1 syn2; syn3 syn4", result);

		genes = new ArrayList<>();
		genes.add(Gene.newBuilder().build());
		genes.add(Gene.newBuilder().build());
		dl = new EntryGeneMap(genes);
		result = dl.getSynonyms();
		assertEquals("; ", result);
	}

	@Test
	void testGetOlnName() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setOlnNames(create(Arrays.asList(new String[] { "oln1", "oln2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		String result = dl.getOlnName();
		assertEquals("oln1 oln2", result);
		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setOlnNames(create(Arrays.asList(new String[] { "oln3", "oln4" })));
		genes.add(builder2.build());
		dl = new EntryGeneMap(genes);
		result = dl.getOlnName();
		assertEquals("oln1 oln2; oln3 oln4", result);
	}

	@Test
	void testGetOrfName() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setOrfNames(create(Arrays.asList(new String[] { "orf1", "orf2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		String result = dl.getOrfName();
		assertEquals("orf1 orf2", result);
		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setOrfNames(create(Arrays.asList(new String[] { "orf3", "orf4" })));
		genes.add(builder2.build());
		dl = new EntryGeneMap(genes);
		result = dl.getOrfName();
		assertEquals("orf1 orf2; orf3 orf4", result);

		genes = new ArrayList<>();
		genes.add(Gene.newBuilder().build());
		genes.add(Gene.newBuilder().build());
		dl = new EntryGeneMap(genes);
		result = dl.getOrfName();
		assertEquals("; ", result);
	}

	@Test
	void testGetOlnNameEmpty() {
		Gene.Builder builder = Gene.newBuilder();
		builder.setOrfNames(create(Arrays.asList(new String[] { "orf1", "orf2" })));
		List<Gene> genes = new ArrayList<>();
		genes.add(builder.build());
		EntryGeneMap dl = new EntryGeneMap(genes);
		String result = dl.getOlnName();
		assertEquals("", result);
		Gene.Builder builder2 = Gene.newBuilder();
		builder2.setOrfNames(create(Arrays.asList(new String[] { "orf3", "orf4" })));
		genes.add(builder2.build());
		dl = new EntryGeneMap(genes);
		result = dl.getOlnName();
		assertEquals("; ", result);
	}

	private List<EvidencedString> create(List<String> value) {
		return value.stream().map(val -> new EvidencedString(val, Collections.emptyList()))
				.collect(Collectors.toList());
	}

	private EvidencedString create(String value) {
		return new EvidencedString(value, Collections.emptyList());
	}
}
