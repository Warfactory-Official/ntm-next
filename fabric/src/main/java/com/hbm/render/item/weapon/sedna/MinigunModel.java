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

import com.hbm.main.ResourceManager;
import com.mojang.math.Axis;
import net.minecraft.resources.Identifier;

final class MinigunModel {

    static final GunAnimationChannels RIG = new GunAnimationChannels();
    private static final GunAnimationChannels.Scalar DRAW = RIG.x("EQUIP");
    private static final GunAnimationChannels.Scalar KICK = RIG.z("RECOIL");
    static final GunAnimationChannels.Scalar SPOOL = RIG.z("ROTATE");

    private MinigunModel() {}

    static GunRenderDefinition.Builder frame(GunRenderDefinition.Builder b, Identifier sheet) {
        return b.mesh(ResourceManager.minigun)
                .skin(sheet)
                .hinge(0, 3, -6, Axis.XP, DRAW)
                .slideZ(KICK);
    }
}
