package org.uniprot.api.uniprotkb.view;

import lombok.Value;

import java.util.Collection;

@Value
public class ViewByResult<T> {
    Collection<T> results;
}
