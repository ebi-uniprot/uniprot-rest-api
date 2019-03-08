# 1. Programming Language

Date: 2018-07-18

## Status

Accepted

## Context

With the goal of producing services (e.g., REST APIs) to support the UniProt website, a programming
language should be chosen so that the requirements can be implemented. The choice will
leverage the skills of the team of developers working on this project.

## Decision

This particular UniProt REST API project will use Java, and currently version 8.
Even though Java 11 is already available, many companies still have not made the leap from 8 to 9
due to the possible amount of work required. In future, we do seek to upgrade to Java 8+.

## Consequences

Java is widely used in back-end technologies, and therefore is suitable for our project. Moreover,
Java is the main programming language used within the [UniProt](http://www.uniprot.org) team.
