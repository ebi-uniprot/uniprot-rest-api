package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.*;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.model.factories.DefaultUniProtFactory;
import uk.ac.ebi.kraken.parser.UniProtParser;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;

class DownloadableEntryTest {
	private static UPEntry entryQ15758;
	private static UPEntry entryP03431;
	private static UPEntry entryQ84MC7;
	private static UPEntry entryQ70KY3;
	
	@BeforeAll
	static void setup() throws Exception {
		 InputStream is = DownloadableEntryTest.class.getResourceAsStream("/downloadIT/Q15758.dat" );
	     UniProtEntry entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	     EntryConverter converter = new EntryConverter();
	     entryQ15758 =converter.apply(entry);
	     is.close();
	     
	     is = DownloadableEntryTest.class.getResourceAsStream("/downloadIT/P03431.dat" );
	      entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	      entryP03431 =converter.apply(entry);
	      is.close();
	      is = DownloadableEntryTest.class.getResourceAsStream("/downloadIT/Q84MC7.dat" );
	      entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	      entryQ84MC7 =converter.apply(entry);
	      is.close();
	      is = DownloadableEntryTest.class.getResourceAsStream("/downloadIT/Q70KY3.dat" );
	      entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	      entryQ70KY3 =converter.apply(entry);
	      is.close();
	      
	      
	}
	@Test
	void testIdAccession() {
		List<String> fields= Arrays.asList("accession", "id");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(2, result.size());
		verify("Q15758", 0, result);
		verify("AAAT_HUMAN", 1, result);
	}
	@Test
	void testInfo() {
		List<String> fields= Arrays.asList("reviewed", "version", "protein_existence");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("reviewed", 0, result);
		verify("142", 1, result);
		verify("Evidence at protein level", 2, result);
	}
	@Test
	void testSequence() {
		List<String> fields= Arrays.asList("length", "mass", "sequence_version", "sequence");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("541", 0, result);
		verify("56598", 1, result);
		verify("2", 2, result);
		String seq ="MVADPPRDSKGLAAAEPTANGGLALASIEDQGAAAGGYCGSRDQVRRCLRANLLVLLTVV" + 
				"AVVAGVALGLGVSGAGGALALGPERLSAFVFPGELLLRLLRMIILPLVVCSLIGGAASLD" + 
				"PGALGRLGAWALLFFLVTTLLASALGVGLALALQPGAASAAINASVGAAGSAENAPSKEV" + 
				"LDSFLDLARNIFPSNLVSAAFRSYSTTYEERNITGTRVKVPVGQEVEGMNILGLVVFAIV" + 
				"FGVALRKLGPEGELLIRFFNSFNEATMVLVSWIMWYAPVGIMFLVAGKIVEMEDVGLLFA" + 
				"RLGKYILCCLLGHAIHGLLVLPLIYFLFTRKNPYRFLWGIVTPLATAFGTSSSSATLPLM" + 
				"MKCVEENNGVAKHISRFILPIGATVNMDGAALFQCVAAVFIAQLSQQSLDFVKIITILVT" + 
				"ATASSVGAAGIPAGGVLTLAIILEAVNLPVDHISLILAVDWLVDRSCTVLNVEGDALGAG" + 
				"LLQNYVDRTESRSTEPELIQVKSELPLDPLPVPTEEGNPLLKHYRGPAGDATVASEKESV" + 
				"M";
		verify(seq, 3, result);
	}
	
