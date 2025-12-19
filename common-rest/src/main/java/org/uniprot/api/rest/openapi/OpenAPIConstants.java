package org.uniprot.api.rest.openapi;

public class OpenAPIConstants {

    public static final String QUERY_ADVANCED =
            "Advanced queries can be built with parentheses and conditionals such as AND, OR and NOT.";
    public static final String QUERY_DESCRIPTION = "Criteria to search. " + QUERY_ADVANCED;
    public static final String FIELDS_DESCRIPTION =
            "List of entry sections (fields) to be returned, separated by commas.";
    public static final String FIELDS_OPERATION_DESC =
            "Specify <tt>fields</tt> to return only data for specific sections of that entry that are of interest to you";
    public static final String STREAM_OPERATION_DESC =
            "The stream endpoint uses a request query to return all entries associated with the search term in a single download. "
                    + FIELDS_OPERATION_DESC;
    public static final String SORT_DESCRIPTION = "Specify field by which to sort results.";
    public static final String INCLUDE_ISOFORM_DESCRIPTION =
            "Specify <tt>true</tt> to include isoforms, default is <tt>false</tt>.";

    public static final String SIZE_DESCRIPTION =
            "Specify the number of entries per page of results (Pagination size). Default is 25, max is 500";
    public static final String SIZE_EXAMPLE = "50";
    public static final String DOWNLOAD_DESCRIPTION =
            "Specify <tt>true</tt> to download as file, default is <tt>false</tt>.";
    public static final String JOB_ID_DESCRIPTION =
            "The <tt>jobId</tt> returned from the <tt>run</tt> submission.";

    // UniProtKB

    public static final String TAG_UNIPROTKB = "UniProtKB";
    public static final String TAG_UNIPROTKB_DESC =
            "The UniProt Knowledgebase (UniProtKB) acts as the global hub of accurate, consistent and expertly curated information on protein sequence and function. Each UniProtKB entry is described by a stable protein identifier (accession ID) and contains core data consisting of the amino acid sequence, protein name or description, taxonomic information and links to relevant scientific publications. Further annotation is added when available, such as protein function, subcellular location and the position of protein features such as active sites, domains and post-translational modifications. Where possible these annotations are described using established biological ontologies, classifications and cross-references. A clear indication of the quality of annotation in the form of evidence attribution of experimental and computational data is added to each piece of data.";

    public static final String TAG_UNIPROTKB_JOB = "UniProtKB async download";
    public static final String TAG_UNIPROTKB_JOB_DESC =
            "UniProtKB asynchronous download jobs are different from synchronous downloads offered via stream API. "
                    + "First, a job must be submitted for download. "
                    + "Much like <tt>ID Mapping</tt> services at UniProt, this job submission request can be submitted via the <tt>run</tt> post request, "
                    + "which will return a job id. This id can be used to monitor the progress of the job via the <tt>status</tt> endpoint. "
                    + "When the submitted job is completed, the <tt>status</tt> endpoint will redirect to the downloadable zip file.";

    public static final String SEARCH_UNIPROTKB_OPERATION =
            "Retrieve UniProtKB entries by a search query.";
    public static final String SEARCH_OPERATION_DESC =
            "The search endpoint uses a request query to return all entries associated with the search term in a paginated list of entries. Use ‘size’ to specify the number of entries per page of results. "
                    + FIELDS_OPERATION_DESC;
    public static final String ID_UNIPROTKB_OPERATION =
            "Get UniProtKB entry by a single accession.";
    public static final String ID_UNIPROTKB_OPERATION_DESC =
            "Search UniProtKB by protein entry accession to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String STREAM_UNIPROTKB_OPERATION =
            "Download UniProtKB entries retrieved by a search query. (Max. 10 million entries)";
    public static final String STREAM_UNIPROTKB_OPERATION_DESC =
            STREAM_OPERATION_DESC
                    + " The stream endpoint has a maximum limit of 10 million entries. For larger requests, please use the 'UniProtKB asynchronous download job' requests described below. The 'UniProtKB asynchronous download job' requests can be used for any size -- the asynchronous download jobs can be paused and resumed at your convenience, unlike the stream endpoint.";
    public static final String IDS_UNIPROTKB_OPERATION =
            "Get UniProtKB entries by a list of accessions. (Max. 1K entries)";
    public static final String IDS_UNIPROTKB_OPERATION_DESC = "";
    public static final String JOB_RUN_UNIPROTKB_OPERATION =
            "Submit UniProtKB asynchronous download job.";
    public static final String JOB_RUN_UNIPROTKB_OPERATION_DESC = "";
    public static final String JOB_STATUS_UNIPROTKB_OPERATION =
            "Get progress of UniProtKB asynchronous download job.";
    public static final String JOB_STATUS_UNIPROTKB_OPERATION_DESC = "";
    public static final String JOB_DETAILS_UNIPROTKB_OPERATION =
            "Get details of UniProtKB asynchronous download job.";
    public static final String JOB_DETAILS_UNIPROTKB_OPERATION_DESC = "";
    public static final String PUBLICATION_UNIPROTKB_OPERATION =
            "Get publications for a UniProtKB entry by accession.";
    public static final String PUBLICATION_UNIPROTKB_OPERATION_DESC =
            "Get all publication data for a UniProtKB entry by accession, including computationally-mapped and community-mapped sources.";
    public static final String INTERACTION_UNIPROTKB_OPERATION =
            "Get interactions for a UniProtKB entry by accession.";
    public static final String INTERACTION_UNIPROTKB_OPERATION_DESC = "";
    public static final String TAG_UNIPROTKB_GROUP = "UniProtKB group by";
    public static final String TAG_UNIPROTKB_GROUP_DESC =
            "Allows you to browse your query results using the taxonomy, keyword, Gene Ontology or Enzyme Classification hierarchies and to view the distribution of your search results across the terms within each group.";
    public static final String QUERY_UNIPROTKB_ID_DESCRIPTION =
            "Criteria to search within the accessions. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_UNIPROTKB_SEARCH_DESCRIPTION =
            "Criteria to search UniProtKB. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/uniprotkb/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
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
                    + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_UNIPROTKB_EXAMPLE =
            "accession,protein_name,cc_function,ft_binding";
    public static final String FACETS_PUBLICATION =
            "List of publication facets to be applied, separated by commas. <a href='https://rest.uniprot.org/configure/uniprotkb/publication/facets' target='_blank' rel='noopener noreferrer'>List of valid facets</a>";
    public static final String FACET_FILTER_PUBLICATION =
            "Criteria to filter publications. <a href='https://rest.uniprot.org/configure/uniprotkb/publication/facets' target='_blank' rel='noopener noreferrer'>List of valid facets</a>";
    public static final String ACCESSIONS_UNIPROTKB_DESCRIPTION =
            "List of UniProtKB accessions, separated by commas. (Max 1K accessions)";
    public static final String ACCESSIONS_UNIPROTKB_EXAMPLE = "P21802,P05067,A2VEY9";
    public static final String SIZE_UNIPROTKB_ID_DESCRIPTION =
            "Pagination size. Defaults to number of accessions passed (Single page).";
    public static final String SORT_UNIPROTKB_ID_DESCRIPTION =
            SORT_DESCRIPTION
                    + " Defaults to order of accessions passed. <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_UNIPROTKB_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_UNIPROTKB_EXAMPLE = "accession desc";
    public static final String ACCESSION_UNIPROTKB_DESCRIPTION =
            "Unique and stable identifier for each UniProtKB entry";
    public static final String ACCESSION_UNIPROTKB_EXAMPLE = "P05067";
    public static final String VERSION_UNIPROTKB_DESCRIPTION =
            "Version of the entry. Versions are integers 1 or above; enter <tt>last</tt> for the latest version.</br>"
                    + "Please note that when passing <tt>version</tt> file formats are restricted to <tt>fasta</tt> and <tt>txt</tt> only";
    public static final String FORMAT_UNIPROTKB_DESCRIPTION =
            "The file format for download. <a href='https://rest.uniprot.org/configure/uniprotkb/formats' target='_blank' rel='noopener noreferrer'>Valid formats are listed here</a>";
    public static final String FORMAT_UNIPROTKB_EXAMPLE = "json";
    public static final String GROUP_PARENT_DESCRIPTION =
            "If nothing is passed, the root of the hierarchical tree is returned with all available children. If the parent is specified, only children below the parent node is returned.";
    public static final String GROUP_EC_OPERATION =
            "List of Enzyme Classification (EC) groups with respect to the given query and parent.";
    public static final String GROUP_EC_OPERATION_DESC = "";
    public static final String GROUP_GO_OPERATION =
            "List of Gene Ontology (GO) groups with respect to the given query and parent";
    public static final String GROUP_GO_OPERATION_DESC = "";
    public static final String GROUP_KEYWORD_OPERATION =
            "List of keyword groups with respect to the given query and parent";
    public static final String GROUP_KEYWORD_OPERATION_DESC = "";
    public static final String GROUP_TAXONOMY_OPERATION =
            "List of taxonomy groups with respect to the given query and parent";
    public static final String GROUP_TAXONOMY_OPERATION_DESC = "";

