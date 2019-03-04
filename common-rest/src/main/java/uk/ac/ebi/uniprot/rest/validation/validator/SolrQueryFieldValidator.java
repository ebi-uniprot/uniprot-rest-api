package uk.ac.ebi.uniprot.rest.validation.validator;

import uk.ac.ebi.uniprot.rest.SearchFieldType;

/**
 * This interface is responsible to define methods that are used to validate solr query fields in solr queries
 * This is used in conjunction with @ValidSolrQueryFields request validator
 *
 * @author lgonzales
 */
public interface SolrQueryFieldValidator {

    boolean hasField(String fieldName);

    String getInvalidFieldErrorMessage(String fieldName);


    boolean hasValidFieldType(String fieldName, SearchFieldType searchFieldType);

    String getInvalidFieldTypeErrorMessage(String fieldName, SearchFieldType searchFieldType);

    SearchFieldType getExpectedSearchFieldType(String fieldName);

    boolean hasValidFieldValue(String fieldName, String value);

    String getInvalidFieldValueErrorMessage(String fieldName, String value);

}
