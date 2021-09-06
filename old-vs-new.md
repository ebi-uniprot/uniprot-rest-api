---
title: UniProtKB APIs
categories: Text_search,Technical,Website,help
---
This document provides new equivalent APIs for UniProtKB.

|Operation                        |Old API                                                                                    |New API                                                                                                                             |Details    |
|---------------------------------|-------------------------------------------------------------------------------------------|------------------------------------------------------------------------------------------------------------------------------------|-----------|
|Get individual entry             |https://www.uniprot.org/uniprot/P12345                                                     |https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/P12345                                                                   |a. [Return Fields Migration](return-fields.md) <br /> b. [_Swagger UI_](http://www.ebi.ac.uk/uniprot/beta/api/docs/#/uniprotkb/getByAccession)|
|Get entries via Lucene query     |https://www.uniprot.org/uniprot/?query=reviewed:yes organism:"Homo sapiens (Human) [9606]" |https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?query=(reviewed:true) AND (organism_id:9606)                                |a. [Return Fields Migration](return-fields.md) <br /> b. [Query Fields Migration](query-fields.md) <br /> c. [_Swagger UI_](http://www.ebi.ac.uk/uniprot/beta/api/docs/#/uniprotkb/searchCursor)|
|Download entries via Lucene query|https://www.uniprot.org/uniprot/?query=reviewed:yes+AND+organism:9606&force=true&format=tab|curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/stream?query=reviewed:true AND organism_id:9606" -H "accept: text/tsv"|a. [Return Fields Migration](return-fields.md) <br /> b. [Query Fields Migration](query-fields.md) <br /> c. [_Swagger UI_](http://www.ebi.ac.uk/uniprot/beta/api/docs/#/uniprotkb/stream)|
