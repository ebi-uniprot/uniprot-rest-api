package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.FieldMaps;

class FieldMapsTest {
	private static FieldMaps instance;

	@BeforeAll
	static void initAll() {
		instance = FieldMaps.INSTANCE;
	}
	@Test
	void testGetMappedGene() {
		String mapped = instance.getField("gene_orf");
		assertEquals("gene", mapped);
		mapped = instance.getField("gene_names");
		assertEquals("gene", mapped);
	}
	@Test
	void testGetMappedSequence() {
		String mapped = instance.getField("fragment");
		assertEquals("sequence", mapped);
	//	mapped = instance.getField("length");
	//	assertEquals("sequence", mapped);
	//	mapped = instance.getField("mass");
	//	assertEquals("sequence", mapped);
	}

	@Test
	void testGetMappedLineage() {
		String mapped = instance.getField("lineage_all");
		assertEquals("lineage", mapped);
		mapped = instance.getField("lin_genus");
		assertEquals("lineage", mapped);
		mapped = instance.getField("lin_species");
		assertEquals("lineage", mapped);
	}
	@Test
	void testGetMappedBiophyProperties() {
		String mapped = instance.getField("kinetics");
		assertEquals("cc:biophysicochemical_properties", mapped);
		mapped = instance.getField("temp_dependence");
		assertEquals("cc:biophysicochemical_properties", mapped);
		mapped = instance.getField("ph_dependence");
		assertEquals("cc:biophysicochemical_properties", mapped);
	}
	@Test
	void testGetMappedGo() {
		String mapped = instance.getField("go_id");
		assertEquals("dr:go", mapped);
		mapped = instance.getField("go_c");
		assertEquals("dr:go", mapped);
		mapped = instance.getField("go_p");
		assertEquals("dr:go", mapped);
	}
	@Test
	void testGetMappedInfo() {
		String mapped = instance.getField("date_create");
		assertEquals("info", mapped);
	//	mapped = instance.getField("date_seq_mod");
	//	assertEquals("info", mapped);
		mapped = instance.getField("version");
		assertEquals("info", mapped);
	}
	@Test
	void testGetMappedOrganism() {
		String mapped = instance.getField("tax_id");
		assertEquals("organism", mapped);
		mapped = instance.getField("organism_id");
		assertEquals("organism", mapped);
	}
	@Test
	public void testMappedAnnotation() {
		String mapped = instance.getField("matched_text");
		assertEquals("annotation", mapped);
	}
	@Test
	public void testUnmappedProtein() {
		String mapped = instance.getField("protein_name");
		assertEquals("protein_name", mapped);
	}
	@Test
	public void testUnmappedComment() {
		String mapped = instance.getField("cc:function");
		assertEquals("cc:function", mapped);
	}
	@Test
	public void testUnmappedFeature() {
		String mapped = instance.getField("ft:domain");
		assertEquals("ft:domain", mapped);
	}
	@Test
	public void testUnmappedXref() {
		String mapped = instance.getField("dr:embl");
		assertEquals("dr:embl", mapped);
	}
}
