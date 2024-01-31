package org.uniprot.api.rest.openapi;

public class OpenApiConstants {

    public static final String QUERY_ADVANCED =
            "Advanced queries can be built with parentheses and conditionals such as AND/OR/NOT.";
    public static final String QUERY_DESCRIPTION = "Criteria to search. " + QUERY_ADVANCED;
    public static final String FIELDS_DESCRIPTION =
            "List of fields to be returned, separated by commas.";
    public static final String SORT_DESCRIPTION = "Name of the field to be sorted on.";
    public static final String INCLUDE_ISOFORM_DESCRIPTION =
            "Default: <tt>false</tt>. Use <tt>true</tt> to include isoforms.";

    public static final String SIZE_DESCRIPTION = "Pagination size. Defaults to 25. (Max. size of 500)";
    public static final String DOWNLOAD_DESCRIPTION =
            "Default: <tt>false</tt>. Use <tt>true</tt> to download as a file.";
    public static final String JOB_ID_DESCRIPTION =
            "The <tt>jobId</tt> returned from the <tt>run</tt> submission.";

    // UniProtKB
    public static final String QUERY_UNIPROTKB_ID_DESCRIPTION =
            "Criteria to search within the accessions. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";
    public static final String QUERY_UNIPROTKB_SEARCH_DESCRIPTION =
            "Criteria to search UniProtKB. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";
    public static final String QUERY_UNIPROTKB_EXAMPLE = "insulin AND reviewed:true";
    public static final String QUERY_UNIPROTKB_EC_DESCRIPTION =
            "Query to search Enzyme Classification hierarchy." + QUERY_ADVANCED;
    public static final String QUERY_UNIPROTKB_GO_DESCRIPTION =
            "Query to search Gene Ontology hierarchy." + QUERY_ADVANCED;
    public static final String QUERY_UNIPROTKB_KEYWORD_DESCRIPTION =
            "Query to search keyword hierarchy." + QUERY_ADVANCED;
    public static final String QUERY_UNIPROTKB_TAXONOMY_DESCRIPTION =
            "Query to search taxonomy hierarchy." + QUERY_ADVANCED;

    public static final String FIELDS_UNIPROTKB_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_UNIPROTKB_EXAMPLE =
            "accession,protein_name,gene_names,organism_name";
    public static final String FACETS_PUBLICATION =
            "List of publication facets to be applied, separated by commas. <a href='https://rest.uniprot.org/configure/uniprotkb/publication/facets'>List of valid facets</a>";
    public static final String FACET_FILTER_PUBLICATION =
            "Criteria to filter publications. <a href='https://rest.uniprot.org/configure/uniprotkb/publication/facets'>List of valid facets</a>";
    public static final String ACCESSIONS_UNIPROTKB_DESCRIPTION =
            "List of UniProtKB accessions, separated by commas.";
    public static final String ACCESSIONS_UNIPROTKB_EXAMPLE =
            "List of UniProtKB accessions, separated by commas.";
    public static final String SIZE_UNIPROTKB_ID_DESCRIPTION =
            "Pagination size. Defaults to number of accessions passed (Single page).";
    public static final String SORT_UNIPROTKB_ID_DESCRIPTION =
            SORT_DESCRIPTION
                    + " Defaults to order of accessions passed. <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_UNIPROTKB_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_UNIPROTKB_EXAMPLE = "accession desc";
    public static final String ACCESSION_DESCRIPTION = "Unique identifier for the UniProtKB entry";
    public static final String VERSION_DESCRIPTION =
            "Version of the entry. Versions are integers 1 or above; enter <tt>last</tt> for the latest version.</br>"
                    + "Please note that when passing <tt>version</tt> file formats are restricted to <tt>fasta</tt> and <tt>txt</tt> only";
    public static final String FORMAT_UNIPROTKB_DESCRIPTION =
            "The file format for download. <a href='https://rest.uniprot.org/configure/uniprotkb/formats'>Valid formats are listed here</a>";
    public static final String GROUP_PARENT_DESCRIPTION =
            "If nothing is passed, the root of the hierarchical tree is returned with all available children. If the parent is specified, only children below the parent node is returned.";
    public static final String GROUP_EC_DESCRIPTION =
            "List of Enzyme Classification groups with respect to the given query and parent.";
    public static final String GROUP_GO_DESCRIPTION =
            "List of Gene Ontology groups with respect to the given query and parent";
    public static final String GROUP_KEYWORD_DESCRIPTION =
            "List of Keyword groups with respect to the given query and parent";
    public static final String GROUP_TAXONOMY_DESCRIPTION =
            "List of Taxonomy groups with respect to the given query and parent";

    // Proteome
    public static final String UPID_DESCRIPTION = "Unique identifier for the Proteome entry";

    // Id Mapping
    public static final String ID_MAPPING_JOB_ID_DESCRIPTION =
            "Unique identifier for idmapping job";
    public static final String SUB_SEQUENCE_DESCRIPTION =
            "Flag to write subsequences. Only accepted in fasta format";
}
