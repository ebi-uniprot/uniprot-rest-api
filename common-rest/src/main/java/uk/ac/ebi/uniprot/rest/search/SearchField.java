package uk.ac.ebi.uniprot.rest.search;

/**
 *
 * @author lgonzales
 */
public interface SearchField {

    Float getBoostValue();

    boolean hasValidValue(String value);

    String getName();

}
