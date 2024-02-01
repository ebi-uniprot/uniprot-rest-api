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

    public static final String SIZE_DESCRIPTION =
            "Pagination size. Defaults to 25. (Max. size of 500)";
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
            "List of UniProtKB accessions, separated by commas. (Max 1K accessions)";
    public static final String ACCESSIONS_UNIPROTKB_EXAMPLE = "P21802,P05067,A2VEY9";
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

    //Support date cross reference
    public static final String QUERY_CROSSREF_DESCRIPTION = "Criteria to search cross-references. It can take any valid Lucene query.";
    public static final String QUERY_CROSSREF_EXAMPLE = "";
    public static final String SORT_CROSSREF_DESCRIPTION = "Name of the field to be sorted on";
    public static final String SORT_CROSSREF_EXAMPLE = "";
    public static final String FIELDS_CROSSREF_DESCRIPTION = "Comma separated list of fields to be returned in response";
    public static final String FIELDS_CROSSREF_EXAMPLE = "";
    public static final String ID_CROSSREF_DESCRIPTION = "cross-references database id to find";
    public static final String ID_CROSSREF_EXAMPLE = "";
    public static final String ID_CROSSREF_OPERATION = "Get cross-references by database id.";
    public static final String SEARCH_CROSSREF_OPERATION = "Search cross-references by given Lucene search query.";
    public static final String STREAM_CROSSREF_OPERATION = "Download cross-references by given Lucene search query.";

    //Support date Keywords
    public static final String ID_KEYWORDS_OPERATION = "Get Keyword by keywordId.";
    public static final String SEARCH_KEYWORDS_OPERATION = "Search Keywords by given Lucene search query.";
    public static final String STREAM_KEYWORDS_OPERATION = "Download Keywords by given Lucene search query.";
    public static final String ID_KEYWORDS_DESCRIPTION = "Keyword id to find";
    public static final String ID_KEYWORDS_EXAMPLE = "";
    public static final String FIELDS_KEYWORDS_DESCRIPTION = "Comma separated list of fields to be returned in response";
    public static final String FIELDS_KEYWORDS_EXAMPLE = "";
    public static final String SORT_KEYWORDS_DESCRIPTION = "Name of the field to be sorted on";
    public static final String SORT_KEYWORDS_EXAMPLE = "";
    public static final String QUERY_KEYWORDS_DESCRIPTION = "Criteria to search keywords. It can take any valid Lucene query.";
    public static final String QUERY_KEYWORDS_EXAMPLE = "";

    //Support date diseases
    public static final String ID_DISEASE_OPERATION = "Get diseases by id.";
    public static final String SEARCH_DISEASE_OPERATION = "Search disease by given Lucene search query.";
    public static final String STREAM_DISEASE_OPERATION = "Download disease by given Lucene search query.";
    public static final String QUERY_DISEASE_DESCRIPTION = "Criteria to search diseases. It can take any valid Lucene query.";
    public static final String QUERY_DISEASE_EXAMPLE = "Alzheimer";
    public static final String SORT_DISEASE_DESCRIPTION = "Name of the field to be sorted on";
    public static final String SORT_DISEASE_EXAMPLE = "id asc";
    public static final String ID_DISEASE_DESCRIPTION = "disease id to find";
    public static final String ID_DISEASE_EXAMPLE = "DI-04530";
    public static final String FIELDS_DISEASE_DESCRIPTION = "Comma separated list of fields to be returned in response";
    public static final String FIELDS_DISEASE_EXAMPLE = "id,name";

    //support- data literature citations
    public static final String ID_LIT_OPERATION = "Get literature by citation id.";
    public static final String ID_LIT_DESCRIPTION = "Citation id to find";
    public static final String ID_LIT_EXAMPLE = "";
    public static final String FIELDS_LIT_DESCRIPTION = "Comma separated list of fields to be returned in response";
    public static final String FIELDS_LIT_EXAMPLE = "";
    public static final String SEARCH_LIT_OPERATION = "Search literature by given Lucene search query.";
    public static final String STREAM_LIT_OPERATION = "Download literature by given Lucene search query.";
    public static final String QUERY_LIT_DESCRIPTION = "Criteria to search literature publications. It can take any valid Lucene query.";
    public static final String QUERY_LIT_EXAMPLE = "";
    public static final String SORT_LIT_DESCRIPTION = "Name of the field to be sorted on";
    public static final String SORT_LIT_EXAMPLE = "";

    //STATISTICS
    public static final String RELEASE_NAME_STATS_DESCRIPTION = "UniProt release name";
    public static final String TYPE_STATS_DESCRIPTION = "Statistic type";
    public static final String CATEGORY_STATS_DESCRIPTION = "List of statistics categories, separated by commas.";
    public static final String RELEASE_NAME_STATS_EXAMPLE = "2023_05";
    public static final String TYPE_STATS_EXAMPLE = "reviewed";
    public static final String CATEGORY_STATS_EXAMPLE = "TOTAL_ORGANISM,COMMENTS";

    //SUBCEL
    public static final String ID_SUBCEL_OPERATION = "Get subcellular locations by id.";
    public static final String SEARCH_SUBCEL_OPERATION = "Search subcellular locations by given Lucene search query.";
    public static final String STREAM_SUBCEL_OPERATION = "Download subcellular locations by given Lucene search query.";
    public static final String ID_SUBCEL_DESCRIPTION = "Subcellular location id to find";
    public static final String ID_SUBCEL_EXAMPLE = "";
    public static final String FIELDS_SUBCEL_DESCRIPTION = "Comma separated list of fields to be returned in response";
    public static final String FIELDS_SUBCEL_EXAMPLE = "";
    public static final String QUERY_SUBCEL_DESCRIPTION = "Criteria to search Subcellular locations. It can take any valid Lucene query.";
    public static final String QUERY_SUBCEL_EXAMPLE = "";
    public static final String SORT_SUBCEL_DESCRIPTION = "Name of the field to be sorted on";
    public static final String SORT_SUBCEL_EXAMPLE = "";

    //SUGGEST
    public static final String SUGGESTER_OPERATION = "Provide suggestions (auto-complete) for a subset of datasets (dictionaries).";
    public static final String DICT_SUGGESTER_DESCRIPTION = "Suggest data dictionary.";
    public static final String DICT_SUGGESTER_EXAMPLE = "ORGANISM";
    public static final String QUERY_SUGGESTER_DESCRIPTION = "Text to look up for auto-complete.";
    public static final String QUERY_SUGGESTER_EXAMPLE = "huma";

    //TAXONOMY
    public static final String ID_TAX_OPERATION = "Get taxonomy by id.";
    public static final String IDS_TAX_OPERATION = "Get taxonomy by comma separated taxon ids.";
    public static final String SEARCH_TAX_OPERATION = "Search taxonomies by given Lucene search query.";
    public static final String STREAM_TAX_OPERATION = "Download taxonomies by given Lucene search query.";
    public static final String ID_TAX_DESCRIPTION = "Taxon id to find";
    public static final String FIELDS_TAX_DESCRIPTION = "Comma separated list of fields to be returned in response";
    public static final String ID_TAX_EXAMPLE = "";
    public static final String FIELDS_TAX_EXAMPLE = "";
}
