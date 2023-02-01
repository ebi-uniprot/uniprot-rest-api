package org.uniprot.api.rest.download.model;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.Objects;
import java.util.function.Function;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;
import org.springframework.http.MediaType;
import org.uniprot.api.common.exception.ServiceException;

/**
 * @author sahmad
 * @created 22/02/2021
 */
public class HashGenerator<T> {
    private static final String ALGORITHM_NAME = "PBKDF2WithHmacSHA1";
    private static final int ITERATION_COUNT = 16;
    private static final int KEY_LENGTH = 160;
    private final byte[] saltBytes;

    private Function<T, char[]> requestToArrayConverter;

    public HashGenerator(Function<T, char[]> requestToArrayConverter, String salt) {
        this.requestToArrayConverter = requestToArrayConverter;
        this.saltBytes = salt.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("squid:S2053")
    public String generateHash(T request) throws ServiceException {
        return generateHash(request, null);
    }

    public String generateHash(T request, MediaType contentType){
        try {
            Function<T, char[]> function = this.requestToArrayConverter.andThen(r -> appendContentType(r, contentType));
            char[] requestArray = function.apply(request);
            PBEKeySpec keySpec =
                    new PBEKeySpec(requestArray, this.saltBytes, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM_NAME);
            byte[] hash = skf.generateSecret(keySpec).getEncoded();
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ServiceException("Problem during hash creation", e);
        }
    }

    private char[] appendContentType(char[] requestArray, MediaType contentType) {
        char[] updatedArray = requestArray;
        if(Objects.nonNull(contentType)){
            String origRequest = new String(updatedArray);
            StringBuilder requestWithType = new StringBuilder(origRequest);
            requestWithType.append(contentType);
            updatedArray = requestWithType.toString().toCharArray();
        }
        return updatedArray;
    }
}
