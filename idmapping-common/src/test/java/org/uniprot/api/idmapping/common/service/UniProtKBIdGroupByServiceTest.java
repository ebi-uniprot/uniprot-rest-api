package org.uniprot.api.idmapping.common.service;

import org.junit.jupiter.api.Test;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByService;
import org.uniprot.api.uniprotkb.common.service.uniprotkb.UniProtEntryService;

import static org.junit.jupiter.api.Assertions.*;

public abstract class UniProtKBIdGroupByServiceTest<T> {
    private final GroupByService<T> groupByService;
    private final UniProtEntryService uniProtEntryService;

    protected UniProtKBIdGroupByServiceTest(GroupByService<T> groupByService, UniProtEntryService uniProtEntryService) {
        this.groupByService = groupByService;
        this.uniProtEntryService = uniProtEntryService;
    }

    @Test
    void getGroupByResult() {

    }
}