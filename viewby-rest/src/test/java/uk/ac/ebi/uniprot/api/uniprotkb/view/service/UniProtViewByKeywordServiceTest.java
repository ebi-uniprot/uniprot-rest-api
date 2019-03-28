package uk.ac.ebi.uniprot.api.uniprotkb.view.service;

import static org.junit.Assert.assertTrue;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import uk.ac.ebi.uniprot.api.uniprotkb.view.model.ViewBy;
import uk.ac.ebi.uniprot.api.uniprotkb.view.service.UniProtViewByKeywordService;
import uk.ac.ebi.uniprot.cv.keyword.KeywordDetail;
import uk.ac.ebi.uniprot.cv.keyword.KeywordService;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordDetailImpl;
import uk.ac.ebi.uniprot.cv.keyword.impl.KeywordImpl;

@RunWith(MockitoJUnitRunner.class)
class UniProtViewByKeywordServiceTest {
	 @Mock
	 private SolrClient solrClient;
	 
	 @Mock
	 private KeywordService keywordService;
	 private UniProtViewByKeywordService service;
	 
	 
	@BeforeEach
	public void setup() {
		solrClient =Mockito.mock(SolrClient.class);
		keywordService =Mockito.mock(KeywordService.class);
		mockKeywordService();
		service = new UniProtViewByKeywordService( solrClient, "uniprot", keywordService);
	 }
	@Test
	void test()  throws IOException, SolrServerException {
		Map<String, Long> counts = new HashMap<>();
		counts.put("KW-0128", 5l);
		counts.put("KW-0130", 45l);	
		counts.put("KW-0131", 102l);	
		MockServiceHelper.mockServiceQueryResponse( solrClient, "keyword_id", counts);
		List<ViewBy> viewBys = service.get("", "KW-9999");
		assertEquals(3, viewBys.size());
		ViewBy viewBy1 = MockServiceHelper.createViewBy("KW-0128", "Catecholamine metabolism", 5l, UniProtViewByKeywordService.URL_PREFIX +"KW-0128" , false);
		assertTrue(viewBys.contains(viewBy1));
		ViewBy viewBy2 = MockServiceHelper.createViewBy("KW-0130", "Cell adhesion", 45l, UniProtViewByKeywordService.URL_PREFIX +"KW-0130" , false);
		assertTrue(viewBys.contains(viewBy2));
		ViewBy viewBy3 = MockServiceHelper.createViewBy("KW-0131", "Cell cycle", 102l, UniProtViewByKeywordService.URL_PREFIX +"KW-0131" , true);
		assertTrue(viewBys.contains(viewBy3));
		
	}
	void mockKeywordService() {
		KeywordDetailImpl keyword = new KeywordDetailImpl();
		keyword.setKeyword(new KeywordImpl( "Catecholamine metabolism","KW-9999"));
		List<KeywordDetail> parents =Arrays.asList(keyword);
		KeywordDetailImpl kw1 = new KeywordDetailImpl();
		kw1.setKeyword(new KeywordImpl( "Catecholamine metabolism", "KW-0128"));
		kw1.setParents(parents);
		KeywordDetailImpl kw2 = new KeywordDetailImpl();
		kw2.setKeyword(new KeywordImpl( "Cell adhesion", "KW-0130"));
		kw2.setParents(parents);
		
		KeywordDetailImpl kw3 = new KeywordDetailImpl();
		kw3.setKeyword(new KeywordImpl( "Cell cycle", "KW-0131"));
		kw3.setParents(parents);
		List<KeywordDetail> kws =new ArrayList<>();
		kws.add(kw1);
		kws.add(kw2);
		kws.add(kw3);
		List<KeywordDetail> kws3 =new ArrayList<>();
		kws3.add(kw3);
		KeywordDetailImpl kw31 = new KeywordDetailImpl();
		kw31.setKeyword(new KeywordImpl( "Cell division", "KW-0132"));
		kw3.setParents(kws3);
		KeywordDetailImpl kw32 = new KeywordDetailImpl();
		kw32.setKeyword(new KeywordImpl( "Growth arrest", "KW-0338"));
		kw3.setParents(kws3);
	
		
		when(keywordService.getByAccession(any())).thenReturn(keyword);

		
	}
}
