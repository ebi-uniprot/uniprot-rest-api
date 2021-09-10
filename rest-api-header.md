## Overview

This subsection explains how UniProt REST api HTTP response headers works and also give some examples about their behaviour 

###Http status header
HTTP status are number codes that indicate whether a specific HTTP request has been successfully completed or not. Below we describe all possibel return status code


####200 OK 
Success status response code indicates that the request has succeeded.

Success Example:
```bash
curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?size=1&query=P53&fields=accession%2Cgene_names" -H "accept: application/json"
```

Important Response Headers
```bash
 HTTP/1.1 200 
 content-type: application/json 
 link: <https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?fields=accession,gene_names&query=P53&cursor=82giuzutyxve1mc8va46o7i2jq3r7fb5tf&size=1>; rel="next" 
 x-release-date: 25-July-2021 
 x-release-number: 2021_03 
 x-total-records: 49524 
 ...
```

Response Body
```json
{
  "results": [
    {
      "primaryAccession": "P04637",
      "genes": [
        {
          "geneName": {"value": "TP53"},
          "synonyms": [{"value": "P53"}]
        }
      ]
    }
  ]
}
```
####303 See Other
Indicates that you are being redirected to another resource.
The "Location" response header indicates the URL to redirect a resource to.

UniProtKB inactive entry example:

```bash
curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/Q00015" -H "accept: application/json"
```

Response Header
```bash
 HTTP/1.1 303
 content-type: application/json 
 Location: /uniprot/beta/api/uniprotkb/accession/P23141
 x-release-date: 25-July-2021 
 x-release-number: 2021_03 
 ...
```

####400 Bad request
Indicates that the server was unable to process the request due to invalid information sent by the client. 
In other words, the client request needs modification. Please access our [API documentation](https://www.ebi.ac.uk/uniprot/beta/api/docs/) for guidance.

Bad request examples: 

- Missing required request parameter
```bash
curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search" -H "accept: application/json"
```

```json
{
  "url": "http://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search",
  "messages": [
    "'query' is a required parameter"
  ]
}
```
- Invalid parameter value provided 
```bash
curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?query=invalidQueryField%3Avalue+AND+accession%3AinvalidValue&fields=invalidField" -H "accept: application/json"
```

```json
{
  "url": "http://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search",
  "messages": [
    "'invalidQueryField' is not a valid search field",
    "The 'accession' filter value 'invalidValue' has invalid format. It should be a valid UniProtKB accession",
    "Invalid fields parameter value 'invalidField'"
  ]
}
```

####404 Resource not found 
Indicates the server was unable to find what was requested.

Resource not found Example:
```bash
curl -X GET "https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/P99997" -H "accept: application/json"
```
```json
{
  "url": "http://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/accession/P99997",
  "messages": [
    "Resource not found"
  ]
}
```

####500 Internal server error
The execution of the server failed unexpectedly.
If you are able to replicate this error consistently, please contact us.


###x-release-date
Indicate the last date that the API was updated.

Example:
```bash
x-release-date: 25-July-2021
```

###x-release-number
Indicate the current UniProt data release number.
```bash
x-release-number: 2021_03 
```

###x-total-records
Indicate the number of entities found in your search. See more details at <rest-pagination.md>
```bash
x-total-records: 49524 
```

###link
Indicate the link for the next page of entities. See more details at <rest-pagination.md>
```bash
link: <https://www.ebi.ac.uk/uniprot/beta/api/uniprotkb/search?query=P53&cursor=1mkycb2xwxbou9vfxnpy5g9gjf6k5i9fxg6s&size=25>; rel="next" 
```