	@Test
	void testDefault() {
		List<String> fields= Arrays.asList("accession", "id", "protein_name", "gene_names", "organism");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		verify("AAAT_HUMAN", 1, result);
		String proteinName ="Neutral amino acid transporter B(0), ATB(0) (Baboon M7 virus receptor)"
				+ " (RD114/simian type D retrovirus receptor)"
				+ " (Sodium-dependent neutral amino acid transporter type 2) (Solute carrier family 1 member 5)";
		verify(proteinName, 2, result);
		verify("SLC1A5 ASCT2 M7V1 RDR RDRC", 3, result);
		verify("Homo sapiens (Human)", 4, result);
	}
	@Test
	void testECnumber() {
		List<String> fields= Arrays.asList("accession","protein_name", "ec");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		String proteinName ="RNA-directed RNA polymerase catalytic subunit, EC 2.7.7.48 (Polymerase basic protein 1, PB1)"
				+ " (RNA-directed RNA polymerase subunit P1)" ;
		verify("P03431", 0, result);
		verify(proteinName, 1, result);
		verify("2.7.7.48", 2, result);
		
	}
	@Test
	void testGene() {
		List<String> fields= Arrays.asList("gene_names", "gene_primary",
				"gene_synonym", "gene_oln",
				"gene_orf");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("SLC1A5 ASCT2 M7V1 RDR RDRC", 0, result);
		verify("SLC1A5", 1, result);
		verify("ASCT2 M7V1 RDR RDRC", 2, result);
		verify("", 3, result);
		verify("", 4, result);
		
	}
	
	@Test
	void testOrganism() {
		List<String> fields= Arrays.asList("organism", "organism_id", "tax_id",
				"lineage", "tl:all");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Homo sapiens (Human)", 0, result);
		verify("9606", 1, result);
		verify("9606", 2, result);
		String lineage="cellular organisms, Eukaryota, Opisthokonta, Metazoa, Eumetazoa,"
				+ " Bilateria, Deuterostomia, Chordata, Craniata, Vertebrata, Gnathostomata,"
				+ " Teleostomi, Euteleostomi, Sarcopterygii, Dipnotetrapodomorpha, Tetrapoda,"
				+ " Amniota, Mammalia, Theria, Eutheria, Boreoeutheria, Euarchontoglires, Primates, Haplorrhini,"
				+ " Simiiformes, Catarrhini, Hominoidea, Hominidae, Homininae, Homo, Homo sapiens";
			
		verify(lineage, 3, result);
		
		verify(lineage, 4, result);
		
	}
	@Test
	void testOrganismHost() {
		List<String> fields= Arrays.asList("accession", "organism", "organism_host",
				"lineage", "tl:all");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		verify("Influenza A virus (strain A/Puerto Rico/8/1934 H1N1)", 1, result);
		verify("Aves [TaxID: 8782]; Homo sapiens (Human) [TaxID: 9606]; Sus scrofa (Pig) [TaxID: 9823]", 2, result);
	
	}
	@Test
	void testAlterProduct() {
		List<String> fields= Arrays.asList("accession", "cc:alternative_products");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String altProd ="ALTERNATIVE PRODUCTS:  Event=Alternative splicing, Alternative initiation;"
				+ " Named isoforms=3;Comment=TEST-altprod-comment-1: A number of isoforms are produced by"
				+ " alternative initiation. {ECO:0000269|PubMed:11350958}. TEST-altprod-comment-2:"
				+ " Isoforms start at multiple alternative CUG and GUG codons. {ECO:0000269|PubMed:14702039};"
				+ "  Name=1; IsoId=Q15758-1; Sequence=displayed; Name=2; IsoId=Q15758-2; Sequence=VSP_046354;"
				+ " Note=TEST-altprod-note-1: Derived from EST data. {ECO:0000269|PubMed:11350958}."
				+ " TEST-altprod-note-2: No experimental confirmation available. {ECO:0000269|PubMed:14702039};"
				+ " Name=3; IsoId=Q15758-3; Sequence=VSP_046851; Note=No experimental confirmation available.;" ;

		verify(altProd, 1, result);
	}
	