    // ID Mapping
    public static final String JOB_ID_IDMAPPING_DESCRIPTION =
            "Unique identifier for ID Mapping job";
    public static final String TAG_IDMAPPING_DOWNLOAD_JOB = "ID Mapping async download";
    public static final String TAG_IDMAPPING_DOWNLOAD_JOB_DESC =
            "ID Mapping asynchronous download jobs are different from synchronous downloads offered via stream API. "
                    + "First, a job must be submitted for download. "
                    + "Much like <tt>ID Mapping</tt> services at UniProt, this job submission request can be submitted via the <tt>run</tt> post request, "
                    + "which will return a job id. This id can be used to monitor the progress of the job via the <tt>status</tt> endpoint. "
                    + "When the submitted job is completed, the <tt>status</tt> endpoint will redirect to the downloadable zip file.";
    public static final String TAG_IDMAPPING_RESULT = "ID Mapping results";
    public static final String TAG_IDMAPPING_RESULT_DESC =
            "API calls to retrieve the results of a submitted ID Mapping job.";
    public static final String ID_MAPPING_RESULT_OPERATION = "Search result by a submitted job id.";
    public static final String ID_MAPPING_GROUP_BY_TAXONOMY_RESULT_OPERATION =
            "Get group by taxonomy results for a submitted job id.";
    public static final String ID_MAPPING_GROUP_BY_KEYWORD_RESULT_OPERATION =
            "Get group by keyword results for a submitted job id.";
    public static final String ID_MAPPING_GROUP_BY_EC_RESULT_OPERATION =
            "Get group by enzyme class  results for a submitted job id.";
    public static final String ID_MAPPING_GROUP_BY_GO_RESULT_OPERATION =
            "Get group by gene ontology results for a submitted job id.";
    public static final String ID_MAPPING_STREAM_OPERATION = "Stream result by a submitted job id.";
    public static final String RUN_IDMAPPING_DOWNLOAD_JOB_OPERATION =
            "Submit ID Mapping asynchronous download job.";
    public static final String RUN_IDMAPPING_DOWNLOAD_JOB_OPERATION_DESC = "";
    public static final String STATUS_IDMAPPING_DOWNLOAD_JOB_OPERATION =
            "Get progress of ID Mapping asynchronous download job.";
    public static final String STATUS_IDMAPPING_DOWNLOAD_JOB_OPERATION_DESC = "";
    public static final String DETAILS_IDMAPPING_DOWNLOAD_JOB_OPERATION =
            "Get details of ID Mapping asynchronous download job";
    public static final String DETAILS_IDMAPPING_DOWNLOAD_JOB_OPERATION_DESC = "";
    public static final String TAG_IDMAPPING = "ID Mapping job";
    public static final String TAG_IDMAPPING_DESC =
            "The ID Mapping service can map between the identifiers used in one database, to the identifiers of another, e.g., from UniProt to Ensembl, or to PomBase, etc. If you map to UniProtKB, UniParc or UniRef data, the full entries will be returned to you for convenience.";
    public static final String RUN_IDMAPPING_OPERATION = "Submit ID Mapping job.";
    public static final String RUN_IDMAPPING_OPERATION_DESC = "";
    public static final String STATUS_IDMAPPING_OPERATION = "Get status of ID Mapping job.";
    public static final String STATUS_IDMAPPING_OPERATION_DESC = "";
    public static final String DETAILS_IDMAPPING_OPERATION = "Get details of ID Mapping job.";
    public static final String DETAILS_IDMAPPING_OPERATION_DESC = "";
    public static final String SUB_SEQUENCE_DESCRIPTION =
            "Flag to write subsequences. Only accepted in fasta format";
    public static final String FROM_IDMAPPING_JOB_DESCRIPTION = "Name of the from type";
    public static final String TO_IDMAPPING_JOB_DESCRIPTION = "Name of the to type";
    public static final String IDS_IDMAPPING_JOB_DESCRIPTION = "Comma separated list of ids";
    public static final String TAX_ID_IDMAPPING_JOB_DESCRIPTION = "Value of the taxon Id";
    public static final String FROM_IDMAPPING_JOB_EXAMPLE = "UniProtKB_AC-ID";
    public static final String TO_IDMAPPING_JOB_EXAMPLE = "UniProtKB";
    public static final String IDS_IDMAPPING_JOB_EXAMPLE = "P21802,P12345,P05067";
    public static final String TAX_ID_IDMAPPING_JOB_EXAMPLE = "9606";
    public static final String IDMAPPING_UNIREF_RESULT_SEARCH_OPERATION =
            "Search result of UniRef cluster (or clusters) by a submitted job id.";
    public static final String IDMAPPING_UNIREF_RESULT_STREAM_OPERATION =
            "Stream an UniRef cluster (or clusters) retrieved by a submitted job id.";
    public static final String IDMAPPING_UNIPARC_RESULT_SEARCH_OPERATION =
            "Search result of UniParc sequence entry (or entries) by a submitted job id.";
    public static final String IDMAPPING_UNIPARC_RESULT_STREAM_OPERATION =
            "Stream a UniParc sequence entry (or entries) by a submitted job id.";
    public static final String IDMAPPING_UNIPROTKB_RESULT_SEARCH_OPERATION =
            "Search result for a UniProtKB protein entry (or entries) mapped by a submitted job id.";
    public static final String IDMAPPING_UNIPROTKB_RESULT_STREAM_OPERATION =
            "Download UniProtKB protein entry (or entries) mapped by a submitted job id.";

