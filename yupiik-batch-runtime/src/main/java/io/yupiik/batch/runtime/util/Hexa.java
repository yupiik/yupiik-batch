/*
 * Copyright (c) 2021-2023 - Yupiik SAS - https://www.yupiik.com
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
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
