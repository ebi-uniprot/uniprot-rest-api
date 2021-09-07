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

## Parameters _from_ and _to_ databases possible values

You can map `from` one database `to` another database. To find the name of all the possible valid databases pairs (both from and to), use the below curl command:

```bash
% curl https://www.ebi.ac.uk/uniprot/beta/api/configure/idmapping/fields
```
The response has two top sections :
- groups
    - items
- rules

Each group represents a logical group of databases. It contains an array of `items`.
The value of `groupName` is used by UI to group the similar kind of databases together.
Each item of `items` array has the following attributes:
- displayName : Used by the [UI](https://beta.uniprot.org/id-mapping)
- name : Name of `from` or `to` database to be passed in the request.
- from: Boolean flag to tell if the `name` can be used as a `from` database. Defaults to false
- to: Boolean flag to tell if the `name` can be used as a `to` database. Defaults to false
- ruleId: Refers a rule in the `rules` section to find all possible values of `to` databases.

Each rule of `rules` array represents all possible `to` databases for a `from` database.
It has the following attributes:

- ruleId : Unique id of the rule. Referred by item of `groups`' `items`.
- tos: List of possible `to` databases for a given `from` item.
- defaultTo: Used by  UI to be selected by default in dropdown.
- taxonId: Flag to tell if a third optional param `taxonId`(Taxonomy Id) is allowed apart `from` and `to` databases.
