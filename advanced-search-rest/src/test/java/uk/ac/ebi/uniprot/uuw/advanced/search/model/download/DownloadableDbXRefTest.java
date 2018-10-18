package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.kraken.interfaces.factories.XRefFactory;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseCrossReference;
import uk.ac.ebi.kraken.interfaces.uniprot.DatabaseType;
import uk.ac.ebi.kraken.model.factories.DefaultUniProtFactory;
import uk.ac.ebi.kraken.model.factories.DefaultXRefFactory;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.DbReferenceConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.DbReference;

class DownloadableDbXRefTest {
	private final XRefFactory factory = DefaultXRefFactory.getInstance();
	private final DbReferenceConverter converter = new DbReferenceConverter();

	@Test
	void testGetDataEmpty() {
		DownloadableDbXRef dl = new DownloadableDbXRef(null);
		Map<String, String> result = dl.attributeValues();
		assertTrue(result.isEmpty());

	}

	@Test
	void hasEmbl() {
		List<DbReference> xrefs = new ArrayList<>();
		xrefs.add(createXref(DatabaseType.EMBL, "AY189288", "AAO86732.1", "-", "mRNA", null));
		xrefs.add(createXref(DatabaseType.EMBL, "AK022746", "BAB14220.1", "-", "mRNA", null));
		DownloadableDbXRef dl = new DownloadableDbXRef(xrefs);
		Map<String, String> result = dl.attributeValues();
		assertEquals(1, result.size());
		verify("AY189288;AK022746;", "dr:embl", result);
	}

	@Test
	void hasEmblAndEnsembl() {
		List<DbReference> xrefs = new ArrayList<>();
		xrefs.add(createXref(DatabaseType.EMBL, "AY189288", "AAO86732.1", "-", "mRNA", null));
		xrefs.add(createXref(DatabaseType.EMBL, "AK022746", "BAB14220.1", "-", "mRNA", null));
		// Ensembl; ENST00000330899; ENSP00000369127; ENSG00000086061. [P31689-1]
		// Ensembl; ENST00000439351; ENSP00000414398; ENSG00000090520.
		xrefs.add(createXref(DatabaseType.ENSEMBL, "ENST00000330899", "ENSP00000369127", "ENSG00000086061", null,
				"P31689-1"));
		xrefs.add(
				createXref(DatabaseType.ENSEMBL, "ENST00000439351", "ENSP00000414398", "ENSG00000090520", null, null));
		DownloadableDbXRef dl = new DownloadableDbXRef(xrefs);
		Map<String, String> result = dl.attributeValues();
		assertEquals(2, result.size());
		verify("AY189288;AK022746;", "dr:embl", result);
		verify("ENST00000330899 [P31689-1];ENST00000439351;", "dr:ensembl", result);
	}

	@Test
	void hasPdbAndSmr() {
		List<DbReference> xrefs = new ArrayList<>();
		xrefs.add(createXref(DatabaseType.PDB, "2LO1", "NMR", "-", "A=1-70", null));
		xrefs.add(createXref(DatabaseType.PDB, "2M6Y", "NMR", "-", "A=1-67", null));
		//PDB; 5TKG; X-ray; 1.20 A; A/B=16-23
		xrefs.add(createXref(DatabaseType.PDB, "5TKG", "X-ray", "1.20 A", "A/B=16-23", null));
		xrefs.add(createXref(DatabaseType.SMR, "P31689", "-", null, null, null));
		DownloadableDbXRef dl = new DownloadableDbXRef(xrefs);
		Map<String, String> result = dl.attributeValues();
		assertEquals(3, result.size());
		verify("2LO1;2M6Y;5TKG;", "dr:pdb", result);
		verify("P31689;", "dr:smr", result);
		String pdb3d="NMR spectroscopy (2); X-ray crystallography (1)";
		verify(pdb3d, "3d", result);
	}
	
	@Test
	void hasIntactAndString() {
		List<DbReference> xrefs = new ArrayList<>();
		xrefs.add(createXref(DatabaseType.INTACT, "P31689", "97",null, null, null));
		
		xrefs.add(createXref(DatabaseType.STRINGXREF, "9606.ENSP00000369127", "-", null, null, null));
		DownloadableDbXRef dl = new DownloadableDbXRef(xrefs);
		Map<String, String> result = dl.attributeValues();
		assertEquals(2, result.size());
		verify("P31689;", "dr:intact", result);
		verify("9606.ENSP00000369127;", "dr:stringxref", result);
	}

	@Test
	void hasChemblAndSwissLipids() {
		List<DbReference> xrefs = new ArrayList<>();
		xrefs.add(createXref(DatabaseType.CHEMBL, "CHEMBL2189122", "-",null, null, null));
		
		xrefs.add(createXref(DatabaseType.SWISSLIPIDS, "SLP:000000475", "-", null, null, null));
		DownloadableDbXRef dl = new DownloadableDbXRef(xrefs);
		Map<String, String> result = dl.attributeValues();
		assertEquals(2, result.size());
		verify("CHEMBL2189122;", "dr:chembl", result);
		verify("SLP:000000475;", "dr:swisslipids", result);
	}
	@Test
	void testBbXrefToString() {
		DbReference dbxref = createXref(DatabaseType.EMBL, "AY189288", "AAO86732.1", "-", "mRNA", null);
		String result = DownloadableDbXRef.dbXrefToString(dbxref);
		assertEquals("AY189288", result);
	}
	@Test
	void testProteomeXrefToString() {
		//UP000006548: Chromosome 4
		DbReference dbxref = createXref(DatabaseType.PROTEOMES, "UP000006548", "Chromosome 4", null, null, null);
		String result = DownloadableDbXRef.proteomeXrefToString(dbxref);
		assertEquals("UP000006548: Chromosome 4", result);
	}
	
	private void verify(String expected, String field, Map<String, String> result) {
		String evaluated = result.get(field);
		assertEquals(expected, evaluated);
	}

	private DbReference createXref(DatabaseType type, String id, String desc, String third, String fourth,
			String isoform) {
		DatabaseCrossReference xref = factory.buildDatabaseCrossReference(type);
		xref.setPrimaryId(factory.buildXDBAttribute(id));
		xref.setDescription(factory.buildXDBAttribute(desc));
		if (third != null) {
			xref.setThird(factory.buildXDBAttribute(third));
		}
		if (fourth != null) {
			xref.setFourth(factory.buildXDBAttribute(fourth));
		}
		if (isoform != null) {
			xref.setIsoformId(DefaultUniProtFactory.getInstance().buildUniProtIsoformId(isoform));
		}
		return converter.apply(xref);
	}
}
