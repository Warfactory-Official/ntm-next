// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.queues;

import java.util.function.LongConsumer;
import java.util.function.LongSupplier;

public interface LongMessagePassingQueue {
    boolean offer(long value);

    long poll();

    long peek();

    long relaxedPoll();

    long relaxedPeek();

    int drain(LongConsumer consumer, int limit);

    int drain(LongConsumer consumer);

    int fill(LongSupplier supplier, int limit);

    int fill(LongSupplier supplier);

    int fill(long[] source, int offset, int length);

    int size();

    boolean isEmpty();

    void clear();

    long currentProducerIndex();

    long currentConsumerIndex();
}
