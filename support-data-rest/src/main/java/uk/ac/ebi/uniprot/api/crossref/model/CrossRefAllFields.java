package uk.ac.ebi.uniprot.api.crossref.model;

import uk.ac.ebi.uniprot.search.field.BoostValue;
import uk.ac.ebi.uniprot.search.field.SearchField;
import uk.ac.ebi.uniprot.search.field.SearchFieldType;

import java.util.function.Predicate;

public enum CrossRefAllFields implements SearchField {
    accession(SearchFieldType.TERM),
    abbrev(SearchFieldType.TERM),
    name_only(SearchFieldType.TERM),
    doi_id(SearchFieldType.TERM),
    pubmed_id(SearchFieldType.TERM),
    link_type(SearchFieldType.TERM),
    category_str(SearchFieldType.TERM),
    category_facet(SearchFieldType.TERM),
    content(SearchFieldType.TERM);

    private final Predicate<String> fieldValueValidator;
    private final SearchFieldType searchFieldType;
    private final BoostValue boostValue;

    CrossRefAllFields(SearchFieldType searchFieldType, Predicate<String> fieldValueValidator, BoostValue boostValue) {
        this.searchFieldType = searchFieldType;
        this.fieldValueValidator = fieldValueValidator;
        this.boostValue = boostValue;
    }

    CrossRefAllFields(SearchFieldType searchFieldType) {
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