    // Support date cross reference
    public static final String TAG_CROSSREF = "Cross-referenced databases";
    public static final String TAG_CROSSREF_DESC =
            "The cross-references section of UniProtKB entries "
                    + "displays explicit and implicit links to databases such as nucleotide sequence databases, "
                    + "model organism databases and genomics and proteomics resources. A single entry can have "
                    + "cross-references to several dozen different databases and have several hundred individual links. "
                    + "The databases are categorized for easy user perusal and understanding of how the "
                    + "different databases relate to both UniProtKB and to each other";
    public static final String QUERY_CROSSREF_DESCRIPTION =
            "Criteria to search cross-reference databases. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/database/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_CROSSREF_EXAMPLE = "Ensembl";
    public static final String SORT_CROSSREF_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/database/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_CROSSREF_EXAMPLE = "id desc";
    public static final String FIELDS_CROSSREF_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/uniprotkb/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_CROSSREF_EXAMPLE = "id,name,abbrev,category";
    public static final String ID_CROSSREF_DESCRIPTION =
            "Unique identifier for the cross-reference database entry";
    public static final String ID_CROSSREF_EXAMPLE = "DB-0244";
    public static final String ID_CROSSREF_OPERATION =
            "Get cross-reference database entry by a single accession.";
    public static final String ID_CROSSREF_OPERATION_DESC =
            "Search cross-reference database entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_CROSSREF_OPERATION =
            "Retrieve cross-reference database entries by a search query.";
    public static final String STREAM_CROSSREF_OPERATION =
            "Download cross-reference database entries retrieved by a search query.";

    // GeneCentric
    public static final String TAG_GENECENTRIC = "GeneCentric";
    public static final String TAG_GENECENTRIC_DESC =
            "GeneCentric services of a proteome, where the set of genes and their products are grouped under a single canonical gene identifier";
    public static final String SEARCH_GENECENTRIC_OPERATION =
            "Retrieve GeneCentric entries by a search query.";
    public static final String STREAM_GENECENTRIC_OPERATION =
            "Download GeneCentric entries retrieved by a search query.";
    public static final String UPID_GENECENTRIC_OPERATION =
            "Retrieve GeneCentric entries of a single Proteome ID.";
    public static final String UPID_GENECENTRIC_OPERATION_DESC =
            "Search GeneCentric entry by Proteome ID to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String ID_GENECENTRIC_OPERATION =
            "Retrieve a GeneCentric entry by a single UniProtKB accession.";
    public static final String ID_GENECENTRIC_OPERATION_DESC =
            "Search GeneCentric entry by protein accession to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String QUERY_GENECENTRIC_DESCRIPTION =
            "Criteria to search GeneCentric. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/genecentric/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_GENECENTRIC_EXAMPLE = "gene:APP";
    public static final String SORT_GENECENTRIC_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/genecentric/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_GENECENTRIC_EXAMPLE = "organism_name asc";
    public static final String FIELDS_GENECENTRIC_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/genecentric/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_GENECENTRIC_EXAMPLE = "accession,gene_name,proteome_id";

    // Proteome
    public static final String TAG_PROTEOME = "Proteomes";
    public static final String TAG_PROTEOME_DESC =
            "The proteomes service offers access to UniProtKB proteomes, allowing users to search for proteomes (including reference or redundant proteomes) using UniProt proteome identifiers, species names, or taxonomy identifiers";
    public static final String SEARCH_PROTEOME_OPERATION =
            "Retrieve proteome entries by a search query.";
    public static final String ID_PROTEOME_OPERATION = "Get proteome entry by a single upid.";
    public static final String ID_PROTEOME_OPERATION_DESC =
            "Search Proteome entry by Proteome ID(upid) to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String STREAM_PROTEOME_OPERATION =
            "Download proteome entries retrieved by a search query.";
    public static final String UPID_PROTEOME_DESCRIPTION =
            "Unique identifier for the proteome entry";
    public static final String UPID_PROTEOME_EXAMPLE = "UP000005640";

    public static final String QUERY_PROTEOME_DESCRIPTION =
            "Criteria to search proteomes. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/proteomes/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_PROTEOME_EXAMPLE = "eukaryota";
    public static final String SORT_PROTEOME_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/proteomes/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_PROTEOME_EXAMPLE = "organism_name asc";
    public static final String FIELDS_PROTEOME_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/proteomes/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_PROTEOME_EXAMPLE = "upid,organism,organism_id";

    // Support date Keywords
    public static final String TAG_KEYWORDS = "Keywords";
    public static final String TAG_KEYWORDS_DESC =
            "UniProtKB Keywords constitute a controlled vocabulary with a hierarchical structure. Keywords summarise the content of a UniProtKB entry and facilitate the search for proteins of interest. An entry often contains several keywords. Keywords can be used to retrieve subsets of protein entries. Keywords are classified in 10 categories: Biological process, Cellular component, Coding sequence diversity, Developmental stage, DiseaseEntry, Domain, Ligand, Molecular function, Post-translational modification, Technical term.";
    public static final String ID_KEYWORDS_OPERATION = "Get keyword entry by a single accession.";
    public static final String ID_KEYWORDS_OPERATION_DESC =
            "Search keyword entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_KEYWORDS_OPERATION =
            "Retrieve keyword entries by a search query.";
    public static final String STREAM_KEYWORDS_OPERATION =
            "Download keyword entries retrieved by a search query.";
    public static final String ID_KEYWORDS_DESCRIPTION = "Unique identifier for the keyword entry";
    public static final String ID_KEYWORDS_EXAMPLE = "KW-0020";
    public static final String FIELDS_KEYWORDS_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/keywords/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_KEYWORDS_EXAMPLE = "id,name,category";
    public static final String SORT_KEYWORDS_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/keywords/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_KEYWORDS_EXAMPLE = "name asc";
    public static final String QUERY_KEYWORDS_DESCRIPTION =
            "Criteria to search keywords. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/keywords/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_KEYWORDS_EXAMPLE = "Phosphoprotein";