	@Test
	void testComments() {
		List<String> fields= Arrays.asList("accession", "cc:function", "cc:domain", "cc:disease", "cc:rna_editing");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String cfunction ="FUNCTION: TEST-function-1: Sodium-dependent amino acids transporter"
				+ " that has a broad substrate specificity, with a preference for zwitterionic amino acids."
				+ " It accepts as substrates all neutral amino acids, including glutamine, asparagine, and"
				+ " branched-chain and aromatic amino acids, and excludes methylated, anionic, and"
				+ " cationic amino acids. May also be activated by insulin. Through binding of the"
				+ " fusogenic protein syncytin-1/ERVW-1 may mediate trophoblasts syncytialization,"
				+ " the spontaneous fusion of their plasma membranes, an essential process in placental"
				+ " development (PubMed:10708449, PubMed:23492904) {ECO:0000269|PubMed:10051606,"
				+ " ECO:0000269|PubMed:10196349}.; TEST-function-2: Acts as a cell surface receptor for"
				+ " feline endogenous virus RD114, baboon M7 endogenous virus and type D simian retroviruses"
				+ " (PubMed:10051606, PubMed:10196349) {ECO:0000269|PubMed:10708449, ECO:0000269|PubMed:23492904}.";

		String cfomain ="";
		String cdisease="DISEASE: Mental retardation, X-linked, associated with fragile site FRAXE (MRFRAXE) [MIM:309548]:"
				+ " A form of mild to moderate mental retardation associated with learning difficulties, communication deficits,"
				+ " attention problems, hyperactivity, and autistic behavior. It is associated with a fragile site on chromosome"
				+ " Xq28. Mental retardation is characterized by significantly below average general intellectual functioning"
				+ " associated with impairments in adaptive behavior and manifested during the developmental period."
				+ " {ECO:0000255}. Note=TEST-disease-note-1: The disease is caused by mutations affecting"
				+ " the gene represented in this entry. It is caused either by silencing of the AFF2 gene as"
				+ " a consequence of a CCG expansion located upstream of this gene or by deletion within the gene"
				+ " {ECO:0000255}.; TEST-disease-note-2: Loss of AFF2 expression is correlated with FRAXE CCG(N) expansion."
				+ " Normal individuals have 6-35 copies of the repeat, whereas cytogenetically positive, developmentally delayed"
				+ " males have more than 200 copies and show methylation of the associated CPG island {ECO:0000269|PubMed:10051606}.";
	
		String crnaediting ="RNA EDITING: Modified_positions=381 {ECO:0000269|PubMed:10708449, ECO:0000269|PubMed:11350958},"
				+ " 398 {ECO:0000269|PubMed:10708449, ECO:0000269|PubMed:11350958}; Note=TEST-rna-editing-note-1: "
				+ "Partially edited {ECO:0000255}. TEST-rna-editing-note-2: Target of Adar {ECO:0000269|PubMed:10051606};" ;
		verify(cfunction, 1, result);
		verify(cfomain, 2, result);
		verify(cdisease, 3, result);
		verify(crnaediting, 4, result);	
	}
	
	@Test
	 void testComments2() {
		List<String> fields= Arrays.asList("accession", "cc:interaction", "cc:subcellular_location", "cc:ptm", "cc:similarity");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		String interaction ="Q14318; P03466; P03433; P03428; Q99959";
		String subcell ="SUBCELLULAR LOCATION: Host nucleus {ECO:0000255|HAMAP-Rule:MF_04065, ECO:0000269|PubMed:19906916}."
				+ " Host cytoplasm {ECO:0000255|HAMAP-Rule:MF_04065, ECO:0000269|PubMed:19906916}." ;
		String ptm="PTM: Phosphorylated by host PRKCA {ECO:0000255|HAMAP-Rule:MF_04065, ECO:0000269|PubMed:19264651}.";
		String similarity="SIMILARITY: Belongs to the influenza viruses polymerase PB1 family {ECO:0000255|HAMAP-Rule:MF_04065}.";
		verify(interaction, 1, result);
		verify(subcell, 2, result);
		verify(ptm, 3, result);
		verify(similarity, 4, result);	
	}
	@Test
	 void testProteinFamily() {
		List<String> fields= Arrays.asList("accession", "protein_families", "cc:similarity");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		String proteinFamily ="Influenza viruses polymerase PB1 family";
		String similarity="SIMILARITY: Belongs to the influenza viruses polymerase PB1 family {ECO:0000255|HAMAP-Rule:MF_04065}.";
		verify(proteinFamily, 1, result);
		verify(similarity, 2, result);	
	}
	
