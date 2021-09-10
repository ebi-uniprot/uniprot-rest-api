---
title: HTTP Status Codes in the REST API 
categories: Technical,Website,help
---

This document explains the HTTP response headers returned by the [UniProt REST API](http://www.ebi.ac.uk/uniprot/beta/api/docs/)
and gives some examples to make explicit what they mean. 

## HTTP Status Headers
[HTTP statuses](https://httpstatuses.com/) are standard numeric codes indicating whether a specific HTTP request successfully 
completed. In the following sections, we describe all status codes returned by the UniProt REST API.


### 200 OK 
The `200 OK` status code indicates the request succeeded. For example:

> **Request**
> ```bash
> curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?size=1&query=P53&fields=accession%2Cgene_names" \ 
>      -H "accept: application/json"
> ```
> **Important Response Headers**
> ```bash
>  HTTP/1.1 200 
>  content-type: application/json 
>  link: <https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?fields=accession,gene_names&query=P53&cursor=82giuzutyxve1mc8va46o7i2jq3r7fb5tf&size=1>; rel="next" 
>  x-release-date: 25-July-2021 
>  x-release-number: 2021_03 
>  x-total-records: 49524 
>  ...
> ```
> **Response Body**
> ```json
> {
>   "results": [
>     {
>       "primaryAccession": "P04637",
>       "genes": [
>         {
>           "geneName": {"value": "TP53"},
>           "synonyms": [{"value": "P53"}]
>         }
>       ]
>     }
>   ]
> }
> ```

###303 See Other
The `303 See Other` status code indicates that you are being redirected to another resource via the "Location" response
header.

#### UniProtKB inactive entry example:
> **Request**
> ```bash
> curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/Q00015" -H "accept: application/json"
> ```
> **Response Headers**
> ```bash
>  HTTP/1.1 303
>  content-type: application/json 
>  Location: /uniprot/beta/api/uniprotkb/accession/P23141
>  x-release-date: 25-July-2021 
>  x-release-number: 2021_03 
>  ...
> ```

###400 Bad request
The `400 Bad request` status code indicates that the server was unable to process the request due to invalid information
sent by the client. In other words, the client request needs modification. If this happens, please refer to our
[API documentation](https://www.ebi.ac.uk/uniprot/beta/api/docs/) for guidance.

The following two subsections give examples of bad requests: 

#### Missing required request parameter
> **Request**
> ```bash
> curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search" -H "accept: application/json"
> ```
> **Response**
> ```json
> {
>   "url": "http://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search",
>   "messages": [
>     "'query' is a required parameter"
>   ]
> }
> ```

#### Invalid parameter value 
> **Request**
> ```bash
> curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?query=invalidQueryField%3Avalue+AND+accession%3AinvalidValue&fields=invalidField" -H "accept: application/json"
> ```
> **Response**
> ```json
> {
>   "url": "http://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search",
>   "messages": [
>     "'invalidQueryField' is not a valid search field",
>     "The 'accession' filter value 'invalidValue' has invalid format. It should be a valid UniProtKB accession",
>     "Invalid fields parameter value 'invalidField'"
>   ]
> }
> ```

###404 Resource not found 
The `404 Resource not found` status code indicates the server was unable to find the resource that was requested. For
example:

> **Request**
> ```bash
> curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/P99997" -H "accept: application/json"
> ```
> **Response**
> ```json
> {
>   "url": "http://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/P99997",
>   "messages": [
>     "Resource not found"
>   ]
> }
> ```

###500 Internal server error
The `500 Internal server error` status code indicates that the processing of the request on the server failed unexpectedly.
If you are able to replicate this error consistently, please [report it to us](mailto:help@uniprot.org).


##x-release-date
This response header indicates the last date that the API was updated. Refer to, [200 OK above](#200-ok), to see an example 
of its generation.

> ```bash
> x-release-date: 25-July-2021
> ```

##x-release-number
This response header indicates the current UniProt data release number.
> ```bash
> x-release-number: 2021_03 
> ```

##x-total-records
This response header indicates the number of entities found in your search. See more details in the [Paginating results](rest-pagination) 
help page.

> ```bash
> x-total-records: 49524 
> ```

##link
This response header indicates the URL link to the next page of entities. See more details in the [Paginating results](rest-pagination) 
help page.

> ```bash
> link: <https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?query=P53&cursor=1mkycb2xwxbou9vfxnpy5g9gjf6k5i9fxg6s&size=25>; rel="next" 
> ```
