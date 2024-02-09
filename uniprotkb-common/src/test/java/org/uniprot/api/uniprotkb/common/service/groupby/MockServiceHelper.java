package org.uniprot.api.uniprotkb.common.service.groupby;

import org.uniprot.api.uniprotkb.common.service.groupby.model.Group;
import org.uniprot.api.uniprotkb.common.service.groupby.model.GroupImpl;

public class MockServiceHelper {

    public static Group createGroupByResult(String id, String label, long count, boolean expand) {
        return GroupImpl.builder().id(id).label(label).count(count).expandable(expand).build();
    }
}
