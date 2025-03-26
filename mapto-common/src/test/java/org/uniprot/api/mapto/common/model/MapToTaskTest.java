package org.uniprot.api.mapto.common.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.api.mapto.common.service.MapToJobService;

@ExtendWith(MockitoExtension.class)
class MapToTaskTest {
    @Mock
    private MapToSearchService mapToSearchService;
    @Mock
    private MapToJobService mapToJobService;
    @Mock
    private MapToJob mapToJob;

    @Test
    void run_withinTheLimits() {
    }
}