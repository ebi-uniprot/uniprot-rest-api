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

For example, to map UniProtKB entries P21802,P12345, we could POST a request to the above REST end-point as follows: 

```bash
% curl --location --request POST 'http://www.ebi.ac.uk/uniprot/beta/api/idmapping/run' --form 'ids="P21802,P12345"' --form 'from="UniProtKB_AC-ID"' --form 'to="UniRef90"'
{"jobId":"27a020f6334184c4eb382111fbcad0e848f40300"}
```
Be sure to take note of the `jobId`. This will be used later to:

* poll the status of the job
* fetch/download the results
* get details about the job
  
* Mention from/to rules -- See API
* Check rules in UI

## Polling the status of a job

> GET /idmapping/status/{jobId}

Continuing the above example, we can use the `jobId` to find out the status of the job as follows:

```bash
% curl -i 'http://www.ebi.ac.uk/uniprot/beta/api/idmapping/status/27a020f6334184c4eb382111fbcad0e848f40300'
HTTP/1.1 303 
Server: nginx/1.17.7
Location: https://www.ebi.ac.uk/uniprot/beta/api/idmapping/uniref/results/27a020f6334184c4eb382111fbcad0e848f40300
Content-Type: application/json
Access-Control-Allow-Origin: *
...

{"jobStatus":"FINISHED"}
```

Note that the `jobStatus` is finished, indicating that the job's results are ready to be fetched. Note also the [HTTP 303](https://httpstatuses.com/303)
header that indicates the results can be retrieved via the URL in the `Location` header. 

## Fetching the results of a job

The results of a job are retrieved one of following above end-points:
      
> GET /idmapping/results/{jobId}
> GET /idmapping/{uniprot_db}/results/{jobId} ## where {uniprot_db} is one of uniparc, uniprotkb or uniref

For example:
              
```bash
curl -s "https://www.ebi.ac.uk/uniprot/beta/api/idmapping/uniref/results/27a020f6334184c4eb382111fbcad0e848f40300" | jq
100 21176    0 21176    0     0   144k      0 --:--:-- --:--:-- --:--:--  144k
{
  "results": [
    {
      "from": "P21802",
      "to": {
        "id": "UniRef90_P21802",
        "name": "Cluster: Fibroblast growth factor receptor 2",
        "updated": "2021-06-02",
        "entryType": "UniRef90",
  ...
```


* /uniprotkb, /uniparc, /uniref, normal
* streamed / paged results

> GET /idmapping/stream/{jobId}
> GET /idmapping/{uniprot_db}/results/stream/{jobId}

## Fetching details about a job

> GET /idmapping/details/{jobId}