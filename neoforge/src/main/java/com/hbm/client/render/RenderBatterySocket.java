// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.flywheel.HbmBlockEntityVisual;
import com.hbm.client.render.flywheel.WorldItem;
import com.hbm.items.machine.EnumBatteryPack;
import com.hbm.items.machine.ItemBatteryCreative;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.render.util.HorsePronter;
import com.hbm.tileentity.machine.storage.BlockEntityBatterySocket;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.client.renderer.blockentity.state.BlockEntityRenderState;
import net.minecraft.client.renderer.feature.ModelFeatureRenderer;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class RenderBatterySocket
        implements BlockEntityRenderer<BlockEntityBatterySocket, RenderBatterySocket.State>,
                ConcurrentRenderStateExtraction {
    private static final int CAPACITOR = ResourceManager.battery_socket.partId("Capacitor");
    private static final int BATTERY = ResourceManager.battery_socket.partId("Battery");
    private static final int SUPPORTS = ResourceManager.battery_socket.partId("Supports");

    private static final double UNICORN_SCALE = 0.75D;
    private static final float UNICORN_DEGREES_PER_TICK = 25F;
    private static final float ITEM_DEGREES_PER_TICK = 2.5F;
    private static final double ARC_OFFSET = 0.4375D;
    private static final double ARC_HEIGHT = 1.1875D;
    private static final int ARC_OUTER = 0x404040;
    private static final int ARC_INNER = 0x002040;

    private static final int NO_PART = -1;

    private final HFRWavefrontObject model;
    private final ItemModelResolver itemModelResolver;
    private final Map<Identifier, RenderType> bodyTypes = new HashMap<>();

    private final Map<EnumBatteryPack, Identifier> tierTextures = new ConcurrentHashMap<>();

    public RenderBatterySocket(BlockEntityRendererProvider.Context context) {
        this.model = ResourceManager.battery_socket;
        this.itemModelResolver = context.itemModelResolver();
    }

    private static float dummyableYaw(Direction facing) {
        return Facing.yaw(facing, 90);
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public AABB getRenderBoundingBox(BlockEntityBatterySocket be) {
        BlockPos pos = be.getBlockPos();
        return new AABB(
                pos.getX() - 1,
                pos.getY(),
                pos.getZ() - 1,
                pos.getX() + 2,
                pos.getY() + 2,
                pos.getZ() + 2);
    }

    @Override
    public boolean shouldRenderOffScreen() {
        return true;
    }

    @Override
    public void extractRenderState(
            BlockEntityBatterySocket socket,
            State state,
            float partialTicks,
            Vec3 cameraPosition,
            ModelFeatureRenderer.@Nullable CrumblingOverlay breakProgress) {
        BlockEntityRenderer.super.extractRenderState(
                socket, state, partialTicks, cameraPosition, breakProgress);
        state.facing = socket.getBlockState().getValue(BlockMultiblockCore.FACING);

        state.frame = socket.frame;
        long gameTime = socket.getLevel() == null ? 0L : socket.getLevel().getGameTime();
        state.spin = (float) (gameTime % 360L) + partialTicks;
        state.arcSeed = gameTime / 5L;

        state.arcStart = (int) (gameTime % 20L);

        ItemStack stack = socket.syncStack;
        state.itemsOnly = HbmBlockEntityVisual.hasVisual(socket);
        state.unicorn = stack.getItem() instanceof ItemBatteryCreative;
        if (stack.getItem() instanceof ItemBatteryPack pack) {
            state.bodyPart = pack.tier.isCapacitor() ? CAPACITOR : BATTERY;
            state.bodyTexture =
                    tierTextures.computeIfAbsent(
                            pack.tier,
                            tier ->
                                    Library.id(
                                            "textures/block/models/machines/" + tier.tex + ".png"));
        } else if (stack.getItem() instanceof ItemBatterySC) {
            state.bodyPart = BATTERY;
            state.bodyTexture = ResourceManager.battery_sc_tex;
        } else {
            state.bodyPart = NO_PART;
            state.bodyTexture = null;
        }
        state.generic = !stack.isEmpty() && state.bodyPart == NO_PART && !state.unicorn;
        ItemStack rendered =
                state.generic && (!state.itemsOnly || !WorldItem.bakesFramed(stack))
                        ? stack.copyWithCount(1)
                        : ItemStack.EMPTY;
        state.itemArm =
                FramedItem.resolve(
                        itemModelResolver, state.item, rendered, socket.getLevel(), null, 0);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.translate(0.5, 0.0, 0.5);
        poseStack.mulPose(Axis.YP.rotationDegrees(dummyableYaw(state.facing)));
        poseStack.translate(-0.5, 0.0, 0.5);
        int light = state.lightCoords;

        if (state.frame && !state.itemsOnly) {
            RenderType socketType =
                    bodyTypes.computeIfAbsent(
                            ResourceManager.battery_socket_tex, RenderTypes::entitySolid);
            collector.submitCustomGeometry(
                    poseStack,
                    socketType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, SUPPORTS));
        }
        if (!state.itemsOnly && state.bodyPart != NO_PART && state.bodyTexture != null) {
            RenderType bodyType =
                    bodyTypes.computeIfAbsent(state.bodyTexture, RenderTypes::entitySolid);
            int part = state.bodyPart;
            collector.submitCustomGeometry(
                    poseStack,
                    bodyType,
                    (pose, buffer) -> model.renderPart(pose, buffer, light, -1, part));
        }
        if (!state.item.isEmpty()) {
            poseStack.pushPose();
            itemPose(poseStack, state.spin, state.itemArm);
            state.item.submit(poseStack, collector, light, OverlayTexture.NO_OVERLAY, 0);
            poseStack.popPose();
        }
        if (!state.itemsOnly && state.unicorn) submitUnicorn(state, poseStack, collector, light);
        if (!state.itemsOnly && (state.unicorn || state.generic))
            submitArcs(state, poseStack, collector);
        poseStack.popPose();
    }

    public static void itemPose(PoseStack poseStack, float spin, FramedItem.Arm arm) {
        poseStack.translate(0, 0.5, 0);
        poseStack.scale(1.5F, 1.5F, 1.5F);
        poseStack.mulPose(Axis.YN.rotationDegrees(spin * ITEM_DEGREES_PER_TICK));
        if (arm.mesh()) poseStack.translate(0, arm.groundLift(), 0);
        else if (arm == FramedItem.Arm.BLOCK) FramedItem.framedBlockPrefix(poseStack);
        else {
            FramedItem.framedSpritePrefix(poseStack);
            FramedItem.framedSpriteSinglePose(poseStack);
        }
    }

    private void submitUnicorn(
            State state, PoseStack poseStack, SubmitNodeCollector collector, int light) {
        poseStack.pushPose();
        poseStack.scale((float) UNICORN_SCALE, (float) UNICORN_SCALE, (float) UNICORN_SCALE);
        poseStack.mulPose(Axis.YN.rotationDegrees(state.spin * UNICORN_DEGREES_PER_TICK));

        RenderType horseType =
                bodyTypes.computeIfAbsent(
                        ResourceManager.horse_sunburst_tex, RenderTypes::entityCutout);
        HorsePronter.reset();
        HorsePronter.enableHorn();
        HorsePronter.pront(poseStack, collector, horseType, light);
        poseStack.popPose();
    }

    private void submitArcs(State state, PoseStack poseStack, SubmitNodeCollector collector) {

        Random rand = new Random(state.arcSeed);
        rand.nextBoolean();
        for (int i = -1; i <= 1; i += 2) {
            for (int j = -1; j <= 1; j += 2) {
                if (rand.nextInt(4) != 0) continue;
                poseStack.pushPose();
                poseStack.translate(0, 0.75, 0);
                Vec3 skeleton = new Vec3(ARC_OFFSET * i, ARC_HEIGHT, ARC_OFFSET * j);
                BeamPronter.prontBeam(
                        poseStack,
                        collector,
                        skeleton,
                        EnumWaveType.RANDOM,
                        EnumBeamType.SOLID,
                        ARC_OUTER,
                        ARC_INNER,
                        state.arcStart,
                        15,
                        0.0625F,
                        3,
                        0.025F);
                BeamPronter.prontBeam(
                        poseStack,
                        collector,
                        skeleton,
                        EnumWaveType.RANDOM,
                        EnumBeamType.SOLID,
                        ARC_OUTER,
                        ARC_INNER,
                        state.arcStart,
                        1,
                        0F,
                        3,
                        0.025F);
                poseStack.popPose();
            }
        }
    }

    public static final class State extends BlockEntityRenderState {
        public Direction facing = Direction.EAST;
        public int bodyPart = NO_PART;
        public @Nullable Identifier bodyTexture;
        public boolean frame;
        public boolean unicorn;
        public boolean generic;
        public boolean itemsOnly;
        public final ItemStackRenderState item = new ItemStackRenderState();
        public FramedItem.Arm itemArm = FramedItem.Arm.SPRITE;
        public float spin;
        public long arcSeed;
        public int arcStart;
    }
}
