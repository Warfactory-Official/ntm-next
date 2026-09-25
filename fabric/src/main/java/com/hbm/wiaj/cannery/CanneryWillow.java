// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.wiaj.cannery;

import com.hbm.blocks.ModBlocks;
import com.hbm.items.EnumPlantType;
import com.hbm.items.ModItems;
import com.hbm.wiaj.JarScene;
import com.hbm.wiaj.JarScript;
import com.hbm.wiaj.WorldInAJar;
import com.hbm.wiaj.actions.ActionCreateActor;
import com.hbm.wiaj.actions.ActionRemoveActor;
import com.hbm.wiaj.actions.ActionSetBlock;
import com.hbm.wiaj.actions.ActionSetZoom;
import com.hbm.wiaj.actions.ActionWait;
import com.hbm.wiaj.actors.ActorFancyPanel.Orientation;
import com.hbm.wiaj.actors.ActorFancyPanel;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.DoublePlantBlock;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;

public class CanneryWillow extends CanneryBase {

    @Override
    public ItemStack getIcon() {
        return ModItems.PLANT_ITEM.stack(EnumPlantType.MUSTARDWILLOW);
    }

    @Override
    public String getName() {
        return "cannery.willow";
    }

    @Override
    public JarScript createScript() {

        WorldInAJar world = new WorldInAJar(5, 5, 5);
        JarScript script = new JarScript(world);

        JarScene scene0 = new JarScene(script);
        scene0.add(new ActionSetZoom(3, 0));

        for (int x = world.sizeX - 1; x >= 0; x--) {
            for (int z = 0; z < world.sizeZ; z++) {
                scene0.add(new ActionSetBlock(x, 1, z, Blocks.GRASS_BLOCK.defaultBlockState()));
                scene0.add(new ActionSetBlock(x, 0, z, Blocks.DIRT.defaultBlockState()));
            }
            scene0.add(new ActionWait(2));
        }

        scene0.add(new ActionWait(8));
        scene0.add(
                new ActionSetBlock(2, 2, 2, ModBlocks.PLANT_FLOWER_CD0.get().defaultBlockState()));

        scene0.add(new ActionWait(10));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.0")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.1")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(0));

        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(2, 1, 1, Blocks.AIR.defaultBlockState()));
        scene0.add(new ActionWait(10));
        scene0.add(new ActionSetBlock(2, 1, 1, Blocks.WATER.defaultBlockState()));

        scene0.add(new ActionWait(20));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.2")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionSetBlock(2, 2, 2, ModBlocks.PLANT_FLOWER_CD1.get().defaultBlockState()));

        scene0.add(new ActionWait(20));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.3")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(80));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionSetBlock(
                        2,
                        2,
                        2,
                        ModBlocks.PLANT_TALL_CD2
                                .get()
                                .defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
        scene0.add(
                new ActionSetBlock(
                        2,
                        3,
                        2,
                        ModBlocks.PLANT_TALL_CD2
                                .get()
                                .defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)));

        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionSetBlock(
                        2,
                        2,
                        2,
                        ModBlocks.PLANT_TALL_CD3
                                .get()
                                .defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
        scene0.add(
                new ActionSetBlock(
                        2,
                        3,
                        2,
                        ModBlocks.PLANT_TALL_CD3
                                .get()
                                .defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)));
        scene0.add(new ActionWait(20));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -35,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.4")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(80));
        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -35,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.5")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));

        scene0.add(new ActionWait(100));
        scene0.add(new ActionRemoveActor(0));
        scene0.add(new ActionWait(20));
        scene0.add(new ActionSetBlock(2, 1, 2, ModBlocks.DIRT_OILY.get().defaultBlockState()));

        scene0.add(new ActionWait(20));
        scene0.add(
                new ActionSetBlock(
                        2,
                        2,
                        2,
                        ModBlocks.PLANT_TALL_CD4
                                .get()
                                .defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.LOWER)));
        scene0.add(
                new ActionSetBlock(
                        2,
                        3,
                        2,
                        ModBlocks.PLANT_TALL_CD4
                                .get()
                                .defaultBlockState()
                                .setValue(DoublePlantBlock.HALF, DoubleBlockHalf.UPPER)));
        scene0.add(new ActionSetBlock(2, 1, 2, Blocks.DIRT.defaultBlockState()));
        scene0.add(new ActionWait(20));

        scene0.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -35,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.6")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene0.add(new ActionWait(100));
        scene0.add(new ActionRemoveActor(0));

        JarScene scene1 = new JarScene(script);
        scene1.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -35,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.7")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(100));
        scene1.add(new ActionRemoveActor(0));

        scene1.add(new ActionWait(20));
        scene1.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        10,
                                        -25,
                                        new Object[][] {
                                            {
                                                Component.literal("="),
                                                new ItemStack(ModBlocks.PLANT_FLOWER_CD0),
                                                Component.literal("x1 + "),
                                                ModItems.PLANT_ITEM.stack(
                                                        EnumPlantType.MUSTARDWILLOW),
                                                Component.literal("x3-6")
                                            }
                                        },
                                        0)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.LEFT)));
        scene1.add(new ActionSetBlock(2, 3, 2, Blocks.AIR.defaultBlockState()));
        scene1.add(
                new ActionSetBlock(2, 2, 2, ModBlocks.PLANT_FLOWER_CD0.get().defaultBlockState()));
        scene1.add(new ActionWait(60));

        scene1.add(new ActionRemoveActor(0));
        scene1.add(new ActionWait(20));
        scene1.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.8")}
                                        },
                                        200)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(100));

        scene1.add(
                new ActionCreateActor(
                        0,
                        new ActorFancyPanel(
                                        0,
                                        -15,
                                        new Object[][] {
                                            {Component.translatable("cannery.willow.9")}
                                        },
                                        150)
                                .setColors(colorCopper)
                                .setOrientation(Orientation.BOTTOM)));
        scene1.add(new ActionWait(100));
        scene1.add(new ActionRemoveActor(0));

        script.addScene(scene0).addScene(scene1);
        return script;
    }
}
