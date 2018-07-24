package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItem;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItems;

public enum UniProtSearchItems implements SearchItems {
	INSTANCE;
	private static final String FILENAME = "uniprot/uniprot_search.json";
	private List<SearchItem> searchItems = new ArrayList<>();

	UniProtSearchItems() {
		init();
	}

	void init() {
		final ObjectMapper objectMapper = new ObjectMapper();
		try (InputStream is = AnnotationEvidences.class.getClassLoader().getResourceAsStream(FILENAME);) {
			List<UniProtSearchItem> items = objectMapper.readValue(is, new TypeReference<List<UniProtSearchItem>>() {
			});
			this.searchItems.addAll(items);
		} catch (Exception e) {
			throw new RuntimeException(e);
		}

	}

	public List<SearchItem> getSearchItems() {
		return searchItems;
	}

	@Override
	public String toString() {
		return searchItems.stream().map(val -> val.toString()).collect(Collectors.joining(",\n"));
	}

}
