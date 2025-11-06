package org.uniprot.api.idmapping.common.service;

import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.idmapping.common.service.impl.UniProtKBIdGroupByGOService;
import org.uniprot.api.uniprotkb.common.service.go.model.GoRelation;
import org.uniprot.api.uniprotkb.common.service.groupby.GroupByGOService;

@ExtendWith(MockitoExtension.class)
class UniProtKBIdGroupByGOServiceTest extends UniProtKBIdGroupByServiceTest<GoRelation> {
    @Mock private GroupByGOService groupByGOService;
    @Mock private GoRelation goRelation0;
    @Mock private GoRelation goRelation1;
    @Mock private GoRelation goRelation2;
    @Mock private GoRelation goRelation3;
    @Mock private GoRelation goRelation4;

    @BeforeEach
    void setUp() {
        idGroupByService = new UniProtKBIdGroupByGOService(groupByGOService, uniProtEntryService);
        groupByService = groupByGOService;
        init();
    }

    @Override
    protected void prepareForEmptyParent() {
        when(groupByGOService.getInitialEntries(null))
                .thenReturn(List.of(goRelation0, goRelation1, goRelation2));
    }

    @Override
    protected void prepareWithParent() {
        when(groupByGOService.getInitialEntries(PARENT))
                .thenReturn(List.of(goRelation0, goRelation1, goRelation2));
    }

    @Override
    protected List<GoRelation> getInitialEntries() {
        return List.of(goRelation0, goRelation1, goRelation2);
    }

    @Override
    protected List<GoRelation> getChildEntries() {
        return List.of(goRelation3, goRelation4);
    }
}
