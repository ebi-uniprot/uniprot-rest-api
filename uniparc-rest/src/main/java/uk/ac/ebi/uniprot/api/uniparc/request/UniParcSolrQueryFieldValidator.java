package uk.ac.ebi.uniprot.api.uniparc.request;

import uk.ac.ebi.uniprot.search.field.ProteomeField;
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
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public boolean hasValidFieldValue(String fieldName, String value) {
		// TODO Auto-generated method stub
		return false;
	}

	@Override
	public String getInvalidFieldValueErrorMessage(String fieldName, String value) {
		// TODO Auto-generated method stub
		return null;
	}

}

