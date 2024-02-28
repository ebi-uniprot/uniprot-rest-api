package org.uniprot.api.uniprotkb.controller;

import static org.uniprot.api.rest.openapi.OpenAPIConstants.*;
import static org.uniprot.api.uniprotkb.controller.UniProtKBController.UNIPROTKB_RESOURCE;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = TAG_UNIPROTKB_GROUP, description = TAG_UNIPROTKB_GROUP_DESC)
public class GroupByController {
    protected static final String GROUPS = UNIPROTKB_RESOURCE + "/groups";
}
