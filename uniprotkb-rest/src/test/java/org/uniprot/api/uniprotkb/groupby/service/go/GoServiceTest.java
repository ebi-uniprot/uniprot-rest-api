package org.uniprot.api.uniprotkb.groupby.service.go;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoClient;
import org.uniprot.api.uniprotkb.groupby.service.go.client.GoRelation;

@ExtendWith(MockitoExtension.class)
class GoServiceTest {
    private static final String ID = "id";
    @Mock private List<GoRelation> result;
    @Mock private GoClient goClient;
    @InjectMocks private GoService goService;

    @BeforeEach
    void setUp() {}

    @Test
    void getChildren() {
        when(goClient.getChildren(ID)).thenReturn(result);

        List<GoRelation> children = goService.getChildren(ID);

        verify(goClient).getChildren(ID);
        assertSame(result, children);
    }
}
