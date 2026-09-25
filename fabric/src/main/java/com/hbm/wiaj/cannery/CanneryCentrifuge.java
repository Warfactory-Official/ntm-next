// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.client.model.Meshes;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.lib.Library;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetPipeFluid;
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

public class CanneryCentrifuge extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.MACHINE_GASCENT);
    }

    @Override
    public String getName() {
        return "cannery.centrifuge";
    }

    @Override
    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(9, 5, 5);
        JarScript script = new JarScript(world);
        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(2, 0));
        for (int x = world.sizeX - 1; x >= 0; x--) {
            for (int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS.defaultBlockState()));
            }

            if (x == 7) {
                scene0.add(
                        new ActionSetBlock(
                                7, 1, 2, ModBlocks.BARREL_TCALLOY.get().defaultBlockState()));
            }

            if (x == 6) {
                scene0.add(
                        new ActionSetBlock(
                                6, 1, 2, Dummies.fluidPipe(Direction.WEST, Direction.EAST)));
                scene0.add(new ActionSetPipeFluid(6, 1, 2, NTMFluids.UF6));
            }

            if (x == 5) {
                CompoundTag cent = position(5, 1, 2);
                scene0.add(new ActionCreateActor(0, new ActorTileEntity(new ActorGasCent(), cent)));
            }

            scene0.add(new ActionWait(2));
        }

        scene0.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        -15,
                                        -50,
                                        new Object[][] {
                                            {Component.translatable("cannery.centrifuge.0")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(1));

        JarScene scene1 = new JarScene(script);

        scene1.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        -15,
                                        10,
                                        new Object[][] {
                                            {Component.translatable("cannery.centrifuge.1")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.CENTER)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(1));

        scene1.add(new ActionSetZoom(4, 20));
        scene1.add(
                new ActionCreateActor(
                        1,
                        new ActorFancyPanel(
                                        0,
                                        40,
                                        new Object[][] {
                                            {Component.translatable("cannery.centrifuge.2")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(1));
        scene1.add(new ActionSetZoom(-2, 20));
        scene1.add(new ActionWait(20));

        scene1.add(
                new ActionCreateActor(
                        1, new ActorTileEntity(new ActorGasCent(), position(4, 1, 2))));
        scene1.add(new ActionWait(10));

        scene1.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        0,
                                        new Object[][] {
                                            {Component.translatable("cannery.centrifuge.3")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.CENTER)));
        scene1.add(new ActionWait(100));
        scene1.add(new ActionRemoveActor(2));
        scene1.add(new ActionSetZoom(-2, 20));
        scene1.add(
                new ActionCreateActor(
                        2,
                        new ActorFancyPanel(
                                        0,
                                        0,
                                        new Object[][] {
                                            {Component.translatable("cannery.centrifuge.4")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.CENTER)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(2));

        scene1.add(
                new ActionCreateActor(
                        2, new ActorTileEntity(new ActorGasCent(), position(3, 1, 2))));
        scene1.add(new ActionWait(10));
        scene1.add(
                new ActionCreateActor(
                        3, new ActorTileEntity(new ActorGasCent(), position(2, 1, 2))));
        scene1.add(new ActionWait(10));

        scene1.add(
                new ActionCreateActor(
                        4,
                        new ActorFancyPanel(
                                        0,
                                        0,
                                        new Object[][] {
                                            {Component.translatable("cannery.centrifuge.5")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.CENTER)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(4));

        scene1.add(
                new ActionCreateActor(
                        4,
                        new ActorFancyPanel(
                                        28,
                                        -30,
                                        new Object[][] {{new ItemStack(ModItems.UPGRADE_GC_SPEED)}},
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(
                new ActionCreateActor(
                        5,
                        new ActorFancyPanel(
                                        45,
                                        35,
                                        new Object[][] {
                                            {
                                                Component.literal(" = "),
                                                new ItemStack(ModItems.nugget(Mats.MAT_U238), 11),
                                                new ItemStack(ModItems.nugget(Mats.MAT_U235))
                                            }
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.LEFT)));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    private static CompoundTag position(int x, int y, int z) {
        CompoundTag data = new CompoundTag();
        data.putDouble("x", x);
        data.putDouble("y", y);
        data.putDouble("z", z);
        data.putInt("rotation", 2);
        return data;
    }

    public static class ActorGasCent implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL =
                Meshes.load(Library.id("models/machines/gascent.obj"));
        private static final int CENTRIFUGE = MODEL.partId("Centrifuge");
        private static final int FLAG = MODEL.partId("Flag");
        private static final RenderType TYPE =
                RenderTypes.entityCutoutCull(
                        Library.id("textures/block/models/machines/gascent.png"));

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            var ps = context.pose();
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
            context.collector()
                    .submitCustomGeometry(
                            ps,
                            TYPE,
                            (pose, buffer) -> {
                                MODEL.renderPart(
                                        pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, CENTRIFUGE);
                                MODEL.renderPart(
                                        pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1, FLAG);
                            });
            ps.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {}
    }
}
