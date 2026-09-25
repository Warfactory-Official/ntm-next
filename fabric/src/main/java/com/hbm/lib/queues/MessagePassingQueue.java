// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: JCTools contributors
// SPDX-License-Identifier: Apache-2.0 AND LGPL-3.0-only

package com.hbm.lib.queues;

public interface MessagePassingQueue<E> {
    int UNBOUNDED_CAPACITY = -1;

    boolean offer(E e);

    E poll();

    E peek();

    int size();

    void clear();

    boolean isEmpty();

    int capacity();

    boolean relaxedOffer(E e);

    E relaxedPoll();

    E relaxedPeek();

    int drain(Consumer<E> consumer, int limit);

    int fill(Supplier<E> supplier, int limit);

    int drain(Consumer<E> consumer);

    int fill(Supplier<E> supplier);

    void drain(Consumer<E> consumer, WaitStrategy wait, ExitCondition exit);

    void fill(Supplier<E> supplier, WaitStrategy wait, ExitCondition exit);

    @FunctionalInterface
    interface Supplier<E> {

        E get();
    }

    @FunctionalInterface
    interface Consumer<E> {

        void accept(E value);
    }

    @FunctionalInterface
    interface WaitStrategy {
        int idle(int idleCounter);
    }

    @FunctionalInterface
    interface ExitCondition {
        boolean keepRunning();
    }
}
