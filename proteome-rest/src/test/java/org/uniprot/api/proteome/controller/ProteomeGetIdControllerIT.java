package org.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.contains;
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
import java.util.List;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.controller.ProteomeController;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.core.DBCrossReference;
import org.uniprot.core.builder.DBCrossReferenceBuilder;
import org.uniprot.core.citation.Citation;
import org.uniprot.core.json.parser.proteome.ProteomeJsonConfig;
import org.uniprot.core.proteome.Component;
import org.uniprot.core.proteome.ProteomeEntry;
import org.uniprot.core.proteome.ProteomeId;
import org.uniprot.core.proteome.ProteomeType;
import org.uniprot.core.proteome.ProteomeXReferenceType;
import org.uniprot.core.proteome.Superkingdom;
import org.uniprot.core.proteome.builder.ComponentBuilder;
import org.uniprot.core.proteome.builder.ProteomeEntryBuilder;
import org.uniprot.core.proteome.builder.ProteomeIdBuilder;
import org.uniprot.core.uniprot.taxonomy.Taxonomy;
import org.uniprot.core.uniprot.taxonomy.builder.TaxonomyBuilder;
import org.uniprot.store.indexer.DataStoreManager;
import org.uniprot.store.search.document.proteome.ProteomeDocument;

import com.fasterxml.jackson.core.JsonProcessingException;

/**
 *
 * @author jluo
 * @date: 12 Jun 2019
 *
*/
@ContextConfiguration(classes= {ProteomeDataStoreTestConfig.class, ProteomeRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "proteome_offline")
@WebMvcTest(ProteomeController.class)
@ExtendWith(value = {SpringExtension.class, ProteomeGetIdControllerIT.ProteomeGetIdParameterResolver.class,
		ProteomeGetIdControllerIT.ProteomeGetIdContentTypeParamResolver.class})
public class ProteomeGetIdControllerIT extends AbstractGetByIdControllerIT {
	 private static final String UPID = "UP000005640";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;
	@Override
	protected void saveEntry() {
		ProteomeEntry entry = create();
		ProteomeDocument document = new ProteomeDocument();
		document.upid =UPID;
		document.proteomeStored = getBinary(entry);
		
		  storeManager.saveDocs(DataStoreManager.StoreType.PROTEOME, document);

	}
	
	 private ByteBuffer getBinary(ProteomeEntry entry) {
	        try {
	            return ByteBuffer.wrap(ProteomeJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
	        } catch (JsonProcessingException e) {
	            throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
	        }
	    }

	private ProteomeEntry create() {
		ProteomeId proteomeId = new ProteomeIdBuilder (UPID).build();
		String description ="about some proteome";
		Taxonomy taxonomy = TaxonomyBuilder.newInstance().taxonId(9606).scientificName("Homo sapiens").build();
		LocalDate modified = LocalDate.of(2015, 11, 5);
	//	String reId = "UP000005641";
	//	ProteomeId redId = new ProteomeIdBuilder (reId).build();
		List<DBCrossReference<ProteomeXReferenceType>> xrefs =new ArrayList<>();
		DBCrossReference<ProteomeXReferenceType> xref1 =
				new DBCrossReferenceBuilder<ProteomeXReferenceType>()
				.databaseType(ProteomeXReferenceType.GENOME_ACCESSION)
				.id("ACA121")
				.build();
		DBCrossReference<ProteomeXReferenceType> xref2 =
				new DBCrossReferenceBuilder<ProteomeXReferenceType>()
				.databaseType(ProteomeXReferenceType.GENOME_ANNOTATION)
				.id("ADFDA121")
				.build();
		xrefs.add(xref1);
		xrefs.add(xref2);
		List<Component> components = new ArrayList<>();
		Component component1 =
		ComponentBuilder.newInstance()
		.name("someName1").description("some description")
		.type(org.uniprot.core.proteome.ComponentType.UNPLACED)				
		.build();
		
		Component component2 =
				ComponentBuilder.newInstance()
				.name("someName2").description("some description 2")
				.type(org.uniprot.core.proteome.ComponentType.SEGMENTED_GENOME)			
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
	
	@Override
	protected MockMvc getMockMvc() {
		return mockMvc;
	}

	@Override
	protected String getIdRequestPath() {
		return  "/proteome/";
	}
	  static class ProteomeGetIdParameterResolver extends AbstractGetIdParameterResolver {

	        @Override
	        public GetIdParameter validIdParameter() {
	            return GetIdParameter.builder().id(UPID)
	                    .resultMatcher(jsonPath("$.id.value",is(UPID)))
//	                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//	                    .resultMatcher(jsonPath("$.commonName",is("common")))
//	                    .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
//	                    .resultMatcher(jsonPath("$.links",contains("link")))
	                    .build();
	        }

	        @Override
	        public GetIdParameter invalidIdParameter() {
	            return GetIdParameter.builder().id("INVALID")
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",contains("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
	                    .build();
	        }

	        @Override
	        public GetIdParameter nonExistentIdParameter() {
	            return GetIdParameter.builder().id("UP000005646")
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",contains("Resource not found")))
	                    .build();
	        }

	        @Override
	        public GetIdParameter withFilterFieldsParameter() {
	            return GetIdParameter.builder().id(UPID).fields("upid,organism")
	                    .resultMatcher(jsonPath("$.id.value",is(UPID)))
//	                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//	                    .resultMatcher(jsonPath("$.commonName").doesNotExist())
//	                    .resultMatcher(jsonPath("$.mnemonic").doesNotExist())
//	                    .resultMatcher(jsonPath("$.links").doesNotExist())
	                    .build();
	        }

	        @Override
	        public GetIdParameter withInvalidFilterParameter() {
	            return GetIdParameter.builder().id(UPID).fields("invalid")
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
	                    .build();
	        }
	    }

	    static class ProteomeGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

	        @Override
	        public GetIdContentTypeParam idSuccessContentTypesParam() {
	            return GetIdContentTypeParam.builder()
	                    .id(UPID)
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.id.value",is(UPID)))
//	                            .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//	                            .resultMatcher(jsonPath("$.commonName",is("common")))
//	                            .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
//	                            .resultMatcher(jsonPath("$.links",contains("link")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
                        .contentType(MediaType.APPLICATION_XML)
                        .resultMatcher(content().string(containsString(UPID)))
//                        .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//                        .resultMatcher(jsonPath("$.commonName",is("common")))
//                        .resultMatcher(jsonPath("$.mnemonic",is("mnemonic")))
//                        .resultMatcher(jsonPath("$.links",contains("link")))
                        .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString(UPID)))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("Proteome ID\tOrganism\tOrganism ID\tProtein count")))
	                            .resultMatcher(content().string(containsString("UP000005640\tHomo sapiens\t9606\t0")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
	                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
	                            .build())
	                    .build();
	        }

	        @Override
	        public GetIdContentTypeParam idBadRequestContentTypesParam() {
	            return GetIdContentTypeParam.builder()
	                    .id("INVALID")
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                      //      .resultMatcher(jsonPath("$.messages.*",contains("The 'upid' value has invalid format. It should be a valid Proteome UPID")))
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

