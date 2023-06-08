package org.uniprot.api.uniprotkb.view;

import lombok.Value;

import java.util.List;

@Value
public class ViewByResult {
    List<Ancestor> ancestors;
    List<ViewBy> results;
}
