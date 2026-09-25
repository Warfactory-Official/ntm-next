// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import java.util.Random;
import org.joml.Quaternionf;
import org.joml.Vector3f;

public final class TorexFlash {

    public static final int CONES = 300;

    public static final float[] CORNERS = build();

    private TorexFlash() {}

    private static float[] build() {
        float[] out = new float[CONES * 9];
        Random rand = new Random(432L);
        Quaternionf rot = new Quaternionf();
        Vector3f p1 = new Vector3f(), p2 = new Vector3f(), p3 = new Vector3f();
        for (int i = 0; i < CONES; i++) {

            rot.rotateX((float) Math.toRadians(rand.nextFloat() * 360F));
            rot.rotateY((float) Math.toRadians(rand.nextFloat() * 360F));
            rot.rotateZ((float) Math.toRadians(rand.nextFloat() * 360F));
            rot.rotateX((float) Math.toRadians(rand.nextFloat() * 360F));
            rot.rotateY((float) Math.toRadians(rand.nextFloat() * 360F));
            float len = rand.nextFloat() * 20F + 15F;
            float wid = rand.nextFloat() * 2F + 3F;
            p1.set(-0.866F * wid, len, -0.5F * wid);
            p2.set(0.866F * wid, len, -0.5F * wid);
            p3.set(0F, len, wid);
            rot.transform(p1);
            rot.transform(p2);
            rot.transform(p3);
            int at = i * 9;
            out[at] = 0.2F * p1.x;
            out[at + 1] = 0.2F * p1.y;
            out[at + 2] = 0.2F * p1.z;
            out[at + 3] = 0.2F * p2.x;
            out[at + 4] = 0.2F * p2.y;
            out[at + 5] = 0.2F * p2.z;
            out[at + 6] = 0.2F * p3.x;
            out[at + 7] = 0.2F * p3.y;
            out[at + 8] = 0.2F * p3.z;
        }
        return out;
    }
}
