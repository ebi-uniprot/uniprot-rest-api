package uk.ac.ebi.uniprot.api.disease.validator;

import lombok.extern.slf4j.Slf4j;
import uk.ac.ebi.uniprot.api.disease.validator.DiseaseFields;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;
import uk.ac.ebi.uniprot.search.field.validator.SolrQueryFieldValidator;

/**
 * This is used in conjunction with @ValidSolrQueryFields request validator
 *
 * @author sahmad
 */

@Slf4j
public class DiseaseSolrQueryFieldValidator implements SolrQueryFieldValidator {
    @Override
    public boolean hasField(String fieldName) {
        boolean result;
        try {
            DiseaseFields field = DiseaseFields.valueOf(fieldName.toUpperCase());
            result = field.isIndexed();
        } catch (IllegalArgumentException ile) {
            result = false;
        }
        return result;
    }

    @Override
    public String getInvalidFieldErrorMessage(String fieldName) {
        return "{search.disease.invalid.query.field}";
    }

    @Override
    public boolean hasValidFieldType(String fieldName, SearchFieldType searchFieldType) {
        return SearchFieldType.TERM.equals(searchFieldType);
    }

    @Override
    public String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType) {
        return "{search.disease.invalid.query.field.type}";
    }

    @Override
    public SearchFieldType getExpectedSearchFieldType(String fieldName) {
        return SearchFieldType.TERM;
    }

    @Override
    public boolean hasValidFieldValue(String fieldName, String value) {
        return true;
    }

    @Override
    public String getInvalidFieldValueErrorMessage(String fieldName, String value) {
        return "{search.disease.invalid.query.field.value." + fieldName + "}";
    }

}