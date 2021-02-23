package org.uniprot.api.idmapping.service;

import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;

/**
 * @author sahmad
 * @created 22/02/2021
 */
class HashGeneratorTest {

    @Test
    void testTwoHashesOfTwoRequests() throws InvalidKeySpecException, NoSuchAlgorithmException {
        HashGenerator generator = new HashGenerator();
        IdMappingBasicRequest req1 = new IdMappingBasicRequest();
        req1.setFrom("from1");
        req1.setTo("to1");
        req1.setIds("1,2,3,4");
        req1.setTaxId("taxonId1");
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingBasicRequest req2 = new IdMappingBasicRequest();
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
        HashGenerator generator = new HashGenerator();
        IdMappingBasicRequest req1 = new IdMappingBasicRequest();
        req1.setFrom("from1");
        req1.setTo("to1");
        req1.setIds("4,2,1,3");
        req1.setTaxId("taxonId1");
        String req1Hash = generator.generateHash(req1);
        Assertions.assertNotNull(req1Hash);
        // create another request object with same fields values
        IdMappingBasicRequest req2 = new IdMappingBasicRequest();
        req2.setFrom("from1");
        req2.setTo("to1");
        req2.setIds("1,2,3,4");
        req2.setTaxId("taxonId1");
        String req2Hash = generator.generateHash(req2);
        Assertions.assertNotNull(req2Hash);
        Assertions.assertNotEquals(req1Hash, req2Hash);
    }
}
