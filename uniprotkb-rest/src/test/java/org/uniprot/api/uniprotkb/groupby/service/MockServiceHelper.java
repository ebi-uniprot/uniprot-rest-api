package org.uniprot.api.uniprotkb.groupby.service;

import org.uniprot.api.uniprotkb.groupby.model.Group;
import org.uniprot.api.uniprotkb.groupby.model.GroupImpl;

public class MockServiceHelper {

    public static Group createViewBy(String id, String label, long count, boolean expand) {
        return GroupImpl.builder().id(id).label(label).count(count).expand(expand).build();
    }
}
