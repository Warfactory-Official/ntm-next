/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: JCTools contributors
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) JCTools contributors. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

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
