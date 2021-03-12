package org.uniprot.api.idmapping.service;

import org.apache.commons.codec.binary.Hex;
import org.uniprot.api.common.exception.ServiceException;
import org.uniprot.api.idmapping.controller.request.IdMappingJobRequest;
import org.uniprot.core.util.Utils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

/**
 * @author sahmad
 * @created 22/02/2021
 */
public class HashGenerator {
    private static final String ALGORITHM_NAME = "PBKDF2WithHmacSHA1";
    private static final int ITERATION_COUNT = 16;
    private static final int KEY_LENGTH = 160;
    private static final String SALT_STR = "UNIPROT_SALT";
    private static final byte[] SALT = SALT_STR.getBytes(StandardCharsets.UTF_8);

    public String generateHash(IdMappingJobRequest request) throws ServiceException {
        try {
            char[] requestArray = convertRequestToArray(request);
            PBEKeySpec keySpec = new PBEKeySpec(requestArray, SALT, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM_NAME);
            byte[] hash = skf.generateSecret(keySpec).getEncoded();
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ServiceException("Problem during hash creation", e);
        }
    }

    private char[] convertRequestToArray(IdMappingJobRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getFrom().strip().toLowerCase());
        builder.append(request.getTo().strip().toLowerCase());
        List.of(request.getIds().strip().split(",")).stream()
                .map(String::toLowerCase)
                .map(String::strip)
                .forEach(builder::append);

        if (Utils.notNullNotEmpty(request.getTaxId())) {
            builder.append(request.getTaxId().strip().toLowerCase());
        }

        return builder.toString().toCharArray();
    }
}
