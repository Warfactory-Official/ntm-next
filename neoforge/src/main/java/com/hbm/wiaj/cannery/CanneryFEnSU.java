// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.PowerDetectorBlock;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionOffsetBy;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionRotateBy;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.ITileActorRenderer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.state.properties.AttachFace;

public class CanneryFEnSU extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.MACHINE_FENSU);
    }

    @Override
    public String getName() {
        return "cannery.fensu";
    }

    @Override
    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(11, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);
        scene0.add(new ActionSetZoom(1.5D, 0));
        scene0.add(new ActionOffsetBy(-2D, 0D, 0D, 0));
        CompoundTag fensu = new CompoundTag();
        fensu.putDouble("x", 7);
        fensu.putDouble("y", 1);
        fensu.putDouble("z", 2);
        fensu.putInt("rotation", 4);
        fensu.putFloat("speed", 10F);
        scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFEnSU(), fensu)));

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        -50,
                                        new Object[][] {
                                            {Component.translatable("cannery.fensu.0")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionRotateBy(45, 90, 20));

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        20,
                                        new Object[][] {
                                            {Component.translatable("cannery.fensu.1")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.TOP)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        20,
                                        new Object[][] {
                                            {Component.translatable("cannery.fensu.2")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.TOP)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));

        scene0.add(
                new ActionSetBlock(7, 0, 2, ModBlocks.RED_WIRE_COATED.get().defaultBlockState()));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(6, 0, 2, lever(false)));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionRotateBy(0, -60, 20));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(6, 0, 2, lever(true)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(6, 0, 2, lever(false)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionRotateBy(-45, -30, 20));
        scene0.add(new ActionOffsetBy(2D, 0D, 0D, 10));

        JarScene scene1 = new JarScene(script);

        for (int x = world.sizeX - 1; x >= 0; x--) {
            for (int z = 0; z < world.sizeZ; z++) {
                if (z == 2 && x > 0 && x < 10)
                    scene1.add(
                            new ActionSetBlock(
                                    x, 0, z, ModBlocks.RED_WIRE_COATED.get().defaultBlockState()));
                else scene1.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS.defaultBlockState()));
            }
            scene1.add(new ActionWait(2));
        }

        scene1.add(new ActionWait(18));
        scene1.add(
                new ActionSetBlock(
                        1,
                        1,
                        2,
                        ModBlocks.MACHINE_DETECTOR
                                .get()
                                .defaultBlockState()
                                .setValue(PowerDetectorBlock.POWERED, false)));
        scene1.add(new ActionWait(10));
        scene1.add(
                new ActionSetBlock(
                        1,
                        1,
                        2,
                        ModBlocks.MACHINE_DETECTOR
                                .get()
                                .defaultBlockState()
                                .setValue(PowerDetectorBlock.POWERED, true)));
        scene1.add(new ActionWait(60));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    private static net.minecraft.world.level.block.state.BlockState lever(boolean powered) {

        return Blocks.LEVER
                .defaultBlockState()
                .setValue(LeverBlock.FACE, AttachFace.WALL)
                .setValue(LeverBlock.FACING, Direction.WEST)
                .setValue(LeverBlock.POWERED, powered);
    }

    public static class ActorFEnSU implements ITileActorRenderer {
        private static final int BASE = ResourceManager.fensu.partId("Base");
        private static final int DISC = ResourceManager.fensu.partId("Disc");
        private static final int LIGHTS = ResourceManager.fensu.partId("Lights");
        private final HFRWavefrontObject model = ResourceManager.fensu;
        private final RenderType type = RenderTypes.entityCutoutCull(ResourceManager.fensu_tex);

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            var ps = context.pose();
            var col = context.collector();
            ps.pushPose();
            ps.translate(
                    data.getDoubleOr("x", 0) + 0.5D,
                    data.getDoubleOr("y", 0),
                    data.getDoubleOr("z", 0) + 0.5D);
            ps.mulPose(
                    Axis.YP.rotationDegrees(
                            switch (data.getIntOr("rotation", 3)) {
                                case 5 -> 90F;
                                case 2 -> 180F;
                                case 4 -> 270F;
                                default -> 0F;
                            }));

            col.submitCustomGeometry(
                    ps,
                    type,
                    (pose, buffer) ->
                            model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, BASE));

            float spin = data.getFloatOr("lastSpin", 0);
            float next = data.getFloatOr("spin", 0);
            ps.translate(0, 2.5, 0);
            ps.mulPose(Axis.XP.rotationDegrees(spin + (next - spin) * interp));
            ps.translate(0, -2.5, 0);
            col.submitCustomGeometry(
                    ps,
                    type,
                    (pose, buffer) -> {
                        model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, DISC);
                        model.renderPart(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, LIGHTS);
                    });
            ps.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {
            float spin = data.getFloatOr("spin", 0);
            float lastSpin = spin;
            spin += data.getFloatOr("speed", 0);
            if (spin >= 360) {
                lastSpin -= 360;
                spin -= 360;
            }
            data.putFloat("lastSpin", lastSpin);
            data.putFloat("spin", spin);
        }
    }
}
