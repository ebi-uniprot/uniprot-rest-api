package org.uniprot.api.idmapping.service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.idmapping.controller.request.IdMappingRequest;

/**
 * @author sahmad
 * @created 22/02/2021
 */
class HashGeneratorTest {

    @Test
    void testTwoHashesOfTwoRequests() throws InvalidKeySpecException, NoSuchAlgorithmException {
        HashGenerator generator = new HashGenerator();
        IdMappingRequest.IdMappingRequestBuilder reqBuilder1 = IdMappingRequest.builder();
        IdMappingRequest req1 =
                reqBuilder1.from("from1").to("to1").ids("1,2,3,4").taxId("taxonId1").build();
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingRequest.IdMappingRequestBuilder reqBuilder2 = IdMappingRequest.builder();
        IdMappingRequest req2 =
                reqBuilder2.from("from1").to("to1").ids("1,2,3,4").taxId("taxonId1").build();
        String req2Hash = generator.generateHash(req2);
        Assertions.assertNotNull(req2Hash);
        Assertions.assertEquals(req1Hash, req2Hash);
    }

    @Test
    void testTwoHashesOfTwoRequestsWithIdsInDifferentOrder()
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        HashGenerator generator = new HashGenerator();
        IdMappingRequest.IdMappingRequestBuilder reqBuilder1 = IdMappingRequest.builder();
        IdMappingRequest req1 =
                reqBuilder1.from("from1").to("to1").ids("2,4,3,1").taxId("taxonId1").build();
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingRequest.IdMappingRequestBuilder reqBuilder2 = IdMappingRequest.builder();
        IdMappingRequest req2 =
                reqBuilder2.from("from1").to("to1").ids("1,2,3,4").taxId("taxonId1").build();
        String req2Hash = generator.generateHash(req2);
        Assertions.assertNotNull(req2Hash);
        Assertions.assertEquals(req1Hash, req2Hash);
    }

    @Test
    void testHashGenerationTiming()
            throws IOException, InvalidKeySpecException, NoSuchAlgorithmException {
        IdMappingRequest.IdMappingRequestBuilder reqBuilder1 = IdMappingRequest.builder();
        reqBuilder1.from("from1").to("to1");
        Path filePath = Paths.get("/Users/sahmad/Downloads/ids.txt");
        String ids = Files.readString(filePath);
        reqBuilder1.ids(ids);
        long start = System.currentTimeMillis();
        HashGenerator generator = new HashGenerator();
        String hash = generator.generateHash(reqBuilder1.build());
        long end = System.currentTimeMillis();
        System.out.println("Total time taken in ms : " + (end - start));
        System.out.println("hash : " + hash);
    }
}
