package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.nio.ByteBuffer;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.apache.solr.client.solrj.SolrClient;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import com.beust.jcommander.internal.Lists;
import com.fasterxml.jackson.core.JsonProcessingException;

import uk.ac.ebi.uniprot.api.configure.proteome.ProteomeResultFields;
import uk.ac.ebi.uniprot.api.proteome.ProteomeRestApplication;
import uk.ac.ebi.uniprot.api.proteome.repository.ProteomeFacetConfig;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractSearchControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.SaveScenario;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.api.taxonomy.TaxonomyController;
import uk.ac.ebi.uniprot.domain.DBCrossReference;
import uk.ac.ebi.uniprot.domain.builder.DBCrossReferenceBuilder;
import uk.ac.ebi.uniprot.domain.citation.Citation;
import uk.ac.ebi.uniprot.domain.proteome.Component;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeEntry;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeId;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeType;
import uk.ac.ebi.uniprot.domain.proteome.ProteomeXReferenceType;
import uk.ac.ebi.uniprot.domain.proteome.Superkingdom;
import uk.ac.ebi.uniprot.domain.proteome.builder.ComponentBuilder;
import uk.ac.ebi.uniprot.domain.proteome.builder.ProteomeEntryBuilder;
import uk.ac.ebi.uniprot.domain.proteome.builder.ProteomeIdBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.taxonomy.Taxonomy;
import uk.ac.ebi.uniprot.domain.uniprot.taxonomy.builder.TaxonomyBuilder;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;
import uk.ac.ebi.uniprot.search.document.proteome.ProteomeDocument;
import uk.ac.ebi.uniprot.search.field.ProteomeField;
import uk.ac.ebi.uniprot.search.field.SearchField;

/**
 *
 * @author jluo
 * @date: 13 Jun 2019
 *
*/
@ContextConfiguration(classes= {ProteomeDataStoreTestConfig.class, ProteomeRestApplication.class})
@ActiveProfiles(profiles = "proteome_offline")
@WebMvcTest(TaxonomyController.class)
@ExtendWith(value = {SpringExtension.class, ProteomeSearchControllerIT.ProteomeSearchContentTypeParamResolver.class,
		ProteomeSearchControllerIT.ProteomeSearchParameterResolver.class})
public class ProteomeSearchControllerIT extends AbstractSearchControllerIT {
	 private static final String UPID_PREF = "UP000005";
	    @Autowired
	    private MockMvc mockMvc;
	    @Autowired
	    @Qualifier("proteome")
	    SolrClient solrClient;
	    @Autowired
	    private DataStoreManager storeManager;
	    
	    @Autowired
	    private ProteomeFacetConfig facetConfig;
	@Override
	protected void cleanEntries() {
		 storeManager.cleanSolr(DataStoreManager.StoreType.PROTEOME);

	}

	@Override
	protected MockMvc getMockMvc() {
		return mockMvc;
	}

	@Override
	protected String getSearchRequestPath() {
		 return "/proteome/search";
	}

	@Override
	protected int getDefaultPageSize() {
		  return 25;
	}

	@Override
	protected List<SearchField> getAllSearchFields() {
		 return Arrays.asList(ProteomeField.Search.values());
	}

	@Override
	protected String getFieldValueForValidatedField(SearchField searchField) {
		 String value = "";
	        switch (searchField.getName()) {
	            case "upid":
	            	value =UPID_PREF +231;
	            	break;
	            case "organism_id":
	            case "taxonomy_id":
	            	value ="9606";
	            	break;
	            	
	            case "organism_name":
	                value = "human";
	                break;
	            case "annotation_score":
	            	value ="3";
	            	break;
	            case "proteome_type":
	            	value ="1";
	            	break;
	        }
	        return value;
	}

	@Override
	protected List<String> getAllSortFields() {
		 return Arrays.stream(ProteomeField.Sort.values())
	                .map(ProteomeField.Sort::name)
	                .collect(Collectors.toList());
	}

	@Override
	protected List<String> getAllFacetFields() {
		 return new ArrayList<>(facetConfig.getFacetNames());
	}

	@Override
	protected List<String> getAllReturnedFields() {
		return Lists.newArrayList(
		ProteomeResultFields.INSTANCE.getAllFields()
		.keySet());
	}

	@Override
	protected void saveEntry(SaveScenario saveContext) {
		saveEntry(231);
		saveEntry(520);

	}

	@Override
	protected void saveEntries(int numberOfEntries) {
		IntStream.rangeClosed(1, numberOfEntries)
		.forEach(i-> saveEntry(i));
	}
	
	private void saveEntry(int i) {
		ProteomeEntry entry = createEntry(i);
		ProteomeDocument document = new ProteomeDocument();
		document.upid =entry.getId().getValue();
		document.organismName.add("Homo sapiens");
		document.organismName.add("human");
		document.organismTaxId=9606;
		document.content.add(document.upid);
		document.content.addAll(document.organismName);
		document.content.add(entry.getDescription());
		document.proteomeStored = getBinary(entry);	
		document.isRedundant =i%2 !=0;
		document.isReferenceProteome =i%2 !=0;
		document.proteomeType =1;
		document.score =3;
		document.organismTaxon = document.organismName;
		document.taxLineageIds.add(9606);
		document.organismSort ="human";
		document.superkingdom="eukaryota";
		document.genomeAccession.add("someAcc");
		document.genomeAssembly.add("someAcc");
	
		storeManager.saveDocs(DataStoreManager.StoreType.PROTEOME, document);
	}
	
