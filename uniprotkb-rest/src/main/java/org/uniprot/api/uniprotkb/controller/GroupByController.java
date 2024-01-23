package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.tags.Tag;

import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
@Tag(name = "UniProtKB group by", description = "UniProtKB group-by functions allow the browsing of hierarchical organisation or graphs of the query results based on various factors. " +
        "Group by taxonomy organises the query results in a taxonomic tree, where one can traverse from species to domain. " +
        "Group by keyword - ? " +
        "Group by Gene Ontology - ? " +
        "and Group by Enzyme Class - ? ")
public class GroupByController {
    protected static final String GROUPS = UNIPROTKB_RESOURCE + "/groups";
}
