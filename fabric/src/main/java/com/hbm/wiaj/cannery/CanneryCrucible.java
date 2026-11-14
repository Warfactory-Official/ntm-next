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

import com.hbm.blockentity.machine.CrucibleBlockEntity;
import com.hbm.blockentity.machine.FoundryBasinBlockEntity;
import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.MultiblockCoreBlock;
import com.hbm.client.model.Meshes;
import com.hbm.client.render.CrucibleRenderer;
import com.hbm.data.MachineData;
import com.hbm.inventory.material.Material;
import com.hbm.inventory.material.MaterialShape;
import com.hbm.inventory.material.MaterialStack;
import com.hbm.inventory.material.Materials;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ScrapsItem;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import com.hbm.wiaj.JarRenderContext;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionRotateBy;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetTile;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionUpdateActor;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorBlockEntity;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorRenderer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import org.joml.Quaternionfc;

public class CanneryCrucible extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return new ItemStack(ModBlocks.MACHINE_CRUCIBLE);
    }

    @Override
    public String getName() {
        return "cannery.crucible";
    }

    @Override
    public CanneryBase[] seeAlso() {
        return new CanneryBase[] {new CanneryFoundryChannel()};
    }

    @Override
    public JarScript createScript() {
        WorldInAJar world = new WorldInAJar(5, 4, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);
        scene0.add(new ActionSetZoom(3, 0));
        for (int x = 0; x < 5; x++) {
            for (int z = 0; z < 5; z++) {
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.BRICKS.defaultBlockState()));
            }
        }
        scene0.add(new ActionWait(5));
        for (int x = 1; x < 4; x++) {
            for (int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(x, 1, z, Blocks.BRICKS.defaultBlockState()));
            }
        }
        scene0.add(new ActionWait(5));

        scene0.add(new ActionSetTile(2, 2, 2, crucible(0)));
        scene0.add(
                new ActionCreateActor(
                        1, new ActorBlockEntity(new ActorCrucible(), position(2, 2, 2))));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionCreateActor(0, panel(0, 0, -30, Orientation.BOTTOM)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionCreateActor(0, panel(1, 0, -30, Orientation.BOTTOM)));
        scene0.add(new ActionWait(60));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(10));

        for (int x = 1; x < 4; x++) {
            for (int z = 1; z < 4; z++) {
                scene0.add(new ActionSetBlock(x, 1, z, Blocks.AIR.defaultBlockState()));
            }
        }

        CompoundTag firebox = new CompoundTag();
        firebox.putDouble("x", 2);
        firebox.putDouble("y", 1);
        firebox.putDouble("z", 2);
        firebox.putInt("rotation", 5);
        scene0.add(
                new ActionCreateActor(
                        2, new ActorBlockEntity(new CanneryFirebox.ActorFirebox(), firebox)));
        scene0.add(new ActionUpdateActor(2, "open", true));
        scene0.add(new ActionWait(30));
        scene0.add(new ActionUpdateActor(2, "isOn", true));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        -50, 25, new Object[][] {{new ItemStack(Items.COAL)}}, 0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.RIGHT)));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionUpdateActor(2, "open", false));
        scene0.add(new ActionWait(30));

        JarScene scene1 = new JarScene(script);
        scene1.add(new ActionCreateActor(0, panel(2, 0, -30, Orientation.BOTTOM)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionRotateBy(45, -60, 20));
        scene1.add(new ActionWait(10));

        scene1.add(new ActionCreateActor(0, panel(3, 0, 0, Orientation.CENTER)));
        scene1.add(new ActionWait(40));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, panel(4, -30, 0, Orientation.LEFT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, panel(5, -30, 0, Orientation.LEFT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, panel(6, -30, 0, Orientation.LEFT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(20));
        scene1.add(new ActionCreateActor(0, panel(7, 30, 0, Orientation.RIGHT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, panel(8, 30, 0, Orientation.RIGHT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionCreateActor(0, panel(9, 30, 0, Orientation.RIGHT)));
        scene1.add(new ActionWait(60));
        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionRotateBy(-45, 60, 20));
        scene1.add(new ActionWait(10));

        JarScene scene2 = new JarScene(script);
        scene2.add(new ActionCreateActor(0, panel(10, 30, 0, Orientation.RIGHT)));
        scene2.add(new ActionWait(60));
        scene2.add(new ActionRemoveActor(0));
        scene2.add(new ActionWait(10));
        scene2.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -40,
                                        new Object[][] {{new ItemStack(Items.GOLD_INGOT, 11)}},
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));

        scene2.add(new ActionSetTile(2, 2, 2, crucible(MaterialShape.BLOCK.amount(16))));
        scene2.add(new ActionWait(10));
        scene2.add(new ActionSetTile(0, 1, 2, basin(ItemStack.EMPTY, null, 0, ItemStack.EMPTY)));
        scene2.add(
                new ActionCreateActor(
                        3,
                        new ActorBlockEntity(
                                new CanneryFoundryChannel.ActorFoundry(), position(0, 1, 2))));
        scene2.add(new ActionSetBlock(0, 1, 2, ModBlocks.FOUNDRY_BASIN.get().defaultBlockState()));

        scene2.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        42,
                                        25,
                                        new Object[][] {{new ItemStack(ModItems.byName("block"))}},
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));
        scene2.add(
                new ActionSetTile(
                        0,
                        1,
                        2,
                        basin(new ItemStack(ModItems.byName("block")), null, 0, ItemStack.EMPTY)));
        scene2.add(new ActionWait(20));

        for (int i = 0; i < 60; i++) {
            int j = i;
            scene2.add(
                    new ActionSetTile(
                            2, 2, 2, crucible(MaterialShape.BLOCK.share(60 - j, 60) * 16)));
            scene2.add(
                    new ActionSetTile(
                            0,
                            1,
                            2,
                            basin(
                                    new ItemStack(ModItems.byName("block")),
                                    Materials.GOLD,
                                    MaterialShape.BLOCK.share(j + 1, 60),
                                    ItemStack.EMPTY)));
            scene2.add(new ActionWait(1));
        }

        scene2.add(new ActionWait(40));
        scene2.add(
                new ActionSetTile(
                        0,
                        1,
                        2,
                        basin(
                                new ItemStack(ModItems.byName("block")),
                                null,
                                0,
                                new ItemStack(Blocks.GOLD_BLOCK))));
        scene2.add(new ActionWait(20));
        scene2.add(
                new ActionSetTile(
                        0,
                        1,
                        2,
                        basin(new ItemStack(ModItems.byName("block")), null, 0, ItemStack.EMPTY)));
        scene2.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        42,
                                        25,
                                        new Object[][] {{new ItemStack(Blocks.GOLD_BLOCK)}},
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));

        scene2.add(new ActionCreateActor(0, panel(11, 0, -30, Orientation.BOTTOM)));
        scene2.add(new ActionWait(60));
        scene2.add(new ActionRemoveActor(0));
        scene2.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -30,
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
                                .setOrientation(Orientation.BOTTOM)));
        scene2.add(new ActionSetTile(2, 2, 2, crucible(0)));
        scene2.add(new ActionWait(20));
        scene2.add(new ActionRemoveActor(0));

        script.addScene(scene0).addScene(scene1).addScene(scene2);
        return script;
    }

    private static ActorFancyPanel panel(int index, int x, int y, Orientation orientation) {
        return new ActorFancyPanel(
                        x,
                        y,
                        new Object[][] {{Component.translatable("cannery.crucible." + index)}},
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

    private static CrucibleBlockEntity crucible(int gold) {
        CrucibleBlockEntity tile =
                new CrucibleBlockEntity(
                        new BlockPos(2, 2, 2),
                        ModBlocks.MACHINE_CRUCIBLE
                                .get()
                                .defaultBlockState()
                                .setValue(MultiblockCoreBlock.FACING, Direction.WEST));
        if (gold > 0) tile.recipeStack.add(new MaterialStack(Materials.GOLD, gold));
        return tile;
    }

    private static FoundryBasinBlockEntity basin(
            ItemStack mold, Material type, int amount, ItemStack output) {
        FoundryBasinBlockEntity tile =
                new FoundryBasinBlockEntity(
                        new BlockPos(0, 1, 2), ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
        tile.inventory.set(0, mold);
        tile.inventory.set(1, output);
        tile.hbm$syncSet$FoundryBaseBlockEntity$type(type);
        tile.hbm$syncSet$FoundryBaseBlockEntity$amount(amount);
        return tile;
    }

    public static class ActorCrucible implements ActorRenderer {
        private static final HFRWavefrontObject MODEL =
                Meshes.flatShaded(Meshes.load(RegistryUtil.id("models/machines/crucible.obj")));
        private static final RenderType BODY =
                RenderTypes.entityCutoutCull(ResourceManager.crucible_tex);
        private final CrucibleRenderer renderer = new CrucibleRenderer();

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            int x = data.getIntOr("x", 0), y = data.getIntOr("y", 0), z = data.getIntOr("z", 0);
            CrucibleBlockEntity tile =
                    (CrucibleBlockEntity) context.world().getBlockEntity(x, y, z);
            var ps = context.pose();
            ps.pushPose();
            ps.translate(x, y, z);
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose((Quaternionfc) Axis.YP.rotationDegrees(180));
            context.collector()
                    .submitCustomGeometry(
                            ps,
                            BODY,
                            (pose, buffer) ->
                                    MODEL.render(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));
            ps.popPose();

            CrucibleRenderer.State state = renderer.createRenderState();
            state.facing = Direction.WEST;
            state.lightCoords = LightCoordsUtil.FULL_BRIGHT;
            int mass = 0;
            for (MaterialStack stack : tile.recipeStack) mass += stack.amount;
            for (MaterialStack stack : tile.wasteStack) mass += stack.amount;
            int cap =
                    MachineData.CRUCIBLE_RECIPE_CAPACITY.get()
                            + MachineData.CRUCIBLE_WASTE_CAPACITY.get();
            state.meltLevel = mass == 0 ? -1 : (double) mass / cap * 0.875D;
            renderer.submit(state, ps, context.collector(), null);
            ps.popPose();
        }

        @Override
        public void updateActor(int ticks, CompoundTag data) {}
    }
}
