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
            "List of Enzyme Classification (EC) groups with respect to the given query and parent.";
    public static final String GROUP_GO_DESCRIPTION =
            "List of Gene Ontology (GO) groups with respect to the given query and parent";
    public static final String GROUP_KEYWORD_DESCRIPTION =
            "List of keyword groups with respect to the given query and parent";
    public static final String GROUP_TAXONOMY_DESCRIPTION =
            "List of taxonomy groups with respect to the given query and parent";

    // Proteome
    public static final String UPID_DESCRIPTION = "Unique identifier for the Proteome entry";

    // Id Mapping
    public static final String ID_MAPPING_JOB_ID_DESCRIPTION =
            "Unique identifier for idmapping job";
    public static final String SUB_SEQUENCE_DESCRIPTION =
            "Flag to write subsequences. Only accepted in fasta format";

    //Support date cross reference
    public static final String QUERY_CROSSREF_DESCRIPTION = "Criteria to search cross-reference databases. "
            + QUERY_ADVANCED
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";;
    public static final String QUERY_CROSSREF_EXAMPLE = "Ensembl";
    public static final String SORT_CROSSREF_DESCRIPTION = SORT_DESCRIPTION
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_CROSSREF_EXAMPLE = "id desc";
    public static final String FIELDS_CROSSREF_DESCRIPTION = FIELDS_DESCRIPTION
            + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_CROSSREF_EXAMPLE = "id,name,abbrev,category";
    public static final String ID_CROSSREF_DESCRIPTION = "Unique identifier for the cross-reference database entry";
    public static final String ID_CROSSREF_EXAMPLE = "DB-0244";
    public static final String ID_CROSSREF_OPERATION = "Get cross-reference database entry by a single accession.";
    public static final String SEARCH_CROSSREF_OPERATION = "Retrieve cross-reference database entries by a search query.";
    public static final String STREAM_CROSSREF_OPERATION = "Download cross-reference database entries retrieved by a search query.";

    //Support date Keywords
    public static final String ID_KEYWORDS_OPERATION = "Get keyword entry by a single accession.";
    public static final String SEARCH_KEYWORDS_OPERATION = "Retrieve keyword entries by a search query.";
    public static final String STREAM_KEYWORDS_OPERATION = "Download keyword entries retrieved by a search query.";
    public static final String ID_KEYWORDS_DESCRIPTION = "Unique identifier for the keyword entry";
    public static final String ID_KEYWORDS_EXAMPLE = "KW-0020";
    public static final String FIELDS_KEYWORDS_DESCRIPTION = FIELDS_DESCRIPTION
            + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_KEYWORDS_EXAMPLE = "id,name,category";
    public static final String SORT_KEYWORDS_DESCRIPTION = SORT_DESCRIPTION
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_KEYWORDS_EXAMPLE = "name asc";
    public static final String QUERY_KEYWORDS_DESCRIPTION = "Criteria to search keywords. "
            + QUERY_ADVANCED
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";;
    public static final String QUERY_KEYWORDS_EXAMPLE = "Phosphoprotein";

    //Support date diseases
    public static final String ID_DISEASE_OPERATION = "Get human disease entry by a single accession.";
    public static final String SEARCH_DISEASE_OPERATION = "Retrieve human disease entries by a search query.";
    public static final String STREAM_DISEASE_OPERATION = "Download human disease entries retrieved by a search query.";
    public static final String QUERY_DISEASE_DESCRIPTION = "Criteria to search human diseases. "
            + QUERY_ADVANCED
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";;
    public static final String QUERY_DISEASE_EXAMPLE = "Alzheimer";
    public static final String SORT_DISEASE_DESCRIPTION = SORT_DESCRIPTION
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_DISEASE_EXAMPLE = "id asc";
    public static final String ID_DISEASE_DESCRIPTION = "Unique identifier for the human disease entry";
    public static final String ID_DISEASE_EXAMPLE = "DI-04530";
    public static final String FIELDS_DISEASE_DESCRIPTION = FIELDS_DESCRIPTION
            + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_DISEASE_EXAMPLE = "id,name";

    //support- data literature citations
    public static final String ID_LIT_OPERATION = "Get literature citation entry by a single accession.";
    public static final String ID_LIT_DESCRIPTION = "Unique identifier for the literature citation entry";
    public static final String ID_LIT_EXAMPLE = "10024047";
    public static final String FIELDS_LIT_DESCRIPTION = FIELDS_DESCRIPTION
            + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_LIT_EXAMPLE = "id,title,authors,publication_date";
    public static final String SEARCH_LIT_OPERATION = "Retrieve literature citation entries by a search query.";
    public static final String STREAM_LIT_OPERATION = "Download literature citation entries retrieved by a search query.";
    public static final String QUERY_LIT_DESCRIPTION = "Criteria to search literature citations. "
            + QUERY_ADVANCED
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";;
    public static final String QUERY_LIT_EXAMPLE = "\"genome analysis\"";
    public static final String SORT_LIT_DESCRIPTION = SORT_DESCRIPTION
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_LIT_EXAMPLE = "id desc";

    //STATISTICS
    public static final String RELEASE_NAME_STATS_DESCRIPTION = "UniProt release name";
    public static final String TYPE_STATS_DESCRIPTION = "Statistic type";
    public static final String CATEGORY_STATS_DESCRIPTION = "List of statistics categories, separated by commas.";
    public static final String RELEASE_NAME_STATS_EXAMPLE = "2023_05";
    public static final String TYPE_STATS_EXAMPLE = "reviewed";
    public static final String CATEGORY_STATS_EXAMPLE = "TOTAL_ORGANISM,COMMENTS";

    //SUBCEL
    public static final String ID_SUBCEL_OPERATION = "Get subcellular location entry by a single accession.";
    public static final String SEARCH_SUBCEL_OPERATION = "Retrieve subcellular location entries by a search query.";
    public static final String STREAM_SUBCEL_OPERATION = "Download subcellular location entries retrieved by a search query.";
    public static final String ID_SUBCEL_DESCRIPTION = "Unique identifier for the subcellular location entry";
    public static final String ID_SUBCEL_EXAMPLE = "SL-0039";
    public static final String FIELDS_SUBCEL_DESCRIPTION = FIELDS_DESCRIPTION
            + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_SUBCEL_EXAMPLE = "id,name,category";
    public static final String QUERY_SUBCEL_DESCRIPTION = "Criteria to search subcellular locations. "
            + QUERY_ADVANCED
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";
    public static final String QUERY_SUBCEL_EXAMPLE = "\"Cell membrane\"";
    public static final String SORT_SUBCEL_DESCRIPTION = SORT_DESCRIPTION
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_SUBCEL_EXAMPLE = "id asc";

    //SUGGEST
    public static final String SUGGESTER_OPERATION = "Provide suggestions (auto-complete) for a subset of datasets (dictionaries).";
    public static final String DICT_SUGGESTER_DESCRIPTION = "Suggest data dictionary.";
    public static final String DICT_SUGGESTER_EXAMPLE = "ORGANISM";
    public static final String QUERY_SUGGESTER_DESCRIPTION = "Text to look up for auto-complete.";
    public static final String QUERY_SUGGESTER_EXAMPLE = "huma";

    //TAXONOMY
    public static final String ID_TAX_OPERATION = "Get taxonomy entry by a single accession.";
    public static final String IDS_TAX_OPERATION = "Get taxonomy entries by a list of taxon ids. (Max. 1K entries)";
    public static final String SEARCH_TAX_OPERATION = "Retrieve taxonomy entries by a search query.";
    public static final String STREAM_TAX_OPERATION = "Download taxonomy entries retrieved by a search query.";
    public static final String ID_TAX_DESCRIPTION = "Unique identifier for the taxonomy entry";
    public static final String ID_TAX_EXAMPLE = "9606";
    public static final String FIELDS_TAX_DESCRIPTION = FIELDS_DESCRIPTION
            + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields'>List of valid fields</a>";
    public static final String FIELDS_TAX_EXAMPLE = "id,common_name,scientific_name,lineage";
    public static final String IDS_TAX_DESCRIPTION = "Comma separated list of taxonIds";
    public static final String IDS_TAX_EXAMPLE = "9606,10116,9913";
    public static final String FACET_FILTER_TAX_DESCRIPTION = "Criteria to filter taxonomy. <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";
    public static final String FACET_FILTER_TAX_EXAMPLE = "superkingdom:Eukaryota";
    public static final String QUERY_TAX_DESCRIPTION = "Criteria to search taxonomy. "
            + QUERY_ADVANCED
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields'>List of valid search fields</a>";
    public static final String QUERY_TAX_EXAMPLE = "\"Homo sapiens\"";
    public static final String SORT_TAX_DESCRIPTION = SORT_DESCRIPTION
            + " <a href='https://rest.uniprot.org/configure/uniprotkb/sort'>List of valid sort fields</a>";
    public static final String SORT_TAX_EXAMPLE = "scientific desc";
    public static final String IDS_SIZE_TAX_DESCRIPTION = "Pagination size. Defaults to number of taxonIds passed (Single page).";

}
