package uk.ac.ebi.uniprot.api.disease.validator;

public enum DiseaseValidSortFields {
    ACCESSION("accession");

    private String solrFieldName;

    DiseaseValidSortFields(String solrFieldName) {
        this.solrFieldName = solrFieldName;
    }

    public String getSolrFieldName() {
        return solrFieldName;
    }

    @Override
    public String toString() {
        return this.solrFieldName;
    }
}
