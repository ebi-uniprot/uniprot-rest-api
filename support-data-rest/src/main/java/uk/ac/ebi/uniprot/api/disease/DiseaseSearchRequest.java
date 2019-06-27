package uk.ac.ebi.uniprot.api.disease;

import lombok.Data;
import uk.ac.ebi.uniprot.api.disease.validator.DiseaseFields;
import uk.ac.ebi.uniprot.api.disease.validator.DiseaseValidSortFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQueryFields;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrQuerySyntax;
import uk.ac.ebi.uniprot.api.rest.validation.ValidSolrSortFields;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Positive;

@Data
public class DiseaseSearchRequest {
    private static final int DEFAULT_RESULTS_SIZE = 25;

    @NotNull(message = "{search.required}")
    @ValidSolrQuerySyntax(message = "{search.invalid.query}")
    @ValidSolrQueryFields(fieldValidatorClazz = DiseaseFields.class, messagePrefix = "search.disease")
    private String query;

    @ValidSolrSortFields(sortFieldEnumClazz = DiseaseValidSortFields.class)
    private String sort;

    private String cursor;

    @Positive(message = "{search.positive}")
    private Integer size = DEFAULT_RESULTS_SIZE;

}
