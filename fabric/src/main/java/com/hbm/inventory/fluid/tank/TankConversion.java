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

package com.hbm.inventory.fluid.tank;

public final class TankConversion {

    private TankConversion() {}

    public static int room(FluidTankNTM out, int yield) {
        return yield == 0 ? Integer.MAX_VALUE : (out.getMaxFill() - out.getFill()) / yield;
    }

    public static int run(FluidTankNTM in, int cost, FluidTankNTM out, int yield, int limit) {
        int ops = Math.min(Math.min(in.getFill() / cost, room(out, yield)), limit);
        if (ops <= 0) return 0;
        in.setFill(in.getFill() - cost * ops);
        out.setFill(out.getFill() + yield * ops);
        return ops;
    }

    public static int run(
            FluidTankNTM in,
            int cost,
            int limit,
            FluidTankNTM outA,
            int yieldA,
            FluidTankNTM outB,
            int yieldB) {
        int ops =
                Math.min(
                        Math.min(
                                in.getFill() / cost,
                                Math.min(room(outA, yieldA), room(outB, yieldB))),
                        limit);
        if (ops <= 0) return 0;
        in.setFill(in.getFill() - cost * ops);
        outA.setFill(outA.getFill() + yieldA * ops);
        outB.setFill(outB.getFill() + yieldB * ops);
        return ops;
    }
}
