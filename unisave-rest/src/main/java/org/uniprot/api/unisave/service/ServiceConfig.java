package org.uniprot.api.unisave.service;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.uniprot.api.unisave.repository.domain.DiffPatch;
import org.uniprot.api.unisave.repository.domain.impl.DiffPatchImpl;

/**
 * Created 24/03/20
 *
 * @author Edd
 */
@Configuration
public class ServiceConfig {
    @Bean
    public DiffPatch diffPatch() {
        return new DiffPatchImpl();
    }
}
