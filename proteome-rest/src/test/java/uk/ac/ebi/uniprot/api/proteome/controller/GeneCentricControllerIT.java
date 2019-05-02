package uk.ac.ebi.uniprot.api.proteome.controller;

import static org.hamcrest.Matchers.is;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.http.HttpHeaders.ACCEPT;
import static org.springframework.http.MediaType.APPLICATION_JSON_VALUE;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.web.context.WebApplicationContext;

import uk.ac.ebi.uniprot.api.proteome.ProteomeRestApplication;
import uk.ac.ebi.uniprot.indexer.DataStoreManager;
import uk.ac.ebi.uniprot.xml.XmlChainIterator;
import uk.ac.ebi.uniprot.xml.jaxb.proteome.Proteome;

/**
 *
 * @author jluo
 * @date: 2 May 2019
 *
*/
@RunWith(SpringRunner.class)
@SpringBootTest(classes = {ProteomeDataStoreTestConfig.class, ProteomeRestApplication.class})
@WebAppConfiguration
class GeneCentricControllerIT {

	 private static final String SEARCH_RESOURCE = "/genecentric/";
	 static final String PROTEOME_ROOT_ELEMENT = "proteome";
	 
	   @Autowired
	    private DataStoreManager storeManager;

	    @Autowired
	    private WebApplicationContext webApplicationContext;

	    private MockMvc mockMvc;
	 
	@Test
	void testByAccession()  throws Exception {
		   // given
        String upid = saveEntry("UP000000798.xml", "UP000000798");
        String accession ="O67543";
        String resource=  SEARCH_RESOURCE + accession;
        // when
        ResultActions response = mockMvc.perform(
                get(resource)
                        .header(ACCEPT, APPLICATION_JSON_VALUE));

        // then
        response.andDo(print())
                .andExpect(status().is(HttpStatus.OK.value()))
                .andExpect(header().string(HttpHeaders.CONTENT_TYPE, APPLICATION_JSON_VALUE))
                .andExpect(jsonPath("$.id.value", is(upid)));
        
	}
    private String saveEntry(String file, String upid) {
  	  List<String> files = Arrays.asList(
                "it/" +file
               
        );

        XmlChainIterator<Proteome, Proteome>  chainingIterators =
        		new XmlChainIterator<>(new XmlChainIterator.FileInputStreamIterator(files),
                        Proteome.class, PROTEOME_ROOT_ELEMENT, Function.identity() );
        		
                new XmlChainIterator<>(new XmlChainIterator.FileInputStreamIterator(files),
                        Proteome.class,
                        PROTEOME_ROOT_ELEMENT, Function.identity() );

        List<Proteome> proteomes = new ArrayList<>();
     
        while (chainingIterators.hasNext()) {
      	  Proteome proteome = chainingIterators.next();
      	  if(upid !=null) {
      		  proteome.setUpid(upid);
      	  }
            proteomes.add( proteome);
        }
        List<String> upids = proteomes.stream()
      		  .map(val ->val.getUpid()).collect(Collectors.toList());
  	storeManager.save(DataStoreManager.StoreType.PROTEOME, proteomes);
  	return upids.get(0);
  }
}

