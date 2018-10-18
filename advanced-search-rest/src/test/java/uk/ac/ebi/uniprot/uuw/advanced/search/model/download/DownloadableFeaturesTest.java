package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.dataservice.restful.features.domain.DbReferenceObject;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Evidence;
import uk.ac.ebi.uniprot.dataservice.restful.features.domain.Feature;

class DownloadableFeaturesTest {

	@Test
	void testGetData() {

		List<Feature> features =createTestFeatures();
		DownloadableFeatures dl = new DownloadableFeatures(features);
		Map<String, String> result = dl.attributeValues();
		assertEquals(4, result.size());
		verify("rs1064793108 rs1064793121", "dr:dbsnp", result);
		verify("DOMAIN 23 23 some domain.", "ft:domain", result);
		verify("HELIX 7 10 {ECO:0000244|PDB:2LO1}.", "ft:helix", result);
		String variantExp = "VARIANT 23 23 A -> G (in SCN1; dbSNP:rs1064793108). /FTId=VAR_064512.;"
				+ " VARIANT 27 27 B -> D (in another; dbSNP:rs1064793121)."
				+ " {ECO:0000269|PubMed:6142052, ECO:0000269|PubMed:12345}. /FTId=VAR_064556.";
		verify(variantExp, "ft:variant", result);
	}
	@Test
	void testGetFeatures() {
		List<Feature> features =createTestFeatures();
		List<String> featureList= DownloadableFeatures.getFeatures(features);
		List<String> expected = Arrays.asList(
				"Domain (1)", "Helix (1)", "Natural variant (2)"
				);
		assertEquals(expected, featureList);
	}

	List<Feature> createTestFeatures(){
		List<Feature> features = new ArrayList<>();
		features.add(createFeature("VARIANT", "23", "23", "in SCN1; dbSNP:rs1064793108", "VAR_064512", "A", "G"));
		Feature feature = createFeature("VARIANT", "27", "27", "in another; dbSNP:rs1064793121", "VAR_064556", "B",
				"D");
		List<Evidence> evidences = new ArrayList<>();
		evidences.add(createEvidence("ECO:0000269", "PubMed", "6142052"));
		evidences.add(createEvidence("ECO:0000269", "PubMed", "12345"));
		feature.setEvidences(evidences);
		features.add(feature);
		features.add(createFeature("DOMAIN", "23", "23", "some domain", null, null, null));
		Feature feature2 = createFeature("HELIX", "7", "10", "", null, null, null);
		List<Evidence> evidences2 = new ArrayList<>();
		evidences2.add(createEvidence("ECO:0000244", "PDB", "2LO1"));
		feature2.setEvidences(evidences2);
		features.add(feature2);

		return features;
	}
	private void verify(String expected, String field, Map<String, String> result) {
		String evaluated = result.get(field);
		assertEquals(expected, evaluated);
	}

	@Test
	void testFeatureWithFtIdToString() {
		Feature feature = createFeature("VARIANT", "23", "23", "in SCN1; dbSNP:rs1064793108", "VAR_064512", "A", "G");
		String result = DownloadableFeatures.featureToString(feature);
		String expected = "VARIANT 23 23 A -> G (in SCN1; dbSNP:rs1064793108). /FTId=VAR_064512.";
		assertEquals(result, expected);

	}

	@Test
	void testFeatureWithFtIdEvidenceToString() {
		Feature feature = createFeature("VARIANT", "23", "23", "in SCN1; dbSNP:rs1064793108", "VAR_064512", "A", "G");
		List<Evidence> evidences = new ArrayList<>();
		evidences.add(createEvidence("ECO:0000269", "PubMed", "6142052"));
		evidences.add(createEvidence("ECO:0000269", "PubMed", "12345"));
		feature.setEvidences(evidences);
		String result = DownloadableFeatures.featureToString(feature);
		String expected = "VARIANT 23 23 A -> G (in SCN1; dbSNP:rs1064793108)."
				+ " {ECO:0000269|PubMed:6142052, ECO:0000269|PubMed:12345}. /FTId=VAR_064512.";
		assertEquals(result, expected);

	}

	@Test
	void testFeatureToString() {
		Feature feature = createFeature("DOMAIN", "23", "23", "some domain", null, null, null);
		String result = DownloadableFeatures.featureToString(feature);
		String expected = "DOMAIN 23 23 some domain.";
		assertEquals(result, expected);

	}

	@Test
	void testFeatureWithEvidenceToString() {
		Feature feature = createFeature("DOMAIN", "23", "23", "some domain", null, null, null);
		List<Evidence> evidences = new ArrayList<>();
		evidences.add(createEvidence("ECO:0000269", "PubMed", "6142052"));
		evidences.add(createEvidence("ECO:0000269", "PubMed", "12345"));
		feature.setEvidences(evidences);
		String result = DownloadableFeatures.featureToString(feature);
		String expected = "DOMAIN 23 23 some domain. {ECO:0000269|PubMed:6142052, ECO:0000269|PubMed:12345}.";
		assertEquals(result, expected);

	}

	@Test
	void testFeatureNoDescWithEvidenceToString() {
		Feature feature = createFeature("HELIX", "7", "10", "", null, null, null);
		List<Evidence> evidences = new ArrayList<>();
		evidences.add(createEvidence("ECO:0000244", "PDB", "2LO1"));
		feature.setEvidences(evidences);
		String result = DownloadableFeatures.featureToString(feature);
		String expected = "HELIX 7 10 {ECO:0000244|PDB:2LO1}.";
		assertEquals(result, expected);

	}

	private Feature createFeature(String type, String begin, String end, String description, String ftid,
			String originSeq, String altSeq) {
		Feature feature = new Feature();
		feature.setType(type);
		feature.setBegin(begin);
		feature.setEnd(end);
		feature.setDescription(description);
		feature.setFtId(ftid);
		feature.setOrginalSequence(originSeq);
		feature.setAlternativeSequence(altSeq);
		return feature;
	}

	private Evidence createEvidence(String code, String dbType, String dbId) {
		DbReferenceObject dbref = new DbReferenceObject();
		dbref.setName(dbType);
		dbref.setId(dbId);
		Evidence evidence = new Evidence();
		evidence.setCode(code);
		evidence.setSource(dbref);
		return evidence;
	}
}