    // Support date diseases
    public static final String TAG_DISEASE = "Human diseases";
    public static final String TAG_DISEASE_DESC =
            "The human diseases in which proteins are involved are "
                    + "described in UniProtKB entries with a controlled vocabulary.";
    public static final String ID_DISEASE_OPERATION =
            "Get human disease entry by a single accession.";
    public static final String ID_DISEASE_OPERATION_DESC =
            "Search human disease entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;

    public static final String SEARCH_DISEASE_OPERATION =
            "Retrieve human disease entries by a search query.";
    public static final String STREAM_DISEASE_OPERATION =
            "Download human disease entries retrieved by a search query.";
    public static final String QUERY_DISEASE_DESCRIPTION =
            "Criteria to search human diseases. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/diseases/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_DISEASE_EXAMPLE = "Alzheimer";
    public static final String SORT_DISEASE_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/diseases/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_DISEASE_EXAMPLE = "id asc";
    public static final String ID_DISEASE_DESCRIPTION =
            "Unique identifier for the human disease entry";
    public static final String ID_DISEASE_EXAMPLE = "DI-04530";
    public static final String FIELDS_DISEASE_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/diseases/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_DISEASE_EXAMPLE = "id,name";

    // support- data literature citations
    public static final String TAG_LITERATURE = "Literature citations";
    public static final String TAG_LITERATURE_DESC =
            "Search publications that are linked to UniProtKB. Publications can be manually curated, computationally mapped, or community curated.";

    public static final String ID_LIT_OPERATION =
            "Get literature citation entry by a single accession.";
    public static final String ID_LIT_OPERATION_DESC =
            "Search literature citation entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String ID_LIT_DESCRIPTION =
            "Unique identifier for the literature citation entry";
    public static final String ID_LIT_EXAMPLE = "10024047";
    public static final String FIELDS_LIT_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/citations/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_LIT_EXAMPLE = "id,title,authors,publication_date";
    public static final String SEARCH_LIT_OPERATION =
            "Retrieve literature citation entries by a search query.";
    public static final String STREAM_LIT_OPERATION =
            "Download literature citation entries retrieved by a search query.";
    public static final String QUERY_LIT_DESCRIPTION =
            "Criteria to search literature citations. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/citations/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_LIT_EXAMPLE = "\"genome analysis\"";
    public static final String SORT_LIT_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/citations/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_LIT_EXAMPLE = "id desc";

    // STATISTICS
    public static final String TAG_RELEASE_STAT = "Release statistics";
    public static final String TAG_RELEASE_STAT_DESC = "UniProtKB release statistics";
    public static final String RELEASE_NAME_STATS_DESCRIPTION = "UniProt release name";
    public static final String TYPE_STATS_DESCRIPTION = "Statistic type";
    public static final String CATEGORY_STATS_DESCRIPTION =
            "List of statistics categories, separated by commas.";
    public static final String RELEASE_NAME_STATS_EXAMPLE = "2023_05";
    public static final String TYPE_STATS_EXAMPLE = "reviewed";
    public static final String CATEGORY_STATS_EXAMPLE = "TOTAL_ORGANISM,COMMENTS";
    public static final String
            GET_RELEASE_STATISTICS_BY_RELEASE_NAME_AND_STATISTICS_TYPE_OPERATION =
                    "Get release statistics by UniProt release name and statistics type.";
    public static final String
            GET_RELEASE_STATISTICS_BY_RELEASE_NAME_AND_STATISTICS_TYPE_OPERATION_DESC = "";
    public static final String GET_RELEASE_STATISTICS_BY_RELEASE_NAME_OPERATION =
            "Get release statistics by UniProt release name.";
    public static final String GET_RELEASE_STATISTICS_BY_RELEASE_NAME_OPERATION_DESC = "";
    public static final String GET_HISTORY_BY_ATTRIBUTE_AND_STATISTICS_TYPE_OPERATION =
            "Get history by attribute and statistics type.";
    public static final String GET_HISTORY_BY_ATTRIBUTE_AND_STATISTICS_TYPE_OPERATION_DESC = "";

    // SUBCEL
    public static final String TAG_SUBCEL = "Subcellular locations";
    public static final String TAG_SUBCEL_DESC =
            "The subcellular locations in which a protein is found are described in UniProtKB entries with a controlled vocabulary, which includes also membrane topology and orientation terms.";
    public static final String ID_SUBCEL_OPERATION =
            "Get subcellular location entry by a single accession.";
    public static final String ID_SUBCEL_OPERATION_DESC =
            "Search subcellular location entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_SUBCEL_OPERATION =
            "Retrieve subcellular location entries by a search query.";
    public static final String STREAM_SUBCEL_OPERATION =
            "Download subcellular location entries retrieved by a search query.";
    public static final String ID_SUBCEL_DESCRIPTION =
            "Unique identifier for the subcellular location entry";
    public static final String ID_SUBCEL_EXAMPLE = "SL-0039";
    public static final String FIELDS_SUBCEL_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/locations/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_SUBCEL_EXAMPLE = "id,name,category";
    public static final String QUERY_SUBCEL_DESCRIPTION =
            "Criteria to search subcellular locations. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/locations/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_SUBCEL_EXAMPLE = "\"Cell membrane\"";
    public static final String SORT_SUBCEL_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/locations/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_SUBCEL_EXAMPLE = "id asc";

    // SUGGEST
    public static final String TAG_SUGGESTER = "Suggester";
    public static final String TAG_SUGGESTER_DESC =
            "This service provides configuration data used in UniProt website for suggester (auto-complete) data";

    public static final String SUGGESTER_OPERATION =
            "Provide suggestions (auto-complete) for a subset of datasets (dictionaries).";
    public static final String SUGGESTER_OPERATION_DESC = "";
    public static final String DICT_SUGGESTER_DESCRIPTION = "Suggest data dictionary.";
    public static final String DICT_SUGGESTER_EXAMPLE = "ORGANISM";
    public static final String QUERY_SUGGESTER_DESCRIPTION = "Text to look up for auto-complete.";
    public static final String QUERY_SUGGESTER_EXAMPLE = "huma";

