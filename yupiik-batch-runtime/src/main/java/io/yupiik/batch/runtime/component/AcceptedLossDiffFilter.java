package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.diff.Diff;

import java.util.function.Predicate;
import java.util.logging.Logger;

public class AcceptedLossDiffFilter<A> implements Predicate<Diff<A>> {
    private final Logger logger = Logger.getLogger(getClass().getName());
    private final double acceptedLoss;

    public AcceptedLossDiffFilter(final double acceptedLoss) {
        this.acceptedLoss = acceptedLoss;
    }

    @Override
    public boolean test(final Diff<A> referenceRowDiff) {
        if (referenceRowDiff.initialTotal() == 0) {
            logger.info(() -> "[F] No initial data, applying diff");
            return true;
        }

        final long newSize = referenceRowDiff.initialTotal() - referenceRowDiff.deleted().size() + referenceRowDiff.added().size();
        if (newSize > referenceRowDiff.initialTotal()) {
            logger.info(() -> "[F] No loss, applying diff");
            return true;
        }

        final var changePc = (referenceRowDiff.initialTotal() - newSize) * 1. / referenceRowDiff.initialTotal();
        if (changePc > acceptedLoss) {
            logger.info(() -> "[F] Too much change, accepted " +
                    (acceptedLoss * 100) + "% but got " +
                    (changePc * 100) + "% (" + referenceRowDiff.initialTotal() + " -> " + newSize + ")");
            return false;
        }
        return true;
    }

    public static <A> AcceptedLossDiffFilter<A> withAcceptedLoss(final double value) {
        return new AcceptedLossDiffFilter<>(value);
    }
}