	@Test
	 void testSequenceCaution() {
		List<String> fields= Arrays.asList("accession", "cc:sequence_caution", "error_gmodel_pred");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ84MC7, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q84MC7", 0, result);
		String seqCaution ="SEQUENCE CAUTION:  Sequence=AAF97339.1; Type=Erroneous initiation; Note=Translation N-terminally extended.;"
				+ " Evidence={ECO:0000305};" ; 
		String seqCaution2="SEQUENCE CAUTION:  Sequence=AAG51053.1; Type=Erroneous gene model prediction; Evidence={ECO:0000305};";
		
		
		verify(seqCaution, 1, result);
		verify(seqCaution2, 2, result);	
	}
	
	@Test
	void testBPCP() {
		List<String> fields= Arrays.asList("accession",  "absorption", "kinetics", "ph_dependence",
				"redox_potential", "temp_dependence");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ70KY3, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q70KY3", 0, result);
		String absorption ="BIOPHYSICOCHEMICAL PROPERTIES: ;  Absorption: Abs(max)=280 {ECO:0000269|PubMed:12111146,"
				+ " ECO:0000269|PubMed:12118243}; Note=Exhibits a shoulder at 360 nm, a smaller absorption peak at 450 nm,"
				+ " and a second, larger peak at 590 nm. {ECO:0000269|PubMed:12118243};" ;
		String kinetic="BIOPHYSICOCHEMICAL PROPERTIES:  Kinetic parameters: KM=5.61 mM for ethanol {ECO:0000269|PubMed:10320337,"
				+ " ECO:0000269|PubMed:16061256, ECO:0000269|PubMed:7730276}; KM=0.105 mM for butane-1-ol {ECO:0000269|PubMed:10320337,"
				+ " ECO:0000269|PubMed:16061256, ECO:0000269|PubMed:7730276}; Vmax=45.5 umol/min/mg enzyme toward potassium"
				+ " ferricyanide (in the presence of 30 mM Tris-HCl pH 8.0) {ECO:0000269|PubMed:10320337, ECO:0000269|PubMed:16061256,"
				+ " ECO:0000269|PubMed:7730276};" ;
		String phDep ="BIOPHYSICOCHEMICAL PROPERTIES: ;  pH dependence: Optimum pH is 3.5 with 2,2'-azinobis-(3-ethylbenzthiazoline-6-sulphonate)"
				+ " as substrate, 5.0-7.5 with guiacol as substrate, and 6.0-7.0 with syringaldazine as substrate."
				+ " {ECO:0000269|PubMed:12111146, ECO:0000269|PubMed:12118243}; Optimum pH is 8.0."
				+ " {ECO:0000269|PubMed:10320337, ECO:0000269|PubMed:16061256, ECO:0000269|PubMed:7730276}" ;
		String redox ="BIOPHYSICOCHEMICAL PROPERTIES: ;  Redox potential: E(0) is +185 mV for heme c at pH 7.0,"
				+ " +188 mV for heme c at pH 8.0, +172 mV for heme c at pH 8.0 and 0.3 M KCl and +189 mV for"
				+ " ADH IIB-Azurin complex. {ECO:0000269|PubMed:10320337, ECO:0000269|PubMed:16061256, ECO:0000269|PubMed:7730276}" ;
		String tempDep= "BIOPHYSICOCHEMICAL PROPERTIES: ;  Temperature dependence: Optimum temperature is 60-70 degrees Celsius."
				+ " {ECO:0000269|PubMed:12111146, ECO:0000269|PubMed:12118243}" ;
		
		verify(absorption, 1, result);
		verify(kinetic, 2, result);	
		verify(phDep, 3, result);	
		verify(redox, 4, result);	
		verify(tempDep, 5, result);	
		
	}
	@Test
	void testFeatures() {
		List<String> fields= Arrays.asList("accession", "ft:chain", "ft:topo_dom",
				"ft:transmem", "ft:mod_res", "ft:carbohyd","ft:var_seq", "ft:variant", "ft:conflict", "ft:domain");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String chain ="CHAIN 1 541 Neutral amino acid transporter B(0). /FTId=PRO_0000202082.";
		String topo_dom ="TOPO_DOM 1 52 Cytoplasmic. {ECO:0000255}.; TOPO_DOM 154 224 Extracellular. {ECO:0000255}.";
		String transmem ="TRANSMEM 53 73 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 99 119 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 133 153 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 225 245 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 266 286 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 306 326 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 336 356 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 377 397 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 399 419 Helical. {ECO:0000255}.;"
				+ " TRANSMEM 426 446 Helical. {ECO:0000255}." ;
		String modRes="MOD_RES 1 1 N-acetylmethionine. {ECO:0000269|PubMed:19413330, ECO:0000269|PubMed:22814378}.;"
				+ " MOD_RES 493 493 Phosphoserine. {ECO:0000269|PubMed:21406692}.;"
				+ " MOD_RES 503 503 Phosphoserine. {ECO:0000269|PubMed:19690332}.;"
				+ " MOD_RES 535 535 Phosphoserine. {ECO:0000269|PubMed:17081983,"
				+ " ECO:0000269|PubMed:18669648, ECO:0000269|PubMed:19690332,"
				+ " ECO:0000269|PubMed:20068231}." ; 
		String carbohyd ="CARBOHYD 163 163 N-linked (GlcNAc...). {ECO:0000255}.;"
				+ " CARBOHYD 212 212 N-linked (GlcNAc...). {ECO:0000269|PubMed:19349973}." ;
		String varSeq="VAR_SEQ 1 228 Missing (in isoform 3). {ECO:0000303|PubMed:14702039}. /FTId=VSP_046851.;"
				+ " VAR_SEQ 1 203 MVADPPRDSKGLAAAEPTANGGLALASIEDQGAAAGGYCGSRDQVRRCLRANLLVLLTVVAVVAGVALGLGVSGAGGALA"
				+ "LGPERLSAFVFPGELLLRLLRMIILPLVVCSLIGGAASLDPGALGRLGAWALLFFLVTTLLASALGVGLALALQPGAASAAINASVGAAGSAENAP"
				+ "SKEVLDSFLDLARNIFPSNLVSAAFRS -> M (in isoform 2). {ECO:0000303|PubMed:14702039}. /FTId=VSP_046354." 
				;
		String variant ="VARIANT 17 17 P -> A (in dbSNP:rs3027956). /FTId=VAR_020439.;"
				+ " VARIANT 512 512 V -> L (in dbSNP:rs3027961)."
				+ " {ECO:0000269|PubMed:14702039, ECO:0000269|PubMed:19690332}. /FTId=VAR_013517." ;
		String conflict ="CONFLICT 18 24 TANGGLA -> PPTGAWQ (in Ref. 1; AAC50629). {ECO:0000305}.;"
				+ " CONFLICT 44 44 Q -> L (in Ref. 1; AAC50629). {ECO:0000305}.;"
				+ " CONFLICT 84 87 ERLS -> GALE (in Ref. 1; AAC50629). {ECO:0000305}.;"
				+ " CONFLICT 341 341 V -> A (in Ref. 5; BAH14917). {ECO:0000305}." ;
		String domain ="";
		verify(chain, 1, result);
		verify(topo_dom, 2, result);
		verify(transmem, 3, result);
		verify(modRes, 4, result);
		verify(carbohyd, 5, result);
		verify(varSeq, 6, result);
		verify(variant, 7, result);
		verify(conflict, 8, result);
		verify(domain, 9, result);
		
		
	}
	@Test
	void testNumberOfFeatures() {
		List<String> fields= Arrays.asList("accession", "feature");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String numOfFeature ="Alternative sequence (2); Chain (1); Glycosylation (2);"
				+ " Modified residue (4); Natural variant (2); Sequence conflict (4);"
				+ " Topological domain (2); Transmembrane (10)";
		verify(numOfFeature, 1, result);
	}
	@Test
	void testReferences() {
		List<String> fields= Arrays.asList("accession", "pm_id");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String pmids ="8702519; 10051606; 10196349; 14702039; 15057824; 15489334;"
				+ " 11350958; 10708449; 17081983; 17081065; 18669648; 19413330; 19349973;"
				+ " 19690332; 20068231; 21269460; 21406692; 22814378; 23492904";
		verify(pmids, 1, result);
	}
	
