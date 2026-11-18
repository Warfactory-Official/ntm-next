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

package com.hbm.util.datafix;

import com.mojang.datafixers.schemas.Schema;
import com.mojang.serialization.Dynamic;
import java.util.Set;
import java.util.stream.IntStream;

final class ProjectileStateFix extends EntityTagFix {

    private static final Set<String> THROWABLES =
            Set.of(
                    "hbm:entity_acid_bomb",
                    "hbm:entity_artillery_shell",
                    "hbm:entity_himars",
                    "hbm:entity_stray_cog",
                    "hbm:disperser_canister",
                    "hbm:grenade_universal",
                    "hbm:entity_missile_anti",
                    "hbm:entity_miner_lander",
                    "hbm:entity_stray_saw",
                    "hbm:entity_siege_laser",
                    "hbm:entity_waste_pearl");
    private static final Set<String> LEGACY_BULLETS =
            Set.of("hbm:entity_bullet", "hbm:entity_beam_bomb");

    ProjectileStateFix(Schema schema) {
        super(schema);
    }

    @Override
    protected Dynamic<?> fixEntity(Dynamic<?> tag) {
        String id = tag.get("id").asString("");
        boolean bullet = LEGACY_BULLETS.contains(id);
        if (!bullet && !THROWABLES.contains(id)) return tag;
        var fixed = rename(tag, bullet ? "life" : "ticksInGround", "GroundAge");
        if (!bullet) fixed = rename(fixed, "ticksInAir", "AirAge");
        if (fixed.get("Attached").result().isPresent() || fixed.get("inGround").result().isEmpty())
            return fixed;
        var tile = fixed.get("inTile");
        var block =
                bullet
                        ? tile.result()
                                .map(BlockStateEncoding14::name)
                                .filter(name -> !name.isEmpty())
                        : tile.asString().result();
        boolean attached = fixed.get("inGround").asBoolean(false) && block.isPresent();
        fixed = fixed.set("Attached", fixed.createBoolean(attached));
        if (attached) {
            fixed =
                    fixed.set(
                            "AttachedPos",
                            fixed.createIntList(
                                    IntStream.of(
                                            fixed.get("xTile").asInt(0),
                                            fixed.get("yTile").asInt(0),
                                            fixed.get("zTile").asInt(0))));
            fixed = fixed.set("AttachedBlock", fixed.createString(block.orElseThrow()));
        }
        return fixed.remove("inGround")
                .remove("inTile")
                .remove("xTile")
                .remove("yTile")
                .remove("zTile");
    }
}
