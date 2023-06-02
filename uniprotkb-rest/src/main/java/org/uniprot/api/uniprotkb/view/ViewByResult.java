package org.uniprot.api.uniprotkb.view;

import java.util.List;

import lombok.Value;

@Value
public class ViewByResult<T> {
    List<T> results;
}
