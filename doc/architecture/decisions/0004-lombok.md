# 4. Lombok

Date: 2018-08-02

## Status

Accepted

## Context

Java projects often contain a large amount of boilerplate code, e.g., defining data/value classes, builders, etc. All
such code follows a certain pattern and needs testing -- and writing both of these can be error prone. A library
that enables cutting down boilerplate code, and which generates tested code would be beneficial to the project.

## Decision

We will use the [Lombok](https://projectlombok.org/) library to reduce the amount of boilerplate code we need to write.

## Consequences

With less code to maintain, we envisage a more succint codebase whose function is more readable.
