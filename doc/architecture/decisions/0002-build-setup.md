# 2. Build Setup

Date: 2018-08-18

## Status

Accepted

## Context

A build tool is necessary for various tasks such as compile, test, packaging and deploying our code.

## Decision

We will use [Maven 3.2.5+](https://maven.apache.org) to build the back-end UniProt REST API components.

## Consequences

Maven automates the building tasks used frequently in development and production. There is a plethora of
plugins and libraries that enable easy customisation and configuration.