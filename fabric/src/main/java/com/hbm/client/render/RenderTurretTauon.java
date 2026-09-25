// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.main.ResourceManager;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.tileentity.turret.BlockEntityTurretTauon;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public class RenderTurretTauon extends RenderTurretBase<BlockEntityTurretTauon> {

    private static final int CARRIAGE = ResourceManager.turret_chekhov.partId("Carriage");
    private static final int CANNON = ResourceManager.turret_tauon.partId("Cannon");
    private static final int ROTOR = ResourceManager.turret_tauon.partId("Rotor");

    @Override
    protected void emit(BlockEntityTurretTauon turret, State state, TurretFrame frame) {
        connectors(frame, turret.connectorMask);
        Matrix4f cannon =
                mount(
                        frame,
                        state,
                        ResourceManager.turret_chekhov,
                        CARRIAGE,
                        ResourceManager.turret_carriage_tex,
                        1.5F,
                        ResourceManager.turret_tauon,
                        CANNON,
                        ResourceManager.turret_tauon_tex);

        float spin = Mth.lerp(state.partialTicks, turret.lastSpin, turret.spin);
        TurretFrame.Part rotor =
                frame.push()
                        .obj(ResourceManager.turret_tauon, ROTOR)
                        .texture(ResourceManager.turret_tauon_tex);
        rotor.pose
                .set(cannon)
                .translate(0F, 1.375F, 0F)
                .rotateX(-spin * Mth.DEG_TO_RAD)
                .translate(0F, -1.375F, 0F);

        if (turret.beam <= 0) return;
        state.beam = true;
        state.beamLength = turret.lastDist;
        state.beamSegments = (int) turret.lastDist + 1;
        state.effect.set(cannon).translate(0F, 1.5F, 0F);
    }

    @Override
    protected void submitEffects(State state, PoseStack poseStack, SubmitNodeCollector collector) {
        if (!state.beam) return;

        poseStack.pushPose();
        poseStack.mulPose(state.effect);
        int start = (int) (state.gameTime / 5 % 360);
        BeamPronter.prontBeam(
                poseStack,
                collector,
                new Vec3(state.beamLength, 0D, 0D),
                EnumWaveType.RANDOM,
                EnumBeamType.LINE,
                0xffa200,
                0xffd000,
                start,
                state.beamSegments,
                0.1F,
                0,
                0);
        poseStack.popPose();
    }
}
