package org.uniprot.api.idmapping.service;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.idmapping.model.IdMappingJob;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Configuration
public class JobConfig {
    @Bean
    BlockingQueue<IdMappingJob> jobQueue() {
        return new LinkedBlockingQueue<>();
    }
}
