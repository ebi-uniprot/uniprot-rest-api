package org.uniprot.api.uniprotkb.groupby.model;

import java.util.List;

import lombok.Value;

@Value
public class GroupByResult {
    List<Ancestor> ancestors;
    List<Group> groups;
}
