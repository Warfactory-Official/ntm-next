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
