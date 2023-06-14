package org.uniprot.api.uniprotkb.view;

import java.util.List;

import lombok.Value;

@Value
public class ViewByResult {
    List<Ancestor> ancestors;
    List<ViewBy> results;
}
