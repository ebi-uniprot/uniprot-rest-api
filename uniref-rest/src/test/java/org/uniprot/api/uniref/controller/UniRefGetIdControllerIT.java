package org.uniprot.api.uniref.controller;

import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.isEmptyOrNullString;
import static org.hamcrest.Matchers.isEmptyString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

import java.time.LocalDate;

import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.uniprot.api.rest.controller.AbstractGetByIdControllerIT;
import org.uniprot.api.rest.controller.param.ContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdContentTypeParam;
import org.uniprot.api.rest.controller.param.GetIdParameter;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdContentTypeParamResolver;
import org.uniprot.api.rest.controller.param.resolver.AbstractGetIdParameterResolver;
import org.uniprot.api.rest.output.UniProtMediaType;
import org.uniprot.api.rest.validation.error.ErrorHandlerConfig;
import org.uniprot.api.uniref.UniRefRestApplication;
import org.uniprot.api.uniref.repository.DataStoreTestConfig;
import org.uniprot.core.Sequence;
import org.uniprot.core.builder.SequenceBuilder;
import org.uniprot.core.uniparc.builder.UniParcIdBuilder;
import org.uniprot.core.uniprot.builder.UniProtAccessionBuilder;
import org.uniprot.core.uniref.GoTermType;
import org.uniprot.core.uniref.RepresentativeMember;
import org.uniprot.core.uniref.UniRefEntry;
import org.uniprot.core.uniref.UniRefEntryId;
import org.uniprot.core.uniref.UniRefMember;
import org.uniprot.core.uniref.UniRefMemberIdType;
import org.uniprot.core.uniref.UniRefType;
import org.uniprot.core.uniref.builder.GoTermBuilder;
import org.uniprot.core.uniref.builder.RepresentativeMemberBuilder;
import org.uniprot.core.uniref.builder.UniRefEntryBuilder;
import org.uniprot.core.uniref.builder.UniRefEntryIdBuilder;
import org.uniprot.core.uniref.builder.UniRefMemberBuilder;
import org.uniprot.core.xml.jaxb.uniref.Entry;
import org.uniprot.core.xml.uniref.UniRefEntryConverter;
import org.uniprot.store.indexer.DataStoreManager;

@ContextConfiguration(classes= {DataStoreTestConfig.class, UniRefRestApplication.class, ErrorHandlerConfig.class})
@ActiveProfiles(profiles = "offline")
@WebMvcTest(UniRefController.class)
@ExtendWith(value = {SpringExtension.class, UniRefGetIdControllerIT.UniRefGetIdParameterResolver.class,
		UniRefGetIdControllerIT.UniRefGetIdContentTypeParamResolver.class})
public class UniRefGetIdControllerIT extends AbstractGetByIdControllerIT {
	private static final String ID = "UniRef50_P03923";
	 private static final String NAME= "Cluster: MoeK5";
    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private DataStoreManager storeManager;
	@Override
	protected MockMvc getMockMvc() {
		return mockMvc;
	}

