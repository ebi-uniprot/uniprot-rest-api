package uk.ac.ebi.uniprot.configure.uniprot.domain.impl;

import java.util.ArrayList;
import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchDataType;
import uk.ac.ebi.uniprot.configure.uniprot.domain.SearchItemType;



@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public class UniProtSearchItem {

	private String label;
	private SearchItemType itemType=SearchItemType.SINGLE;
	private String term;
	private SearchDataType dataType=SearchDataType.STRING;

	private List<ValuePair> values = new ArrayList<>();
	private List<UniProtSearchItem> items = new ArrayList<>(); 
	private boolean isRange =false;
	private boolean hasEvidence= false;
	private String autoComplete;
	private String description;
	private String example;
	

	public String getLabel() {
		return label;
	}
	public void setLabel(String label) {
		this.label = label;
	}
	public SearchItemType getItemType() {
		return itemType;
	}
	public void setItemType(SearchItemType itemType) {
		this.itemType = itemType;
	}
	public String getTerm() {
		return term;
	}
	public void setTerm(String term) {
		this.term = term;
	}
	public SearchDataType getDataType() {
		return dataType;
	}
	public void setDataType(SearchDataType type) {
		this.dataType = type;
	}
	public List<ValuePair> getValues() {
		return values;
	}
	public void setValues(List<ValuePair> values) {
		this.values = values;
	}
	public List<UniProtSearchItem> getItems() {
		return items;
	}
	public void setItems(List<UniProtSearchItem> items) {
		this.items = items;
	}
	public boolean isRange() {
		return isRange;
	}
	public void setRange(boolean isRange) {
		this.isRange = isRange;
	}
	public boolean isHasEvidence() {
		return hasEvidence;
	}
	public void setHasEvidence(boolean hasEvidence) {
		this.hasEvidence = hasEvidence;
	}
	public String getDescription() {
		return description;
	}
	public void setDescription(String description) {
		this.description = description;
	}
	public String getExample() {
		return example;
	}
	public void setExample(String example) {
		if(example.contains(":")) {
			System.err.println(example);
		}
		this.example = example;
	}
	

	public String getAutoComplete() {
		return autoComplete;
	}
	public void setAutoComplete(String autoComplete) {
		this.autoComplete = autoComplete;
	}
	public int getCount() {
		if(itemType==SearchItemType.GROUP) {
			return items.stream().mapToInt(val->val.getCount()).sum();
		}else
			return 1;
	}
	private boolean isNullOrEmpty(String val) {
		return (val ==null)||( val.length()==0);
	}
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder();
		sb.append("{\n");
		if(!isNullOrEmpty(label)) {
			sb.append("label:").append(label).append("\n");
		}
	
		sb.append("itemType:").append(itemType).append("\n");
		
		if(!isNullOrEmpty(term)) {
			sb.append("term:").append(term).append("\n");
		}
		if(dataType != SearchDataType.UNKNOWN) {
			sb.append("dataType:").append(dataType.getName()).append("\n");
		}
	
		if(!isNullOrEmpty(description)) {
			sb.append("description:").append(description).append("\n");
		}
	
		if(!values.isEmpty()) {
			sb.append("values:[\n");
			values.forEach(val->sb.append("\t").append(val).append("\n"));
			sb.append("]\n");
		}
		if(!values.isEmpty()) {
			sb.append("values:[\n");
			values.forEach(val->sb.append("\t").append(val).append("\n"));
			sb.append("]\n");
		}
		if(isRange) {
			sb.append("isRange: true\n");
		}
		if(hasEvidence) {
			sb.append("hasEvidence: true\n");
		}
		if(!isNullOrEmpty(autoComplete)) {
			sb.append("autoComplete:").append(autoComplete).append("\n");
		}
		if(!isNullOrEmpty(example)) {
			sb.append("example:").append(example).append("\n");
		}
	
		
		if(!items.isEmpty()) {
			sb.append("items:[");
			items.forEach(val->sb.append(val).append("\n"));
			sb.append("]\n");
		}
		sb.append("}");
		return sb.toString();
	}
	public static class ValuePair{
		private String name;
		private String value;
		public ValuePair() {
			
		}
		public ValuePair(String name, String value) {
			this.name = name;
			this.value = value;
		}

		public String getName() {
			return name;
		}

		public void setName(String name) {
			this.name = name;
		}

		public String getValue() {
			return value;
		}

		public void setValue(String value) {
			this.value = value;
		}
		@Override
		public String toString() {
			return name+":" +value;
		}
		
	}
}
