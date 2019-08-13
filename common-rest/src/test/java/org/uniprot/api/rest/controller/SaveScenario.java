package org.uniprot.api.rest.controller;

public enum SaveScenario {
    SEARCH_ALL_FIELDS,
    SEARCH_ALL_RETURN_FIELDS,
    SEARCH_SUCCESS,
    SEARCH_NOT_FOUND,
    ALLOW_QUERY_ALL,
    ALLOW_WILDCARD_QUERY,
    SORT_SUCCESS,
    FIELDS_SUCCESS,
    FACETS_SUCCESS;
}
