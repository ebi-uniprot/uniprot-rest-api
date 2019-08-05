package org.uniprot.api.proteome.repository;

import static org.hamcrest.CoreMatchers.hasItems;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;

import java.util.Collection;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.junit4.SpringRunner;
import org.uniprot.api.common.repository.search.facet.FacetProperty;
import org.uniprot.api.proteome.ProteomeRestApplication;
import org.uniprot.api.proteome.repository.ProteomeFacetConfig;

/**
 *
 * @author jluo
 * @date: 2 May 2019
 *
*/
@RunWith(SpringRunner.class)
@SpringBootTest
@Import(ProteomeFacetConfig.class)
public class ProteomeFacetConfigTest {
	@Autowired
	ProteomeFacetConfig config;
	@Test
	public void testGetFacetPropertyMap() {
	
		Map<String, FacetProperty>  map =config.getFacetPropertyMap();
		assertEquals(2, map.size());
		
	}

	@Test
	public void testGetFacetNames() {
	
		Collection<String>  map =config.getFacetNames();
		assertThat(map, hasItems("superkingdom", "reference"));
		
	}
}

