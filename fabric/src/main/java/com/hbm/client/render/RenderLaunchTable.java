// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.MissileStruct;
import com.hbm.items.weapon.ItemCustomMissilePart.PartSize;
import com.hbm.items.weapon.ItemCustomMissilePart;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.bomb.BlockEntityLaunchTable;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public final class RenderLaunchTable
        implements BlockEntityRenderer<BlockEntityLaunchTable, RenderLaunchTable.State>,
                ConcurrentRenderStateExtraction {

    private static final RenderType SMALL_PAD = cutout(ResourceManager.launch_table_small_pad_tex);
    private static final RenderType LARGE_PAD = cutout(ResourceManager.launch_table_large_pad_tex);
    private static final RenderType SMALL_SCAFFOLD =
            cutout(ResourceManager.launch_table_small_scaffold_base_tex);
    private static final RenderType SMALL_CONNECTOR =
            cutout(ResourceManager.launch_table_small_scaffold_connector_tex);
    private static final RenderType LARGE_SCAFFOLD =
            cutout(ResourceManager.launch_table_large_scaffold_base_tex);
    private static final RenderType LARGE_CONNECTOR =
            cutout(ResourceManager.launch_table_large_scaffold_connector_tex);

    private static RenderType cutout(Identifier texture) {
        return RenderTypes.entityCutoutCull(texture);
    }

    private final MissilePronter pronter = new MissilePronter();

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public int getViewDistance() {
        return 256;
    }

    @Override
    public void extractRenderState(
            BlockEntityLaunchTable table,
            State state,
            float partialTicks,
            Vec3 camera,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                table, state, partialTicks, camera, breakProgress);
        state.facing = BlockMultiblockCore.coreFacing(table.getBlockState());
        state.padSize = table.padSize;
        state.missileValid = table.missileValid;
        state.parts = table.loadedMissile;

        if (state.parts.fuselage() != null) table.height = (int) state.parts.height();
        state.height = table.height;
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        boolean large = state.padSize == PartSize.SIZE_20;
        int light = state.lightCoords;

        poseStack.pushPose();
        poseStack.translate(0.5D, 0D, 0.5D);
        poseStack.mulPose(Axis.YP.rotationDegrees(Facing.yaw(state.facing, 90)));

        draw(
                poseStack,
                collector,
                light,
                large ? LARGE_PAD : SMALL_PAD,
                large
                        ? ResourceManager.launch_table_large_pad
                        : ResourceManager.launch_table_small_pad);

        boolean smallScaffold = state.padSize == PartSize.SIZE_10;
        RenderType scaffoldType = smallScaffold ? SMALL_SCAFFOLD : LARGE_SCAFFOLD;
        RenderType connectorType = smallScaffold ? SMALL_CONNECTOR : LARGE_CONNECTOR;
        HFRWavefrontObject base =
                smallScaffold
                        ? ResourceManager.launch_table_small_scaffold_base
                        : ResourceManager.launch_table_large_scaffold_base;
        HFRWavefrontObject connector =
                smallScaffold
                        ? ResourceManager.launch_table_small_scaffold_connector
                        : ResourceManager.launch_table_large_scaffold_connector;
        HFRWavefrontObject empty =
                smallScaffold
                        ? ResourceManager.launch_table_small_scaffold_empty
                        : ResourceManager.launch_table_large_scaffold_empty;

        int split = (int) (state.height * 0.75);
        poseStack.pushPose();
        if (smallScaffold) poseStack.translate(0D, 0D, -1D);
        poseStack.translate(0D, 1D, 3.5D);
        for (int i = 0; i < state.height + 1; i++) {
            if (i < split) {
                draw(poseStack, collector, light, scaffoldType, base);
            } else if (i > split) {

                draw(poseStack, collector, light, scaffoldType, empty);
            } else if (state.missileValid) {
                draw(poseStack, collector, light, connectorType, connector);
            } else {
                draw(poseStack, collector, light, scaffoldType, base);
            }
            poseStack.translate(0D, 1D, 0D);
        }
        poseStack.popPose();

        poseStack.translate(0D, 2.0625D, 0D);
        ItemCustomMissilePart fuselage = state.parts.fuselage();
        if (fuselage != null && fuselage.top == state.padSize) {
            pronter.pront(state.parts, poseStack, collector, light);
        }

        poseStack.popPose();
    }

    private static void draw(
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            RenderType type,
            HFRWavefrontObject mesh) {
        collector.submitCustomGeometry(
                poseStack, type, (pose, buffer) -> mesh.render(pose, buffer, light, -1));
    }

    public static final class State extends BlockEntityRenderState {
        Direction facing = Direction.NORTH;
        PartSize padSize = PartSize.SIZE_10;
        boolean missileValid;
        MissileStruct parts = MissileStruct.EMPTY;
        int height = 10;
    }
}
