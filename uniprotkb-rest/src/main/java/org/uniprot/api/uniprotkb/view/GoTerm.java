package org.uniprot.api.uniprotkb.view;

import java.util.List;

import lombok.Data;

@Data
public class GoTerm {
    private String id;
    private String name;
    private List<GoRelation> children;
}
