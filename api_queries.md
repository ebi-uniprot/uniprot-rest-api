
---
title: Programmatic access - Retrieving entries via queries
categories: UniProtKB,UniRef,UniParc,Programmatic_access,Text_search,Technical,help
---

You can use any query to define the set of entries that you are interested in. It is perhaps simplest to start with an interactive [text search](https://beta.uniprot.org/help/text%2Dsearch) to find the URL for your set, e.g., all reviewed human entries:
      
```bash
https://beta.uniprot.org/uniprot?query=reviewed:true+AND+organism:9606
```

The data for the website is provided by our REST API. For the above example, the REST request is:

```bash
https://rest.uniprot.org/beta/uniprotkb/search?query=reviewed:true+AND+organism:9606
```

## Formats

Formats for search results / an entry can be requested in several ways.
     
### Format Parameter
All requests allow the `format` parameter, which can be used to indicate the desired format. For example:

```bash
# TAB SEPARATED VALUES
"https://rest.uniprot.org/beta/uniprotkb/search?query=reviewed:true+AND+organism_id:9606&format=tsv"

# XML
"https://rest.uniprot.org/beta/uniprotkb/P12345?format=xml"
```

### File Extensions
Entries for any data-set can be displayed in different formats by specifying a suffix/file-extension. For example:        

# XML
```bash
"https://rest.uniprot.org/beta/uniprotkb/P12345.xml"
```
                                 
### Accept Header
As is typical with REST requests, the desired format can be specified via the Accept header. For example:

```bash
# TAB SEPARATED VALUES
curl -H "Accept: text/tsv" "https://rest.uniprot.org/beta/uniprotkb/search?query=reviewed:true+AND+organism_id:9606"

# XML
curl -H "Accept: application/xml" "https://rest.uniprot.org/beta/uniprotkb/P12345"
```
                   
## What formats are available?

The different end-points in our API provide different formats, though generally, JSON and tab-separated-values formats (TSV) are
available throughout. 

Please refer to [our comprehensive guide](https://rest.uniprot.org/beta/docs/) to see the formats available to specific end-points.
### List of all formats

| Description | Accept Header Media Type | Format Parameter | File Extension |
|-----|-----|-----|-----|
|JavaScript Object Notation (JSON) |application/json|json|.json|
|Extensible Markup Language (XML) |application/xml|xml|.xml|
|Text file representation|text/flatfile|txt|.txt|
|List of one or more IDs|text/list|list|.list|
|Tab-Separated-Values|text/tsv|tsv|.tsv|
|FASTA: a text-based format representing nucleotide / peptide sequences|text/fasta|fasta|.fasta|
|Genomic Feature Format (GFF) |text/gff|gff|.gff|
|Open Biomedical Ontologies (OBO) |text/obo|obo|.obo|
|Resource Description Framework (RDF)|application/rdf+xml|rdf|.rdf|
|Excel|application/vnd.ms-excel|xlsx|.xlsx|

Below we show some examples of how to use the table above:

```bash
## Accept header
curl -H "Accept: text/flatfile" "https://rest.uniprot.org/beta/uniprotkb/P12345"

## File Extension
curl "https://rest.uniprot.org/beta/uniprotkb/P12345.txt"

## Format Parameter 
curl "https://rest.uniprot.org/beta/uniprotkb/search?query=human&format=gff"
```

## Tips

*   Get familiar with the [query builder](http://beta.uniprot.org/help/advanced%5Fsearch) (advanced search form) by clicking on **Advanced**.
*   Click [**Customize data**](http://beta.uniprot.org/help/customize) on the search results page to select the columns for retrieving result tables in tab-separated or Excel format.
*   You can also look up your relevant column names in the full list of [UniProtKB column names for programmatic access](http://beta.uniprot.org/help/uniprotkb%5Fcolumn%5Fnames).

The URL for a query result consists of a data set name (e.g. `uniprot`, `uniref`, `uniparc`, `taxonomy`, ...) and the actual query. The following query parameters are supported:

Parameter

Values

Description

`query`

_string_

See [query syntax](http://www.uniprot.org/help/text-search)

and [query fields for UniProtKB](http://www.uniprot.org/help/query-fields).

An empty query string will retrieve all entries in a data set. **Tip:** Click **Advanced**

in the search bar.

`format`

`html | tab | xls | fasta | gff | txt | xml | rdf | list | rss`

Format in which to return results:

*   `tab` returns data for the selected `columns` in tab-separated format.
*   `xls` returns data for the selected `columns` for import into Excel.
*   `fasta` returns sequence data only, where applicable.
*   `gff` returns sequence annotation, where applicable.
*   `txt`, `xml` and `rdf` return full entries.
*   `list` returns a list of identifiers.
*   `rss` returns an [OpenSearch](http://opensearch.a9.com/) RSS feed.

**Tip:** Click `Download` above the list of results.

`columns`

comma-separated list of column names

Columns to select for retrieving results in `tab` or `xls` format.

Click **Columns** on the search results page to see the available columns

(for UniProtKB you can also read the [full list of UniProtKB column names](http://www.uniprot.org/help/uniprotkb_column_names)).

**Tip:** Some columns can be parameterized, e.g. `database(PDB)` (see the example at the end of this section).

`include`

`yes | no`

Include isoform sequences when the `format` parameter is set to `fasta`.

Include description of referenced data when the `format` parameter is set to `rdf`.

This parameter is ignored for all other values of the `format` parameter.

`compress`

`yes | no`

Return results gzipped. Note that if the client supports HTTP compression,

results may be compressed transparently even if this parameter is

not set to `yes`.

`limit`

_integer_

Maximum number of results to retrieve.

`offset`

_integer_

Offset of the first result, typically used together with

the `limit` parameter.

The following example retrieves all human entries matching the term '`antigen`' in RDF/XML and tab-separated format, respectively.

https://www.uniprot.org/uniprot/?query=organism:9606+AND+antigen&format=rdf&compress=yes

https://www.uniprot.org/uniprot/?query=organism:9606+AND+antigen&format=tab&compress=yes&columns=id,reviewed,protein names

The next example retrieves all human entries with cross-references to PDB in tab-separated format, showing only the UniProtKB and PDB identifiers.

https://www.uniprot.org/uniprot/?query=organism:9606+AND+database:pdb&format=tab&compress=yes&columns=id,database(PDB)

See also:

[REST API - Access the UniProt website programmatically](http://www.uniprot.org/help/api) - batch retrieval, ID mapping, queries, downloads, etc

Related terms: programmatic access, program, script, wget, curl, web services, API
        