	 private ByteBuffer getBinary(ProteomeEntry entry) {
	        try {
	            return ByteBuffer.wrap(ProteomeJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
	        } catch (JsonProcessingException e) {
	            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
	        }
	    }
	
	private String getName(String prefix, int i) {
		if(i<10) {
			return prefix + "00" +i;
		}else if(i<100) {
			return prefix +"0" +i;
		}else
			return prefix  + i;
		
	}
	private ProteomeEntry createEntry(int i) {
		ProteomeId proteomeId = new ProteomeIdBuilder (getName(UPID_PREF , i)).build();
		String description =getName("Description", i);
		Taxonomy taxonomy = TaxonomyBuilder.newInstance().taxonId(9606).scientificName("Homo sapiens").build();
		LocalDate modified = LocalDate.of(2015, 11, 5);
	//	String reId = "UP000005641";
	//	ProteomeId redId = new ProteomeIdBuilder (reId).build();
		List<DBCrossReference<ProteomeXReferenceType>> xrefs =new ArrayList<>();
		DBCrossReference<ProteomeXReferenceType> xref1 =
				new DBCrossReferenceBuilder<ProteomeXReferenceType>()
				.databaseType(ProteomeXReferenceType.GENOME_ACCESSION)
				.id(getName("ACA", i))
				.build();
		xrefs.add(xref1);
		List<Component> components = new ArrayList<>();
		Component component1 =
		ComponentBuilder.newInstance()
		.name("someName1").description("some description")
		.type(uk.ac.ebi.uniprot.domain.proteome.ComponentType.UNPLACED)				
		.build();
		
		Component component2 =
				ComponentBuilder.newInstance()
				.name("someName2").description("some description 2")
				.type(uk.ac.ebi.uniprot.domain.proteome.ComponentType.SEGMENTED_GENOME)			
				.build();
		
		components.add(component1);
		components.add(component2);
		List<Citation> citations = new ArrayList<>();
		ProteomeEntryBuilder builder = ProteomeEntryBuilder.newInstance().proteomeId(proteomeId)
				.description(description)
				.taxonomy(taxonomy)
				.modified(modified)
				.proteomeType(ProteomeType.NORMAL)
			//	.redundantTo(redId)
				.dbXReferences(xrefs)
				.components(components)
				.superkingdom(Superkingdom.EUKARYOTA)
				.references(citations)
				.annotationScore(15);
		
		return builder.build();
	}
	
	
	  static class ProteomeSearchParameterResolver extends AbstractSearchParameterResolver {

	        @Override
	        protected SearchParameter searchCanReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upid:UP000005231"))
	                    .resultMatcher(jsonPath("$.results.*.id.value",contains("UP000005231")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchCanReturnNotFoundParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upid:UP000004231"))
	                    .resultMatcher(jsonPath("$.results.size()",is(0)))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchAllowWildcardQueryAllDocumentsParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upid:*"))
	                    .resultMatcher(jsonPath("$.results.*.id.value",contains("UP000005231", "UP000005520")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("organism_name:[1 TO 10]"))
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",
	                    		contains("'organism_name' filter type 'range' is invalid. Expected 'term' filter type")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upid:INVALID OR organism_id:INVALID " +
	                            "OR organism_name:INVALID OR taxonomy_id:invalid OR superkingdom:invalid"))
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",containsInAnyOrder(
	                    		"The 'upid' value has invalid format. It should be a valid Proteome UPID",
	                            "The organism id filter value should be a number",
	                            "The taxonomy id filter value should be a number"
	                      )))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("sort", Collections.singletonList("annotation_score desc"))
	                    .resultMatcher(jsonPath("$.results.*.id.value",contains("UP000005231", "UP000005520")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("fields", Collections.singletonList("organism"))
	                    .resultMatcher(jsonPath("$.results.*.id.value",contains("UP000005231", "UP000005520")))
	                  
	                    .resultMatcher(jsonPath("$.results.*.description",contains("Description231","Description520")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("facets", Collections.singletonList("reference"))
	                    .resultMatcher(jsonPath("$.results.*.id.value",contains("UP000005231", "UP000005520")))
	                    .build();
	        }
	    }


	    static class ProteomeSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver{

	        @Override
	        protected SearchContentTypeParam searchSuccessContentTypesParam() {
	            return SearchContentTypeParam.builder()
	                    .query("organism_id:9606")
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.results.*.id.value",contains("UP000005231", "UP000005520")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_XML)
	                            .resultMatcher(content().string(containsString("UP000005231")))
	                            .resultMatcher(content().string(containsString("UP000005520")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("UP000005231")))
	                            .resultMatcher(content().string(containsString("UP000005520")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("\tOrganism\tOrganism ID\tProtein count")))
	                            .resultMatcher(content().string(containsString("UP000005231\tHomo sapiens\t9606\t0")))
	                            .resultMatcher(content().string(containsString("UP000005520\tHomo sapiens\t9606\t0")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
	                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
	                            .build())
	                    .build();
	        }

	        @Override
	        protected SearchContentTypeParam searchBadRequestContentTypesParam() {
	            return SearchContentTypeParam.builder()
	                    .query("upid:invalid")
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                            .resultMatcher(jsonPath("$.messages.*",contains("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_XML)
	                            .resultMatcher(content().string(containsString("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .build();
	        }
	    }
}

