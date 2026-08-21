package com.financeiro.idempotency.application;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;

public final class Sha256Fingerprint {

    private Sha256Fingerprint() {
    }

    public static String fromOrderedComponents(List<String> components) {
        if (components == null || components.isEmpty()) {
            throw new IllegalArgumentException("fingerprint components must not be empty");
        }

        MessageDigest digest = sha256();
        for (String component : components) {
            if (component == null) {
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(-1).array());
            } else {
                byte[] bytes = component.getBytes(StandardCharsets.UTF_8);
                digest.update(ByteBuffer.allocate(Integer.BYTES).putInt(bytes.length).array());
                digest.update(bytes);
            }
        }
        return HexFormat.of().formatHex(digest.digest());
    }

    private static MessageDigest sha256() {
        try {
            return MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 must be available in the JDK", exception);
        }
    }
}
