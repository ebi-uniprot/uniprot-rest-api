package uk.ac.ebi.uniprot.configure.api.domain;

import static org.junit.Assert.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchDataType;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItemType;
import uk.ac.ebi.uniprot.configure.uniprot.domain.Tuple;
import uk.ac.ebi.uniprot.configure.uniprot.domain.impl.UniProtSearchItems;

class UniProtSearchItemsTest {
	private static UniProtSearchItems searchItems;

	@BeforeAll
	static void initAll() {
		searchItems = UniProtSearchItems.INSTANCE;
	}

	@Test
	void testSize() {
		List<SearchItem> items = searchItems.getSearchItems();
		assertEquals(27, items.size());
		AtomicInteger counter = new AtomicInteger();
		items.forEach(val -> numberOfItem(val, counter));
		assertEquals(123, counter.get());
	}

	private void numberOfItem(SearchItem item, AtomicInteger counter) {
		if ((item.getItems() == null) || (item.getItems().isEmpty())) {
			counter.incrementAndGet();
		} else {
			item.getItems().forEach(val -> numberOfItem(val, counter));
		}
	}

	@Test
	void testSingleGeneNameItem() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Gene Name [GN]")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("gene", item.get().getTerm());
		assertEquals(SearchDataType.STRING, item.get().getDataType());
	}

	@Test
	void testSingleOrganismItem() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Organism [OS]")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("organism", item.get().getTerm());
		assertEquals(SearchDataType.STRING, item.get().getDataType());
		assertNotNull(item.get().getAutoComplete());
		assertEquals("https://www.ebi.ac.uk/proteins/api/selector?taxonomy=?", item.get().getAutoComplete());
	}

	@Test
	void testSingleProteinExistenceItem() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Protein Existence [PE]")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("existence", item.get().getTerm());
		assertEquals(SearchDataType.ENUM, item.get().getDataType());
		assertNull(item.get().getAutoComplete());
		assertNotNull(item.get().getValues());
		assertEquals(5, item.get().getValues().size());
		Optional<Tuple> tuple = item.get().getValues().stream()
				.filter(val -> val.getName().equals("Inferred from homology")).findFirst();
		assertTrue(tuple.isPresent());
		assertEquals("3", tuple.get().getValue());
	}

	@Test
	void testFunctionCatalyticActivity() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Function")).findFirst();
		assertTrue(item.isPresent());
		assertEquals(SearchItemType.GROUP, item.get().getItemType());
		assertNotNull(item.get().getItems());
		Optional<SearchItem> subItem = item.get().getItems().stream()
				.filter(val -> val.getLabel().equals("Catalytic Activity")).findFirst();
		assertTrue(subItem.isPresent());
		assertEquals(SearchDataType.STRING, subItem.get().getDataType());
		assertEquals(SearchItemType.COMMENT, subItem.get().getItemType());
		assertEquals("catalytic_activity", subItem.get().getTerm());
		assertTrue(subItem.get().isHasEvidence());
	}

	@Test
	void testFunctionChebiTerm() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Function")).findFirst();
		assertTrue(item.isPresent());
		assertEquals(SearchItemType.GROUP, item.get().getItemType());
		assertNotNull(item.get().getItems());
		Optional<SearchItem> subItem = item.get().getItems().stream().filter(val -> val.getLabel().equals("Cofactors"))
				.findFirst();
		assertTrue(subItem.isPresent());
		assertEquals(SearchItemType.GROUP, subItem.get().getItemType());
		assertNotNull(subItem.get().getItems());

		Optional<SearchItem> subSubItem = subItem.get().getItems().stream()
				.filter(val -> val.getLabel().equals("ChEBI term")).findFirst();
		assertTrue(subSubItem.isPresent());
		assertEquals(SearchDataType.STRING, subSubItem.get().getDataType());
		assertEquals(SearchItemType.COMMENT, subSubItem.get().getItemType());
		assertEquals("https://www.ebi.ac.uk/proteins/api/selector?chebi=?", subSubItem.get().getAutoComplete());
		assertEquals("cofactor_chebi", subSubItem.get().getTerm());
		assertTrue(subSubItem.get().isHasEvidence());
	}

	@Test
	void testStructureTurn() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Structure")).findFirst();
		assertTrue(item.isPresent());
		assertEquals(SearchItemType.GROUP, item.get().getItemType());
		assertNotNull(item.get().getItems());
		Optional<SearchItem> subItem = item.get().getItems().stream()
				.filter(val -> val.getLabel().equals("Secondary structure")).findFirst();
		assertTrue(subItem.isPresent());
		assertEquals(SearchItemType.GROUP, subItem.get().getItemType());
		assertNotNull(subItem.get().getItems());

		Optional<SearchItem> subSubItem = subItem.get().getItems().stream().filter(val -> val.getLabel().equals("Turn"))
				.findFirst();
		assertTrue(subSubItem.isPresent());
		assertEquals(SearchDataType.STRING, subSubItem.get().getDataType());
		assertEquals(SearchItemType.FEATURE, subSubItem.get().getItemType());

		assertEquals("turn", subSubItem.get().getTerm());
		assertTrue(subSubItem.get().isHasEvidence());
		assertTrue(subSubItem.get().isHasRange());
	}

	@Test
	void testDatabase() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Cross-references")).findFirst();
		assertTrue(item.isPresent());
		assertEquals("xref", item.get().getTerm());
		assertEquals(SearchDataType.STRING, item.get().getDataType());
		assertEquals(SearchItemType.DATABASE, item.get().getItemType());
		assertEquals("embl-EU518936", item.get().getExample());
	}

	@Test
	void testDateType() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Date Of")).findFirst();
		assertTrue(item.isPresent());
		assertEquals(SearchItemType.GROUP, item.get().getItemType());
		assertNotNull(item.get().getItems());
		Optional<SearchItem> subItem = item.get().getItems().stream()
				.filter(val -> val.getLabel().equals("Date of last entry modification")).findFirst();
		assertTrue(subItem.isPresent());
		assertEquals(SearchDataType.DATE, subItem.get().getDataType());
		assertEquals(SearchItemType.SINGLE, subItem.get().getItemType());
		assertEquals("modified", subItem.get().getTerm());
		assertTrue(subItem.get().isHasRange());
	}

	@Test
	void testGroupDisplay() {
		Optional<SearchItem> item = searchItems.getSearchItems().stream()
				.filter(val -> val.getLabel().equals("Literature Citation")).findFirst();
		assertTrue(item.isPresent());
		assertEquals(SearchItemType.GROUP_DISPLAY, item.get().getItemType());
		assertNotNull(item.get().getItems());
		Optional<SearchItem> subItem = item.get().getItems().stream().filter(val -> val.getLabel().equals("Published"))
				.findFirst();
		assertTrue(subItem.isPresent());
		assertEquals(SearchDataType.DATE, subItem.get().getDataType());
		assertEquals(SearchItemType.SINGLE, subItem.get().getItemType());
		assertEquals("lit_pubdate", subItem.get().getTerm());
		assertTrue(subItem.get().isHasRange());
	}

}
