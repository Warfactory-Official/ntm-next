// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.PowerDetectorBlock;
import com.hbm.client.model.Meshes;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionUpdateActor;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.ITileActorRenderer;
import com.hbm.wiaj.cannery.CanneryFirebox.ActorFirebox;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CanneryStirling extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.MACHINE_STIRLING);
    }

    @Override
    public String getName() {
        return "cannery.stirling";
    }

    public CanneryBase[] seeAlso() {
        return new CanneryBase[] {new CanneryFirebox()};
    }

    @Override
    public JarScript createScript() {

        WorldInAJar world = new WorldInAJar(5, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(3, 0));

        for (int x = world.sizeX - 1; x >= 0; x--) {
            for (int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS.defaultBlockState()));
            }
            scene0.add(new ActionWait(2));
        }

        scene0.add(new ActionWait(8));

        CompoundTag firebox = new CompoundTag();
        firebox.putDouble("x", 2);
        firebox.putDouble("y", 1);
        firebox.putDouble("z", 2);
        firebox.putInt("rotation", 5);
        scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorFirebox(), firebox)));

        scene0.add(new ActionWait(10));

        CompoundTag stirling = new CompoundTag();
        stirling.putDouble("x", 2);
        stirling.putDouble("z", 2);
        stirling.putInt("rotation", 2);
        scene0.add(new ActionCreateActor(1, new ActorTileEntity(new ActorStirling(), stirling)));

        scene0.add(new ActionUpdateActor(1, "speed", 0F));
        scene0.add(new ActionUpdateActor(1, "y", 2D));
        scene0.add(new ActionUpdateActor(1, "type", 0));
        scene0.add(new ActionUpdateActor(1, "hasCog", true));
        scene0.add(new ActionUpdateActor(1, "spin", 0F));
        scene0.add(new ActionUpdateActor(1, "lastSpin", 0F));

        scene0.add(new ActionWait(10));

        scene0.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        -45,
                                        new Object[][] {
                                            {Component.translatable("cannery.stirling.0")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));

        scene0.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        40,
                                        new Object[][] {
                                            {Component.translatable("cannery.stirling.1")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(2));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionUpdateActor(0, "open", true));
        scene0.add(new ActionWait(30));
        scene0.add(new ActionUpdateActor(0, "isOn", true));
        scene0.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        -50, 40, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionRemoveActor(2));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionUpdateActor(0, "open", false));
        scene0.add(new ActionWait(30));

        for (int i = 0; i < 60; i++) {
            scene0.add(new ActionUpdateActor(1, "speed", i / 5F));
            scene0.add(new ActionWait(1));
        }

        scene0.add(new ActionWait(20));

        scene0.add(new ActionSetBlock(0, 2, 2, Dummies.cable(Direction.EAST, Direction.DOWN)));
        scene0.add(new ActionSetBlock(0, 1, 2, Dummies.cable(Direction.UP, Direction.SOUTH)));
        scene0.add(
                new ActionSetBlock(0, 1, 3, ModBlocks.MACHINE_DETECTOR.get().defaultBlockState()));
        scene0.add(new ActionWait(10));
        scene0.add(
                new ActionSetBlock(
                        0,
                        1,
                        3,
                        ModBlocks.MACHINE_DETECTOR
                                .get()
                                .defaultBlockState()
                                .setValue(PowerDetectorBlock.POWERED, true)));
        scene0.add(new ActionWait(40));

        JarScene scene1 = new JarScene(script);

        scene1.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        -45,
                                        new Object[][] {
                                            {Component.translatable("cannery.stirling.2")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));

        CompoundTag burner = new CompoundTag();
        burner.putDouble("x", 2);
        burner.putDouble("y", 1);
        burner.putDouble("z", 2);
        burner.putInt("rotation", 5);
        scene1.add(new ActionCreateActor(0, new ActorTileEntity(new ActorBurner(), burner)));
        scene1.add(new ActionUpdateActor(1, "y", 3D));

        scene1.add(new ActionSetBlock(0, 2, 2, Dummies.cable(Direction.UP, Direction.DOWN)));
        scene1.add(new ActionSetBlock(0, 3, 2, Dummies.cable(Direction.EAST, Direction.DOWN)));

        for (int i = 0; i < 100; i++) {
            scene1.add(new ActionUpdateActor(1, "speed", (i + 60) / 5F));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionWait(20));
        scene1.add(new ActionUpdateActor(1, "hasCog", false));
        scene1.add(
                new ActionSetBlock(0, 1, 3, ModBlocks.MACHINE_DETECTOR.get().defaultBlockState()));

        for (int i = 0; i < 160; i += 10) {
            scene1.add(new ActionUpdateActor(1, "speed", (160 - i) / 5F));
            scene1.add(new ActionWait(1));
        }
        scene1.add(new ActionUpdateActor(1, "speed", 0F));

        scene1.add(new ActionWait(20));

        scene1.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        -45,
                                        new Object[][] {
                                            {Component.translatable("cannery.stirling.3")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));
        scene1.add(new ActionWait(20));
        scene1.add(new ActionUpdateActor(1, "hasCog", true));
        scene1.add(new ActionUpdateActor(1, "type", 1));
        scene1.add(new ActionWait(20));

        for (int i = 0; i < 60; i++) {
            scene1.add(new ActionUpdateActor(1, "speed", i / 5F));
            scene1.add(new ActionWait(1));
        }

        scene1.add(
                new ActionSetBlock(
                        0,
                        1,
                        3,
                        ModBlocks.MACHINE_DETECTOR
                                .get()
                                .defaultBlockState()
                                .setValue(PowerDetectorBlock.POWERED, true)));
        scene1.add(new ActionWait(100));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    public static final class ActorStirling implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL = ResourceManager.stirling;
        private static final int BASE = MODEL.partId("Base");
        private static final int COG = MODEL.partId("Cog");
        private static final int COG_SMALL = MODEL.partId("CogSmall");
        private static final int PISTON = MODEL.partId("Piston");
        private static final RenderType[] TYPES = {
            RenderTypes.entityCutoutCull(ResourceManager.stirling_tex),
            RenderTypes.entityCutoutCull(ResourceManager.stirling_steel_tex),
            RenderTypes.entityCutoutCull(ResourceManager.stirling_creative_tex)
        };

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            double x = data.getDoubleOr("x", 0D);
            double y = data.getDoubleOr("y", 0D);
            double z = data.getDoubleOr("z", 0D);
            int rotation = data.getIntOr("rotation", 0);
            int type = data.getIntOr("type", 0);
            boolean hasCog = data.getBooleanOr("hasCog", false);
            float lastSpin = data.getFloatOr("lastSpin", 0F);
            float spin = data.getFloatOr("spin", 0F);
            float rot = lastSpin + (spin - lastSpin) * interp;

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
            RenderType renderType = TYPES[type];
            part(context, renderType, BASE);

            if (hasCog) {
                pose.pushPose();
                pose.translate(0, 1.375, 0);
                pose.mulPose(Axis.ZP.rotationDegrees(-rot));
                pose.translate(0, -1.375, 0);
                part(context, renderType, COG);
                pose.popPose();
            }

            pose.pushPose();
            pose.translate(0, 1.375, 0.25);
            pose.mulPose(Axis.XP.rotationDegrees(rot * 2F + 3F));
            pose.translate(0, -1.375, -0.25);
            part(context, renderType, COG_SMALL);
            pose.popPose();

            pose.translate(Math.sin(rot * Math.PI / 90D) * 0.25 + 0.125, 0, 0);
            part(context, renderType, PISTON);
            pose.popPose();
        }

        private static void part(JarRenderContext context, RenderType renderType, int part) {
            context.collector()
                    .submitCustomGeometry(
                            context.pose(),
                            renderType,
                            (pose, buffer) -> MODEL.renderPart(pose, buffer, 0xF000F0, -1, part));
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {
            float lastSpin = data.getFloatOr("spin", 0F);
            float spin = lastSpin + data.getFloatOr("speed", 0F);
            if (spin >= 360) {
                lastSpin -= 360;
                spin -= 360;
            }
            data.putFloat("lastSpin", lastSpin);
            data.putFloat("spin", spin);
        }
    }

    public static final class ActorBurner implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL =
                Meshes.load(Library.id("models/machines/oilburner.obj"));
        private static final RenderType TYPE =
                RenderTypes.entityCutoutCull(
                        Library.id("textures/block/models/machines/oilburner.png"));

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            PoseStack pose = context.pose();
            pose.pushPose();
            pose.translate(
                    data.getDoubleOr("x", 0D) + 0.5D,
                    data.getDoubleOr("y", 0D),
                    data.getDoubleOr("z", 0D) + 0.5D);
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
