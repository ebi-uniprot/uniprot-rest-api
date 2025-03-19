package org.uniprot.api.mapto.common.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.uniprot.api.mapto.common.model.MapToJob;
import org.uniprot.api.mapto.common.repository.MapToJobRepository;

import java.util.Optional;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class MapToJobServiceTest {

    @Mock
    private MapToJobRepository jobRepository;


    @Mock
    private MapToJob mapToJob;

    private MapToJobService jobService;

    @BeforeEach
    void setUp(){
        this.jobService = new MapToJobService(jobRepository);
    }

    @Test
    void testCreate(){
        String id = "ID";
        this.jobService.createMapToJob(mapToJob);
        verify(jobRepository).save(mapToJob);
    }

    @Test
    void testCreateDuplicateJob(){
        String id = "ID";
        when(this.mapToJob.getId()).thenReturn(id);
        when(this.jobRepository.findById(id)).thenReturn(Optional.of(mapToJob));
        Assertions.assertThrows(RuntimeException.class, () -> this.jobService.createMapToJob(mapToJob));
    }

    @Test
    void testFindById(){
        String id = "ID";
        when(this.jobRepository.findById(id)).thenReturn(Optional.of(this.mapToJob));
        Assertions.assertEquals(Optional.of(this.mapToJob), this.jobService.findMapToJob(id));
    }

    @Test
    void testDelete(){
        String id = "ID";
        jobService.deleteMapToJob(id);
        verify(this.jobRepository).deleteById(id);
    }
    @Test
    void testExists() {
        String id = "ID";
        Boolean answer = true;
        when(this.jobRepository.existsById(id)).thenReturn(answer);
        Assertions.assertEquals(answer, jobService.mapToJobExists(id));
    }

}
