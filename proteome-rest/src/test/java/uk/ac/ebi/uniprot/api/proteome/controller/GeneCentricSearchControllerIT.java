package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.hamcrest.CoreMatchers.hasItems;
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

import com.fasterxml.jackson.core.JsonProcessingException;

import uk.ac.ebi.uniprot.api.proteome.ProteomeRestApplication;
import uk.ac.ebi.uniprot.api.proteome.repository.GeneCentricFacetConfig;
import uk.ac.ebi.uniprot.api.rest.controller.AbstractSearchControllerIT;
import uk.ac.ebi.uniprot.api.rest.controller.SaveScenario;
import uk.ac.ebi.uniprot.api.rest.controller.param.ContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchContentTypeParam;
import uk.ac.ebi.uniprot.api.rest.controller.param.SearchParameter;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchContentTypeParamResolver;
import uk.ac.ebi.uniprot.api.rest.controller.param.resolver.AbstractSearchParameterResolver;
import uk.ac.ebi.uniprot.api.rest.output.UniProtMediaType;
import uk.ac.ebi.uniprot.domain.proteome.CanonicalProtein;
import uk.ac.ebi.uniprot.domain.proteome.Protein;
import uk.ac.ebi.uniprot.domain.proteome.builder.CanonicalProteinBuilder;
import uk.ac.ebi.uniprot.domain.proteome.builder.ProteinBuilder;
import uk.ac.ebi.uniprot.domain.uniprot.UniProtEntryType;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.json.parser.proteome.ProteomeJsonConfig;
import uk.ac.ebi.uniprot.search.document.proteome.GeneCentricDocument;
import uk.ac.ebi.uniprot.search.document.proteome.GeneCentricDocument.GeneCentricDocumentBuilder;
import uk.ac.ebi.uniprot.search.field.GeneCentricField;
import uk.ac.ebi.uniprot.search.field.SearchField;

/**
 *
 * @author jluo
 * @date: 17 Jun 2019
 *
 */
@ContextConfiguration(classes = { GeneCentriDataStoreTestConfig.class, ProteomeRestApplication.class })
@ActiveProfiles(profiles = "genecentric_offline")
@WebMvcTest(GeneCentricController.class)
@ExtendWith(value = { SpringExtension.class, GeneCentricSearchControllerIT.GeneCentricSearchParameterResolver.class,
		GeneCentricSearchControllerIT.GeneCentricSearchContentTypeParamResolver.class })
public class GeneCentricSearchControllerIT extends AbstractSearchControllerIT {
	private static final String ACCESSION_PREF = "P00";
	private static final String RELATED_ACCESSION_PREF_1 = "P20";
	private static final String RELATED_ACCESSION_PREF_2 = "P30";
	private static final String UPID = "UP000005640";
	private static final int TAX_ID = 9606;

	@Autowired
	@Qualifier("genecentric")
	SolrClient solrClient;

	@Autowired
	private MockMvc mockMvc;

	@Autowired
	private DataStoreManager storeManager;
	
    @Autowired
    private GeneCentricFacetConfig facetConfig;

	@Override
	protected void cleanEntries() {
		storeManager.cleanSolr(DataStoreManager.StoreType.GENECENTRIC);

	}

	@Override
	protected MockMvc getMockMvc() {
		return mockMvc;
	}

	@Override
	protected String getSearchRequestPath() {
		return "/genecentric/search";
	}

	@Override
	protected int getDefaultPageSize() {
		return 25;
	}

	@Override
	protected List<SearchField> getAllSearchFields() {
		return Arrays.asList(GeneCentricField.Search.values());
	}

	@Override
	protected String getFieldValueForValidatedField(SearchField searchField) {
		 String value = "";
	        switch (searchField.getName()) {
	        	case "accession_id":
	            case "accession":
	            	value =ACCESSION_PREF +123;
	            	break;
	            case "upid":
	            	value =UPID;
	            	break;
	            case "organism_id":
	            	value ="9606";
	            	break;
	            case "length":
	            	value ="30";
	            	break;
	            case "active":
	            	value ="true";
	            	break;
	        }
	        return value;
	}

