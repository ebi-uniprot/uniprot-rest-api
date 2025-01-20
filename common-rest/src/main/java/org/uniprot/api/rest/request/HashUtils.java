package org.uniprot.api.rest.request;

import java.math.BigInteger;

public class HashUtils {
    private static final String BASE62_CHARS =
            "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";

    public static String toBase62(byte[] hash) {
        BigInteger bigInt = new BigInteger(1, hash);
        StringBuilder base62 = new StringBuilder();

        while (bigInt.compareTo(BigInteger.ZERO) > 0) {
            int remainder = bigInt.mod(BigInteger.valueOf(62)).intValue();
            base62.append(BASE62_CHARS.charAt(remainder));
            bigInt = bigInt.divide(BigInteger.valueOf(62));
        }
        return base62.reverse().toString();
    }
}
