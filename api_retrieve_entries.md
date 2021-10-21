
---
title: Programmatic access - Retrieving individual entries
categories: UniProtKB,UniRef,UniParc,Programmatic_access,help
---

The web address for an entry consists of a data set name (e.g. `uniprot`, `uniref`, `uniparc`, `taxonomy`, ...) and the 
entry's unique identifier, e.g.:

https://beta.uniprot.org/uniprot/P12345

By default, a web page is returned. To retrieve only the entry data, it is possible
to use the REST API and specify a file extension after the entry id indicating the format. 
Here are some examples:

https://rest.uniprot.org/beta/uniprotkb/P12345.json
https://rest.uniprot.org/beta/uniprotkb/P12345.txt
https://rest.uniprot.org/beta/uniprotkb/P12345.fasta
https://rest.uniprot.org/beta/uniprotkb/P12345.gff
https://rest.uniprot.org/beta/uniprotkb/P12345.tsv
https://rest.uniprot.org/beta/uniprotkb/P12345.xml
https://rest.uniprot.org/beta/uniprotkb/P12345.rdf

https://rest.uniprot.org/beta/uniref/UniRef90_P99999.json
https://rest.uniprot.org/beta/uniref/UniRef90_P99999.xml
https://rest.uniprot.org/beta/uniref/UniRef90_P99999.rdf
https://rest.uniprot.org/beta/uniref/UniRef90_P99999.fasta
https://rest.uniprot.org/beta/uniref/UniRef90_P99999.tsv
                                                                     
https://rest.uniprot.org/beta/uniparc/UPI000000001F.json
https://rest.uniprot.org/beta/uniparc/UPI000000001F.xml
https://rest.uniprot.org/beta/uniparc/UPI000000001F.rdf
https://rest.uniprot.org/beta/uniparc/UPI000000001F.fasta
https://rest.uniprot.org/beta/uniparc/UPI000000001F.tsv
                                                        
#### Notes
* UniRef identifiers cannot be guaranteed to be stable, since the sequence clusters are recomputed at every 
release, and the representative protein may change. 
See also: [How to link to UniProt entries](http://www.uniprot.org/help/linking%5Fto%5Funiprot).
* UniRef also has a "light" version of the entry, which does not contain the full entry details, but enough to meet 
many demands. Notably, it contains a list of Strings that represent an entry's members;
rather than a list of member objects.

https://rest.uniprot.org/beta/uniref/UniRef90_P99999/light

Note that to get the light entry in different formats, one should use send an HTTP [`'Accept'` request header](http://www.w3.org/Protocols/rfc2616/rfc2616%2Dsec14.html),
specifying the media type desired. For more information, please refer to the [interactive API documentation](https://rest.uniprot.org/beta/docs/?urls.primaryName=uniref#/uniref/getById).

#### Resolving RDF identifiers

A request for an address such as:

http://purl.uniprot.org/uniprot/P12345

will be resolved, where possible, by redirection to the corresponding resource (see previous section). For UniProt 
resources, entries are returned in RDF/XML format if the 
HTTP [`'Accept'` request header](http://www.w3.org/Protocols/rfc2616/rfc2616%2Dsec14.html) is set to
`'application/rdf+xml'`.

See also:

[REST API - Access the UniProt website programmatically](http://www.uniprot.org/help/api) - batch retrieval, ID mapping, queries, downloads, etc
