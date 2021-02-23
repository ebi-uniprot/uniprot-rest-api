package org.uniprot.api.idmapping.service;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.uniprot.api.idmapping.controller.request.IdMappingRequest;
import org.uniprot.core.util.Utils;

/**
 * @author sahmad
 * @created 22/02/2021
 */
public class HashGenerator {
    private final String ALGORITHM_NAME = "PBKDF2WithHmacSHA1";
    private final int ITERATION_COUNT = 16;
    private final int KEY_LENGTH = 160;
    private final String SALT_STR = "UNIPROT_SALT";
    private final byte[] SALT = SALT_STR.getBytes(StandardCharsets.UTF_8);

    public String generateHash(IdMappingRequest request)
            throws NoSuchAlgorithmException, InvalidKeySpecException {
        char[] requestArray = convertRequestToArray(request);
        PBEKeySpec keySpec = new PBEKeySpec(requestArray, SALT, ITERATION_COUNT, KEY_LENGTH);
        SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM_NAME);
        byte[] hash = skf.generateSecret(keySpec).getEncoded();
        return Hex.encodeHexString(hash);
    }

    private char[] convertRequestToArray(IdMappingRequest request) {
        StringBuilder builder = new StringBuilder();
        builder.append(request.getFrom().strip().toLowerCase());
        builder.append(request.getTo().strip().toLowerCase());
        // sort list of ids and append them into builder
        List<String> sortedIds = Arrays.asList(request.getIds().strip().split(","));
        Collections.sort(sortedIds); // remove it
        sortedIds.stream().map(String::toLowerCase).map(String::strip).forEach(builder::append);

        if (Utils.notNullNotEmpty(request.getTaxId())) {
            builder.append(request.getTaxId().strip().toLowerCase());
        }

        return builder.toString().toCharArray();
    }
}
