// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.model.Meshes;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumWavelengths;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumBeamType;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.render.util.BeamPronter;
import com.hbm.util.GameTime;
import com.hbm.util.RegistryUtil;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetPipeFluid;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionUpdateActor;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.ITileActorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import java.awt.Color;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;

public class CannerySILEX extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.MACHINE_SILEX);
    }

    @Override
    public String getName() {
        return "cannery.silex";
    }

    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(17, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(2, 0));

        for (int x = world.sizeX - 1; x >= 0; x--) {
            for (int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS.defaultBlockState()));
            }

            scene0.add(new ActionWait(1));
        }

        scene0.add(new ActionWait(9));

        CompoundTag fel = new CompoundTag();
        fel.putDouble("x", 11D);
        fel.putDouble("y", 1D);
        fel.putDouble("z", 2D);
        fel.putInt("rotation", 5);
        fel.putInt("length", 11);
        scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFEL(), fel)));

        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        15,
                                        -5,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.0")}
                                        },
                                        100)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.LEFT)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(3));
        scene0.add(new ActionWait(10));

        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {new ItemStack(ModItems.LASER_CRYSTAL_CO2)}
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 1));
        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {new ItemStack(ModItems.LASER_CRYSTAL_BISMUTH)}
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 2));
        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {new ItemStack(ModItems.LASER_CRYSTAL_CMB)}
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 3));
        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {new ItemStack(ModItems.LASER_CRYSTAL_DNT)}
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 4));
        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {new ItemStack(ModItems.LASER_CRYSTAL_DIGAMMA)}
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 5));
        scene0.add(new ActionWait(20));

        scene0.add(new ActionRemoveActor(3));
        scene0.add(new ActionUpdateActor(0, "mode", 0));
        scene0.add(new ActionWait(20));

        for (int y = 1; y < 4; y++) {
            for (int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(5, y, z, Blocks.STONE.defaultBlockState()));
            }
            scene0.add(new ActionWait(5));
        }

        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.1")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionUpdateActor(0, "mode", 3));
        scene0.add(new ActionUpdateActor(0, "length", 4));

        scene0.add(new ActionWait(40));
        scene0.add(new ActionUpdateActor(0, "length", 11));
        scene0.add(new ActionSetBlock(5, 2, 2, Blocks.FIRE.defaultBlockState()));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(5, 2, 2, Blocks.AIR.defaultBlockState()));
        scene0.add(new ActionWait(40));

        for (int y = 1; y < 4; y++) {
            for (int z = 1; z < 4; z++) {
                scene0.add(
                        new ActionSetBlock(
                                5,
                                y,
                                z,
                                RegistryUtil.block("hbm:brick_concrete").defaultBlockState()));
            }

            if (y == 2) {
                scene0.add(new ActionUpdateActor(0, "length", 4));
            }

            scene0.add(new ActionWait(5));
        }

        scene0.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.2")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(40));
        scene0.add(new ActionRemoveActor(3));

        for (int y = 3; y > 0; y--) {
            for (int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(5, y, z, Blocks.AIR.defaultBlockState()));
            }

            if (y == 2) {
                scene0.add(new ActionUpdateActor(0, "length", 11));
            }

            scene0.add(new ActionWait(5));
        }

        scene0.add(new ActionUpdateActor(0, "mode", 0));

        JarScene scene1 = new JarScene(script);

        CompoundTag silex0 = new CompoundTag();
        silex0.putDouble("x", 5D);
        silex0.putDouble("y", 1D);
        silex0.putDouble("z", 2D);
        silex0.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(1, new ActorTileEntity(new ActorSILEX(), silex0)));

        scene1.add(new ActionWait(20));

        scene1.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        42,
                                        -18,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.3")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(80));

        scene1.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        60,
                                        32,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.4")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.RIGHT)));

        scene1.add(new ActionWait(60));

        scene1.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        12,
                                        32,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.5")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.RIGHT)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(3));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionSetBlock(5, 2, 0, Dummies.fluidPipe(Direction.SOUTH, Direction.DOWN)));
        scene1.add(new ActionSetPipeFluid(5, 2, 0, NTMFluids.PEROXIDE));
        scene1.add(new ActionSetBlock(5, 1, 0, Dummies.fluidPipe(Direction.UP, Direction.EAST)));
        scene1.add(new ActionSetPipeFluid(5, 1, 0, NTMFluids.PEROXIDE));
        scene1.add(new ActionSetBlock(6, 1, 0, Dummies.fluidPipe(Direction.WEST, Direction.EAST)));
        scene1.add(new ActionSetPipeFluid(6, 1, 0, NTMFluids.PEROXIDE));
        scene1.add(new ActionSetBlock(7, 1, 0, ModBlocks.BARREL_TCALLOY.get().defaultBlockState()));

        scene1.add(new ActionWait(20));
        scene1.add(new ActionUpdateActor(0, "mode", 3));
        scene1.add(new ActionWait(10));

        scene1.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        42,
                                        -18,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.6")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(80));

        scene1.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        42,
                                        -18,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.7")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(60));

        scene1.add(
                new ActionCreateActor(
                        3,
                        new ActorFancyPanel(
                                        -7,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.silex.8")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(3));
        scene1.add(new ActionWait(10));

        CompoundTag silex1 = new CompoundTag();
        silex1.putDouble("x", 1D);
        silex1.putDouble("y", 1D);
        silex1.putDouble("z", 2D);
        silex1.putInt("rotation", 2);
        scene1.add(new ActionCreateActor(2, new ActorTileEntity(new ActorSILEX(), silex1)));
        scene1.add(new ActionWait(10));

        for (int i = 1; i < 5; i++) {
            scene1.add(
                    new ActionSetBlock(
                            i,
                            1,
                            0,
                            i == 1
                                    ? Dummies.fluidPipe(Direction.EAST, Direction.UP)
                                    : Dummies.fluidPipe(Direction.WEST, Direction.EAST)));
            scene1.add(new ActionSetPipeFluid(i, 1, 0, NTMFluids.PEROXIDE));
        }

        scene1.add(new ActionSetBlock(1, 2, 0, Dummies.fluidPipe(Direction.DOWN, Direction.SOUTH)));
        scene1.add(new ActionSetPipeFluid(1, 2, 0, NTMFluids.PEROXIDE));
        scene1.add(
                new ActionSetBlock(
                        5, 1, 0, Dummies.fluidPipe(Direction.UP, Direction.EAST, Direction.WEST)));

        scene1.add(new ActionWait(20));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    public static final class ActorFEL implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL =
                Meshes.load(Library.id("models/machines/fel.obj"));
        private static final RenderType TYPE =
                RenderTypes.entityCutoutCull(Library.id("textures/block/models/machines/fel.png"));

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            double x = data.getDoubleOr("x", 0D);
            double y = data.getDoubleOr("y", 0D);
            double z = data.getDoubleOr("z", 0D);
            int rotation = data.getIntOr("rotation", 0);
            int mode = data.getIntOr("mode", 0);
            int length = data.getIntOr("length", 0);

            PoseStack pose = context.pose();
            pose.pushPose();
            pose.translate(x + 0.5D, y, z + 0.5D);
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            switch (rotation) {
                                case 3 -> 0;
                                case 5 -> 90;
                                case 2 -> 180;
                                case 4 -> 270;
                                default -> 0;
                            }));
            context.collector()
                    .submitCustomGeometry(
                            pose,
                            TYPE,
                            (partPose, buffer) -> MODEL.render(partPose, buffer, 0xF000F0, -1));
            pose.translate(0, 1.5, -1.5);
            if (mode > 0 && mode < 6) {
                int color = EnumWavelengths.values()[mode].guiColor;
                long time = GameTime.ticks();
                if (color == 0) color = Color.HSBtoRGB(time / 50.0F, 0.5F, 0.1F) & 0xFFFFFF;
                Vec3 beam = new Vec3(0, 0, -length - 1);
                BeamPronter.prontBeam(
                        pose,
                        context.collector(),
                        beam,
                        EnumWaveType.SPIRAL,
                        EnumBeamType.SOLID,
                        color,
                        color,
                        0,
                        1,
                        0F,
                        2,
                        0.0625F);
                BeamPronter.prontBeam(
                        pose,
                        context.collector(),
                        beam,
                        EnumWaveType.RANDOM,
                        EnumBeamType.SOLID,
                        color,
                        color,
                        (int) (time % 1000 / 2),
                        length / 2 + 1,
                        0.0625F,
                        2,
                        0.0625F);
            }
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {}
    }

    public static final class ActorSILEX implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL =
                Meshes.load(Library.id("models/machines/silex.obj"));
        private static final RenderType TYPE =
                RenderTypes.entityCutoutCull(
                        Library.id("textures/block/models/machines/silex.png"));

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            double x = data.getDoubleOr("x", 0D);
            double y = data.getDoubleOr("y", 0D);
            double z = data.getDoubleOr("z", 0D);
            int rotation = data.getIntOr("rotation", 0);

            PoseStack pose = context.pose();
            pose.pushPose();
            pose.translate(x + 0.5D, y, z + 0.5D);
            pose.mulPose(
                    Axis.YP.rotationDegrees(
                            switch (rotation) {
                                case 3 -> 0;
                                case 5 -> 90;
                                case 2 -> 180;
                                case 4 -> 270;
                                default -> 0;
                            }));
            context.collector()
                    .submitCustomGeometry(
                            pose,
                            TYPE,
                            (partPose, buffer) -> MODEL.render(partPose, buffer, 0xF000F0, -1));
            pose.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {}
    }
}
