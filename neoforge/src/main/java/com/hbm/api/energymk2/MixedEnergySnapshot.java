/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
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

package com.hbm.api.energymk2;

import com.hbm.uninos.graph.BufferPool;
import com.hbm.uninos.graph.Distribution.Total;
import java.util.Arrays;

public final class MixedEnergySnapshot implements BufferPool.Observer<EnergyHandler> {
    final Total ordinary = new Total();
    final Total demand = new Total();
    long[] stock = new long[4];
    long[] capacity = new long[4];
    long[] input = new long[4];
    long[] output = new long[4];
    long[] target = new long[4];
    Total[] stockExact = new Total[4];
    Total[] capacityExact = new Total[4];
    Total[] targetExact = new Total[4];
    int[] tier = new int[4];
    int buffers;
    long debt;

    private static long scaled(long value, long multiplier) {
        return value <= Long.MAX_VALUE / multiplier ? value * multiplier : Long.MAX_VALUE;
    }

    private static void addScaled(Total total, long value, long multiplier) {
        if (value <= Long.MAX_VALUE / multiplier) total.add(value * multiplier);
        else total.add(new Total().set(value).scale(multiplier, 1));
    }

    public void source(long he) {
        addScaled(ordinary, he, EnergyConversion.feRatio());
    }

    public void sourceFe(long fe) {
        addScaled(ordinary, fe, EnergyConversion.heRatio());
    }

    public void demandFe(long fe) {
        addScaled(demand, fe, EnergyConversion.heRatio());
    }

    public void demand(long he) {
        addScaled(demand, he, EnergyConversion.feRatio());
    }

    public void debt(long he) {
        debt = scaled(he, EnergyConversion.feRatio());
    }

    @Override
    public void accept(EnergyHandler handler, int priority, long output, long input) {
        buffer(handler.getPower(), handler.getMaxPower(), input, output, priority);
    }

    public void buffer(long he, long maxHe, long maxInput, long maxOutput, int priority) {
        appendScaled(
                he,
                maxHe,
                Math.min(maxInput, maxHe - he),
                Math.min(maxOutput, he),
                priority,
                EnergyConversion.feRatio());
    }

    public void bufferFe(long fe, long maxFe, long maxInputFe, long maxOutputFe, int priority) {
        appendScaled(fe, maxFe, maxInputFe, maxOutputFe, priority, EnergyConversion.heRatio());
    }

    private void appendScaled(
            long value,
            long maxValue,
            long maxInput,
            long maxOutput,
            int priority,
            long multiplier) {
        long stored = scaled(value, multiplier);
        long maximum = scaled(maxValue, multiplier);
        int index = buffers;
        append(
                stored,
                maximum,
                scaled(maxInput, multiplier),
                scaled(maxOutput, multiplier),
                priority);
        if (stored == Long.MAX_VALUE)
            stockExact[index] = new Total().set(value).scale(multiplier, 1);
        if (maximum == Long.MAX_VALUE)
            capacityExact[index] = new Total().set(maxValue).scale(multiplier, 1);
    }

    private void append(long he, long maxHe, long maxInput, long maxOutput, int priority) {
        if (buffers == stock.length) {
            int size = buffers * 2;
            stock = Arrays.copyOf(stock, size);
            capacity = Arrays.copyOf(capacity, size);
            input = Arrays.copyOf(input, size);
            output = Arrays.copyOf(output, size);
            target = Arrays.copyOf(target, size);
            stockExact = Arrays.copyOf(stockExact, size);
            capacityExact = Arrays.copyOf(capacityExact, size);
            targetExact = Arrays.copyOf(targetExact, size);
            tier = Arrays.copyOf(tier, size);
        }
        stock[buffers] = he;
        capacity[buffers] = maxHe;
        input[buffers] = maxInput;
        output[buffers] = maxOutput;
        tier[buffers++] = priority;
    }

    Total stock(int index, Total scratch) {
        return stockExact[index] == null ? scratch.set(stock[index]) : stockExact[index];
    }

    Total capacity(int index, Total scratch) {
        return capacityExact[index] == null ? scratch.set(capacity[index]) : capacityExact[index];
    }

    Total target(int index, Total scratch) {
        return targetExact[index] == null ? scratch.set(target[index]) : targetExact[index];
    }

    void debit(int index, long amount) {
        output[index] -= amount;
        if (stockExact[index] == null) stock[index] -= amount;
        else stock[index] = stockExact[index].subtract(amount).limit();
    }

    void credit(int index, long amount) {
        input[index] -= amount;
        if (stockExact[index] == null && Long.MAX_VALUE - stock[index] < amount)
            stockExact[index] = new Total().set(stock[index]);
        if (stockExact[index] == null) stock[index] += amount;
        else stock[index] = stockExact[index].add(amount).limit();
    }
}
