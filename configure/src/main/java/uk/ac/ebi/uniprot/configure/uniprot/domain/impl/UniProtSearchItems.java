package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import javax.xml.bind.annotation.XmlRootElement;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
@XmlRootElement(name = "searchItems")
public class UniProtSearchItems {
	private List<UniProtSearchItem> searchItems =new ArrayList<>();
	private UniProtSearchItems() {
		
	}
	public List<UniProtSearchItem> getSearchItems() {
		return searchItems;
	}
	public void setSearchItems(List<UniProtSearchItem> searchItems) {
		this.searchItems = searchItems;
	}

	@Override
	public String toString() {
		return searchItems.stream().map(val ->val.toString())
		.collect(Collectors.joining(",\n"));
	}

	
	public static UniProtSearchItems readFromFile(String filename) throws Exception{
		ObjectMapper m = new ObjectMapper();
		UniProtSearchItems searchTerms =
				m.readValue(new File(filename), UniProtSearchItems.class);
		
	
		return searchTerms;
	}
	
	public static  List<UniProtSearchItem> readFromFile2(String filename) throws Exception{
		ObjectMapper m = new ObjectMapper();
		List<UniProtSearchItem> searchTerms =
				m.readValue(new File(filename),  new TypeReference<List<UniProtSearchItem>>(){});
		
	
		return searchTerms;
	}
}