    // TAXONOMY
    public static final String TAG_TAXONOMY = "Taxonomy";
    public static final String TAG_TAXONOMY_DESC =
            "UniProtKB taxonomy data is manually curated: next to manually verified organism names, we provide a selection of external links, organism strains and viral host information.";
    public static final String ID_TAX_OPERATION = "Get taxonomy entry by a single taxon id.";
    public static final String ID_TAX_OPERATION_DESC =
            "Search taxonomy entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String IDS_TAX_OPERATION =
            "Get taxonomy entries by a list of taxon ids. (Max. 1K entries)";
    public static final String IDS_TAX_OPERATION_DESC = "";
    public static final String SEARCH_TAX_OPERATION =
            "Retrieve taxonomy entries by a search query.";
    public static final String STREAM_TAX_OPERATION =
            "Download taxonomy entries retrieved by a search query.";
    public static final String ID_TAX_DESCRIPTION = "Unique identifier for the taxonomy entry";
    public static final String ID_TAX_EXAMPLE = "9606";
    public static final String FIELDS_TAX_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/taxonomy/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_TAX_EXAMPLE = "id,common_name,scientific_name,lineage";
    public static final String IDS_TAX_DESCRIPTION = "Comma separated list of taxon ids";
    public static final String IDS_TAX_EXAMPLE = "9606,10116,9913";
    public static final String FACET_FILTER_TAX_DESCRIPTION =
            "Criteria to filter taxonomy. <a href='https://rest.uniprot.org/configure/taxonomy/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String FACET_FILTER_TAX_EXAMPLE = "superkingdom:Eukaryota";
    public static final String QUERY_TAX_DESCRIPTION =
            "Criteria to search taxonomy. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/taxonomy/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_TAX_EXAMPLE = "\"Homo sapiens\"";
    public static final String SORT_TAX_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/taxonomy/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_TAX_EXAMPLE = "scientific desc";
    public static final String IDS_SIZE_TAX_DESCRIPTION =
            "Pagination size. Defaults to number of taxonIds passed (Single page).";

    // CONFIGURATION
    public static final String TAG_CONFIG = "Configuration";
    public static final String TAG_CONFIG_DESC =
            "These services provide configuration data used in the UniProt website";
    public static final String CONFIG_UNIRULE_FIELDS_OPERATION =
            "List of return fields available in the UniRule services.";
    public static final String CONFIG_UNIRULE_SEARCH_OPERATION =
            "List of search fields available in the UniRule services.";
    public static final String CONFIG_ARBA_SEARCH_OPERATION =
            "List of return fields available in the ARBA services.";
    public static final String CONFIG_ARBA_FIELDS_OPERATION =
            "List of search fields available in the ARBA services.";
    public static final String CONFIG_CROSSREF_FIELDS_OPERATION =
            "List of return fields available in the database services.";
    public static final String CONFIG_CROSSREF_SEARCH_OPERATION =
            "List of search fields available in the database services.";
    public static final String CONFIG_DISEASE_SEARCH_OPERATION =
            "List of search fields available in the disease services.";
    public static final String CONFIG_DISEASE_FIELDS_OPERATION =
            "List of return fields available in the disease services.";
    public static final String CONFIG_IDMAPPING_FIELDS_OPERATION =
            "List of fields available to use in ID Mapping from and to dropdown lists.";
    public static final String CONFIG_KEYWORD_FIELDS_OPERATION =
            "List of return fields available in the keyword services.";
    public static final String CONFIG_KEYWORD_SEARCH_OPERATION =
            "List of search fields available in the keyword services.";
    public static final String CONFIG_PROTEOME_FIELDS_OPERATION =
            "List of return fields available in the proteomes services.";
    public static final String CONFIG_PROTEOME_SEARCH_OPERATION =
            "List of search fields available in the proteomes services.";
    public static final String CONFIG_GENECENTRIC_FIELDS_OPERATION =
            "List of return fields available in the gene centric services.";
    public static final String CONFIG_GENECENTRIC_SEARCH_OPERATION =
            "List of search fields available in the gene centric services.";
    public static final String CONFIG_SUBCEL_FIELDS_OPERATION =
            "List of return fields available in the locations services.";
    public static final String CONFIG_SUBCEL_SEARCH_OPERATION =
            "List of search fields available in the locations services.";
    public static final String CONFIG_TAXONOMY_FIELDS_OPERATION =
            "List of return fields available in the taxonomy services.";
    public static final String CONFIG_TAXONOMY_SEARCH_OPERATION =
            "List of search fields available in taxonomy services.";
    public static final String CONFIG_UNIPARC_FIELDS_OPERATION =
            "List of return fields available in the UniParc services.";
    public static final String CONFIG_UNIPARC_SEARCH_OPERATION =
            "List of search fields available in the UniParc services.";
    public static final String CONFIG_UNIPARC_DATABASE_OPERATION =
            "List of database details available for UniParc entry page.";
    public static final String CONFIG_UNIPARC_ENTRY_FIELDS_OPERATION =
            "List of return fields available in a UniParc entry.";
    public static final String CONFIG_UNIREF_FIELDS_OPERATION =
            "List of return fields available in the UniRef services.";
    public static final String CONFIG_UNIREF_SEARCH_OPERATION =
            "List of search fields available in the UniRef services.";
    public static final String CONFIG_UNIPROTKB_SEARCH_OPERATION =
            "List of search fields available to use in UniProtKB query.";
    public static final String CONFIG_UNIPROTKB_ANNOTATION_OPERATION =
            "List of annotation evidences available in the UniProtKB services.";
    public static final String CONFIG_UNIPROTKB_GO_OPERATION =
            "List of GO annotation evidences available in the UniProtKB services.";
    public static final String CONFIG_UNIPROTKB_DATABASE_OPERATION =
            "List of databases available in the UniProtKB services.";
    public static final String CONFIG_UNIPROTKB_FIELDS_OPERATION =
            "List of return fields available in the UniProtKB services.";
    public static final String CONFIG_UNIPROTKB_ALL_DATABASE_OPERATION =
            "List of database details available in the UniProtKB services.";
    public static final String CONFIG_UNIPROTKB_EVID_DATABASE_OPERATION =
            "List of evidence database details available in the UniProtKB services.";
    public static final String CONFIG_UTIL_QUERY_PARSER_OPERATION =
            "Utility service that parse a query string into a Structured response object.";
    public static final String CONFIG_UTIL_QUERY_DESCRIPTION = "Query string to be parsed.";
    public static final String CONFIG_UTIL_QUERY_EXAMPLE = " (gene:cdc7) AND (organism_id:9606)";
    // UniParc
    public static final String FORMAT_UNIPARC_DESCRIPTION =
            "The file format for download. <a href='https://rest.uniprot.org/configure/uniparc/formats' target='_blank' rel='noopener noreferrer'>Valid formats are listed here</a>";
    public static final String FORMAT_UNIPARC_EXAMPLE = "json";
    public static final String TAG_UNIPARC_JOB = "UniParc async download";
    public static final String TAG_UNIPARC_JOB_DESC =
            "UniParc asynchronous download jobs are different from synchronous downloads offered via stream API. "
                    + "First, a job must be submitted for download. "
                    + "Much like <tt>ID Mapping</tt> services at UniProt, this job submission request can be submitted via the <tt>run</tt> post request, "
                    + "which will return a job id. This id can be used to monitor the progress of the job via the <tt>status</tt> endpoint. "
                    + "When the submitted job is completed, the <tt>status</tt> endpoint will redirect to the downloadable zip file.";
    public static final String JOB_RUN_UNIPARC_OPERATION =
            "Submit UniParc asynchronous download job.";
    public static final String JOB_RUN_UNIPARC_OPERATION_DESC = "";
    public static final String JOB_STATUS_UNIPARC_OPERATION =
            "Get progress of UniParc asynchronous download job.";
    public static final String JOB_STATUS_UNIPARC_OPERATION_DESC = "";
    public static final String JOB_ID_UNIPARC_DESCRIPTION = JOB_ID_DESCRIPTION;
    public static final String JOB_DETAILS_UNIPARC_OPERATION =
            "Get details of UniParc asynchronous download job.";
    public static final String JOB_DETAILS_UNIPARC_OPERATION_DESC = "";
    public static final String TAG_UNIPARC = "UniParc";
    public static final String TAG_UNIPARC_DESC =
            "UniParc is a comprehensive and non-redundant database that contains most of the publicly available protein sequences in the world. Proteins may exist in different source databases and in multiple copies in the same database. UniParc avoids such redundancy by storing each unique sequence only once and giving it a stable and unique identifier (UPI).";
    public static final String SEARCH_UNIPARC_OPERATION =
            "Retrieve UniParc entries by a search query.";
    public static final String ID_UNIPARC_OPERATION = "Get UniParc entry by a single upi.";

