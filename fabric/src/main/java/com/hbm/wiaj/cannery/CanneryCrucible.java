// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.model.Meshes;
import com.hbm.client.render.RenderCrucible;
import com.hbm.data.MachineData;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemScraps;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityCrucible;
import com.hbm.tileentity.machine.BlockEntityFoundryBasin;
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
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.ITileActorRenderer;
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
                        1, new ActorTileEntity(new ActorCrucible(), position(2, 2, 2))));
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
                        2, new ActorTileEntity(new CanneryFirebox.ActorFirebox(), firebox)));
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

        scene2.add(new ActionSetTile(2, 2, 2, crucible(MaterialShapes.BLOCK.q(16))));
        scene2.add(new ActionWait(10));
        scene2.add(new ActionSetTile(0, 1, 2, basin(ItemStack.EMPTY, null, 0, ItemStack.EMPTY)));
        scene2.add(
                new ActionCreateActor(
                        3,
                        new ActorTileEntity(
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
                    new ActionSetTile(2, 2, 2, crucible(MaterialShapes.BLOCK.q(60 - j, 60) * 16)));
            scene2.add(
                    new ActionSetTile(
                            0,
                            1,
                            2,
                            basin(
                                    new ItemStack(ModItems.byName("block")),
                                    Mats.MAT_GOLD,
                                    MaterialShapes.BLOCK.q(j + 1, 60),
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
                                                ItemScraps.create(
                                                        new MaterialStack(Mats.MAT_GOLD, 1))
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

    private static BlockEntityCrucible crucible(int gold) {
        BlockEntityCrucible tile =
                new BlockEntityCrucible(
                        new BlockPos(2, 2, 2),
                        ModBlocks.MACHINE_CRUCIBLE
                                .get()
                                .defaultBlockState()
                                .setValue(BlockMultiblockCore.FACING, Direction.WEST));
        if (gold > 0) tile.recipeStack.add(new MaterialStack(Mats.MAT_GOLD, gold));
        return tile;
    }

    private static BlockEntityFoundryBasin basin(
            ItemStack mold,
            com.hbm.inventory.material.NTMMaterial type,
            int amount,
            ItemStack output) {
        BlockEntityFoundryBasin tile =
                new BlockEntityFoundryBasin(
                        new BlockPos(0, 1, 2), ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
        tile.inventory.set(0, mold);
        tile.inventory.set(1, output);
        tile.type = type;
        tile.amount = amount;
        return tile;
    }

    public static class ActorCrucible implements ITileActorRenderer {
        private static final HFRWavefrontObject MODEL =
                Meshes.flatShaded(Meshes.load(Library.id("models/machines/crucible.obj")));
        private static final RenderType BODY =
                RenderTypes.entityCutoutCull(ResourceManager.crucible_tex);
        private final RenderCrucible renderer = new RenderCrucible();

        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            int x = data.getIntOr("x", 0), y = data.getIntOr("y", 0), z = data.getIntOr("z", 0);
            BlockEntityCrucible tile = (BlockEntityCrucible) context.world().getTileEntity(x, y, z);
            var ps = context.pose();
            ps.pushPose();
            ps.translate(x, y, z);
            ps.pushPose();
            ps.translate(0.5, 0, 0.5);
            ps.mulPose(Axis.YP.rotationDegrees(180));
            context.collector()
                    .submitCustomGeometry(
                            ps,
                            BODY,
                            (pose, buffer) ->
                                    MODEL.render(pose, buffer, LightCoordsUtil.FULL_BRIGHT, -1));
            ps.popPose();

            RenderCrucible.State state = renderer.createRenderState();
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
