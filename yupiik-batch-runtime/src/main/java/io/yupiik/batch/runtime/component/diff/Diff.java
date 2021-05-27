package io.yupiik.batch.runtime.component.diff;

import io.yupiik.batch.runtime.batch.builder.BatchChain;

import java.util.Collection;

public record Diff<T>(Collection<T> deleted, Collection<T> added, Collection<T> updated,
                      long initialTotal) implements BatchChain.Commentifiable {
    @Override
    public String toComment() {
        return "deleted: " + deleted.size() + ", added: " + added.size() + ", updated: " + updated.size() + ", initial-size=" + initialTotal;
    }
}