package io.yupiik.batch.runtime.util;

public final class Passthrough {
    // useful with PG to_json
    // can be registered in h2 using:
    // $ statement.execute("CREATE ALIAS TO_JSON FOR \"io.yupiik.batch.runtime.util.Passthrough.identity\"");
    private Passthrough() {
        // no-op
    }

    public static String identity(final String id) {
        return id;
    }
}
