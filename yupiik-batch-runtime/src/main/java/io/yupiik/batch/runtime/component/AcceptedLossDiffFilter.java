/*
 * Copyright (c) 2021-2022 - Yupiik SAS - https://www.yupiik.com
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
package io.yupiik.batch.runtime.component;

import io.yupiik.batch.runtime.component.diff.Diff;
import io.yupiik.batch.runtime.documentation.Component;

import java.util.function.Predicate;
import java.util.logging.Logger;

@Component("""
        Enables to filter a `Diff`. If the `acceptedLoss` is not reached, i.e. more row would be deleted than this percentage, the chain will end there.

        Goal is to not delete a database if incoming data are corrupted or not properly up to date.""")
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
