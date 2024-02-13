package org.uniprot.api.uniprotkb.common.service.go.model;

import lombok.Data;

@Data
public class GoRelation {
    private String id;
    private String name;
    private String relation;
    private boolean hasChildren;
}
