package org.uniprot.api.unisave;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;

/**
 * Created 20/03/20
 *
 * @author Edd
 */
@SpringBootApplication
@EntityScan( basePackages = {"org.uniprot.api.unisave"} )
public class UniSaveRESTApplication {
    public static void main(String[] args) {
        SpringApplication.run(UniSaveRESTApplication.class, args);
    }
}

// org.uniprot.api.unisave.repository.domain.
// org.uniprot.api.unisave.repository.domain
