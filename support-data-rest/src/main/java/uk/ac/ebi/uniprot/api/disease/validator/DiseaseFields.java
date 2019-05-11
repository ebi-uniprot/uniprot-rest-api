package uk.ac.ebi.uniprot.api.disease.validator;

import lombok.Getter;

@Getter
public enum DiseaseFields {
    ACCESSION("accession", true, true),
    NAME("name", true, false, false),
    CONTENT("content", true, false, false);

    private String solrFieldName;
    private boolean indexed;
    private boolean stored;
    private boolean visible = true;

    DiseaseFields(String solrFieldName, boolean indexed, boolean stored) {
        this.solrFieldName = solrFieldName;
        this.indexed = indexed;
        this.stored = stored;
    }

    DiseaseFields(String solrFieldName, boolean indexed, boolean stored, boolean visible) {
        this.solrFieldName = solrFieldName;
        this.indexed = indexed;
        this.stored = stored;
        this.visible = visible;
    }

    @Override
    public String toString() {
        return this.solrFieldName;
    }

    public static boolean isVisible(String solrFieldName) {
        for(DiseaseFields field : DiseaseFields.values()){
            if(field.getSolrFieldName().equals(solrFieldName)){
                return field.isVisible();
            }
        }
        return false;
    }
}