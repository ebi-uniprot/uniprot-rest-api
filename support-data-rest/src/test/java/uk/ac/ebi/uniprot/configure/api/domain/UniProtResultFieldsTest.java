package uk.ac.ebi.uniprot.configure.api.domain;

import org.assertj.core.util.Strings;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.FieldGroupImpl;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.FieldImpl;
import uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl.UniProtResultFields;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

class UniProtResultFieldsTest {
	private static UniProtResultFields instance;

	@BeforeAll
	static void initAll() {
		instance = UniProtResultFields.INSTANCE;
	}

	@Test
	void fieldUniqueness() {
		Map<String, List<Field>> result = instance.getResultFields()
				.stream().flatMap(val -> val.getFields().stream()).collect(Collectors.groupingBy(Field::getName));
		
		assertFalse( result.entrySet()
		.stream()
		.anyMatch(val -> val.getValue().size()>1))
		;
		
	
	}
	@Test
	void testGetField() {
		assertTrue(instance.getField("accession").isPresent());
		assertTrue(instance.getField("protein_name").isPresent());
		assertFalse(instance.getField("protein").isPresent());
	}

	@Test
	void testResultFieldSize() {
		List<FieldGroup> groups = instance.getResultFields();
		assertEquals(16, groups.size());
		verifyGroupSize(groups, "Names & Taxonomy", 13);
		verifyGroupSize(groups, "Sequences", 19);
		verifyGroupSize(groups, "Function", 18);
		verifyGroupSize(groups, "Miscellaneous", 11);
		verifyGroupSize(groups, "Interaction", 2);
		verifyGroupSize(groups, "Expression", 3);
		verifyGroupSize(groups, "Gene Ontology (GO)", 5);
		verifyGroupSize(groups, "Pathology & Biotech", 7);
		verifyGroupSize(groups, "Subcellular location", 4);
		verifyGroupSize(groups, "PTM / Processing", 12);
		verifyGroupSize(groups, "Structure", 4);
		verifyGroupSize(groups, "Publications", 2);
		verifyGroupSize(groups, "Date of", 4);
		verifyGroupSize(groups, "Family & Domains", 10);
		verifyGroupSize(groups, "Taxonomic lineage", 31);
		verifyGroupSize(groups, "Taxonomic identifier", 1);
	}

	private void verifyGroupSize(List<FieldGroup> groups, String groupName, int size) {
		Optional<FieldGroup> group = getGroup(groups, groupName);
		assertTrue(group.isPresent());
		assertEquals(size, group.orElse(new FieldGroupImpl()).getFields().size());
	}

	@Test
	void testResultFieldGroup() {
		List<FieldGroup> groups = instance.getResultFields();
		assertEquals(16, groups.size());
		System.out.println(
		groups.stream().flatMap(val->val.getFields().stream())
		.map(Field::getName)
		.filter(val ->val.startsWith("ft:"))
		.map(val -> "\"" + val +"\"")
		.collect(Collectors.joining(", ")));
	
		Optional<FieldGroup> seqGroup = getGroup(groups, "Sequences");
		assertTrue(seqGroup.isPresent());
		assertEquals(19, seqGroup.orElse(new FieldGroupImpl()).getFields().size());
		Optional<Field> massField = getField(seqGroup.orElse(new FieldGroupImpl()), "Mass");
		assertTrue(massField.isPresent());
		assertEquals("mass", massField.orElse(new FieldImpl()).getName());
	}

	@Test
	void testResultField() {
		List<FieldGroup> groups = instance.getResultFields();
		verifyField(groups, "Names & Taxonomy", "Gene Names", "gene_names");
		verifyField(groups, "Sequences", "Alternative sequence", "ft:var_seq");
		verifyField(groups, "Function", "Kinetics", "kinetics");
		verifyField(groups, "Miscellaneous", "Caution", "cc:caution");
		verifyField(groups, "Interaction", "Subunit structure", "cc:subunit");
		verifyField(groups, "Expression", "Induction", "cc:induction");
		verifyField(groups, "Gene Ontology (GO)", "Gene Ontology (cellular component)", "go_c");
		verifyField(groups, "Pathology & Biotech", "Mutagenesis", "ft:mutagen");
		verifyField(groups, "Subcellular location", "Subcellular location [CC]", "cc:subcellular_location");
		verifyField(groups, "PTM / Processing", "Cross-link", "ft:crosslnk");
		verifyField(groups, "Structure", "3D", "3d");
		verifyField(groups, "Publications", "PubMed ID", "pm_id");
		verifyField(groups, "Date of", "Date of creation", "date_create");
		verifyField(groups, "Family & Domains", "Compositional bias", "ft:compbias");
		verifyField(groups, "Taxonomic lineage", "Taxonomic lineage (CLASS)", "tl:class");
		verifyField(groups, "Taxonomic identifier", "Taxonomic lineage IDs", "tax_id");

	}

