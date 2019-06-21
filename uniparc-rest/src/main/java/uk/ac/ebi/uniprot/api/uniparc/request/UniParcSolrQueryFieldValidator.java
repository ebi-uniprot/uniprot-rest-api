package uk.ac.ebi.uniprot.api.uniparc.request;

import java.util.function.Predicate;

import uk.ac.ebi.uniprot.search.field.SearchFieldType;
import uk.ac.ebi.uniprot.search.field.UniParcField;
import uk.ac.ebi.uniprot.search.field.validator.SolrQueryFieldValidator;

/**
 *
 * @author jluo
 * @date: 20 Jun 2019
 *
*/

public class UniParcSolrQueryFieldValidator implements SolrQueryFieldValidator {

	@Override
	public boolean hasField(String fieldName) {
		 boolean result = true;
	        try{
	            UniParcField.Search.valueOf(fieldName);
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
		UniParcField.Search search = UniParcField.Search.valueOf(fieldName);
        return search.getSearchFieldType().equals(searchFieldType);
	}

	@Override
	public String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType) {
		return "{search.uniparc.invalid.query.field.type}";
	}

	@Override
	public SearchFieldType getExpectedSearchFieldType(String fieldName) {
		 return UniParcField.Search.valueOf(fieldName).getSearchFieldType();
	}

	@Override
	public boolean hasValidFieldValue(String fieldName, String value) {
		UniParcField.Search search = UniParcField.Search.valueOf(fieldName);
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
		 return "{search.uniparc.invalid.query.field.value."+fieldName+"}";
	}

}

