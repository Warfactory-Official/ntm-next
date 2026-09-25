// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.render.item.weapon.sedna;

import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.loader.UnitQuad;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import org.joml.Matrix4fc;
import org.joml.Vector3fc;

public interface RestBody {

    void part(
            Matrix4fc pose,
            HFRWavefrontObject model,
            int part,
            Identifier texture,
            int color,
            boolean fullBright);

    default void part(Matrix4fc pose, HFRWavefrontObject model, int part, Identifier texture) {
        part(pose, model, part, texture, -1, false);
    }

    default void model(Matrix4fc pose, HFRWavefrontObject model, Identifier texture) {
        for (int part = 0; part < model.groups.length; part++) part(pose, model, part, texture);
    }

    void quad(Matrix4fc pose, Matrix4fc corners, UnitQuad quad, Identifier texture);

    void balefireGlint(Matrix4fc pose, HFRWavefrontObject model, int part);

    void text(Matrix4fc before, Vector3fc shift, Matrix4fc after, String text, int color);

    void spinZ(Matrix4fc pivot, double msPerDegree, Consumer<RestBody> turned);
}
