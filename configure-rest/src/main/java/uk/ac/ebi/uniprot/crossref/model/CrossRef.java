package uk.ac.ebi.uniprot.crossref.model;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.apache.solr.client.solrj.beans.Field;

import java.util.List;

//TODO move this class to common, we have similar class(DBXRef.java) in uniprot-indexer project
@Getter
@Setter
@Builder
public class CrossRef {
    @Field
    private String accession;
    @Field
    private String abbrev;
    @Field
    private String name;
    @Field("pubmed_id")
    private String pubMedId;
    @Field("doi_id")
    private String doiId;
    @Field("link_type")
    private String linkType;
    @Field
    private String server;
    @Field("db_url")
    private String dbUrl;
    @Field
    private List<String> content;
    @Field("category_facet")
    private String category;
}
