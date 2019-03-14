# 6. Voldemort

Date: 2018-08-02

## Status

Accepted

## Context

We need a store from which entry data will be retrieved. Moreover, this store should scale as our data grows,
and should also be resilient to issues it might face (e.g., network, filesystem, etc.).

## Decision

We will use Project Voldemort, as we have 4+ years of experience with this already with the Proteins API and UniProt's Java API.

## Consequences

We will need to adhere to how Voldemort does things.
