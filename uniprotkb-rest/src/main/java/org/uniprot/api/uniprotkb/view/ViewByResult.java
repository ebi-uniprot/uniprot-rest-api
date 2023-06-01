package org.uniprot.api.uniprotkb.view;

import lombok.Value;

import java.util.List;

@Value
public class ViewByResult<T> {
    List<T> results;
}
