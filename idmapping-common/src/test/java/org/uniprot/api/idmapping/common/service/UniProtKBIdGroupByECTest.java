package org.uniprot.api.idmapping.common.service;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByECService;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByECService;

@ExtendWith(MockitoExtension.class)
class UniProtKBIdGroupByECTest extends UniProtKBIdGroupByServiceTest<String> {
    @Mock private GroupByECService groupByECService;
    private final String EC_0 = "ec0";
    private final String EC_1 = "ec1";
    private final String EC_2 = "ec2";
    private final String EC_3 = "ec3";
    private final String EC_4 = "ec4";

    @BeforeEach
    void setUp() {
        idGroupByService = new UniProtKBIdGroupByECService(groupByECService, uniProtEntryService);
        groupByService = groupByECService;
        lenient()
                .when(groupByECService.getShortFormEc(anyString()))
                .thenAnswer(in -> in.getArguments()[0]);
        init();
    }

    @Override
    protected void prepareForEmptyParent() {
        when(groupByECService.getInitialEntries(null)).thenReturn(List.of(EC_0, EC_1, EC_2));
    }

    @Override
    protected void prepareWithParent() {
        when(groupByECService.getInitialEntries(PARENT)).thenReturn(List.of(EC_0, EC_1, EC_2));
    }

    @Override
    protected List<String> getInitialEntries() {
        return List.of(EC_0, EC_1, EC_2);
    }

    @Override
    protected List<String> getChildEntries() {
        return List.of(EC_3, EC_4);
    }
}
