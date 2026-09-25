// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.machine.FoundryChannel;
import com.hbm.blocks.machine.FoundryOutlet;
import com.hbm.blocks.machine.FoundryTank;
import com.hbm.client.render.FoundryFaces;
import com.hbm.client.render.RenderFoundry;
import com.hbm.client.render.RenderFoundryChannel;
import com.hbm.client.render.RenderFoundryTank;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats.MaterialStack;
import com.hbm.inventory.material.Mats;
import com.hbm.inventory.material.NTMMaterial;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemScraps;
import com.hbm.tileentity.machine.BlockEntityFoundryBasin;
import com.hbm.tileentity.machine.BlockEntityFoundryCastingBase;
import com.hbm.tileentity.machine.BlockEntityFoundryChannel;
import com.hbm.tileentity.machine.BlockEntityFoundryMold;
import com.hbm.tileentity.machine.BlockEntityFoundryOutlet;
import com.hbm.tileentity.machine.BlockEntityFoundryTank;
import com.hbm.tileentity.machine.IRenderFoundry;
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
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import com.hbm.wiaj.actors.ActorTileEntity;
import com.hbm.wiaj.actors.ITileActorRenderer;
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
                        1, new ActorTileEntity(new ActorFoundry(), position(3, 1, 1))));
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
                        2, new ActorTileEntity(new ActorFoundry(), position(2, 1, 1))));
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
                        3, new ActorTileEntity(new ActorFoundry(), position(1, 2, 2))));
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
                                .setValue(FoundryOutlet.FACING, Direction.NORTH)));
        scene0.add(new ActionSetTile(3, 2, 1, outlet(3, 2, 1)));
        scene0.add(
                new ActionSetBlock(
                        3,
                        2,
                        1,
                        ModBlocks.FOUNDRY_OUTLET
                                .get()
                                .defaultBlockState()
                                .setValue(FoundryOutlet.FACING, Direction.NORTH)));
        scene0.add(new ActionWait(5));
        scene0.add(new ActionSetTile(3, 3, 3, tank(Mats.MAT_GOLD, MaterialShapes.BLOCK.q(1))));
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
                                .setValue(FoundryOutlet.FACING, Direction.NORTH)));
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
                        3, 2, 2, channel(3, 2, 2, Mats.MAT_GOLD, MaterialShapes.INGOT.q(1))));
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
                                    Mats.MAT_GOLD,
                                    MaterialShapes.BLOCK.q(j, 60),
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
                        2, 2, 2, channel(2, 2, 2, Mats.MAT_GOLD, MaterialShapes.INGOT.q(1))));
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
                                    Mats.MAT_GOLD,
                                    MaterialShapes.INGOT.q(j, 60),
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
                                    Mats.MAT_GOLD,
                                    MaterialShapes.INGOT.q(j, 60),
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
                                                ItemScraps.create(
                                                        new MaterialStack(Mats.MAT_GOLD, 1))
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

    private static net.minecraft.world.level.block.state.BlockState channelState(int x) {
        var state =
                ModBlocks.FOUNDRY_CHANNEL
                        .get()
                        .defaultBlockState()
                        .setValue(FoundryChannel.NORTH, true)
                        .setValue(FoundryChannel.WEST, true);
        return x == 2 ? state.setValue(FoundryChannel.EAST, true) : state;
    }

    private static BlockEntityFoundryBasin basin(
            int x,
            int y,
            int z,
            ItemStack installed,
            NTMMaterial type,
            int amount,
            ItemStack output) {
        var tile =
                new BlockEntityFoundryBasin(
                        new BlockPos(x, y, z), ModBlocks.FOUNDRY_BASIN.get().defaultBlockState());
        tile.inventory.set(0, installed);
        tile.inventory.set(1, output);
        tile.type = type;
        tile.amount = amount;
        return tile;
    }

    private static BlockEntityFoundryMold mold(
            int x,
            int y,
            int z,
            ItemStack installed,
            NTMMaterial type,
            int amount,
            ItemStack output) {
        var tile =
                new BlockEntityFoundryMold(
                        new BlockPos(x, y, z), ModBlocks.FOUNDRY_MOLD.get().defaultBlockState());
        tile.inventory.set(0, installed);
        tile.inventory.set(1, output);
        tile.type = type;
        tile.amount = amount;
        return tile;
    }

    private static BlockEntityFoundryChannel channel(
            int x, int y, int z, NTMMaterial type, int amount) {
        var tile = new BlockEntityFoundryChannel(new BlockPos(x, y, z), channelState(x));
        tile.type = type;
        tile.amount = amount;
        return tile;
    }

    private static BlockEntityFoundryOutlet outlet(int x, int y, int z) {
        return new BlockEntityFoundryOutlet(
                new BlockPos(x, y, z),
                ModBlocks.FOUNDRY_OUTLET
                        .get()
                        .defaultBlockState()
                        .setValue(FoundryOutlet.FACING, Direction.NORTH));
    }

    private static BlockEntityFoundryTank tank(NTMMaterial type, int amount) {
        var tile =
                new BlockEntityFoundryTank(
                        new BlockPos(3, 3, 3), ModBlocks.FOUNDRY_TANK.get().defaultBlockState());
        tile.type = type;
        tile.amount = amount;
        return tile;
    }

    public static class ActorFoundry implements ITileActorRenderer {
        @Override
        public void renderActor(
                JarRenderContext context, int ticks, float interp, CompoundTag data) {
            int x = data.getIntOr("x", 0), y = data.getIntOr("y", 0), z = data.getIntOr("z", 0);
            var tile = (BlockEntityFoundryCastingBase) context.world().getTileEntity(x, y, z);
            var foundry = (IRenderFoundry) tile;
            BlockEntityRenderer<?, ?> registered =
                    Minecraft.getInstance().getBlockEntityRenderDispatcher().getRenderer(tile);
            RenderFoundry renderer = (RenderFoundry) registered;
            RenderFoundry.State state = renderer.createRenderState();
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
                state.color = foundry.getMat().moltenColor;
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
        private final RenderFoundryTank tankRenderer = new RenderFoundryTank();
        private final RenderFoundryChannel channelRenderer = new RenderFoundryChannel();

        @Override
        public void drawBackgroundComponent(JarRenderContext context, int ticks, float interp) {
            WorldInAJar world = context.world();
            for (int x = 0; x < world.sizeX; x++) {
                for (int y = 0; y < world.sizeY; y++) {
                    for (int z = 0; z < world.sizeZ; z++) {
                        BlockPos pos = new BlockPos(x, y, z);
                        if (world.getBlockEntity(pos) instanceof BlockEntityFoundryTank tank) {
                            var state = tankRenderer.createRenderState();
                            state.doRender = tank.amount > 0 && tank.type != null;
                            if (!state.doRender) continue;
                            int mask = FoundryTank.mask(world, pos);
                            for (Direction dir : Direction.VALUES)
                                state.tank[dir.ordinal()] = (mask & 1 << dir.ordinal()) != 0;
                            double max =
                                    0.75D
                                            + (state.tank[Direction.DOWN.ordinal()] ? 0.125D : 0)
                                            + (state.tank[Direction.UP.ordinal()] ? 0.125D : 0);
                            state.level = (float) (tank.amount * max / tank.getCapacity());
                            state.color = FoundryFaces.brightenMolten(tank.type.moltenColor);
                            context.pose().pushPose();
                            context.pose().translate(x, y, z);
                            tankRenderer.submit(state, context.pose(), context.collector(), null);
                            context.pose().popPose();
                        } else if (world.getBlockEntity(pos)
                                instanceof BlockEntityFoundryChannel channel) {
                            var state = channelRenderer.createRenderState();
                            state.doRender = channel.amount > 0 && channel.type != null;
                            if (!state.doRender) continue;
                            state.level = (float) (channel.amount * 0.25D / channel.getCapacity());
                            state.color = FoundryFaces.brightenMolten(channel.type.moltenColor);
                            var block = world.getBlockState(pos);
                            state.north = block.getValue(FoundryChannel.NORTH);
                            state.south = block.getValue(FoundryChannel.SOUTH);
                            state.west = block.getValue(FoundryChannel.WEST);
                            state.east = block.getValue(FoundryChannel.EAST);
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
        public void drawForegroundComponent(
                GuiGraphicsExtractor graphics, int width, int height, int ticks, float interp) {}

        @Override
        public void updateActor(JarScene scene) {}
    }
}
