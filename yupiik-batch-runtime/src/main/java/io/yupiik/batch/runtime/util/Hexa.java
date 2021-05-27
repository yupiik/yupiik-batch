package io.yupiik.batch.runtime.util;

import java.util.stream.IntStream;

import static java.util.stream.Collectors.joining;

public final class Hexa {
    private Hexa() {
        // no-op
    }

    public static byte[] toByteArray(final String hash) {
        if (hash.length() % 2 != 0) {
            throw new IllegalArgumentException("Invalid hex string parity: '" + hash + "', size=" + hash.length());
        }
        final var out = new byte[hash.length() / 2];
        for (int i = 0; i < hash.length(); i += 2) {
            out[i / 2] = (byte) (toBin(hash.charAt(i)) * 16 + toBin(hash.charAt(i + 1)));
        }
        return out;
    }

    public static String toHex(final byte[] hash) {
        return IntStream.range(0, hash.length)
                .mapToObj(i -> {
                    final byte b = hash[i];
                    final var s = Integer.toHexString(b & 0xff);
                    return (s.length() == 1 ? "0" : "") + s;
                })
                .collect(joining());
    }

    private static int toBin(final char ch) {
        if ('0' <= ch && ch <= '9') {
            return ch - '0';
        }
        if ('A' <= ch && ch <= 'F') {
            return ch - 'A' + 10;
        }
        if ('a' <= ch && ch <= 'f') {
            return ch - 'a' + 10;
        }
        throw new IllegalArgumentException("Illegal character for hexa: " + ch);
    }
}