	@Override
	protected String getIdRequestPath() {
		return  "/uniref/";
	}
	
    
	@Override
	protected void saveEntry() {
		UniRefEntry unirefEntry =createEntry() ;
		UniRefEntryConverter converter = new UniRefEntryConverter();
		Entry entry =converter.toXml(unirefEntry);
		storeManager.saveToStore(DataStoreManager.StoreType.UNIREF, unirefEntry);
		storeManager.saveEntriesInSolr	(DataStoreManager.StoreType.UNIREF, entry);
		

	}
	
	
	private UniRefEntry createEntry() {
	
		UniRefType type = UniRefType.UniRef100;
	
		UniRefEntryId entryId = new UniRefEntryIdBuilder(ID).build();

		return new UniRefEntryBuilder()
				.id(entryId)
				.name(NAME)
				.updated(LocalDate.now())
				.entryType(type)
				.commonTaxonId(9606L)
				.commonTaxon("Homo sapiens")
				.representativeMember(createReprestativeMember())
				.addMember(createMember())
				.addGoTerm(new GoTermBuilder().type(GoTermType.COMPONENT).id("GO:0044444").build())
				.addGoTerm(new GoTermBuilder().type(GoTermType.FUNCTION).id("GO:0044459").build())
				.addGoTerm(new GoTermBuilder().type(GoTermType.PROCESS).id("GO:0032459").build())
				.memberCount(2)
				.build();
	}
	private UniRefMember createMember() {
		String memberId = "P12345_HUMAN";
		int length=312;
		String pName ="some protein name"; 
		String upi = "UPI0000083A08";
		
		UniRefMemberIdType type =UniRefMemberIdType.UNIPROTKB;
		return new UniRefMemberBuilder()
				.memberIdType(type).memberId(memberId)
				.organismName("Homo sapiens")
				.organismTaxId(9606)
				.sequenceLength(length)
				.proteinName(pName)
				.uniparcId(new UniParcIdBuilder(upi).build())
				.accession(new UniProtAccessionBuilder("P12345").build())
				.uniref100Id(new UniRefEntryIdBuilder("UniRef100_P03923").build())
				.uniref90Id(new UniRefEntryIdBuilder("UniRef90_P03943").build())
				.uniref50Id(new UniRefEntryIdBuilder("UniRef50_P03973").build())
				.build();
	}
	private RepresentativeMember createReprestativeMember() {
		String seq = "MVSWGRFICLVVVTMATLSLARPSFSLVEDDFSAGSADFAFWERDGDSDGFDSHSDJHETRHJREH";
		Sequence sequence = new SequenceBuilder(seq).build();
		String memberId = "P12345_HUMAN";
		int length=312;
		String pName ="some protein name"; 
		String upi = "UPI0000083A08";
		
		UniRefMemberIdType type =UniRefMemberIdType.UNIPROTKB;

		return new RepresentativeMemberBuilder()
				.memberIdType(type).memberId(memberId)
				.organismName("Homo sapiens")
				.organismTaxId(9606)
				.sequenceLength(length)
				.proteinName(pName)
				.uniparcId(new UniParcIdBuilder(upi).build())
				.accession(new UniProtAccessionBuilder("P12345").build())
				.uniref100Id(new UniRefEntryIdBuilder("UniRef100_P03923").build())
				.uniref90Id(new UniRefEntryIdBuilder("UniRef90_P03943").build())
				.uniref50Id(new UniRefEntryIdBuilder("UniRef50_P03973").build())
				.isSeed(true)
				.sequence(sequence)
				.build();
	}
	
	  static class UniRefGetIdParameterResolver extends AbstractGetIdParameterResolver {

	        @Override
	        public GetIdParameter validIdParameter() {
	            return GetIdParameter.builder().id(ID)
	                    .resultMatcher(jsonPath("$.id",is(ID)))
	                    .build();
	        }

	        @Override
	        public GetIdParameter invalidIdParameter() {
	            return GetIdParameter.builder().id("INVALID")
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",contains("The 'id' value has invalid format. It should be a valid UniRef Cluster id")))
	                    .build();
	        }

	        @Override
	        public GetIdParameter nonExistentIdParameter() {
	            return GetIdParameter.builder().id("UniRef50_P03925")
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",contains("Resource not found")))
	                    .build();
	        }

	        @Override
	        public GetIdParameter withFilterFieldsParameter() {
	            return GetIdParameter.builder().id(ID).fields("id,name,count")
	                    .resultMatcher(jsonPath("$.id",is(ID)))
//	                    .resultMatcher(jsonPath("$.scientificName",is("scientific")))
//	                    .resultMatcher(jsonPath("$.commonName").doesNotExist())
//	                    .resultMatcher(jsonPath("$.mnemonic").doesNotExist())
//	                    .resultMatcher(jsonPath("$.links").doesNotExist())
	                    .build();
	        }

	        @Override
	        public GetIdParameter withInvalidFilterParameter() {
	            return GetIdParameter.builder().id(ID).fields("invalid")
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*", contains("Invalid fields parameter value 'invalid'")))
	                    .build();
	        }
	    }

	    static class UniRefGetIdContentTypeParamResolver extends AbstractGetIdContentTypeParamResolver {

	        @Override
	        public GetIdContentTypeParam idSuccessContentTypesParam() {
	            return GetIdContentTypeParam.builder()
	                    .id(ID)
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.id",is(ID)))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
                      .contentType(MediaType.APPLICATION_XML)
                      .resultMatcher(content().string(containsString(ID)))
                      .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString(ID)))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString(ID)))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("Cluster ID\tCluster Name\tCommon taxon\tSize\tDate of creation")))
	                            .resultMatcher(content().string(containsString("UniRef50_P03923	Cluster: MoeK5	Homo sapiens	2	2019-08-27")))
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
	                            .resultMatcher(jsonPath("$.messages.*",contains("The 'id' value has invalid format. It should be a valid UniRef Cluster id")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_XML)
	                            .resultMatcher(content().string(containsString("The 'id' value has invalid format. It should be a valid UniRef Cluster id")))
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
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.FASTA_MEDIA_TYPE)
	                            .resultMatcher(content().string(isEmptyString()))
	                            .build())
	                    .build();
	        }
	    }
}

