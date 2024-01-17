package org.uniprot.api.uniprotkb.controller;

import io.swagger.v3.oas.annotations.tags.Tag;

import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;
@Tag(name = "UniProtKB group by")
public class GroupByController {
    protected static final String GROUPS = UNIPROTKB_RESOURCE + "/groups";
}
