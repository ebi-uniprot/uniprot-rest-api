package uk.ac.ebi.uniprot.uuw.advanced.search.model.request;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItems;
import static org.junit.jupiter.api.Assertions.*;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.uuw.advanced.search.model.response.filter.FieldsParser;

class FieldsParserTest {

	@Test
	void onlySimpleFields() {
		String fields ="accession,protein_name,gene_name,organism";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(4, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
	}

	@Test
	void withComment() {
		String fields ="accession,protein_name,gene_name,organism,comment";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("comment"));
		List<String> comments = filters.get("comment");
		assertThat(comments, hasItems("all" ));
	}
	@Test
	void withCommentSpecific() {
		String fields ="accession,protein_name,gene_name,organism,comment:function,comment:domain";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("comment"));
		List<String> comments = filters.get("comment");
		assertThat(comments, hasItems("function","domain" ));
	}
	
	@Test
	void withCommentCCSpecific() {
		String fields ="accession,protein_name,gene_name,organism,comment:function,cc:domain";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("comment"));
		List<String> comments = filters.get("comment");
		assertThat(comments, hasItems("function","domain" ));
	}
	
	@Test
	void withCCSpecific() {
		String fields ="accession,protein_name,gene_name,organism,cc:function,cc:domain";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("comment"));
		List<String> comments = filters.get("comment");
		assertThat(comments, hasItems("function","domain" ));
	}
	
	
	@Test
	void withFeature() {
		String fields ="accession,protein_name,gene_name,organism,feature";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("feature"));
		List<String> comments = filters.get("feature");
		assertThat(comments, hasItems("all" ));
	}
	@Test
	void withFeatureSpecific() {
		String fields ="accession,protein_name,gene_name,organism,feature:binding,feature:signal";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("feature"));
		List<String> comments = filters.get("feature");
		assertThat(comments, hasItems("binding","signal" ));
	}
	@Test
	void withFeatureFTSpecific() {
		String fields ="accession,protein_name,gene_name,organism,feature:binding,ft:signal";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("feature"));
		List<String> comments = filters.get("feature");
		assertThat(comments, hasItems("binding","signal" ));
	}
	@Test
	void withFTSpecific() {
		String fields ="accession,protein_name,gene_name,organism,ft:binding,ft:signal";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("feature"));
		List<String> comments = filters.get("feature");
		assertThat(comments, hasItems("binding","signal" ));
	}
	
	@Test
	void withXref() {
		String fields ="accession,protein_name,gene_name,organism,xref";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("xref"));
		List<String> comments = filters.get("xref");
		assertThat(comments, hasItems("all" ));
	}
	@Test
	void withXrefSpecific() {
		String fields ="accession,protein_name,gene_name,organism,xref:embl,xref:pdb";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("xref"));
		List<String> comments = filters.get("xref");
		assertThat(comments, hasItems("embl","pdb" ));
	}
	@Test
	void withXrefDRSpecific() {
		String fields ="accession,protein_name,gene_name,organism,xref:embl,dr:pdb";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("xref"));
		List<String> comments = filters.get("xref");
		assertThat(comments, hasItems("embl","pdb" ));
	}
	@Test
	void withDRSpecific() {
		String fields ="accession,protein_name,gene_name,organism,dr:embl,dr:pdb";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(5, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("organism"));
		assertTrue(filters.containsKey("xref"));
		List<String> comments = filters.get("xref");
		assertThat(comments, hasItems("embl","pdb" ));
	}
	@Test
	void withGenes() {
		String fields ="accession,protein_name,gene_name,gene_orf,gene_oln";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(3, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("gene"));
	}

	@Test
	void withGo() {
		String fields ="accession,protein_name,go_id,go_f,go_c";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(3, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("xref"));
		List<String> comments = filters.get("xref");
		assertThat(comments, hasItems("go" ));
	}
	@Test
	void withLineage() {
		String fields ="accession,protein_name,lineage_all,lin_genus,lin_superfamily";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(3, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("lineage"));
	}
	@Test
	void withInfo() {
		String fields ="accession,protein_name,date_create,date_mod,version";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(3, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("info"));
	}
	@Test
	void withSequence() {
		String fields ="accession,protein_name,fragment,length,mass";
		Map<String, List<String> > filters = FieldsParser.parse(fields);
		assertEquals(3, filters.size());
		assertTrue(filters.containsKey("protein_name"));
		assertTrue(filters.containsKey("sequence"));
	}
}
