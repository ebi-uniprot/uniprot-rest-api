package org.uniprot.api.rest.request;

import java.nio.charset.StandardCharsets;
import java.security.NoSuchAlgorithmException;
import java.security.spec.InvalidKeySpecException;
import java.util.function.Function;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import org.apache.commons.codec.binary.Hex;
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

    private final Function<T, char[]> requestToArrayConverter;

    public HashGenerator(Function<T, char[]> requestToArrayConverter, String salt) {
        this.requestToArrayConverter = requestToArrayConverter;
        this.saltBytes = salt.getBytes(StandardCharsets.UTF_8);
    }

    @SuppressWarnings("squid:S2053")
    public String generateHash(T request) throws ServiceException {
        try {
            char[] requestArray = this.requestToArrayConverter.apply(request);

            PBEKeySpec keySpec =
                    new PBEKeySpec(requestArray, this.saltBytes, ITERATION_COUNT, KEY_LENGTH);
            SecretKeyFactory skf = SecretKeyFactory.getInstance(ALGORITHM_NAME);
            byte[] hash = skf.generateSecret(keySpec).getEncoded();
            return Hex.encodeHexString(hash);
        } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
            throw new ServiceException("Problem during hash creation", e);
        }
    }
}
