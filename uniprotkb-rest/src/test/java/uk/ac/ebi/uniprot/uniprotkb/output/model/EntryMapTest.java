package uk.ac.ebi.uniprot.uniprotkb.output.model;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import uk.ac.ebi.kraken.interfaces.uniprot.UniProtEntry;
import uk.ac.ebi.kraken.model.factories.DefaultUniProtFactory;
import uk.ac.ebi.kraken.parser.UniProtParser;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.converter.EntryConverter;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.UPEntry;

import java.io.InputStream;
import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class EntryMapTest {
	private static UPEntry entryQ15758;
	private static UPEntry entryP03431;
	private static UPEntry entryQ84MC7;
	private static UPEntry entryQ70KY3;
	
	@BeforeAll
	static void setup() throws Exception {
		 InputStream is = EntryMapTest.class.getResourceAsStream("/downloadIT/Q15758.dat" );
	     UniProtEntry entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	     EntryConverter converter = new EntryConverter();
	     entryQ15758 =converter.apply(entry);
	     is.close();
	     
	     is = EntryMapTest.class.getResourceAsStream("/downloadIT/P03431.dat" );
	      entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	      entryP03431 =converter.apply(entry);
	      is.close();
	      is = EntryMapTest.class.getResourceAsStream("/downloadIT/Q84MC7.dat" );
	      entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	      entryQ84MC7 =converter.apply(entry);
	      is.close();
	      is = EntryMapTest.class.getResourceAsStream("/downloadIT/Q70KY3.dat" );
	      entry= UniProtParser.parse(is, DefaultUniProtFactory.getInstance());
	      entryQ70KY3 =converter.apply(entry);
	      is.close();
	      
	      
	}
	@Test
	void testIdAccession() {
		List<String> fields= Arrays.asList("accession", "id");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(2, result.size());
		verify("Q15758", 0, result);
		verify("AAAT_HUMAN", 1, result);
	}
	@Test
	void testInfo() {
		List<String> fields= Arrays.asList("reviewed", "version", "protein_existence");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("reviewed", 0, result);
		verify("185", 1, result);
		verify("Evidence at protein level", 2, result);
	}
	@Test
	void testSequence() {
		List<String> fields= Arrays.asList("length", "mass", "sequence_version", "sequence");
		EntryMap dl =new EntryMap(entryQ15758, fields);
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
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
		EntryMap dl =new EntryMap(entryP03431, fields);
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
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
		EntryMap dl =new EntryMap(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		verify("Influenza A virus (strain A/Puerto Rico/8/1934 H1N1)", 1, result);
		verify("Aves [TaxID: 8782]; Homo sapiens (Human) [TaxID: 9606]; Sus scrofa (Pig) [TaxID: 9823]", 2, result);
	
	}
	@Test
	void testAlterProduct() {
		List<String> fields= Arrays.asList("accession", "cc:alternative_products");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);

		String altProd = "ALTERNATIVE PRODUCTS: " +
				" Event=Alternative splicing, Alternative initiation; Named isoforms=3;" +
				"Comment=A number of isoforms are produced by alternative" +
				" initiation. Isoforms start at multiple alternative CUG and GUG" +
				" codons. {ECO:0000269|PubMed:11350958}; " +
				" Name=1;" +
				" IsoId=Q15758-1; Sequence=displayed;" +
				" Name=2;" +
				" IsoId=Q15758-2; Sequence=VSP_046354;" +
				" Note=No experimental confirmation available.;" +
				" Name=3;" +
				" IsoId=Q15758-3; Sequence=VSP_046851;" +
				" Note=No experimental confirmation available.;" ;
		verify(altProd, 1, result);
	}
	
	@Test
	void testComments() {
		List<String> fields= Arrays.asList("accession", "cc:function", "cc:domain", "cc:subunit", "cc:interaction");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String cfunction ="FUNCTION: Sodium-dependent amino acids transporter that has a" +
				" broad substrate specificity, with a preference for zwitterionic" +
				" amino acids. It accepts as substrates all neutral amino acids," +
				" including glutamine, asparagine, and branched-chain and aromatic" +
				" amino acids, and excludes methylated, anionic, and cationic amino" +
				" acids (PubMed:8702519, PubMed:29872227). Through binding of the" +
				" fusogenic protein syncytin-1/ERVW-1 may mediate trophoblasts" +
				" syncytialization, the spontaneous fusion of their plasma" +
				" membranes, an essential process in placental development" +
				" (PubMed:10708449, PubMed:23492904) {ECO:0000269|PubMed:10708449," +
				" ECO:0000269|PubMed:23492904, ECO:0000269|PubMed:29872227," +
				" ECO:0000269|PubMed:8702519}.;" +
				" FUNCTION: (Microbial infection) Acts as a cell surface receptor" +
				" for Feline endogenous virus RD114 {ECO:0000269|PubMed:10051606," +
				" ECO:0000269|PubMed:10196349}.;" +
				" FUNCTION: (Microbial infection) Acts as a cell surface receptor" +
				" for Baboon M7 endogenous virus {ECO:0000269|PubMed:10196349}.;" +
				" FUNCTION: (Microbial infection) Acts as a cell surface receptor" +
				" for type D simian retroviruses {ECO:0000269|PubMed:10196349}.";

		String cfomain ="";
		String csubunit="SUBUNIT: Homotrimer (Probable) (PubMed:29872227). Interacts with" +
				" ERVH48-1/suppressyn; may negatively regulate syncytialization" +
				" (PubMed:23492904) {ECO:0000269|PubMed:23492904," +
				" ECO:0000269|PubMed:29872227, ECO:0000305|PubMed:28424515}.";
	
		String cinteraction ="Q99942" ;
		verify(cfunction, 1, result);
		verify(cfomain, 2, result);
		verify(csubunit, 3, result);
		verify(cinteraction, 4, result);
	}
	
	@Test
	 void testComments2() {
		List<String> fields= Arrays.asList("accession", "cc:interaction", "cc:subcellular_location", "cc:ptm", "cc:similarity");
		EntryMap dl =new EntryMap(entryP03431, fields);
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
		EntryMap dl =new EntryMap(entryP03431, fields);
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
		EntryMap dl =new EntryMap(entryQ84MC7, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q84MC7", 0, result);
		String seqCaution ="SEQUENCE CAUTION:  Sequence=AAF97339.1; Type=ERRONEOUS_INITIATION; Note=Translation N-terminally extended.; Evidence={ECO:0000305};" ;
		String seqCaution2="SEQUENCE CAUTION:  Sequence=AAM65514.1; Type=ERRONEOUS_PREDICTION; Evidence={ECO:0000305};";
		
		
		verify(seqCaution, 1, result);
		verify(seqCaution2, 2, result);	
	}
	
	@Test
	void testBPCP() {
		List<String> fields= Arrays.asList("accession",  "absorption", "kinetics", "ph_dependence",
				"redox_potential", "temp_dependence");
		EntryMap dl =new EntryMap(entryQ70KY3, fields);
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String chain ="CHAIN 1 541 Neutral amino acid transporter B(0). /FTId=PRO_0000202082.";
		String topo_dom ="TOPO_DOM 1 51 Cytoplasmic. {ECO:0000305}.;" +
				" TOPO_DOM 82 94 Extracellular. {ECO:0000305}.;" +
				" TOPO_DOM 117 130 Cytoplasmic. {ECO:0000305}.;" +
				" TOPO_DOM 154 224 Extracellular. {ECO:0000305}.;" +
				" TOPO_DOM 249 257 Cytoplasmic. {ECO:0000305}.;" +
				" TOPO_DOM 286 306 Extracellular. {ECO:0000305}.;" +
				" TOPO_DOM 329 333 Cytoplasmic. {ECO:0000305}.;" +
				" TOPO_DOM 365 373 Cytoplasmic. {ECO:0000305}.;" +
				" TOPO_DOM 401 413 Extracellular. {ECO:0000305}.;" +
				" TOPO_DOM 448 460 Extracellular. {ECO:0000305}.;" +
				" TOPO_DOM 483 541 Cytoplasmic. {ECO:0000305}.";
		String transmem ="TRANSMEM 52 81 Helical; Name=1. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 95 116 Helical; Name=2. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 131 153 Helical; Name=3. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 225 248 Helical; Name=4. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 258 285 Helical; Name=5. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 307 328 Helical; Name=6. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 374 400 Helical; Name=7. {ECO:0000305|PubMed:29872227}.;" +
				" TRANSMEM 461 482 Helical; Name=8. {ECO:0000305|PubMed:29872227}." ;
		String modRes="MOD_RES 1 1 N-acetylmethionine. {ECO:0000244|PubMed:19413330, ECO:0000244|PubMed:22814378}.;" +
				" MOD_RES 493 493 Phosphoserine. {ECO:0000244|PubMed:21406692, ECO:0000244|PubMed:23186163}.;" +
				" MOD_RES 494 494 Phosphothreonine. {ECO:0000244|PubMed:23186163}.;" +
				" MOD_RES 503 503 Phosphoserine. {ECO:0000244|PubMed:19690332}.;" +
				" MOD_RES 535 535 Phosphoserine. {ECO:0000244|PubMed:17081983, ECO:0000244|PubMed:18669648," +
					" ECO:0000244|PubMed:19690332, ECO:0000244|PubMed:20068231, ECO:0000244|PubMed:23186163}.;" +
				" MOD_RES 539 539 Phosphoserine. {ECO:0000244|PubMed:23186163}." ;
		String carbohyd ="CARBOHYD 163 163 N-linked (GlcNAc...) asparagine. {ECO:0000255}.; " +
				"CARBOHYD 212 212 N-linked (GlcNAc...) asparagine. {ECO:0000269|PubMed:19349973}." ;
		String varSeq="VAR_SEQ 1 228 Missing (in isoform 3). {ECO:0000303|PubMed:14702039}. /FTId=VSP_046851.;"
				+ " VAR_SEQ 1 203 MVADPPRDSKGLAAAEPTANGGLALASIEDQGAAAGGYCGSRDQVRRCLRANLLVLLTVVAVVAGVALGLGVSGAGGALA"
				+ "LGPERLSAFVFPGELLLRLLRMIILPLVVCSLIGGAASLDPGALGRLGAWALLFFLVTTLLASALGVGLALALQPGAASAAINASVGAAGSAENAP"
				+ "SKEVLDSFLDLARNIFPSNLVSAAFRS -> M (in isoform 2). {ECO:0000303|PubMed:14702039}. /FTId=VSP_046354." 
				;
		String variant ="VARIANT 17 17 P -> A (in dbSNP:rs3027956). /FTId=VAR_020439.;" +
				" VARIANT 512 512 V -> L (in dbSNP:rs3027961). {ECO:0000244|PubMed:19690332, ECO:0000269|PubMed:14702039}. /FTId=VAR_013517." ;
		String conflict ="CONFLICT 18 24 TANGGLA -> PPTGAWQ (in Ref. 1; AAC50629). {ECO:0000305}.;" +
				" CONFLICT 44 44 Q -> L (in Ref. 1; AAC50629). {ECO:0000305}.;" +
				" CONFLICT 84 87 ERLS -> GALE (in Ref. 1; AAC50629). {ECO:0000305}.;" +
				" CONFLICT 341 341 V -> A (in Ref. 5; BAH14917). {ECO:0000305}.;" +
				" CONFLICT 453 453 I -> V (in Ref. 2; AAD09812). {ECO:0000305}.;" +
				" CONFLICT 460 460 D -> G (in Ref. 2; AAD09812). {ECO:0000305}.;" +
				" CONFLICT 463 463 V -> A (in Ref. 2; AAD09812). {ECO:0000305}.;" +
				" CONFLICT 508 508 D -> G (in Ref. 2; AAD09812). {ECO:0000305}." ;
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String numOfFeature ="Alternative sequence (2); Beta strand (2); Chain (1); Glycosylation (2);"
				+ " Helix (11); Intramembrane (2); Metal binding (5);"
				+ " Modified residue (6); Natural variant (2); Sequence conflict (8);"
				+ " Topological domain (11); Transmembrane (8)";
		verify(numOfFeature, 1, result);
	}
	@Test
	void testReferences() {
		List<String> fields= Arrays.asList("accession", "pm_id");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String pmids ="8702519; 10051606; 10196349; 14702039; 15057824; 15489334; 11350958; 10708449;" +
				" 17081983; 17081065; 18669648; 19413330; 19349973; 19690332; 20068231; 21269460;" +
				" 21406692; 22814378; 23186163; 23492904; 25944712; 28424515; 29872227";
		verify(pmids, 1, result);
	}
	
	@Test
	void testGOTerm() {
		List<String> fields= Arrays.asList("accession", "go", "go_c", "go_f", "go_p", "go_id");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String go="extracellular exosome [GO:0070062];" +
				" integral component of membrane [GO:0016021];" +
				" integral component of plasma membrane [GO:0005887];" +
				" melanosome [GO:0042470];" +
				" membrane [GO:0016020];" +
				" plasma membrane [GO:0005886];" +
				" amino acid transmembrane transporter activity [GO:0015171];" +
				" L-glutamine transmembrane transporter activity [GO:0015186];" +
				" L-serine transmembrane transporter activity [GO:0015194];" +
				" metal ion binding [GO:0046872];" +
				" neutral amino acid transmembrane transporter activity [GO:0015175];" +
				" signaling receptor activity [GO:0038023];" +
				" symporter activity [GO:0015293];" +
				" virus receptor activity [GO:0001618];" +
				" amino acid transport [GO:0006865];" +
				" glutamine secretion [GO:0010585];" +
				" glutamine transport [GO:0006868];" +
				" L-glutamine import across plasma membrane [GO:1903803];" +
				" neutral amino acid transport [GO:0015804];" +
				" protein homotrimerization [GO:0070207]";
			
		String go_c= "extracellular exosome [GO:0070062];" +
				" integral component of membrane [GO:0016021];" +
				" integral component of plasma membrane [GO:0005887];" +
				" melanosome [GO:0042470];" +
				" membrane [GO:0016020];" +
				" plasma membrane [GO:0005886]" ;

		String go_f ="amino acid transmembrane transporter activity [GO:0015171];" +
				" L-glutamine transmembrane transporter activity [GO:0015186];" +
				" L-serine transmembrane transporter activity [GO:0015194];" +
				" metal ion binding [GO:0046872];" +
				" neutral amino acid transmembrane transporter activity [GO:0015175];" +
				" signaling receptor activity [GO:0038023];" +
				" symporter activity [GO:0015293];" +
				" virus receptor activity [GO:0001618]" ;

		String go_p = "amino acid transport [GO:0006865];" +
				" glutamine secretion [GO:0010585];" +
				" glutamine transport [GO:0006868];" +
				" L-glutamine import across plasma membrane [GO:1903803];" +
				" neutral amino acid transport [GO:0015804];" +
				" protein homotrimerization [GO:0070207]";

		String go_id ="GO:0001618; GO:0005886; GO:0005887; GO:0006865; GO:0006868; GO:0010585; " +
				"GO:0015171; GO:0015175; GO:0015186; GO:0015194; GO:0015293; GO:0015804; GO:0016020; " +
				"GO:0016021; GO:0038023; GO:0042470; GO:0046872; GO:0070062; GO:0070207; GO:1903803" ;
		verify(go, 1, result);
		verify(go_c, 2, result);
		verify(go_f, 3, result);
		verify(go_p, 4, result);
		verify(go_id, 5, result);
		
	}
	@Test
	void testXRefs1() {
		List<String> fields= Arrays.asList("accession", "dr:embl", "dr:ccds", "dr:refseq", "dr:unigene", "dr:proteinmodelportal");
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String embl ="U53347;AF102826;AF105423;GQ919058;AK292690;AK299137;AK301661;AK316546;AC008622;CH471126;BC000062;AF334818;";
		String ccds="CCDS12692.1 [Q15758-1];CCDS46125.1 [Q15758-2];CCDS46126.1 [Q15758-3];";
		String refseq="NP_001138616.1 [Q15758-3];NP_001138617.1 [Q15758-2];NP_005619.1 [Q15758-1];";
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String smr ="Q15758;";
		String biogrid="112401;";
		String intact="Q15758;";
		String mint="Q15758;";
		String string ="9606.ENSP00000444408;";
		verify(smr, 1, result);
		verify(biogrid, 2, result);
		verify(intact, 3, result);
		verify(mint, 4, result);
		verify(string, 5, result);
	}
	
	@Test
	void testXRefs3() {
		List<String> fields= Arrays.asList("accession", "dr:drugbank", "dr:guidetopharmacology", "dr:tcdb", "dr:dmdm", "dr:maxqb");
		EntryMap dl =new EntryMap(entryQ15758, fields);
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
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
		EntryMap dl =new EntryMap(entryQ15758, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("Q15758", 0, result);
		String ensembl ="ENST00000412532 [Q15758-3];ENST00000434726 [Q15758-2];ENST00000542575 [Q15758-1];" ;
		String reactome="R-HSA-352230;";
		String interpro="IPR001991;IPR018107;IPR036458;";
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
		EntryMap dl =new EntryMap(entryP03431, fields);
		List<String> result= dl.getData();
		assertEquals(fields.size(), result.size());
		verify("P03431", 0, result);
		String proteome="UP000009255: Genome; UP000116373: Genome; UP000170967: Genome";
		verify(proteome, 1, result);
		
	}
	@Test
	void testPdb() {
		List<String> fields= Arrays.asList("accession", "dr:pdb", "3d");
		EntryMap dl =new EntryMap(entryP03431, fields);
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
		EntryMap dl =new EntryMap(entryP03431, fields);
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
