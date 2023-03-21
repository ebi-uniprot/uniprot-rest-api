package org.uniprot.api.rest.request;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.*;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequest;
import org.uniprot.api.rest.request.idmapping.IdMappingJobRequestToArrayConverter;

/**
 * @author sahmad
 * @created 22/02/2021
 */
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class HashGeneratorTest {
    private static final String SALT_STR = "TEST_SALT";
    private HashGenerator generator;

    @BeforeAll
    void testSetup() {
        generator = new HashGenerator<>(new IdMappingJobRequestToArrayConverter(), SALT_STR);
    }

    @Test
    void testTwoHashesOfTwoRequests() throws InvalidKeySpecException, NoSuchAlgorithmException {
        IdMappingJobRequest req1 = new IdMappingJobRequest();
        req1.setFrom("from1");
        req1.setTo("to1");
        req1.setIds("1,2,3,4");
        req1.setTaxId("taxonId1");
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingJobRequest req2 = new IdMappingJobRequest();
        req2.setFrom("from1");
        req2.setTo("to1");
        req2.setIds("1,2,3,4");
        req2.setTaxId("taxonId1");
        String req2Hash = generator.generateHash(req2);
        Assertions.assertNotNull(req2Hash);
        Assertions.assertEquals(req1Hash, req2Hash);
    }

    @Test
    void testTwoHashesOfTwoRequestsWithIdsInDifferentOrder()
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        IdMappingJobRequest req1 = new IdMappingJobRequest();
        req1.setFrom("from1");
        req1.setTo("to1");
        req1.setIds("4,2,1,3");
        req1.setTaxId("taxonId1");
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingJobRequest req2 = new IdMappingJobRequest();
        req2.setFrom("from1");
        req2.setTo("to1");
        req2.setIds("1,2,3,4");
        req2.setTaxId("taxonId1");
        String req2Hash = generator.generateHash(req2);
        Assertions.assertNotNull(req2Hash);
        Assertions.assertNotEquals(req1Hash, req2Hash);
    }

    @Test
    void testTwoHashesOfTwoRequestsWithIdsOfSameCharacters()
            throws InvalidKeySpecException, NoSuchAlgorithmException {
        IdMappingJobRequest req1 = new IdMappingJobRequest();
        req1.setFrom("from1");
        req1.setTo("to1");
        req1.setIds("4,2,1,3");
        req1.setTaxId("taxonId1");
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingJobRequest req2 = new IdMappingJobRequest();
        req2.setFrom("from1");
        req2.setTo("to1");
        req2.setIds("4213");
        req2.setTaxId("taxonId1");
        String req2Hash = generator.generateHash(req2);
        Assertions.assertNotNull(req2Hash);
        Assertions.assertNotEquals(req1Hash, req2Hash);
    }
}