	@Test
	void testGOTerm() {
		List<String> fields= Arrays.asList("accession", "go", "go_c", "go_f", "go_p", "go_id");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String go="extracellular vesicular exosome [GO:0070062];"
				+ " Golgi apparatus [GO:0005794];"
				+ " integral component of plasma membrane [GO:0005887];"
				+ " melanosome [GO:0042470]; membrane [GO:0016020];"
				+ " plasma membrane [GO:0005886];"
				+ " L-glutamine transmembrane transporter activity [GO:0015186];"
				+ " L-serine transmembrane transporter activity [GO:0015194];"
				+ " neutral amino acid transmembrane transporter activity [GO:0015175];"
				+ " protein binding [GO:0005515];"
				+ " receptor activity [GO:0004872];"
				+ " sodium:dicarboxylate symporter activity [GO:0017153];"
				+ " virus receptor activity [GO:0001618];"
				+ " amino acid transport [GO:0006865];"
				+ " extracellular amino acid transport [GO:0006860];"
				+ " glutamine transport [GO:0006868];"
				+ " ion transport [GO:0006811];"
				+ " neutral amino acid transport [GO:0015804];"
				+ " transmembrane transport [GO:0055085]";
			
		String go_c="extracellular vesicular exosome [GO:0070062];"
				+ " Golgi apparatus [GO:0005794];"
				+ " integral component of plasma membrane [GO:0005887];"
				+ " melanosome [GO:0042470];"
				+ " membrane [GO:0016020];"
				+ " plasma membrane [GO:0005886]" ;
		String go_f ="L-glutamine transmembrane transporter activity [GO:0015186];"
				+ " L-serine transmembrane transporter activity [GO:0015194];"
				+ " neutral amino acid transmembrane transporter activity [GO:0015175];"
				+ " protein binding [GO:0005515]; receptor activity [GO:0004872];"
				+ " sodium:dicarboxylate symporter activity [GO:0017153];"
				+ " virus receptor activity [GO:0001618]" ;
		String go_p ="amino acid transport [GO:0006865];"
				+ " extracellular amino acid transport [GO:0006860];"
				+ " glutamine transport [GO:0006868];"
				+ " ion transport [GO:0006811];"
				+ " neutral amino acid transport [GO:0015804];"
				+ " transmembrane transport [GO:0055085]" ;

		String go_id ="GO:0001618; GO:0004872; GO:0005515; GO:0005794; GO:0005886;"
				+ " GO:0005887; GO:0006811; GO:0006860; GO:0006865; GO:0006868;"
				+ " GO:0015175; GO:0015186; GO:0015194; GO:0015804; GO:0016020;"
				+ " GO:0017153; GO:0042470; GO:0055085; GO:0070062" ;
		verify(go, 1, result);
		verify(go_c, 2, result);
		verify(go_f, 3, result);
		verify(go_p, 4, result);
		verify(go_id, 5, result);
		
	}
	@Test
	void testXRefs1() {
		List<String> fields= Arrays.asList("accession", "dr:embl", "dr:ccds", "dr:refseq", "dr:unigene", "dr:proteinmodelportal");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String embl ="U53347;AF102826;AF105423;GQ919058;AK292690;AK299137;AK301661;AK316546;AC008622;CH471126;BC000062;AF334818;";
		String ccds="CCDS12692.1;CCDS46125.1;CCDS46126.1;";
		String refseq="NP_001138616.1;NP_001138617.1;NP_005619.1;";
		String unigen="Hs.631582;";
		String proteinmodelportal ="Q15758;";
		verify(embl, 1, result);
		verify(ccds, 2, result);
		verify(refseq, 3, result);
		verify(unigen, 4, result);
		verify(proteinmodelportal, 5, result);
	}
	
