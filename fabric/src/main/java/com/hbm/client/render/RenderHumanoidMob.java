// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityDummy;
import com.hbm.entity.mob.EntityFBI;
import com.hbm.entity.mob.EntityGhost;
import com.hbm.main.ResourceManager;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.function.Function;
import net.minecraft.client.model.HumanoidModel;
import net.minecraft.client.model.geom.builders.CubeDeformation;
import net.minecraft.client.model.geom.builders.LayerDefinition;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.HumanoidMobRenderer;
import net.minecraft.client.renderer.entity.state.HumanoidRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Mob;

public class RenderHumanoidMob<T extends Mob>
        extends HumanoidMobRenderer<T, HumanoidRenderState, HumanoidModel<HumanoidRenderState>>
        implements ConcurrentRenderStateExtraction {

    private final Identifier texture;

    private RenderHumanoidMob(
            EntityRendererProvider.Context context,
            Identifier texture,
            Function<Identifier, RenderType> renderType) {
        super(
                context,
                new HumanoidModel<>(
                        LayerDefinition.create(
                                        HumanoidModel.createMesh(CubeDeformation.NONE, 0F), 64, 32)
                                .bakeRoot(),
                        renderType),
                0.5F);
        this.texture = texture;
    }

    public static RenderHumanoidMob<EntityGhost> ghost(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(
                context, ResourceManager.ghost_tex, RenderTypes::entityTranslucent);
    }

    public static RenderHumanoidMob<EntityDummy> dummy(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(
                context, ResourceManager.dummy_tex, RenderTypes::entityCutout);
    }

    public static RenderHumanoidMob<EntityFBI> fbi(EntityRendererProvider.Context context) {
        return new RenderHumanoidMob<>(context, ResourceManager.fbi_tex, RenderTypes::entityCutout);
    }

    @Override
    public HumanoidRenderState createRenderState() {
        return new HumanoidRenderState();
    }

    @Override
    public Identifier getTextureLocation(HumanoidRenderState state) {
        return texture;
    }
}
