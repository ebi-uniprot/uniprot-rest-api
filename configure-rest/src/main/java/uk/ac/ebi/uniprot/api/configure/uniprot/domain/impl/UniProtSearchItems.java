package uk.ac.ebi.uniprot.api.configure.uniprot.domain.impl;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.api.configure.uniprot.domain.*;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public enum UniProtSearchItems implements SearchItems {
	INSTANCE;
	private static final String FILENAME = "uniprot/uniprot_search.json";
	private List<SearchItem> searchItems = new ArrayList<>();

	UniProtSearchItems() {
		init();
	}

	void init() {
		ObjectMapper objectMapper = JsonConfig.getJsonMapper();
		try (InputStream is = UniProtSearchItems.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<UniProtSearchItem> items = objectMapper.readValue(is, new TypeReference<List<UniProtSearchItem>>() {});
			this.searchItems.addAll(items);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

		Optional<SearchItem> databaseSearchItem = searchItems.stream()
				.filter(searchItem -> searchItem.getItemType().equals(SearchItemType.GROUP) &&
						searchItem.getLabel().equalsIgnoreCase("cross-references"))
				.findFirst();

		if(databaseSearchItem.isPresent()) {
			UniProtSearchItem databaseSearch = (UniProtSearchItem) databaseSearchItem.get();
			List<UniProtSearchItem> databaseSearchItems = getDatabaseSearchItems();
			databaseSearch.setItems(databaseSearchItems);
		}

	}

	private List<UniProtSearchItem> getDatabaseSearchItems() {
		List<UniProtSearchItem> searchGroupItems = new ArrayList<>();
		searchGroupItems.add(getAnyDatabaseSearchItem());
		List<DatabaseGroup> databases = Databases.INSTANCE.getDatabases();
		databases.stream().filter(databaseGroup -> !databaseGroup.getGroupName().equals("Any")).forEach(databaseGroup -> {
			UniProtSearchItem groupItem = new UniProtSearchItem();
			String groupId = databaseGroup.getGroupName().replaceAll("\\W","_").toLowerCase();
			groupItem.setId("id_group_"+groupId);
			groupItem.setLabel(databaseGroup.getGroupName());
			groupItem.setItemType(SearchItemType.GROUP);
			groupItem.setDataType(SearchDataType.UNKNOWN);

			List<UniProtSearchItem> groupItems = new ArrayList<>();
			for (Tuple databaseItem : databaseGroup.getItems()) {
				UniProtSearchItem databaseGroupItem = new UniProtSearchItem();

				databaseGroupItem.setId("id_xref_"+databaseItem.getValue());
				databaseGroupItem.setItemType(SearchItemType.DATABASE);
				databaseGroupItem.setDataType(SearchDataType.STRING);
				databaseGroupItem.setTerm("xref");
				databaseGroupItem.setLabel(databaseItem.getName());
				if(!databaseItem.getValue().equalsIgnoreCase("any")) {
					databaseGroupItem.setValuePrefix(databaseItem.getValue());
				}

				groupItems.add(databaseGroupItem);
			}
			groupItem.setItems(groupItems);
			searchGroupItems.add(groupItem);
		});
		return searchGroupItems;
	}

	private UniProtSearchItem getAnyDatabaseSearchItem() {
		UniProtSearchItem anyItem = new UniProtSearchItem();
		anyItem.setId("id_xref_any");
		anyItem.setLabel("Any cross-reference");
		anyItem.setItemType(SearchItemType.DATABASE);
		anyItem.setTerm("xref");
		anyItem.setDataType(SearchDataType.STRING);
		anyItem.setValuePrefix("any");
		return anyItem;
	}

	public List<SearchItem> getSearchItems() {
		return searchItems;
	}

	@Override
	public String toString() {
		return searchItems.stream().map(val -> val.toString()).collect(Collectors.joining(",\n"));
	}

	public static void main(String[] args) {

		UniProtSearchItems.INSTANCE.init();
		System.out.println("DONE!!!!");
	}

}
