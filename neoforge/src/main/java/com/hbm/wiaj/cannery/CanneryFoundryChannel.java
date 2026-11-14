/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.wiaj.cannery;

import com.hbm.blockentity.machine.FoundryBasinBlockEntity;
import com.hbm.blockentity.machine.FoundryCastingBaseBlockEntity;
import com.hbm.blockentity.machine.FoundryChannelBlockEntity;
import com.hbm.blockentity.machine.FoundryDischarge;
import com.hbm.blockentity.machine.FoundryMoldBlockEntity;
import com.hbm.blockentity.machine.FoundryRenderProvider;
import com.hbm.blockentity.machine.FoundryTankBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.FoundryChannelBlock;
import com.hbm.blocks.machine.FoundryOutletBlock;
import com.hbm.blocks.machine.FoundryTankBlock;
import com.hbm.client.render.FoundryChannelRenderer;
import com.hbm.client.render.FoundryFaces;
import com.hbm.client.render.FoundryRenderer;
import com.hbm.client.render.FoundryTankRenderer;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetTile;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorBase;
import com.hbm.wiaj.actors.ActorBlockEntity;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorRenderer;
import com.mojang.blaze3d.vertex.PoseStack;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;

public class CanneryFoundryChannel extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.FOUNDRY_CHANNEL);
    }

    @Override
    public String getName() {
        return "cannery.foundryChannel";
    }

    @Override
    public CanneryBase[] seeAlso() {
        return new CanneryBase[] {new CanneryCrucible()};
    }

    @Override
    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(5, 4, 4);
        JarScript script = new JarScript(world);
        JarScene scene0 = new JarScene(script);

        scene0.add(new ActionSetZoom(3, 0));
        scene0.add(new ActionCreateActor(10, new ActorFoundryWorld()));
        for (int x = world.sizeX - 1; x >= 0; x--) {
            for (int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS.defaultBlockState()));
            }
        }

        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetBlock(1, 1, 2, Blocks.BRICKS.defaultBlockState()));
        scene0.add(new ActionSetBlock(2, 1, 2, Blocks.BRICKS.defaultBlockState()));
        scene0.add(new ActionSetBlock(3, 1, 2, Blocks.BRICKS.defaultBlockState()));
        scene0.add(new ActionSetBlock(3, 1, 3, Blocks.BRICKS.defaultBlockState()));

        scene0.add(
                new ActionSetTile(
                        3,
                        1,
                        1,
                        basin(
                                3,
                                1,
                                1,
                                new ItemStack(ModItems.byName("block")),
                                null,
                                0,
                                ItemStack.EMPTY)));
        scene0.add(
                new ActionCreateActor(
                        1, new ActorBlockEntity(new ActorFoundry(), position(3, 1, 1))));
        scene0.add(new ActionSetBlock(3, 1, 1, ModBlocks.FOUNDRY_BASIN.get().defaultBlockState()));
        scene0.add(
                new ActionSetTile(
                        2,
                        1,
                        1,
                        mold(
                                2,
                                1,
                                1,
                                new ItemStack(ModItems.byName("ingot")),
                                null,
                                0,
                                ItemStack.EMPTY)));
        scene0.add(
                new ActionCreateActor(
                        2, new ActorBlockEntity(new ActorFoundry(), position(2, 1, 1))));
        scene0.add(new ActionSetBlock(2, 1, 1, ModBlocks.FOUNDRY_MOLD.get().defaultBlockState()));

        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetBlock(3, 2, 3, Blocks.BRICKS.defaultBlockState()));
        scene0.add(
                new ActionSetTile(
                        1,
                        2,
                        2,
                        mold(
                                1,
                                2,
                                2,
                                new ItemStack(ModItems.byName("ingot")),
                                null,
                                0,
                                ItemStack.EMPTY)));
        scene0.add(
                new ActionCreateActor(
                        3, new ActorBlockEntity(new ActorFoundry(), position(1, 2, 2))));
        scene0.add(new ActionSetBlock(1, 2, 2, ModBlocks.FOUNDRY_MOLD.get().defaultBlockState()));
        scene0.add(new ActionSetTile(2, 2, 2, channel(2, 2, 2, null, 0)));
        scene0.add(new ActionSetBlock(2, 2, 2, channelState(2)));
        scene0.add(new ActionSetTile(3, 2, 2, channel(3, 2, 2, null, 0)));
        scene0.add(new ActionSetBlock(3, 2, 2, channelState(3)));

        scene0.add(new ActionSetTile(2, 2, 1, outlet(2, 2, 1)));
        scene0.add(
                new ActionSetBlock(
                        2,
                        2,
                        1,
                        ModBlocks.FOUNDRY_OUTLET
                                .get()
                                .defaultBlockState()
                                .setValue(FoundryOutletBlock.FACING, Direction.NORTH)));
        scene0.add(new ActionSetTile(3, 2, 1, outlet(3, 2, 1)));
        scene0.add(
                new ActionSetBlock(
                        3,
                        2,
                        1,
                        ModBlocks.FOUNDRY_OUTLET
                                .get()
                                .defaultBlockState()
                                .setValue(FoundryOutletBlock.FACING, Direction.NORTH)));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetTile(3, 3, 3, tank(Materials.GOLD, MaterialShape.BLOCK.amount(1))));
        scene0.add(new ActionSetBlock(3, 3, 3, ModBlocks.FOUNDRY_TANK.get().defaultBlockState()));
        scene0.add(new ActionSetTile(3, 3, 2, outlet(3, 3, 2)));
        scene0.add(
                new ActionSetBlock(
                        3,
                        3,
                        2,
                        ModBlocks.FOUNDRY_OUTLET
                                .get()
                                .defaultBlockState()
                                .setValue(FoundryOutletBlock.FACING, Direction.NORTH)));
        scene0.add(new ActionWait(10));

        scene0.add(new ActionCreateActor(0, panel(0, 0, -25, Orientation.LEFT)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionCreateActor(0, panel(1, -5, -40, Orientation.TOP)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(10));
        scene0.add(
                new ActionSetTile(
                        3, 2, 2, channel(3, 2, 2, Materials.GOLD, MaterialShape.INGOT.amount(1))));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(0, panel(2, 0, -25, Orientation.LEFT)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));

        for (int i = 0; i < 60; i++) {
            int j = i;
            scene0.add(
                    new ActionSetTile(
                            3,
                            1,
                            1,
                            basin(
                                    3,
                                    1,
                                    1,
                                    new ItemStack(ModItems.byName("block")),
                                    Materials.GOLD,
                                    MaterialShape.BLOCK.share(j, 60),
                                    ItemStack.EMPTY)));
            scene0.add(new ActionWait(1));
        }

        JarScene scene1 = new JarScene(script);
        scene1.add(new ActionWait(10));
        scene1.add(new ActionCreateActor(0, panel(3, 0, -25, Orientation.LEFT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(10));
        scene1.add(
                new ActionSetTile(
                        2, 2, 2, channel(2, 2, 2, Materials.GOLD, MaterialShape.INGOT.amount(1))));
        scene1.add(new ActionWait(10));

        for (int i = 0; i < 60; i++) {
            int j = i;
            scene1.add(
                    new ActionSetTile(
                            2,
                            1,
                            1,
                            mold(
                                    2,
                                    1,
                                    1,
                                    new ItemStack(ModItems.byName("ingot")),
                                    Materials.GOLD,
                                    MaterialShape.INGOT.share(j, 60),
                                    ItemStack.EMPTY)));
            scene1.add(
                    new ActionSetTile(
                            1,
                            2,
                            2,
                            mold(
                                    1,
                                    2,
                                    2,
                                    new ItemStack(ModItems.byName("ingot")),
                                    Materials.GOLD,
                                    MaterialShape.INGOT.share(j, 60),
                                    ItemStack.EMPTY)));
            scene1.add(new ActionWait(1));
        }

        scene1.add(new ActionWait(2));
        scene1.add(new ActionSetTile(3, 3, 3, tank(null, 0)));
        scene1.add(new ActionWait(20));
        scene1.add(new ActionCreateActor(0, panel(4, 0, -25, Orientation.LEFT)));
        scene1.add(new ActionWait(40));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionSetTile(2, 2, 2, channel(2, 2, 2, null, 0)));
        scene1.add(new ActionSetTile(3, 2, 2, channel(3, 2, 2, null, 0)));
        scene1.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -25,
                                        new Object[][] {
                                            {
                                                new ItemStack(Items.IRON_SHOVEL),
                                                Component.literal(" -> "),
                                                ScrapsItem.create(
                                                        new MaterialStack(Materials.GOLD, 1))
                                            }
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.LEFT)));
        scene1.add(new ActionWait(40));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(60));
        scene1.add(
                new ActionSetTile(
                        2,
                        1,
                        1,
                        mold(2, 1, 1, ItemStack.EMPTY, null, 0, new ItemStack(Items.GOLD_INGOT))));
        scene1.add(
                new ActionSetTile(
                        1,
                        2,
                        2,
                        mold(1, 2, 2, ItemStack.EMPTY, null, 0, new ItemStack(Items.GOLD_INGOT))));
        scene1.add(
                new ActionSetTile(
                        3,
                        1,
                        1,
                        basin(
                                3,
                                1,
                                1,
                                ItemStack.EMPTY,
                                null,
                                0,
                                new ItemStack(Blocks.GOLD_BLOCK))));

        script.addScene(scene0).addScene(scene1);
        return script;
    }

    private static ActorFancyPanel panel(int index, int x, int y, Orientation orientation) {
        return new ActorFancyPanel(
                        x,
                        y,
                        new Object[][] {
                            {Component.translatable("cannery.foundryChannel." + index)}
                        },
                        200)
                .setColors(colorCopper)
                .setOrientation(orientation);
    }

    private static CompoundTag position(int x, int y, int z) {
        CompoundTag data = new CompoundTag();
        data.putInt("x", x);
        data.putInt("y", y);
        data.putInt("z", z);
        return data;
    }

    private static BlockState channelState(int x) {
        var state =
                ModBlocks.FOUNDRY_CHANNEL
                        .get()
                        .defaultBlockState()
                        .setValue(FoundryChannelBlock.NORTH, true)
                        .setValue(FoundryChannelBlock.WEST, true);
        return x == 2 ? state.setValue(FoundryChannelBlock.EAST, true) : state;
    }

    private static FoundryBasinBlockEntity basin(
            int x, int y, int z, ItemStack installed, Material type, int amount, ItemStack output) {
        var tile =
                new FoundryBasinBlockEntity(
                        new BlockPos(x, y, z), ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
        tile.inventory.set(0, installed);
        tile.inventory.set(1, output);
        tile.hbm$syncSet$FoundryBaseBlockEntity$type(type);
        tile.hbm$syncSet$FoundryBaseBlockEntity$amount(amount);
        return tile;
    }

    private static FoundryMoldBlockEntity mold(
            int x, int y, int z, ItemStack installed, Material type, int amount, ItemStack output) {
        var tile =
                new FoundryMoldBlockEntity(
                        new BlockPos(x, y, z), ModBlocks.FOUNDRY_MOLD.get().defaultBlockState());
        tile.inventory.set(0, installed);
        tile.inventory.set(1, output);
        tile.hbm$syncSet$FoundryBaseBlockEntity$type(type);
        tile.hbm$syncSet$FoundryBaseBlockEntity$amount(amount);
        return tile;
    }

    private static FoundryChannelBlockEntity channel(
            int x, int y, int z, Material type, int amount) {
        var tile = new FoundryChannelBlockEntity(new BlockPos(x, y, z), channelState(x));
        tile.hbm$syncSet$FoundryBaseBlockEntity$type(type);
        tile.hbm$syncSet$FoundryBaseBlockEntity$amount(amount);
        return tile;
    }

    private static FoundryDischarge outlet(int x, int y, int z) {
        return new FoundryDischarge(
                new BlockPos(x, y, z),
                ModBlocks.FOUNDRY_OUTLET
                        .get()
                        .defaultBlockState()
                        .setValue(FoundryOutletBlock.FACING, Direction.NORTH));
    }

    private static FoundryTankBlockEntity tank(Material type, int amount) {
        var tile =
                new FoundryTankBlockEntity(
                        new BlockPos(3, 3, 3), ModBlocks.FOUNDRY_TANK.get().defaultBlockState());
        tile.hbm$syncSet$FoundryBaseBlockEntity$type(type);
        tile.hbm$syncSet$FoundryBaseBlockEntity$amount(amount);
        return tile;
    }

    public static class ActorFoundry implements ActorRenderer {
        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            int x = data.getIntOr("x", 0), y = data.getIntOr("y", 0), z = data.getIntOr("z", 0);
            var tile = (FoundryCastingBaseBlockEntity) context.world().getBlockEntity(x, y, z);
            var foundry = (FoundryRenderProvider) tile;
            BlockEntityRenderer<?, ?> registered =
                    Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tile);
            FoundryRenderer renderer = (FoundryRenderer) registered;
            FoundryRenderer.State state = renderer.createRenderState();
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            state.moldHeight = foundry.moldHeight();
            state.outHeight = foundry.outHeight();
            state.minX = foundry.minX();
            state.maxX = foundry.maxX();
            state.minZ = foundry.minZ();
            state.maxZ = foundry.maxZ();
            state.shouldRender = foundry.shouldRender();
            if (state.shouldRender) {
                state.level = foundry.getMoltenLevel();
                state.color = foundry.getMat().moltenColor();
            }

            ItemStack mold = tile.inventory.get(0);
            ItemStack output = tile.inventory.get(1);
            if (output.getItem() instanceof BlockItem blockItem) {
                TextureAtlasSprite sprite =
                        Minecraft.getInstance()
                                .getModelManager()
                                .getBlockStateModelSet()
                                .getParticleMaterial(blockItem.getBlock().defaultBlockState())
                                .sprite();
                state.blockSprite = sprite;
                output = ItemStack.EMPTY;
            }
            var resolver = context.itemModelResolver();
            resolver.updateForTopItem(
                    state.mold,
                    mold,
                    ItemDisplayContext.NONE,
                    Minecraft.getInstance().level,
                    null,
                    0);
            resolver.updateForTopItem(
                    state.out,
                    output,
                    ItemDisplayContext.NONE,
                    Minecraft.getInstance().level,
                    null,
                    0);

            PoseStack ps = context.pose();
            ps.pushPose();
            ps.translate(x, y, z);
            renderer.submit(state, ps, context.collector(), null);
            ps.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {}
    }

    public static class ActorFoundryWorld extends ActorBase {
        private final FoundryTankRenderer tankRenderer = new FoundryTankRenderer();
        private final FoundryChannelRenderer channelRenderer = new FoundryChannelRenderer();

        @Override
        public void drawInJar(JarRenderContext context, int ticks, float interp) {
            WorldInAJar world = context.world();
            for (int x = 0; x < world.sizeX; x++) {
                for (int y = 0; y < world.sizeY; y++) {
                    for (int z = 0; z < world.sizeZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (world.getBlockEntity(pos) instanceof FoundryTankBlockEntity tank) {
                            var state = tankRenderer.createRenderState();
                            state.doRender = tank.amount > 0 && tank.type != null;
                            if (!state.doRender) continue;
                            int mask = FoundryTankBlock.mask(world, pos);
                            for (Direction dir : Direction.VALUES)
                                state.tank[dir.ordinal()] = (mask & 1 << dir.ordinal()) != 0;
                            double max =
                                    0.75D
                                            + (state.tank[Direction.DOWN.ordinal()] ? 0.125D : 0)
                                            + (state.tank[Direction.UP.ordinal()] ? 0.125D : 0);
                            state.level = (float) (tank.amount * max / tank.getCapacity());
                            state.color = FoundryFaces.brightenMolten(tank.type.moltenColor());
                            context.pose().pushPose();
                            context.pose().translate(x, y, z);
                            tankRenderer.submit(state, context.pose(), context.collector(), null);
                            context.pose().popPose();
                        } else if (world.getBlockEntity(pos)
                                instanceof FoundryChannelBlockEntity channel) {
                            var state = channelRenderer.createRenderState();
                            state.doRender = channel.amount > 0 && channel.type != null;
                            if (!state.doRender) continue;
                            state.level = (float) (channel.amount * 0.25D / channel.getCapacity());
                            state.color = FoundryFaces.brightenMolten(channel.type.moltenColor());
                            var block = world.getBlockState(pos);
                            state.north = block.getValue(FoundryChannelBlock.NORTH);
                            state.south = block.getValue(FoundryChannelBlock.SOUTH);
                            state.west = block.getValue(FoundryChannelBlock.WEST);
                            state.east = block.getValue(FoundryChannelBlock.EAST);
                            context.pose().pushPose();
                            context.pose().translate(x, y, z);
                            channelRenderer.submit(
                                    state, context.pose(), context.collector(), null);
                            context.pose().popPose();
                        }
                    }
                }
            }
        }

        @Override
        public void drawOverlay(
                GuiGraphicsExtractor graphics, int width, int height, int ticks, float interp) {}

        @Override
        public void tick(JarScene scene) {}
    }
}