	@Test
	void allFields() {
		List<FieldGroup> groups = instance.getResultFields();
		groups.stream().flatMap(val -> val.getFields().stream()).map(val -> val.getName()).distinct()
				.filter(val -> !val.startsWith("ft:")).filter(val -> !val.startsWith("cc:"))
				.filter(val -> !val.startsWith("dr:")).filter(val -> !Strings.isNullOrEmpty(val))
				.forEach(System.out::println);
	}

	@Test
	void testDatabaseFieldSize() {
		List<FieldGroup> groups = instance.getDatabaseFields();
		assertEquals(19, groups.size());
		verifyGroupSize(groups, "SEQ", 4);
		verifyGroupSize(groups, "3DS", 4);
		verifyGroupSize(groups, "PPI", 8);
		verifyGroupSize(groups, "CHEMISTRY", 5);
		verifyGroupSize(groups, "PFAM", 12);
		verifyGroupSize(groups, "PTM", 7);
		verifyGroupSize(groups, "PMD", 3);
		verifyGroupSize(groups, "2DG", 7);
		verifyGroupSize(groups, "PROTEOMIC", 10);
		verifyGroupSize(groups, "PAM", 1);
		verifyGroupSize(groups, "GMA", 14);
		verifyGroupSize(groups, "ORG", 37);
		verifyGroupSize(groups, "PLG", 9);
		verifyGroupSize(groups, "EAP", 7);
		verifyGroupSize(groups, "OTHER", 7);
		verifyGroupSize(groups, "GEP", 5);
		verifyGroupSize(groups, "FMD", 14);
		verifyGroupSize(groups, "OTG", 1);
		verifyGroupSize(groups, "PRM", 0);
	}

	@Test
	void testDatabaseField() {
		List<FieldGroup> groups = instance.getDatabaseFields();
		assertEquals(19, groups.size());
		verifyField(groups, "SEQ", "EMBL", "dr:embl");
		verifyField(groups, "3DS", "PDB", "dr:pdb");
		verifyField(groups, "PPI", "CORUM", "dr:corum");
		verifyField(groups, "CHEMISTRY", "ChEMBL", "dr:chembl");
		verifyField(groups, "PFAM", "IMGT_GENE-DB", "dr:imgt_gene-db");
		verifyField(groups, "PTM", "GlyConnect", "dr:glyconnect");
		verifyField(groups, "PMD", "dbSNP", "dr:dbsnp");
		verifyField(groups, "2DG", "SWISS-2DPAGE", "dr:swiss-2dpage");
		verifyField(groups, "PROTEOMIC", "PRIDE", "dr:pride");
		verifyField(groups, "PAM", "DNASU", "dr:dnasu");
		verifyField(groups, "GMA", "Ensembl", "dr:ensembl");
		verifyField(groups, "ORG", "DisGeNET", "dr:disgenet");
		verifyField(groups, "PLG", "KO", "dr:ko");
		verifyField(groups, "EAP", "BRENDA", "dr:brenda");
		verifyField(groups, "OTHER", "GeneWiki", "dr:genewiki");
		verifyField(groups, "GEP", "Bgee", "dr:bgee");
		verifyField(groups, "FMD", "HAMAP", "dr:hamap");
	}

	private void verifyField(List<FieldGroup> groups, String groupName, String label, String name) {
		Optional<FieldGroup> group = getGroup(groups, groupName);
		assertTrue(group.isPresent());
		Optional<Field> field = getField(group.orElse(new FieldGroupImpl()), label);
		assertTrue(field.isPresent());
		assertEquals(name, field.orElse(new FieldImpl()).getName());
	}

	private Optional<FieldGroup> getGroup(List<FieldGroup> groups, String groupName) {
		return groups.stream().filter(group -> group.getGroupName().equals(groupName)).findFirst();
	}

	private Optional<Field> getField(FieldGroup group, String label) {
		return group.getFields().stream().filter(val -> val.getLabel().equals(label)).findFirst();
	}
}