    public static final String ID_UNIPARC_LIGHT_OPERATION =
            "Get UniParc light entry by a single upi.";
    public static final String STREAM_UNIPARC_OPERATION =
            "Download UniParc entries retrieved by a search query.";
    public static final String ID_UNIPARC_OPERATION_DESC =
            "Search UniParc entry by id(upi) to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String STREAM_UNIPARC_OPERATION_DESC =
            STREAM_OPERATION_DESC
                    + " The stream endpoint has a maximum limit of 10 million entries. For larger requests, please use the 'UniParc asynchronous download job' requests described below. The 'UniParc asynchronous download job' requests can be used for any size -- the asynchronous download jobs can be paused and resumed at your convenience, unlike the stream endpoint.";
    public static final String PROTEOME_UPID_STREAM_UNIPARC_OPERATION =
            "Download UniParc entries retrieved by a proteome id.";
    public static final String PROTEOME_UPID_STREAM_UNIPARC_OPERATION_DESC =
            STREAM_OPERATION_DESC
                    + " The stream endpoint has a maximum limit of 10 million entries. For larger requests, please use the 'UniParc asynchronous download job' requests described below. The 'UniParc asynchronous download job' requests can be used for any size -- the asynchronous download jobs can be paused and resumed at your convenience, unlike the stream endpoint.";
    public static final String ACCESSION_UNIPARC_OPERATION =
            "Get UniParc entry by UniProtKB accession";
    public static final String ACCESSION_UNIPARC_OPERATION_DESC = "";
    public static final String DBID_UNIPARC_OPERATION =
            "Get UniParc entries by all UniParc cross reference accessions";
    public static final String DBID_UNIPARC_OPERATION_DESC = "";
    public static final String PROTEOME_UPID_UNIPARC_OPERATION =
            "Get UniParc entries by Proteome UPID";

    public static final String PROTEOME_UPID_FASTA_UNIPARC_OPERATION =
            "Get UniParc fasta by Proteome UPID";
    public static final String PROTEOME_UPID_UNIPARC_STREAM_OPERATION =
            "Download UniParc entries by Proteome UPID";
    public static final String CROSS_REFERENCE_FASTA_UNIPARC_OPERATION =
            "Get UniParc Cross Reference fasta by UPI and Cross Reference ID";
    public static final String PROTEOME_UPID_UNIPARC_OPERATION_DESC =
            "Search UniParc entries by proteome id(upid) to return all data associated with these entries. "
                    + FIELDS_OPERATION_DESC;
    public static final String DATABASES_UNIPARC_OPERATION =
            "Retrieve UniParc database cross-reference entries by a upi.";
    public static final String STREAM_DATABASES_UNIPARC_OPERATION =
            "Download all UniParc database cross-reference entries by a upi.";
    public static final String DATABASES_UNIPARC_OPERATION_DESC =
            "Get a page of database cross-reference entries by a upi";
    public static final String STREAM_DATABASES_UNIPARC_OPERATION_DESC =
            "Download all database cross-reference entries by a upi";
    public static final String BEST_GUESS_UNIPARC_OPERATION =
            "Best Guess returns UniParc entry with a cross-reference to the longest active UniProtKB sequence.";
    public static final String BEST_GUESS_UNIPARC_OPERATION_DESC =
            "For a given user input (request parameters), Best Guess returns the UniParcEntry with a cross-reference to the longest active UniProtKB sequence (preferably from Swiss-Prot and if not then TrEMBL). It also returns the sequence and related information. If it finds more than one longest active UniProtKB sequence it returns 400 (Bad Request) error response with the list of cross references found.";
    public static final String SEQUENCE_UNIPARC_OPERATION = "Get UniParc entry by protein sequence";
    public static final String SEQUENCE_UNIPARC_OPERATION_DESC = "";
    public static final String IDS_UNIPARC_OPERATION = "Get UniParc entries by a list of upis.";
    public static final String IDS_UNIPARC_OPERATION_DESC = "";

