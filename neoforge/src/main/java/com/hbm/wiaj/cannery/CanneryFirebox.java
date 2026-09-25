// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.PowerDetectorBlock;
import com.hbm.client.render.FlatCutout;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumCokeType;
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
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;

public class CanneryFirebox extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.HEATER_FIREBOX);
    }

    @Override
    public String getName() {
        return "cannery.firebox";
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

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        -10,
                                        new Object[][] {
                                            {Component.translatable("cannery.firebox.0")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(60));

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        -10,
                                        new Object[][] {
                                            {Component.translatable("cannery.firebox.1")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionUpdateActor(0, "open", true));
        scene0.add(new ActionWait(30));

        scene0.add(new ActionUpdateActor(0, "isOn", true));

        scene0.add(new ActionCreateActor(1, fuel(Items.COAL.getDefaultInstance())));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, fuel(ModItems.COKE.stack(EnumCokeType.COAL))));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, fuel(new ItemStack(ModItems.SOLID_FUEL))));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, fuel(new ItemStack(ModItems.ROCKET_FUEL))));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(1, fuel(new ItemStack(ModItems.SOLID_FUEL_BF))));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionUpdateActor(0, "open", false));
        scene0.add(new ActionWait(30));

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        -10,
                                        new Object[][] {
                                            {Component.translatable("cannery.firebox.2")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(80));

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        -10,
                                        new Object[][] {
                                            {Component.translatable("cannery.firebox.3")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));
        scene0.add(new ActionWait(10));

        JarScene scene1 = new JarScene(script);
        CompoundTag stirling = new CompoundTag();
        stirling.putDouble("x", 2);
        stirling.putDouble("y", 2);
        stirling.putDouble("z", 2);
        stirling.putInt("rotation", 2);
        stirling.putBoolean("hasCog", true);
        scene1.add(
                new ActionCreateActor(
                        1, new ActorTileEntity(new CanneryStirling.ActorStirling(), stirling)));
        scene1.add(new ActionUpdateActor(1, "speed", 0F));

        scene1.add(new ActionWait(10));
        scene1.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        -45,
                                        new Object[][] {
                                            {Component.translatable("cannery.firebox.4")}
                                        },
                                        250)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));
        scene1.add(new ActionWait(10));

        for (int i = 0; i < 60; i++) {
            scene1.add(new ActionUpdateActor(1, "speed", i / 5F));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionSetBlock(0, 2, 2, Dummies.cable(Direction.EAST, Direction.DOWN)));
        scene1.add(new ActionSetBlock(0, 1, 2, Dummies.cable(Direction.UP, Direction.SOUTH)));
        scene1.add(
                new ActionSetBlock(
                        0,
                        1,
                        3,
                        ModBlocks.MACHINE_DETECTOR
                                .get()
                                .defaultBlockState()
                                .setValue(PowerDetectorBlock.POWERED, false)));
        scene1.add(new ActionWait(10));
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

    private static ActorFancyPanel fuel(ItemStack stack) {
        return new ActorFancyPanel(-50, 40, new Object[][] {{stack}}, 0)
                .setColors(colorCopper)
                .setOrientation(Orientation.RIGHT);
    }

    public static class ActorFirebox implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL = ResourceManager.heater_firebox;
        private static final int MAIN = MODEL.partId("Main");
        private static final int DOOR = MODEL.partId("Door");
        private static final int INNER_BURNING = MODEL.partId("InnerBurning");
        private static final int INNER_EMPTY = MODEL.partId("InnerEmpty");
        private static final RenderType BODY =
                RenderTypes.entityCutoutCull(ResourceManager.heater_firebox_tex);
        private static final RenderType HOT = FlatCutout.of(ResourceManager.heater_firebox_tex);

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

            part(context, BODY, MAIN);

            ps.pushPose();
            float prev = data.getFloatOr("lastAngle", 0);
            float door = prev + (data.getFloatOr("angle", 0) - prev) * interp;
            ps.translate(1.375, 0, 0.375);
            ps.mulPose(Axis.YN.rotationDegrees(door));
            ps.translate(-1.375, 0, -0.375);
            part(context, BODY, DOOR);
            ps.popPose();

            if (data.getBooleanOr("isOn", false)) part(context, HOT, INNER_BURNING);
            else part(context, BODY, INNER_EMPTY);
            ps.popPose();
        }

        private static void part(JarRenderContext context, RenderType type, int id) {
            context.collector()
                    .submitCustomGeometry(
                            context.pose(),
                            type,
                            (pose, buffer) ->
                                    MODEL.renderPart(
                                            pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, id));
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {
            boolean open = data.getBooleanOr("open", false);
            float angle = data.getFloatOr("angle", 0);
            data.putFloat("lastAngle", angle);
            float speed = angle / 10F + 3F;
            angle += open ? speed : -speed;
            data.putFloat("angle", Mth.clamp(angle, 0F, 135F));
        }
    }
}
