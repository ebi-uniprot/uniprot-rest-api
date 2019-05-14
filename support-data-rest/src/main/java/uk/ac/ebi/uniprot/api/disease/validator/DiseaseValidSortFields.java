package uk.ac.ebi.uniprot.api.disease.validator;

import lombok.Getter;

@Getter
public enum DiseaseValidSortFields {
    ACCESSION("accession");

    private String solrFieldName;

    DiseaseValidSortFields(String solrFieldName) {
        this.solrFieldName = solrFieldName;
    }

    @Override
    public String toString() {
        return this.solrFieldName;
    }
}
