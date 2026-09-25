// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderRADBeast extends MobRenderer<EntityRADBeast, RenderRADBeast.State, BlazeModel>
        implements ConcurrentRenderStateExtraction {

    private static final int COLOR = 0x004000;
    private static final double ORIGIN = 1.25D;

    public RenderRADBeast(EntityRendererProvider.Context context) {
        super(context, new BlazeModel(context.bakeLayer(ModelLayers.BLAZE)), 0.5F);
    }

    @Override
    protected int getBlockLightLevel(EntityRADBeast entity, BlockPos blockPos) {
        return 15;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRADBeast entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.beam = null;
        Entity victim = entity.getUnfortunateSoul();
        if (victim != null && entity.getY() > 0.1D) {
            double targetY = victim.getY() + victim.getBbHeight() * 0.5D;

            if (victim == Minecraft.getInstance().player) targetY -= 1.5D;
            Vec3 skeleton =
                    new Vec3(
                            victim.getX() - entity.getX(),
                            targetY - (entity.getY() + ORIGIN),
                            victim.getZ() - entity.getZ());
            if (skeleton.length() < 200D) state.beam = skeleton;
        }
        state.start = (int) entity.level().getGameTime() % 1000 + 1;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        if (state.beam != null) {
            poseStack.pushPose();
            poseStack.translate(0D, ORIGIN, 0D);
            BeamPronter.prontBeam(
                    poseStack,
                    collector,
                    state.beam,
                    EnumWaveType.RANDOM,
                    EnumBeamType.SOLID,
                    COLOR,
                    COLOR,
                    state.start,
                    (int) (state.beam.length() * 5D),
                    0.125F,
                    2,
                    0.03125F);
            poseStack.popPose();
        }
        super.submit(state, poseStack, collector, camera);
    }

    @Override
    public Identifier getTextureLocation(State state) {
        return ResourceManager.radbeast_tex;
    }

    public static final class State extends LivingEntityRenderState {
        @Nullable Vec3 beam;
        int start;
    }
}
