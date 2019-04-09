package uk.ac.ebi.uniprot.api.crossref.model;

public enum CrossRefValidSortFields {
    ACCESSION("accession"),
    ABBREV("abbrev"),
    NAME("name"),
    DOIID("doi_id"),
    PUBMEDID("pubmed_id"),
    LINKTYPE("link_type"),
    CATEGORY("category_str");

    private String solrFieldName;

    CrossRefValidSortFields(String solrFieldName) {
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