	@Override
	protected List<String> getAllSortFields() {
		 return Arrays.stream(GeneCentricField.Sort.values())
	                .map(GeneCentricField.Sort::name)
	                .collect(Collectors.toList());
	}

	@Override
	protected List<String> getAllFacetFields() {
		 return new ArrayList<>(facetConfig.getFacetNames());
	}

	@Override
	protected List<String> getAllReturnedFields() {
		  return Arrays.stream(GeneCentricField.ResultFields.values())
	                .map(GeneCentricField.ResultFields::name)
	                .collect(Collectors.toList());
	}

	@Override
	protected void saveEntry(SaveScenario saveContext) {
		saveEntry(123);
		saveEntry(124);
	}

	private void saveEntry(int i) {
		GeneCentricDocument doc= createDocument(i);
		storeManager.saveDocs(DataStoreManager.StoreType.GENECENTRIC, doc);
	}
	@Override
	protected void saveEntries(int numberOfEntries) {
		IntStream.rangeClosed(1, numberOfEntries)
		.forEach(i-> saveEntry(i));

	}

	private String getName(String prefix, int i) {
		if (i < 10) {
			return prefix + "00" + i;
		} else if (i < 100) {
			return prefix + "0" + i;
		} else
			return prefix + i;

	}

	private GeneCentricDocument createDocument(int i) {
		CanonicalProtein protein = createProtein(i);
		return convert(protein);
	}

	private GeneCentricDocument convert(CanonicalProtein protein) {
		GeneCentricDocumentBuilder builder = GeneCentricDocument.builder();
		List<String> accessions = new ArrayList<>();
		accessions.add(protein.getCanonicalProtein().getAccession().getValue());
		protein.getRelatedProteins().stream().map(val -> val.getAccession().getValue())
				.forEach(val -> accessions.add(val));
		List<String> genes = new ArrayList<>();
		genes.add(protein.getCanonicalProtein().getGeneName());
		protein.getRelatedProteins().stream().map(val -> val.getGeneName()).forEach(val -> genes.add(val));

		builder.accession(protein.getCanonicalProtein().getAccession().getValue()).accessions(accessions)
				.geneNames(genes).reviewed(protein.getCanonicalProtein().getEntryType() == UniProtEntryType.SWISSPROT)
				.upid(UPID).organismTaxId(TAX_ID);
		builder.geneCentricStored(getBinary(protein));
		return builder.build();
	}

	private CanonicalProtein createProtein(int i) {
		Protein protein = ProteinBuilder.newInstance().accession(getName(ACCESSION_PREF, i))
				.entryType(UniProtEntryType.SWISSPROT).geneName(getName("gene", i))
				.geneNameType(uk.ac.ebi.uniprot.domain.proteome.GeneNameType.ENSEMBL).sequenceLength(324).build();

		Protein protein2 = ProteinBuilder.newInstance().accession(getName(RELATED_ACCESSION_PREF_1, i))
				.entryType(UniProtEntryType.SWISSPROT).geneName(getName("agene", i))
				.geneNameType(uk.ac.ebi.uniprot.domain.proteome.GeneNameType.ENSEMBL).sequenceLength(334).build();
		Protein protein3 = ProteinBuilder.newInstance().accession(getName(RELATED_ACCESSION_PREF_2, i))
				.entryType(UniProtEntryType.TREMBL).geneName(getName("twogene", i))
				.geneNameType(uk.ac.ebi.uniprot.domain.proteome.GeneNameType.OLN).sequenceLength(434).build();
		CanonicalProteinBuilder builder = CanonicalProteinBuilder.newInstance();
		CanonicalProtein cProtein = builder.canonicalProtein(protein).addRelatedProtein(protein2)
				.addRelatedProtein(protein3).build();

		return cProtein;
	}

	private ByteBuffer getBinary(CanonicalProtein entry) {
		try {
			return ByteBuffer.wrap(ProteomeJsonConfig.getInstance().getFullObjectMapper().writeValueAsBytes(entry));
		} catch (JsonProcessingException e) {
			throw new RuntimeException("Unable to parse TaxonomyEntry to binary json: ", e);
		}
	}
	

	  static class GeneCentricSearchParameterResolver extends AbstractSearchParameterResolver {

