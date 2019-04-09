package uk.ac.ebi.uniprot.api.crossref.model;

public enum CrossRefAllFields {
    ACCESSION("accession", true, true),
    ABBREV("abbrev", true, true),
    NAME("name", true, true),
    DOIID("doi_id", true, true),
    PUBMEDID("pubmed_id", true, true),
    LINKTYPE("link_type", true, true),
    CATEGORY("category_str", true, false),
    SERVER("server", false, true),
    DBURL("db_url", false, true),
    CATEGORY_FACET("category_facet", true, true, false),
    CONTENT("content", true, false, false);

    private String solrFieldName;
    private boolean indexed;
    private boolean stored;
    private boolean visible = true;

    CrossRefAllFields(String solrFieldName, boolean indexed, boolean stored) {
        this.solrFieldName = solrFieldName;
        this.indexed = indexed;
        this.stored = stored;
    }

    CrossRefAllFields(String solrFieldName, boolean indexed, boolean stored, boolean visible) {
        this.solrFieldName = solrFieldName;
        this.indexed = indexed;
        this.stored = stored;
        this.visible = visible;
    }

    public String getSolrFieldName(){
        return this.solrFieldName;
    }

    public boolean isIndexed(){
        return this.indexed;
    }

    public boolean isStored(){
        return this.stored;
    }

    public boolean isVisible(){
        return this.visible;
    }

    @Override
    public String toString() {
        return this.solrFieldName;
    }

    public static boolean isVisible(String solrFieldName) {
        for(CrossRefAllFields field : CrossRefAllFields.values()){
            if(field.getSolrFieldName().equals(solrFieldName)){
                return field.isVisible();
            }
        }
        return false;
    }
}