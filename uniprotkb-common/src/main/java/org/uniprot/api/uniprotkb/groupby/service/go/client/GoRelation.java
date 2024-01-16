package org.uniprot.api.uniprotkb.groupby.service.go.client;

import lombok.Data;

@Data
public class GoRelation {
    private String id;
    private String name;
    private String relation;
    private boolean hasChildren;
}