	@Test
	void testXRefs2() {
		List<String> fields= Arrays.asList("accession", "dr:smr", "dr:biogrid", "dr:intact", "dr:mint", "dr:stringxref");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String smr ="Q15758;";
		String biogrid="112401;";
		String intact="Q15758;";
		String mint="MINT-5001314;";
		String string ="9606.ENSP00000303623;";
		verify(smr, 1, result);
		verify(biogrid, 2, result);
		verify(intact, 3, result);
		verify(mint, 4, result);
		verify(string, 5, result);
	}
	
	@Test
	void testXRefs3() {
		List<String> fields= Arrays.asList("accession", "dr:drugbank", "dr:guidetopharmacology", "dr:tcdb", "dr:dmdm", "dr:maxqb");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String drugbank ="DB00174;DB00130;";
		String guidetopharmacology="874;";
		String tcdb="2.A.23.3.3;";
		String dmdm="21542389;";
		String maxqb ="Q15758;";
		verify(drugbank, 1, result);
		verify(guidetopharmacology, 2, result);
		verify(tcdb, 3, result);
		verify(dmdm, 4, result);
		verify(maxqb, 5, result);
	}
	
	@Test
	void testXRefs4() {
		List<String> fields= Arrays.asList("accession", "dr:drugbank", "dr:guidetopharmacology", "dr:tcdb", "dr:dmdm", "dr:maxqb");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String drugbank ="DB00174;DB00130;";
		String guidetopharmacology="874;";
		String tcdb="2.A.23.3.3;";
		String dmdm="21542389;";
		String maxqb ="Q15758;";
		verify(drugbank, 1, result);
		verify(guidetopharmacology, 2, result);
		verify(tcdb, 3, result);
		verify(dmdm, 4, result);
		verify(maxqb, 5, result);
	}
	