    public static final String IDS_POST_UNIPARC_OPERATION =
            "Get UniParc entries by a list of upis.";
    public static final String IDS_POST_UNIPARC_OPERATION_DESC = "";
    public static final String PROTEOME_UPID_UNIPARC_DESCRIPTION = UPID_PROTEOME_DESCRIPTION;
    public static final String PROTEOME_UPID_UNIPARC_EXAMPLE = UPID_PROTEOME_EXAMPLE;
    public static final String QUERY_UNIPARC_DESCRIPTION =
            "Criteria to search UniParc. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/uniparc/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_UNIPARC_EXAMPLE = "\"Homo Sapiens\"";
    public static final String SORT_UNIPARC_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/uniparc/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_UNIPARC_EXAMPLE = "upi asc";
    public static final String FIELDS_UNIPARC_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/uniparc/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String XREF_ID_UNIPARC_DESCRIPTION = "id of the database";
    public static final String XREF_ID_UNIPARC_EXAMPLE = "Q0GNZ6";
    public static final String XREF_INCLUDE_SOURCES_UNIPARC_DESCRIPTION =
            "need to retrieve sources";
    public static final String XREF_INCLUDE_SOURCES_UNIPARC_EXAMPLE = "true";
    public static final String FIELDS_UNIPARC_EXAMPLE = "upi,organism,length";
    public static final String SEQUENCE_UNIPARC_DESCRIPTION = "Protein Sequence";
    public static final String SEQUENCE_UNIPARC_EXAMPLE = "MLMPKRTKYR";
    public static final String ID_UNIPARC_DESCRIPTION =
            "Unique identifier for the UniParc id (UPI)";
    public static final String ID_UNIPARC_EXAMPLE = "UPI000002DB1C";
    public static final String IDS_UNIPARC_DESCRIPTION =
            "Comma separated list of UniParc ids (upis)";
    public static final String IDS_UNIPARC_EXAMPLE = "UPI000002DB1C,UPI000002A2F2";
    public static final String SIZE_IDS_UNIPARC_DESCRIPTION =
            "Pagination size. Defaults to number of upis passed (Single page).";
    public static final String TAXON_IDS_UNIPARC_DESCRIPTION = IDS_TAX_DESCRIPTION + ". (Max. 100)";
    public static final String TAXON_IDS_UNIPARC_EXAMPLE = IDS_TAX_EXAMPLE;
    public static final String DBTYPES_UNIPARC_DESCRIPTION =
            "Comma separated list of UniParc cross reference database names. (Max. 50)";
    public static final String DBTYPES_UNIPARC_EXAMPLE = "EnsemblBacteria,FlyBase";
    public static final String ACTIVE_UNIPARC_DESCRIPTION =
            "Flag to filter by active(true) or inactive(false) cross reference";
    public static final String DBID_UNIPARC_DESCRIPTION = "UniParc cross-referenced id";
    public static final String DBID_UNIPARC_EXAMPLE = "AAC02967,XP_006524055";
    public static final String ACCESSION_UNIPARC_DESCRIPTION = ACCESSION_UNIPROTKB_DESCRIPTION;
    public static final String ACCESSION_UNIPARC_EXAMPLE = ACCESSION_UNIPROTKB_EXAMPLE;

    // Arba
    public static final String TAG_ARBA = "ARBA";
    public static final String TAG_ARBA_DESC =
            "The Association-Rule-Based Annotator(ARBA) resource for automatic annotation in the UniProt Knowledgebase ";
    public static final String ID_ARBA_OPERATION = "Get ARBA entry by a single accession.";
    public static final String ID_ARBA_OPERATION_DESC =
            "Search ARBA entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_ARBA_OPERATION = "Retrieve ARBA entries by a search query.";
    public static final String STREAM_ARBA_OPERATION =
            "Download ARBA entries retrieved by a search query.";
    public static final String ID_ARBA_DESCRIPTION = "Get ARBA entry by an arbaId";
    public static final String FIELDS_ARBA_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/arba/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String ID_ARBA_EXAMPLE = "ARBA00000063";
    public static final String FIELDS_ARBA_EXAMPLE = "rule_id,statistics,annotation_covered";
    public static final String QUERY_ARBA_DESCRIPTION =
            "Criteria to search ARBA. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/arba/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_ARBA_EXAMPLE = "Insulin";
    public static final String SORT_ARBA_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/arba/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String SORT_ARBA_EXAMPLE = "rule_id asc";

    // UniRule
    public static final String TAG_UNIRULE = "UniRule";
    public static final String TAG_UNIRULE_DESC =
            "The unified rule(UniRule) resource for automatic annotation in the UniProt Knowledgebase ";

    public static final String ID_UNIRULE_OPERATION = "Get UniRule entry by a single accession.";
    public static final String ID_UNIRULE_OPERATION_DESC =
            "Search UniRule entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String ID_UNIRULE_DESCRIPTION = "Get UniRule entry by an accession";
    public static final String FIELDS_UNIRULE_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/unirule/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String ID_UNIRULE_EXAMPLE = "UR000000076";
    public static final String FIELDS_UNIRULE_EXAMPLE =
            "rule_id,statistics,taxonomic_scope,annotation_covered";
    public static final String SEARCH_UNIRULE_OPERATION =
            "Retrieve UniRule entries by a search query.";
    public static final String STREAM_UNIRULE_OPERATION =
            "Download UniRule entries retrieved by a search query.";

    public static final String QUERY_UNIRULE_DESCRIPTION =
            "Criteria to search UniRule. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/unirule/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String SORT_UNIRULE_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/unirule/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String QUERY_UNIRULE_EXAMPLE = "Eukaryota";
    public static final String SORT_UNIRULE_EXAMPLE = "unirule_id asc";

