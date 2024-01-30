package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(
        name = "UniProtKB group by",
        description =
                "Allows you to browse your query results using the taxonomy, keyword, Gene Ontology or Enzyme Classification hierarchies and to view the distribution of your search results across the terms within each group.")
public class GroupByController {
    protected static final String GROUPS = UNIPROTKB_RESOURCE + "/groups";
}
