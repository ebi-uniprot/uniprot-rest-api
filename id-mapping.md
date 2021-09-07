---
title: ID Mapping
categories: Text_search,Technical,Website,help
---

> :information_source: To explore and try out the ID Mapping services, please refer to:
> * The [ID Mapping website tool](http://beta.uniprot.org/id-mapping)
> * Our interactive [API documentation](http://www.ebi.ac.uk/uniprot/beta/api/docs/?urls.primaryName=idmapping)

## Overview

The ID Mapping service can map between the identifiers used in one database, to the identifiers of another, e.g., 
Ensembl to PomBase. If you map to UniProtKB, UniParc or UniRef data, the full entries will be returned to you
for convenience.

## Submitting an ID Mapping job

> POST /idmapping/run

For example: 

```bash
% curl --location --request POST 'http://www.ebi.ac.uk/uniprot/beta/api/idmapping/run' --form 'ids="P12345"' --form 'from="UniProtKB_AC-ID"' --form 'to="UniRef90"'
{"jobId":"81016d9d7fb55c00999015039ae9e9178b4d001c"}
```

* Mention from/to rules -- See API
* Check rules in UI

## Polling the status of a job

> GET /idmapping/status/{jobId}

```bash
% curl -- request GET 'http://www.ebi.ac.uk/uniprot/beta/api/idmapping/status/81016d9d7fb55c00999015039ae9e9178b4d001c'
{"jobStatus":"FINISHED"}
```

## Fetching the results of a job
      
> GET /idmapping/results/{jobId}
> GET /idmapping/{uniprot_db}/results/{jobId}

* /uniprotkb, /uniparc, /uniref, normal
* streamed / paged results

> GET /idmapping/stream/{jobId}
> GET /idmapping/{uniprot_db}/results/stream/{jobId}

## Fetching details about a job

> GET /idmapping/details/{jobId}