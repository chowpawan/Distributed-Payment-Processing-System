package com.paymentprocessor.notification;

import com.paymentprocessor.notification.webhook.WebhookSignatureSigner;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class WebhookSignatureSignerTest {

    private final WebhookSignatureSigner signer = new WebhookSignatureSigner();

    @Test
    void sign_producesHmacSha256Prefix() {
        String signature = signer.sign("{\"id\":\"pay-001\"}", "secret");
        assertTrue(signature.startsWith("sha256="));
    }

    @Test
    void sign_samePayloadAndSecret_producesConsistentSignature() {
        String payload = "{\"paymentId\":\"pay-001\",\"status\":\"COMPLETED\"}";
        String secret = "test-secret";

        String sig1 = signer.sign(payload, secret);
        String sig2 = signer.sign(payload, secret);

        assertEquals(sig1, sig2);
    }

    @Test
    void sign_differentPayloads_produceDifferentSignatures() {
        String secret = "test-secret";
        String sig1 = signer.sign("{\"id\":\"pay-001\"}", secret);
        String sig2 = signer.sign("{\"id\":\"pay-002\"}", secret);

        assertNotEquals(sig1, sig2);
    }

    @Test
    void sign_differentSecrets_produceDifferentSignatures() {
        String payload = "{\"id\":\"pay-001\"}";
        String sig1 = signer.sign(payload, "secret-a");
        String sig2 = signer.sign(payload, "secret-b");

        assertNotEquals(sig1, sig2);
    }

    @Test
    void sign_knownVector() {
        // HMAC-SHA256("Hello", "key") = 9307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b
        String sig = signer.sign("Hello", "key");
        assertEquals("sha256=9307b3b915efb5171ff14d8cb55fbcc798c6c0ef1456d66ded1a6aa723a58b7b", sig);
    }
}
