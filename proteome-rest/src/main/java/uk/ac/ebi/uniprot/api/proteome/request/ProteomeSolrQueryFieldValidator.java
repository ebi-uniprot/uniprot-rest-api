package uk.ac.ebi.uniprot.api.proteome.request;

import java.util.function.Predicate;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.search.field.ProteomeField;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;
import uk.ac.ebi.uniprot.search.field.validator.SolrQueryFieldValidator;

/**
 *
 * @author jluo
 * @date: 26 Apr 2019
 *
*/
@Slf4j
public class ProteomeSolrQueryFieldValidator implements SolrQueryFieldValidator {

	@Override
	public boolean hasField(String fieldName) {
		 boolean result = true;
	        try{
	            ProteomeField.Search.valueOf(fieldName);
	        } catch (Exception e){
	            result = false;
	        }
	        return result;
	}

	@Override
	public String getInvalidFieldErrorMessage(String fieldName) {
		return "{search.invalid.query.field}";
	}

	@Override
	public boolean hasValidFieldType(String fieldName, SearchFieldType searchFieldType) {
		ProteomeField.Search search = ProteomeField.Search.valueOf(fieldName);
	        return search.getSearchFieldType().equals(searchFieldType);
	}

	@Override
	public String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType) {
		  return "{search.proteome.invalid.query.field.type}";
	}

	@Override
	public SearchFieldType getExpectedSearchFieldType(String fieldName) {
		 return ProteomeField.Search.valueOf(fieldName).getSearchFieldType();
	}

	@Override
	public boolean hasValidFieldValue(String fieldName, String value) {
		ProteomeField.Search search = ProteomeField.Search.valueOf(fieldName);
	        Predicate<String> fieldValueValidator = search.getFieldValueValidator();
	        if(fieldValueValidator != null) {
	            try {
	                return fieldValueValidator.test(value);
	            }catch (Exception e){
	          
	                return false;
	            }
	        } else {
	            return true;
	        }
	}

	@Override
	public String getInvalidFieldValueErrorMessage(String fieldName, String value) {
		 return "{search.proteome.invalid.query.field.value."+fieldName+"}";
	}

}

