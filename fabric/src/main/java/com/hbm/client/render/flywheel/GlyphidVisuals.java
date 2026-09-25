// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.glyphid.EntityGlyphid;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.world.entity.EntityType;

public final class GlyphidVisuals {
    private static final Predicate<EntityGlyphid> DRAWN = MobVisuals.unvanished(entity -> true);

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

    private static <T extends EntityGlyphid> void glyphid(
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
