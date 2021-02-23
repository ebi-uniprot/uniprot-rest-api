package org.uniprot.api.idmapping.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.idmapping.model.IdMappingJob;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * @author sahmad
 * @created 23/02/2021
 */
@Configuration
public class JobConfig {

    @Bean
    BlockingQueue<IdMappingJob> jobQueue(){
        return new LinkedBlockingQueue<>();
    }
}
