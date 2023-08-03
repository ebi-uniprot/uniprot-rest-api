package org.uniprot.api.uniprotkb.groupby.service.go;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GOClient;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoRelation;

@ExtendWith(MockitoExtension.class)
class GOServiceTest {
    private static final String ID = "id";
    @Mock private List<GoRelation> resultChildren;
    @Mock private Optional<GoRelation> resultSingle;
    @Mock private GOClient goClient;
    @InjectMocks private GOService goService;

    @BeforeEach
    void setUp() {}

    @Test
    void getChildren() {
        when(goClient.getChildren(ID)).thenReturn(resultChildren);

        List<GoRelation> children = goService.getChildren(ID);

        verify(goClient).getChildren(ID);
        assertSame(resultChildren, children);
    }

    @Test
    void getGoRelation() {
        when(goClient.getGoEntry(ID)).thenReturn(resultSingle);

        Optional<GoRelation> children = goService.getGoRelation(ID);

        verify(goClient).getGoEntry(ID);
        assertSame(resultSingle, children);
    }
}
