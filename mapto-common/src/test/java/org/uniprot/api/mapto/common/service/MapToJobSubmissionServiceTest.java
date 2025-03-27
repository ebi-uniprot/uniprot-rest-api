package org.uniprot.api.mapto.common.service;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.model.MapToJobRequest;
import org.uniprot.api.mapto.common.model.MapToTask;
import org.uniprot.api.mapto.common.search.MapToSearchFacade;
import org.uniprot.api.mapto.common.search.MapToSearchService;
import org.uniprot.store.config.UniProtDataType;

import net.jodah.failsafe.RetryPolicy;

@ExtendWith(MockitoExtension.class)
class MapToJobSubmissionServiceTest {
    public static final String ID = "jobId";
    public static final UniProtDataType UNI_PROT_DATA_TYPE = UniProtDataType.UNIPROTKB;
    @Mock private ThreadPoolTaskExecutor jobTaskExecutor;
    @Mock private MapToHashGenerator hashGenerator;
    @Mock private MapToJobService mapToJobService;
    @Mock private MapToSearchFacade mapToSearchFacade;
    @Mock private RetryPolicy<Object> retryPolicy;
    @InjectMocks private MapToJobSubmissionService mapToJobSubmissionService;

    @Mock private MapToJobRequest mapToJobRequest;
    @Mock private MapToJob mapToJob;
    @Mock private MapToSearchService mapToSearchService;

    @BeforeEach
    void setUp() {
        when(hashGenerator.generateHash(mapToJobRequest)).thenReturn(ID);
        when(mapToJobService.createMapToJob(ID, mapToJobRequest)).thenReturn(mapToJob);
        when(mapToSearchFacade.getMapToSearchService(UNI_PROT_DATA_TYPE))
                .thenReturn(mapToSearchService);
        when(mapToJobRequest.getSource()).thenReturn(UNI_PROT_DATA_TYPE);
    }

    @Test
    void submit() {
        mapToJobSubmissionService.submit(mapToJobRequest);
        verify(jobTaskExecutor)
                .execute(new MapToTask(mapToSearchService, mapToJobService, mapToJob, retryPolicy));
    }
}
