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

package com.hbm.blockentity.machine;

import java.util.Arrays;
import java.util.Random;
import net.minecraft.util.Mth;

public abstract class JointedArm {

    private static final int PIVOT = 0;
    private static final int ARM = 1;
    protected static final int PISTON = 2;
    protected static final int STRIKER = 3;

    public final double[] angles;
    public final double[] prevAngles;
    protected final double[] targets = new double[STRIKER + 1];

    protected JointedArm(int channels) {
        angles = new double[channels];
        prevAngles = new double[channels];
    }

    protected final void snapshot() {
        System.arraycopy(angles, 0, prevAngles, 0, angles.length);
    }

    protected final boolean settle(double swing, double reach, double plunge) {
        return approach(PIVOT, swing)
                & approach(ARM, swing)
                & approach(PISTON, reach)
                & approach(STRIKER, plunge);
    }

    private boolean approach(int joint, double limit) {
        double from = angles[joint], to = targets[joint];
        if (from == to) return true;
        angles[joint] = Math.abs(from - to) <= limit ? to : from < to ? from + limit : from - limit;
        return false;
    }

    protected final void aimAt(double[][] poses, Random rand) {
        var pose = poses[rand.nextInt(poses.length)];
        System.arraycopy(pose, 0, targets, 0, pose.length);
    }

    protected final void home() {
        Arrays.fill(targets, 0D);
    }

    public final void getPositions(float interp, double[] out) {
        interpolate(prevAngles, angles, interp, out);
    }

    public static void interpolate(
            double[] previous, double[] current, float partialTick, double[] output) {
        for (int i = 0; i < current.length; i++)
            output[i] = Mth.lerp(partialTick, previous[i], current[i]);
    }
}
