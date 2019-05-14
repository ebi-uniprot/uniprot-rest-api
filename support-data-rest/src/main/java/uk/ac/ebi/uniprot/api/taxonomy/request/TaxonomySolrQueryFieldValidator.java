package uk.ac.ebi.uniprot.api.taxonomy.request;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;
import uk.ac.ebi.uniprot.search.field.TaxonomyField;
import uk.ac.ebi.uniprot.search.field.validator.SolrQueryFieldValidator;

import java.util.function.Predicate;

@Slf4j
public class TaxonomySolrQueryFieldValidator implements SolrQueryFieldValidator {

    @Override
    public boolean hasField(String fieldName) {
        boolean result = true;
        try{
            TaxonomyField.Search.valueOf(fieldName);
        } catch (Exception e){
            result = false;
        }
        return result;
    }

    @Override
    public String getInvalidFieldErrorMessage(String fieldName) {
        return "{search.taxonomy.invalid.query.field}";
    }

    @Override
    public boolean hasValidFieldType(String fieldName, SearchFieldType searchFieldType) {
        TaxonomyField.Search search = TaxonomyField.Search.valueOf(fieldName);
        return search.getSearchFieldType().equals(searchFieldType);
    }

    @Override
    public String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType) {
        return "{search.taxonomy.invalid.query.field.type}";
    }

    @Override
    public SearchFieldType getExpectedSearchFieldType(String fieldName) {
        return TaxonomyField.Search.valueOf(fieldName).getSearchFieldType();
    }

    @Override
    public boolean hasValidFieldValue(String fieldName, String value) {
        TaxonomyField.Search search = TaxonomyField.Search.valueOf(fieldName);
        Predicate<String> fieldValueValidator = search.getFieldValueValidator();
        if(fieldValueValidator != null) {
            try {
                return fieldValueValidator.test(value);
            }catch (Exception e){
                log.debug("exception validating field '"+fieldName+"' value: '"+value+"' with error message"+e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public String getInvalidFieldValueErrorMessage(String fieldName, String value) {
        return "{search.taxonomy.invalid.query.field.value."+fieldName+"}";
    }

}
