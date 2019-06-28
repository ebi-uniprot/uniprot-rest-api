package uk.ac.ebi.uniprot.api.disease.validator;

import lombok.Getter;
import uk.ac.ebi.uniprot.search.field.BoostValue;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;

import java.util.function.Predicate;

@Getter
public enum DiseaseFields implements SearchField {
    accession(SearchFieldType.TERM),
    name(SearchFieldType.TERM),
    content(SearchFieldType.TERM);

    private final Predicate<String> fieldValueValidator;
    private final SearchFieldType searchFieldType;
    private final BoostValue boostValue;

    DiseaseFields(SearchFieldType searchFieldType, Predicate<String> fieldValueValidator, BoostValue boostValue) {
        this.searchFieldType = searchFieldType;
        this.fieldValueValidator = fieldValueValidator;
        this.boostValue = boostValue;
    }

    DiseaseFields(SearchFieldType searchFieldType) {
        this(searchFieldType, null, null);
    }

    @Override
    public BoostValue getBoostValue() {
        return this.boostValue;
    }

    @Override
    public String getName() {
        return this.name();
    }

    @Override
    public SearchFieldType getSearchFieldType() {
        return this.searchFieldType;
    }

    @Override
    public Predicate<String> getFieldValueValidator() {
        return this.fieldValueValidator;
    }
}