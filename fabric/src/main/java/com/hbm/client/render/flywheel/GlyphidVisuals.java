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

package com.hbm.client.render.flywheel;

import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.GlyphidEntity;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;

public final class GlyphidVisuals {
    private static final Predicate<GlyphidEntity> DRAWN = MobVisuals.unvanished(entity -> true);

    private GlyphidVisuals() {}

    public static void register() {
        glyphid(ModEntities.GLYPHID.get(), () -> GlyphidVisual.GLYPHID);
        glyphid(ModEntities.GLYPHID_SCOUT.get(), () -> GlyphidVisual.GLYPHID_SCOUT);
        glyphid(ModEntities.GLYPHID_BLASTER.get(), () -> GlyphidVisual.GLYPHID_BLASTER);
        glyphid(ModEntities.GLYPHID_BOMBARDIER.get(), () -> GlyphidVisual.GLYPHID_BOMBARDIER);
        glyphid(ModEntities.GLYPHID_BRAWLER.get(), () -> GlyphidVisual.GLYPHID_BRAWLER);
        glyphid(ModEntities.GLYPHID_BRENDA.get(), () -> GlyphidVisual.GLYPHID_BRENDA);
        glyphid(ModEntities.GLYPHID_DIGGER.get(), () -> GlyphidVisual.GLYPHID_DIGGER);
        SimpleEntityVisualizer.builder(ModEntities.GLYPHID_NUCLEAR.get())
                .factory(GlyphidNuclearVisual::new)
                .skipVanillaRender(DRAWN::test)
                .apply();
        glyphid(ModEntities.GLYPHID_BEHEMOTH.get(), () -> GlyphidVisual.GLYPHID_BEHEMOTH);
    }

    private static <T extends GlyphidEntity> void glyphid(
            EntityType<T> type, Supplier<Model[]> source) {
        var binding = new Binding();
        FlywheelResources.onInitialize(() -> binding.models = source.get());
        SimpleEntityVisualizer.builder(type)
                .factory(
                        (ctx, entity, partialTick) ->
                                new GlyphidVisual(ctx, entity, partialTick, binding.models))
                .skipVanillaRender(DRAWN::test)
                .apply();
    }

    private static final class Binding {
        Model[] models;
    }
}
