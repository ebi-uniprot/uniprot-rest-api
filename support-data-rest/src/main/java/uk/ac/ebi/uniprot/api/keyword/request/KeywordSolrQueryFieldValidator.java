package uk.ac.ebi.uniprot.api.keyword.request;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.search.field.KeywordField;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;
import uk.ac.ebi.uniprot.search.field.validator.SolrQueryFieldValidator;

import java.util.function.Predicate;

@Slf4j
public class KeywordSolrQueryFieldValidator implements SolrQueryFieldValidator {

    @Override
    public boolean hasField(String fieldName) {
        boolean result = true;
        try {
            KeywordField.Search.valueOf(fieldName);
        } catch (Exception e) {
            result = false;
        }
        return result;
    }

    @Override
    public String getInvalidFieldErrorMessage(String fieldName) {
        return "{search.keyword.invalid.query.field}";
    }

    @Override
    public boolean hasValidFieldType(String fieldName, SearchFieldType searchFieldType) {
        KeywordField.Search search = KeywordField.Search.valueOf(fieldName);
        return search.getSearchFieldType().equals(searchFieldType);
    }

    @Override
    public String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType) {
        return "{search.keyword.invalid.query.field.type}";
    }

    @Override
    public SearchFieldType getExpectedSearchFieldType(String fieldName) {
        return KeywordField.Search.valueOf(fieldName).getSearchFieldType();
    }

    @Override
    public boolean hasValidFieldValue(String fieldName, String value) {
        KeywordField.Search search = KeywordField.Search.valueOf(fieldName);
        Predicate<String> fieldValueValidator = search.getFieldValueValidator();
        if (fieldValueValidator != null) {
            try {
                return fieldValueValidator.test(value);
            } catch (Exception e) {
                log.debug("exception validating field '" + fieldName + "' value: '" + value + "' with error message" + e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public String getInvalidFieldValueErrorMessage(String fieldName, String value) {
        return "{search.keyword.invalid.query.field.value." + fieldName + "}";
    }

}