	        @Override
	        protected SearchParameter searchCanReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("gene:*"))
	                    .resultMatcher(jsonPath("$.results.*..accession.value",contains("P00123","P20123","P30123","P00124","P20124","P30124")))
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
	                    .queryParam("query", Collections.singletonList("gene:*"))
	                    .resultMatcher(jsonPath("$.results.*..accession.value",hasItems("P00123", "P00124")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchQueryWithInvalidTypeQueryReturnBadRequestParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("gene:[1 TO 10]"))
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",
	                    		contains("'gene' filter type 'range' is invalid. Expected 'term' filter type")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchQueryWithInvalidValueQueryReturnBadRequestParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("upid:INVALID OR organism_id:INVALID " +
	                            "OR reviewed:invalid"))
	                    .resultMatcher(jsonPath("$.url",not(isEmptyOrNullString())))
	                    .resultMatcher(jsonPath("$.messages.*",containsInAnyOrder(
	                    		"The 'upid' value has invalid format. It should be a valid Proteome UPID",
	                            "The organism id filter value should be a number",
	                            "The reviewed id filter value should be a true or false"
	                      )))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchSortWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("sort", Collections.singletonList("accession_id asc"))
	                    .resultMatcher(jsonPath("$.results.*..accession.value",hasItems("P00123", "P00124")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchFieldsWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("*:*"))
	                    .queryParam("fields", Collections.singletonList("accession_id"))
	                    .resultMatcher(jsonPath("$.results.*..accession.value", contains("P00123","P20123","P30123","P00124","P20124","P30124")))
	                  
	      //              .resultMatcher(jsonPath("$.results.*.description",contains("Description231","Description520")))
	                    .build();
	        }

	        @Override
	        protected SearchParameter searchFacetsWithCorrectValuesReturnSuccessParameter() {
	            return SearchParameter.builder()
	                    .queryParam("query", Collections.singletonList("reviewed:true"))
	                    .queryParam("facets", Collections.singletonList("reviewed"))
	                    .resultMatcher(jsonPath("$.results.*..accession.value",contains("P00123","P20123","P30123","P00124","P20124","P30124")))
	                    .build();
	        }
	    }


	    static class GeneCentricSearchContentTypeParamResolver extends AbstractSearchContentTypeParamResolver{

	        @Override
	        protected SearchContentTypeParam searchSuccessContentTypesParam() {
	            return SearchContentTypeParam.builder()
	                    .query("organism_id:9606")
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_JSON)
	                            .resultMatcher(jsonPath("$.results.*..accession.value",hasItems("P00123", "P00124")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(MediaType.APPLICATION_XML)
	                            .resultMatcher(content().string(containsString("P00123")))
	                            .resultMatcher(content().string(containsString("P00124")))
	                            .build())
	                    .contentTypeParam(ContentTypeParam.builder()
	                            .contentType(UniProtMediaType.LIST_MEDIA_TYPE)
	                            .resultMatcher(content().string(containsString("P00123")))
	                            .resultMatcher(content().string(containsString("P00124")))
	                            .build())
//	                    .contentTypeParam(ContentTypeParam.builder()
//	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
//	                            .resultMatcher(content().string(containsString("\tOrganism\tOrganism ID\tProtein count")))
//	                            .resultMatcher(content().string(containsString("UP000005231\tHomo sapiens\t9606\t0")))
//	                            .resultMatcher(content().string(containsString("UP000005520\tHomo sapiens\t9606\t0")))
//	                            .build())
//	                    .contentTypeParam(ContentTypeParam.builder()
//	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
//	                            .resultMatcher(content().contentType(UniProtMediaType.XLS_MEDIA_TYPE))
//	                            .build())
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
//	                    .contentTypeParam(ContentTypeParam.builder()
//	                            .contentType(UniProtMediaType.TSV_MEDIA_TYPE)
//	                            .resultMatcher(content().string(isEmptyString()))
//	                            .build())
//	                    .contentTypeParam(ContentTypeParam.builder()
//	                            .contentType(UniProtMediaType.XLS_MEDIA_TYPE)
//	                            .resultMatcher(content().string(isEmptyString()))
//	                            .build())
	                    .build();
	        }
	    }
	
}
