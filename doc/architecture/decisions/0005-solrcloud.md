# 5. Solrcloud

Date: 2018-08-02

## Status

Accepted

## Context

We need a search engine to which we will send user queries, and from which we will receive their results.
Moreover, we need an engine that can scale with our data and be resilient to faults (network, filesystem, etc.).

## Decision

The current UniProt website uses lucene as the search engine. This is very fast. However, the drawback is that it does not easily scale.
This can be provided by Solrcloud. We have used this for the Proteins API with success, and therefore have 4 years of experience with it.

## Consequences

Search will therefore rely on Solrcloud. From our experience this has been no problem, when we have simply required retrieval of data
from a single context (Solrcloud collection). However, we do not yet know whether we will need to perform joins across collections,
or whether we will rearchitect the collections so that there is just one; thus, simplifying joining (which is easier within a single collection).