	@Test
	void testXRefs5() {
		List<String> fields= Arrays.asList("accession", "dr:ensembl", "dr:reactome", "dr:interpro", "dr:prosite", "dr:pfam");
		DownloadableEntry  dl =new  DownloadableEntry(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String ensembl ="ENST00000412532;ENST00000434726;ENST00000542575;" ;
		String reactome="REACT_13796;";
		String interpro="IPR001991;IPR018107;";
		String prosite="PS00713;PS00714;";
		String pfam ="PF00375;";
		verify(ensembl, 1, result);
		verify(reactome, 2, result);
		verify(interpro, 3, result);
		verify(prosite, 4, result);
		verify(pfam, 5, result);
	}
	@Test
	void testProteome() {
		List<String> fields= Arrays.asList("accession", "dr:proteomes");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		String proteome="UP000009255: Genome; UP000116373: Genome; UP000170967: Genome";
		verify(proteome, 1, result);
		
	}
	@Test
	void testPdb() {
		List<String> fields= Arrays.asList("accession", "dr:pdb", "3d");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		String pdb="2ZNL;2ZTT;3A1G;";
		String d3d="X-ray crystallography (3)";
		verify(pdb, 1, result);
		verify(d3d, 2, result);
	}
	
	@Test
	void testkeyword() {
		List<String> fields= Arrays.asList("accession", "keyword", "keywordid");
		DownloadableEntry  dl =new  DownloadableEntry(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		String keyword="3D-structure;Complete proteome;Eukaryotic host gene expression shutoff by virus;"
				+ "Eukaryotic host transcription shutoff by virus;Host cytoplasm;"
				+ "Host gene expression shutoff by virus;Host nucleus;Host-virus interaction;"
				+ "Inhibition of host RNA polymerase II by virus;Nucleotide-binding;Nucleotidyltransferase;"
				+ "Phosphoprotein;Reference proteome;RNA-directed RNA polymerase;Transferase;Viral RNA replication;Viral transcription" ;
		String keywordid="";
		verify(keyword, 1, result);
		verify(keywordid, 2, result);
	}
	private void verify(String expected, int pos, List<String> result) {
		assertEquals(expected, result.get(pos));
	}
}
