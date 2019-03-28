package uk.ac.ebi.uniprot.api.rest.validation.validator;

/**
 * This class is responsible to have search field value validation Predicates
 *
 *
 * @author lgonzales
 */
public class FieldValueValidator {

    public static final String ACCESSION_REGEX = "([O,P,Q][0-9][A-Z|0-9]{3}[0-9]|[A-N,R-Z]([0-9][A-Z][A-Z|0-9]{2}){1,2}[0-9])(-\\d+)*";

    /**
     *  This method is responsible to validate any accession value
     *
     * @param value uniprot accession value
     * @return true if it is a valid value, otherwise returns false.
     */
    public static boolean isAccessionValid(String value){
        boolean result = false;
        if(value != null) {
            result = value.toUpperCase().matches(ACCESSION_REGEX);
        }
        return result;
    }


    /**
     *  This method is responsible to validate any true|false boolean value
     *
     * @param value field value
     * @return true if it is a valid value, otherwise returns false.
     */
    public static boolean isBooleanValue(String value){
        boolean result = false;
        String booleanRegex = "^true|false$";
        if(value != null) {
            result = value.matches(booleanRegex);
        }
        return result;
    }

    /**
     *  This method is responsible to validate any numeric value
     *
     * @param value field value
     * @return true if it is a valid value, otherwise returns false.
     */
    public static boolean isNumberValue(String value){
        boolean result = false;
        String numberRegex = "^[0-9]+$";
        if(value != null) {
            result = value.matches(numberRegex);
        }
        return result;
    }

    /**
     *  This method is responsible to validate proteome id value
     *
     * @param value field value
     * @return true if it is a valid value, otherwise returns false.
     */
    public static boolean isProteomeIdValue(String value) {
        boolean result = false;
        String numberRegex = "^UP[0-9]{9}$";
        if(value != null) {
            result = value.toUpperCase().matches(numberRegex);
        }
        return result;
    }
}
