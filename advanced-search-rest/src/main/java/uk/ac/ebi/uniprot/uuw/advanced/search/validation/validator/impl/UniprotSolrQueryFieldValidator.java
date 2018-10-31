package uk.ac.ebi.uniprot.uuw.advanced.search.validation.validator.impl;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import uk.ac.ebi.uniprot.dataservice.client.SearchFieldType;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtField;
import uk.ac.ebi.uniprot.uuw.advanced.search.validation.validator.SolrQueryFieldValidator;

import java.util.function.Predicate;

/**
 * This class is responsible to implement methods that are used to validate UNIPROT solr query fields in solr queries
 * This is used in conjunction with @ValidSolrQueryFields request validator
 *
 * @author lgonzales
 */
public class UniprotSolrQueryFieldValidator  implements SolrQueryFieldValidator{

    private static final Logger LOGGER = LoggerFactory.getLogger(UniprotSolrQueryFieldValidator.class);

    @Override
    public boolean hasField(String fieldName) {
        boolean result = true;
        try{
            UniProtField.Search.valueOf(fieldName);
        } catch (Exception e){
            result = false;
        }
        return result;
    }

    @Override
    public String getInvalidFieldErrorMessage(String fieldName) {
        return "{uk.ac.ebi.uniprot.uuw.advanced.search.uniprot.invalid.query.field}";
    }

    @Override
    public boolean hasValidFieldType(String fieldName, SearchFieldType searchFieldType) {
        UniProtField.Search search = UniProtField.Search.valueOf(fieldName);
        return search.getSearchFieldType().equals(searchFieldType);
    }

    @Override
    public String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType) {
        return "{uk.ac.ebi.uniprot.uuw.advanced.search.uniprot.invalid.query.field.type}";
    }

    @Override
    public SearchFieldType getExpectedSearchFieldType(String fieldName) {
        return UniProtField.Search.valueOf(fieldName).getSearchFieldType();
    }

    @Override
    public boolean hasValidFieldValue(String fieldName, String value) {
        UniProtField.Search search = UniProtField.Search.valueOf(fieldName);
        Predicate<String> fieldValueValidator = search.getFieldValueValidator();
        if(fieldValueValidator != null) {
            try {
                return fieldValueValidator.test(value);
            }catch (Exception e){
                LOGGER.debug("exception validating field '"+fieldName+"' value: '"+value+"' with error message"+e.getMessage());
                return false;
            }
        } else {
            return true;
        }
    }

    @Override
    public String getInvalidFieldValueErrorMessage(String fieldName, String value) {
        return "{uk.ac.ebi.uniprot.uuw.advanced.search.uniprot.invalid.query.field.value."+fieldName+"}";
    }

}
