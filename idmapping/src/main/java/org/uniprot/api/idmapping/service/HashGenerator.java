package org.uniprot.api.idmapping.service;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.uniprot.api.idmapping.controller.request.IdMappingBasicRequest;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 22/02/2021
 */
public class HashGenerator {
    private final static String ALGORITHM_NAME = "PBKDF2WithHmacSHA1";
    private final static int ITERATION_COUNT = 16;
    private final static int KEY_LENGTH = 160;
    private final static String SALT_STR = "UNIPROT_SALT";
    private final static byte[] SALT = SALT_STR.getBytes(StandardCharsets.UTF_8);

    public String generateHash(IdMappingBasicRequest request)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] requestArray = convertRequestToArray(request);
        PBEKeySpec keySpec = new PBEKeySpec(requestArray, SALT, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM_NAME);
        byte[] hash = skf.generateSecret(keySpec).getEncoded();
        return Hex.encodeHexString(hash);
    }

    private char[] convertRequestToArray(IdMappingBasicRequest request) {
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
