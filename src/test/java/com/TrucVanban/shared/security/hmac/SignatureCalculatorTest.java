package com.TrucVanban.shared.security.hmac;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SignatureCalculatorTest {

    private SignatureCalculator signatureCalculator;

    @BeforeEach
    void setUp() {
        HmacProperties properties = new HmacProperties();
        properties.setAlgorithm("HmacSHA256");
        signatureCalculator = new SignatureCalculator(properties);
    }

    @Test
    void calculateCanonicalString_emptyQueryAndBody_shouldMatchExpected() {
        String canonical = signatureCalculator.calculateCanonicalString(
                "POST",
                "/ack",
                Map.of(),
                "tvb_live_a1b2c3d4",
                "1753855000",
                "9f2c8e51-1234-5678-90ab-cdef12345678",
                new byte[0]
        );

        String expected = String.join("\n",
                "POST",
                "/ack",
                "",
                "tvb_live_a1b2c3d4",
                "1753855000",
                "9f2c8e51-1234-5678-90ab-cdef12345678",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );

        assertEquals(expected, canonical);
    }

    @Test
    void calculateSignature_shouldReturnExpectedHmacForKnownSecret() {
        String canonical = String.join("\n",
                "POST",
                "/ack",
                "",
                "tvb_live_a1b2c3d4",
                "1753855000",
                "9f2c8e51-1234-5678-90ab-cdef12345678",
                "e3b0c44298fc1c149afbf4c8996fb92427ae41e4649b934ca495991b7852b855"
        );

        String signature = signatureCalculator.calculateSignature("secret123", canonical);

        assertEquals("52f625a5de03f1a0e5fca0705691efcaa197cbb5d43492e245af6a4638d2e3a3", signature);
    }

    @Test
    void calculateCanonicalString_queryParametersAreSortedAndEncoded() {
        String canonical = signatureCalculator.calculateCanonicalString(
                "GET",
                "/abc/def",
                Map.of(
                        "b", new String[]{"value2"},
                        "a", new String[]{"value 1"}
                ),
                "some-api-key",
                "1234567890",
                "nonce-value",
                new byte[0]
        );

        String expectedQuery = "a=value+1&b=value2";
        String[] parts = canonical.split("\n", -1);

        assertEquals("GET", parts[0]);
        assertEquals("/abc/def", parts[1]);
        assertEquals(expectedQuery, parts[2]);
    }
}
