package org.uniprot.api.rest.request;

import static org.junit.jupiter.api.Assertions.*;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import org.junit.jupiter.api.Test;

class HashUtilsTest {
    @Test
    public void testToBase62_ValidInput() {
        byte[] hash = new byte[] {(byte) 0x12, (byte) 0xAB, (byte) 0x34};
        String base62 = HashUtils.toBase62(hash);
        assertEquals("58hu", base62, "Base62 conversion failed for valid input");
    }

    @Test
    public void testToBase62_EmptyInput() {
        byte[] hash = new byte[] {};
        String base62 = HashUtils.toBase62(hash);
        assertEquals("", base62, "Base62 conversion should return an empty string for empty input");
    }

    @Test
    public void testToBase62_SingleByte() {
        byte[] hash = new byte[] {(byte) 0x7F};
        String base62 = HashUtils.toBase62(hash);
        assertEquals("23", base62, "Base62 conversion failed for single byte input");
    }

    @Test
    public void testToBase62_LargeInput() throws NoSuchAlgorithmException {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        byte[] hash = md.digest("example input".getBytes());
        String base62 = HashUtils.toBase62(hash);

        // Check for non-null and non-empty output
        assertNotNull(base62, "Base62 conversion result should not be null");
        assertFalse(base62.isEmpty(), "Base62 conversion result should not be empty");
        assertTrue(
                base62.matches("[0-9a-zA-Z]+"),
                "Base62 result should only contain valid Base62 characters");
    }

    @Test
    public void testToBase62_EdgeCaseZero() {
        byte[] hash = new byte[] {0, 0, 0}; // Edge case: All zero bytes
        String base62 = HashUtils.toBase62(hash);
        assertEquals("", base62, "Base62 conversion failed for input with all zero bytes");
    }

    @Test
    public void testToBase62_NegativeBytes() {
        byte[] hash = new byte[] {(byte) -1, (byte) -2, (byte) -3}; // Negative byte values
        String base62 = HashUtils.toBase62(hash);
        assertNotNull(base62, "Base62 conversion should handle negative byte values");
        assertTrue(
                base62.matches("[0-9a-zA-Z]+"),
                "Base62 result should only contain valid Base62 characters");
    }
}
