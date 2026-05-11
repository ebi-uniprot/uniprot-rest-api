# UniProt REST API

Backend REST services for UniProt website and API domains.

This repository is a Maven multi-module Spring Boot project. It exposes REST APIs for UniProtKB, UniParc, UniRef, ID mapping, proteomes, support data, asynchronous downloads, UniSave, automatic annotation, and help-centre data.

## Repository Layout

Top-level REST modules:

- `common-rest` - shared REST infrastructure: controllers, request handling, content negotiation, validation, message converters, pagination, facets, and common search support.
- `uniprotkb-common`, `uniprotkb-rest` - UniProtKB services, stores, converters, controllers, and integration tests.
- `uniparc-common`, `uniparc-rest` - UniParc services and endpoints.
- `uniref-common`, `uniref-rest` - UniRef services and endpoints.
- `idmapping-common`, `idmapping-rest` - ID mapping job, result, stream, and group-by APIs.
- `async-download-rest` - asynchronous download jobs and messaging.
- `mapto-common`, `mapto-rest` - map-to jobs and result APIs.
- `support-data-common`, `support-data-rest` - taxonomy, keyword, disease, literature, cross-reference, subcellular location, suggester, and configure endpoints.
- `proteome-rest` - proteome and gene-centric endpoints.
- `unisave-rest` - UniSave endpoints.
- `aa-rest` - UniRule and ARBA endpoints.
- `help-centre-rest` - help centre, contact, and release notes endpoints.
- `jacoco-aggregate-report` - aggregate coverage reports.

## Related Repositories

This project depends on sibling UniProt projects that are usually checked out in the same parent directory:

- `../uniprot-core` - UniProt domain model, builders, parsers, JSON/XML/FASTA/TSV support, and controlled vocabulary code.
- `../uniprot-store` - Solr document models, search/return field configuration, Voldemort datastore clients, index configuration, indexers, and test datastore helpers.

The REST parent POM uses `${project.version}` for `uniprot-core.version` and `uniprot-store.version`, so local development often needs matching snapshot versions across these repositories.

## Prerequisites

- Java 17
- Maven
- Access to UniProt Maven repositories / Artifactory for internal artifacts

There is no Maven wrapper in this repository.

## Build And Test

Build and test the whole project:

```bash
mvn test
```

Build or test one module:

```bash
mvn -pl uniprotkb-rest test
```

Run one integration test class:

```bash
mvn -pl uniprotkb-rest -Dtest=PrecomputedUniProtKBControllerIT test
```

Build a module with dependencies from this reactor:

```bash
mvn -pl uniprotkb-rest -am test
```

Spotless formatting runs during the Maven lifecycle and may rewrite Java files.

## Common Development Flow

1. Identify the domain module, for example `uniprotkb-rest` for UniProtKB controllers.
2. Check the matching `*-common` module for services, stores, repositories, request conversion, and response converter configuration.
3. For domain model or parser behavior, check `../core`.
4. For Solr documents, return/search fields, index configuration, or Voldemort stores, check `../store`.
5. Add or update focused integration tests in the relevant `*-rest/src/test/java/.../controller` package.
6. Run the focused test first, then the module test suite.

## REST Architecture

Most search-style controllers extend:

```text
common-rest/src/main/java/org/uniprot/api/rest/controller/BasicSearchController.java
```

`BasicSearchController` centralizes entity responses, search responses, streaming responses, common headers, pagination events, redirects, and `Accept` header handling.

Output is normally selected by Spring media type negotiation:

- Controllers declare supported media types in `@GetMapping(... produces = {...})`.
- Controllers return a `MessageConverterContext` through `getEntityResponse(...)`, `getSearchResponse(...)`, or streaming helpers.
- Registered `HttpMessageConverter`s serialize the response.
- Media type constants live in:

```text
common-rest/src/main/java/org/uniprot/api/rest/output/UniProtMediaType.java
```

UniProtKB converter registration is in:

```text
uniprotkb-common/src/main/java/org/uniprot/api/uniprotkb/common/response/UniProtKBMessageConverterConfig.java
```

## UniProtKB Pointers

Key files for UniProtKB work:

- `uniprotkb-rest/src/main/java/org/uniprot/api/uniprotkb/UniProtKBREST.java`
- `uniprotkb-rest/src/main/java/org/uniprot/api/uniprotkb/controller/UniProtKBController.java`
- `uniprotkb-common/src/main/java/org/uniprot/api/uniprotkb/common/service/uniprotkb/UniProtEntryService.java`
- `uniprotkb-common/src/main/java/org/uniprot/api/uniprotkb/common/repository/search/UniprotQueryRepository.java`
- `uniprotkb-common/src/main/java/org/uniprot/api/uniprotkb/common/repository/store/`
- `uniprotkb-common/src/main/java/org/uniprot/api/uniprotkb/common/response/`
- `uniprotkb-rest/src/test/java/org/uniprot/api/uniprotkb/controller/`

Thin store-backed UniProtKB endpoints include:

- `PrecomputedUniProtKBController`
- `ProtNLMUniProtKBController`

These typically validate path variables, fetch a `UniProtKBEntry` from a store-backed service, and return through `BasicSearchController.getEntityResponse(...)`.

## Testing Notes

Controller integration tests commonly use:

- `@WebMvcTest`
- `@ContextConfiguration(classes = {<Application>.class})`
- `@ActiveProfiles("offline")`
- `DataStoreManager`
- in-memory Voldemort stores from `../store`
- embedded Solr for search-related tests

When testing FASTA output for UniProtKB entries, make sure the test entry has enough data for core FASTA serialization, especially:

- entry audit with sequence version
- organism with scientific name and taxon ID
- sequence