    // UniRef
    public static final String TAG_UNIREF = "UniRef";
    public static final String TAG_UNIREF_DESC =
            "The UniProt Reference Clusters (UniRef) provide clustered sets of sequences from the UniProt Knowledgebase (including isoforms) and selected UniParc records. This hides redundant sequences and obtains complete coverage of the sequence space at three resolutions: UniRef100, UniRef90 and UniRef50.";
    public static final String TAG_UNIREF_JOB = "UniRef async download";
    public static final String TAG_UNIREF_JOB_DESC =
            "UniRef asynchronous download jobs are different from synchronous downloads offered via stream API. "
                    + "First, a job must be submitted for download. "
                    + "Much like <tt>ID Mapping</tt> services at UniProt, this job submission request can be submitted via the <tt>run</tt> post request, "
                    + "which will return a job id. This id can be used to monitor the progress of the job via the <tt>status</tt> endpoint. "
                    + "When the submitted job is completed, the <tt>status</tt> endpoint will redirect to the downloadable zip file.";
    public static final String FORMAT_UNIREF_DESCRIPTION =
            "The file format for download. <a href='https://rest.uniprot.org/configure/uniref/formats' target='_blank' rel='noopener noreferrer'>Valid formats are listed here</a>";
    public static final String FORMAT_UNIREF_EXAMPLE = "json";
    public static final String ID_UNIREF_MEMBER_OPERATION =
            "Retrieve UniRef cluster members by a single cluster id.";
    public static final String UNIREF_MEMBER_SEARCH_BY_ID_DESC =
            "Search UniRef entry by id to return all member data associated with that entry. ";
    public static final String ID_UNIREF_MEMBER_OPERATION_DESC =
            UNIREF_MEMBER_SEARCH_BY_ID_DESC + FIELDS_OPERATION_DESC;
    public static final String STREAM_ID_UNIREF_MEMBER_OPERATION =
            "Download UniRef cluster members by a single cluster id.";
    public static final String STREAM_ID_UNIREF_MEMBER_OPERATION_DESC =
            UNIREF_MEMBER_SEARCH_BY_ID_DESC;
    public static final String ID_UNIREF_OPERATION =
            "Get UniRef cluster entry by a single cluster id.";
    public static final String ID_UNIREF_OPERATION_DESC =
            "Search UniRef entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String JOB_ID_UNIREF_DESCRIPTION = JOB_ID_DESCRIPTION;
    public static final String JOB_RUN_UNIREF_OPERATION =
            "Submit UniRef asynchronous download job.";
    public static final String JOB_RUN_UNIREF_OPERATION_DESC = "";
    public static final String JOB_STATUS_UNIREF_OPERATION =
            "Get progress of UniRef asynchronous download job.";
    public static final String JOB_STATUS_UNIREF_OPERATION_DESC = "";
    public static final String JOB_DETAILS_UNIREF_OPERATION =
            "Get details of UniRef asynchronous download job.";
    public static final String JOB_DETAILS_UNIREF_OPERATION_DESC = "";
    public static final String ID_UNIREF_LIGHT_OPERATION =
            "Get a light UniRef cluster entry by a single cluster id.";
    public static final String ID_UNIREF_LIGHT_OPERATION_DESC =
            "Search light UniRef entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_UNIREF_LIGHT_OPERATION =
            "Retrieve light UniRef cluster entries by a search query.";
    public static final String STREAM_UNIREF_LIGHT_OPERATION =
            "Download light UniRef cluster entries retrieved by a search query. (Max. 10 million entries)";
    public static final String STREAM_UNIREF_LIGHT_OPERATION_DESC =
            STREAM_OPERATION_DESC
                    + " The stream endpoint has a maximum limit of 10 million entries. For larger requests, please use the 'UniRef asynchronous download job' requests described below. The 'UniRef asynchronous download job' requests can be used for any size -- the asynchronous download jobs can be paused and resumed at your convenience, unlike the stream endpoint.";
    public static final String IDS_UNIREF_LIGHT_OPERATION =
            "Get UniRef entries by a list of cluster ids.";
    public static final String IDS_UNIREF_LIGHT_OPERATION_DESC = "";
    public static final String ID_UNIREF_DESCRIPTION = "Unique identifier for the UniRef cluster";
    public static final String IDS_POST_UNIREF_LIGHT_OPERATION =
            "Unique identifier for the UniRef cluster";
    public static final String IDS_POST_UNIREF_LIGHT_OPERATION_DESC = "";
    public static final String ID_UNIREF_EXAMPLE = "UniRef100_P05067";
    public static final String FIELDS_UNIREF_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/uniref/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String FIELDS_UNIREF_EXAMPLE = "id,name,types,organism,identity";
    public static final String QUERY_UNIREF_DESCRIPTION =
            "Criteria to search UniRef. "
                    + QUERY_ADVANCED
                    + " <a href='https://rest.uniprot.org/configure/uniref/search-fields' target='_blank' rel='noopener noreferrer'>List of valid search fields</a>";
    public static final String QUERY_UNIREF_EXAMPLE = "\"Transcription factors\" AND identity:1.0";
    public static final String SORT_UNIREF_DESCRIPTION =
            SORT_DESCRIPTION
                    + " <a href='https://rest.uniprot.org/configure/uniref/result-fields' target='_blank' rel='noopener noreferrer'>List of valid sort fields</a>";
    public static final String COMPLETE_UNIREF_DESCRIPTION =
            "Flag to include all member ids and organisms, or not. By default, it returns a maximum of 10 member ids and organisms";
    public static final String IDS_UNIREF_DESCRIPTION =
            "Comma separated list of UniRef cluster ids";
    public static final String IDS_UNIREF_EXAMPLE =
            "UniRef100_P21802,UniRef90_P05067,UniRef50_A0A007";
    public static final String SORT_UNIREF_EXAMPLE = "id asc";
    public static final String SIZE_UNIREF_ID_DESCRIPTION =
            "Pagination size. Defaults to number of ids passed. (Single page)";
    public static final String FACET_FILTER_UNIREF_DESCRIPTION =
            "Facet filter query for UniRef Cluster Members";
    public static final String FACET_FILTER_UNIREF_EXAMPLE = "member_id_type:uniprotkb_id";

    // HELP
    public static final String TAG_HELP = "help";
    public static final String TAG_HELP_DESC = "UniProt Help centre API";
    public static final String ID_HELP_OPERATION = "Get Help Centre Page by Id.";
    public static final String ID_HELP_OPERATION_DESC =
            "Search Help Centre Page entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_HELP_OPERATION = "Retrieve help pages by a search query.";
    public static final String ID_HELP_DESCRIPTION = "Help centre page id to find";
    public static final String FIELDS_HELP_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/help/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";
    public static final String TAG_RELEASE_NOTES = "release-notes";
    public static final String TAG_RELEASE_NOTES_DESC = "UniProt Release Notes API";
    public static final String ID_RELEASE_NOTES_OPERATION = "Get Release Notes by Id.";
    public static final String ID_RELEASE_NOTES_OPERATION_DESC =
            "Search Release Notes Page entry by id to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String SEARCH_RELEASE_NOTES_OPERATION =
            "Retrieve release note pages by a search query..";
    public static final String ID_RELEASE_NOTES_DESCRIPTION = "Release Notes page id to find";
    public static final String FIELDS_RELEASE_NOTES_DESCRIPTION =
            FIELDS_DESCRIPTION
                    + "  <a href='https://rest.uniprot.org/configure/release-notes/result-fields' target='_blank' rel='noopener noreferrer'>List of valid fields</a>";

    // UniSave
    public static final String TAG_UNISAVE = "UniSave";
    public static final String TAG_UNISAVE_DESC =
            "An archive of every entry version, in every UniProtKB release.";
    public static final String ID_UNISAVE_OPERATION =
            "Get entry information based on a single accession.";
    public static final String ID_UNISAVE_OPERATION_DESC =
            "Search UniSave entry by protein accession to return all data associated with that entry. "
                    + FIELDS_OPERATION_DESC;
    public static final String DIFF_UNISAVE_OPERATION =
            "Get the differences between the contents of two versions of an entry.";
    public static final String DIFF_UNISAVE_OPERATION_DESC = "";
    public static final String STATUS_UNISAVE_OPERATION =
            "Get status information a single accession.";
    public static final String STATUS_UNISAVE_OPERATION_DESC = "";
    public static final String INCLUDE_CONTENT_UNISAVE_DESCRIPTION =
            "Whether or not to include the entry content (true|false).";
    public static final String VERSIONS_UNISAVE_DESCRIPTION =
            "Can be a greater-than-zero entry version number, comma separated list of numbers, a range written with a dash(-), or a combination of the above. e.g. 6; 1,3-8; 15-20.";
    public static final String VERSIONS_UNISAVE_EXAMPLE = "1,3-8";
    public static final String UNIQUE_SEQUENCE_UNISAVE_DESCRIPTION =
            "Whether or not to aggregate sequences that are unique (true|false)";
    public static final String VERSION1_UNISAVE_DESCRIPTION =
            "One of the entry versions, whose contents is analysed in the diff.";
    public static final String VERSION2_UNISAVE_DESCRIPTION =
            "The other entry version, whose contents is analysed in the diff.";
    public static final String RELEASE_NAME_ATTRIBUTE_DESCRIPTION = "attribute type";
    public static final String RELEASE_NAME_ATTRIBUTE_EXAMPLE = "entry";
}
