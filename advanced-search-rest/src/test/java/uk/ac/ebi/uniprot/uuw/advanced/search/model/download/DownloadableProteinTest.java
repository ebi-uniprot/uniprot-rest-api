package uk.ac.ebi.uniprot.uuw.advanced.search.model.download;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.Protein;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.ProteinName;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.ProteinName.Name;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtResultFields;
import uk.ac.ebi.uniprot.dataservice.restful.entry.domain.model.EvidencedString;

class DownloadableProteinTest {
	
	@Test
	void testFields() {
		List<String> fields = DownloadableProtein.FIELDS;
		List<String> expected = Arrays
				.asList("protein_name", "ec" );
		assertEquals(expected, fields);
		for (String field : fields) {
			assertTrue(UniProtResultFields.INSTANCE.getField(field).isPresent());
		}
	}

	@Test
	void testRecName() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2";
		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}
	
	@Test
	void testRecNameWithAltNames() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<Name> alternativeName =new ArrayList<>();
		alternativeName.add(createName("alter name1", Collections.emptyList(), ecs));
		List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
		alternativeName.add(createName("altr name 2", shortNames2, Collections.emptyList()));
		builder.addAlternativeName(alternativeName);
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " (alter name1, EC 1.1.2.3, EC 1.2.22.2) (altr name 2, short11, short12)";

		assertEquals(expected, value);
		
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}
	
	@Test
	void testRecNameWithAltNamesAllergen() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<Name> alternativeName =new ArrayList<>();
		alternativeName.add(createName("alter name1", Collections.emptyList(), ecs));
		List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
		alternativeName.add(createName("altr name 2", shortNames2, Collections.emptyList()));
		builder.addAlternativeName(alternativeName);
		builder.addAllergenName(createEvidenceString("someAller"));
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " (alter name1, EC 1.1.2.3, EC 1.2.22.2) (altr name 2, short11, short12)"
				+ " (allergen someAller)";

		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}
	
	@Test
	void testRecNameWithAltNamesAllergenBiotech() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<Name> alternativeName =new ArrayList<>();
		alternativeName.add(createName("alter name1", Collections.emptyList(), ecs));
		List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
		alternativeName.add(createName("altr name 2", shortNames2, Collections.emptyList()));
		builder.addAlternativeName(alternativeName);
		builder.addAllergenName(createEvidenceString("someAller"));
		builder.addBiotechName(createEvidenceString("some biote"));
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " (alter name1, EC 1.1.2.3, EC 1.2.22.2) (altr name 2, short11, short12)"
				+ " (allergen someAller) (biotech some biote)";

		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}
	
	@Test
	void testRecNameWithAltNamesAllergenCdAntigen() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<Name> alternativeName =new ArrayList<>();
		alternativeName.add(createName("alter name1", Collections.emptyList(), ecs));
		List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
		alternativeName.add(createName("altr name 2", shortNames2, Collections.emptyList()));
		builder.addAlternativeName(alternativeName);
		builder.addAllergenName(createEvidenceString("someAller"));
		List<EvidencedString> cdAntigenName = new ArrayList<>();
		cdAntigenName.add(createEvidenceString("some antig1"));
		cdAntigenName.add(createEvidenceString("some antig2"));
		builder.addCdAntigenName(cdAntigenName);
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " (alter name1, EC 1.1.2.3, EC 1.2.22.2) (altr name 2, short11, short12)"
				+ " (allergen someAller)"
				+ " (CD antigen some antig1) (CD antigen some antig2)";

		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}
	
	@Test
	void testRecNameWithAltNamesAllergenInn() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<Name> alternativeName =new ArrayList<>();
		alternativeName.add(createName("alter name1", Collections.emptyList(), ecs));
		List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
		alternativeName.add(createName("altr name 2", shortNames2, Collections.emptyList()));
		builder.addAlternativeName(alternativeName);
		builder.addAllergenName(createEvidenceString("someAller"));
		List<EvidencedString> inns = new ArrayList<>();
		inns.add(createEvidenceString("some antig1"));
		inns.add(createEvidenceString("some antig2"));
		builder.addInnName(inns);
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " (alter name1, EC 1.1.2.3, EC 1.2.22.2) (altr name 2, short11, short12)"
				+ " (allergen someAller)"
				+ " (some antig1) (some antig2)";

		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}
	
	@Test
	void testSubnames() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		List<Name> subnames =new ArrayList<>();
		subnames.add(createName("subname name1", Collections.emptyList(), ecs));
		List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
		subnames.add(createName("subname name 2", shortNames2, Collections.emptyList()));
		builder.addSubmittedName(subnames);
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "subname name1, EC 1.1.2.3, EC 1.2.22.2 (subname name 2, short11, short12)";
		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.2.22.2", ec);
	}

	@Test
	void testRecNameWithContain() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<ProteinName> component  =new ArrayList<>();
		component.add(createProteinName("some contains1", true));
		component.add(createProteinName("some contains 2", false));
		builder.addComponents(component);
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " [Cleaved into: some contains1, sh1, sh2, EC 1.1.22.3, EC 1.2.34.2 (new Altname1, EC 1.1.22.3, EC 1.2.34.2) (new Altname 2, short11, short12);"
				+ " some contains 2, sh1, sh2, EC 1.1.22.3, EC 1.2.34.2 ]" 
				;
		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.1.22.3; 1.2.22.2; 1.2.34.2", ec);
	}
	
	@Test
	void testRecNameWithContainInclude() {
		Protein.Builder builder =Protein.newBuilder();
		List<String> shortNames = Arrays.asList(new String[]{"short1", "short2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.2.3", "1.2.22.2"});
		builder.addRecommendedName(createName("some full name", shortNames, ecs));
		List<ProteinName> component  =new ArrayList<>();
		component.add(createProteinName("some contains1", true));
		component.add(createProteinName("some contains 2", false));
		builder.addComponents(component);
		List<ProteinName> domain  =new ArrayList<>();
		domain.add(createProteinName("some domain1", false));
		domain.add(createProteinName("some domain 2", true));
		builder.addDomain(domain);
		Protein protein = builder.build();
		DownloadableProtein downloadable = new DownloadableProtein(protein);
		Map<String, String> result = downloadable.attributeValues();
		assertEquals(2, result.size());
		String value = result.get(DownloadableProtein.FIELDS.get(0));
		assertNotNull(value);
		String expected = "some full name, short1, short2, EC 1.1.2.3, EC 1.2.22.2"
				+ " [Cleaved into: some contains1, sh1, sh2, EC 1.1.22.3, EC 1.2.34.2 (new Altname1, EC 1.1.22.3, EC 1.2.34.2) (new Altname 2, short11, short12);"
				+ " some contains 2, sh1, sh2, EC 1.1.22.3, EC 1.2.34.2 ]"
				+ " [Includes: some domain1, sh1, sh2, EC 1.1.22.3, EC 1.2.34.2; some domain 2, sh1, sh2, EC 1.1.22.3, EC 1.2.34.2"
				+ " (new Altname1, EC 1.1.22.3, EC 1.2.34.2) (new Altname 2, short11, short12) ]" 
				;
		assertEquals(expected, value);
		String ec = result.get(DownloadableProtein.FIELDS.get(1));
		assertEquals("1.1.2.3; 1.1.22.3; 1.2.22.2; 1.2.34.2", ec);
	}
	private ProteinName createProteinName(String fullName, boolean hasAltName) {
		List<String> shortNames = Arrays.asList(new String[]{"sh1", "sh2"});
		List<String> ecs = Arrays.asList(new String[]{"1.1.22.3", "1.2.34.2"});
		Name recName = createName(fullName, shortNames, ecs);
		if(hasAltName) {
			List<Name> alternativeName =new ArrayList<>();
			alternativeName.add(createName("new Altname1", Collections.emptyList(), ecs));
			List<String> shortNames2 = Arrays.asList(new String[]{"short11", "short12"});
			alternativeName.add(createName("new Altname 2", shortNames2, Collections.emptyList()));
			return new ProteinName(recName, alternativeName );
		}else
		return new ProteinName(recName, Collections.emptyList() );
	}
	private Name createName(String fullname, List<String> shortNames, List<String> ecs) {
		Name name = new Name(
				createEvidenceString(fullname),
				shortNames.stream().map(this::createEvidenceString)
				.collect(Collectors.toList()),
				ecs.stream().map(this::createEvidenceString)
				.collect(Collectors.toList())
				);
		
		return name;
	}
	private EvidencedString createEvidenceString(String val) {
		return new EvidencedString(val, Collections.emptyList());
	}
}
