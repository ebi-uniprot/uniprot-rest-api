package org.uniprot.api.uniprotkb.groupby.model;

import lombok.Value;

import java.util.List;

@Value
public class GroupByResult {
    List<Ancestor> ancestors;
    List<Group> groups;
    Parent parent;
}
