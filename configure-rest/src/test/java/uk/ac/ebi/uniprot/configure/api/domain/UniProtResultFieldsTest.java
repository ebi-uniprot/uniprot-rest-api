package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.Field;
import uk.ac.ebi.uniprot.configure.uniprot.domain.FieldGroup;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;

class UniProtResultFieldsTest {
	private static UniProtResultFields instance;

	@BeforeAll
	static void initAll() {
		instance = UniProtResultFields.INSTANCE;
	}

	@Test
	void testResultFieldSize() {
		List<FieldGroup> groups = instance.getResultFields();
		assertEquals(16, groups.size());
		verifyGroupSize(groups, "Names & Taxonomy", 12);
		verifyGroupSize(groups, "Sequences", 19);
		verifyGroupSize(groups, "Function", 18);
		verifyGroupSize(groups, "Miscellaneous", 10);
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
	private void verifyGroupSize( List<FieldGroup> groups, String groupName, int size) {
		Optional<FieldGroup> group =getGroup (groups, groupName);
		assertTrue(group.isPresent());
		assertEquals(size, group.get().getFields().size());
	}
	@Test
	void testResultFieldGroup() {
		List<FieldGroup> groups = instance.getResultFields();
		assertEquals(16, groups.size());
		Optional<FieldGroup> seqGroup =getGroup (groups, "Sequences");
		assertTrue(seqGroup.isPresent());
		assertEquals(19, seqGroup.get().getFields().size());
		Optional<Field> massField = getField(seqGroup.get(), "Mass");
		assertTrue(massField.isPresent());
		assertEquals("sequence", massField.get().getName());
	}
	
	@Test
	void testResultField() {
		List<FieldGroup> groups = instance.getResultFields();
		verifyField(groups, "Names & Taxonomy", "Gene Names", "gene");
		verifyField(groups, "Sequences", "Alternative sequence", "ft:var_seq");
		verifyField(groups, "Function", "Kinetics", "cc:biophysicochemical_properties");
		verifyField(groups, "Miscellaneous", "Caution", "cc:alternative_products");
		verifyField(groups, "Interaction", "Subunit structure", "cc:subunit");
		verifyField(groups, "Expression", "Induction", "cc:induction");
		verifyField(groups, "Gene Ontology (GO)", "Gene Ontology (cellular component)", "dr:go");
		verifyField(groups, "Pathology & Biotech", "Mutagenesis", "ft:mutagen");
		verifyField(groups, "Subcellular location",  "Subcellular location [CC]", "cc:subcellular_location");
		verifyField(groups, "PTM / Processing", "Cross-link", "ft:crosslnk");
		verifyField(groups, "Structure",  "3D", "dr:pdb");
		verifyField(groups, "Publications",  "PubMed ID", "reference");
		verifyField(groups, "Date of", "Date of creation", "info");
		verifyField(groups, "Family & Domains",  "Compositional bias", "ft:compbias");
		verifyField(groups, "Taxonomic lineage",  "Taxonomic lineage (CLASS)", "lineage");
		verifyField(groups, "Taxonomic identifier",  "Taxonomic lineage IDs", "organism");
		
		
	}
	
	@Test
	void testDatabaseFieldSize() {
		List<FieldGroup> groups = instance.getDatabaseFields();
		assertEquals(17, groups.size());
		verifyGroupSize(groups, "Sequence", 5);
		verifyGroupSize(groups, "3D structure", 5);
		verifyGroupSize(groups, "Protein-protein interaction", 8);
		verifyGroupSize(groups, "Chemistry", 5);
		verifyGroupSize(groups, "Protein family/group", 12);
		verifyGroupSize(groups, "PTM", 7);
		verifyGroupSize(groups, "Polymorphism and mutation", 3);
		verifyGroupSize(groups, "2D gel", 7);
		verifyGroupSize(groups, "Proteomic", 8);
		verifyGroupSize(groups, "Protocols and materials", 1);
		verifyGroupSize(groups, "Genome annotation", 14);
		verifyGroupSize(groups, "Organism-specific", 37);
		verifyGroupSize(groups, "Phylogenomic", 10);
		verifyGroupSize(groups, "Enzyme and pathway", 7);
		verifyGroupSize(groups, "Other", 6);
		verifyGroupSize(groups, "Gene expression", 5);
		verifyGroupSize(groups, "Family and domain", 14);
	}
	
	@Test
	void testDatabaseField() {
		List<FieldGroup> groups = instance.getDatabaseFields();
		assertEquals(17, groups.size());
		verifyField(groups, "Sequence", "EMBL", "dr:embl");
		verifyField(groups, "3D structure",  "PDB", "dr:pdb");
		verifyField(groups, "Protein-protein interaction", "CORUM", "dr:corum");
		verifyField(groups, "Chemistry", "ChEMBL", "dr:chembl");
		verifyField(groups, "Protein family/group",  "IMGT_GENE-DB", "dr:imgt_gene_db");
		verifyField(groups, "PTM",  "GlyConnect", "dr:glyconnect");
		verifyField(groups, "Polymorphism and mutation",  "dbSNP", "dr:dbsnp");
		verifyField(groups, "2D gel",  "SWISS-2DPAGE", "dr:swiss2dpage");
		verifyField(groups, "Proteomic", "PRIDE", "dr:pride");
		verifyField(groups, "Protocols and materials",  "DNASU", "dr:dnasu");
		verifyField(groups, "Genome annotation", "Ensembl", "dr:ensembl");
		verifyField(groups, "Organism-specific",  "DisGeNET", "dr:disgenet");
		verifyField(groups, "Phylogenomic",  "KO", "dr:ko");
		verifyField(groups, "Enzyme and pathway",  "BRENDA", "dr:brenda");
		verifyField(groups, "Other",  "GeneWiki", "dr:genewiki");
		verifyField(groups, "Gene expression",  "Bgee", "dr:bgee");
		verifyField(groups, "Family and domain",  "HAMAP", "dr:hamap");
	}
	
	
	private void verifyField( List<FieldGroup> groups, String groupName, String label, String name) {
		Optional<FieldGroup> group =getGroup (groups, groupName);
		assertTrue(group.isPresent());
		Optional<Field> field = getField(group.get(), label);
		assertTrue(field.isPresent());
		assertEquals(name, field.get().getName());
	}
	
	private Optional<FieldGroup> getGroup (List<FieldGroup> groups, String groupName) {
		return groups.stream().filter(group ->group.getGroupName().equals(groupName)).findFirst();
	}
	private Optional<Field> getField(FieldGroup group, String label) {
		return group.getFields().stream().filter(val ->val.getLabel().equals(label)).findFirst();
	}
}
