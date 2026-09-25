// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.blocks.ModBlocks;
import com.hbm.handler.ArmorModHandler;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

public class ItemModSensor extends ItemArmorMod {

    private static final int REACH = 3, HEIGHT = 1, STRIDE = 2;
    private static final int CADENCE = 20;

    public ItemModSensor(Properties properties) {
        super(properties, ArmorModHandler.EXTRA, true, true, true, true);
    }

    @Override
    protected void addSpecificTooltip(ItemStack stack, Consumer<Component> adder) {
        adder.accept(
                Component.translatable("desc.item.armorMod.sensor")
                        .withStyle(ChatFormatting.YELLOW));
        adder.accept(
                Component.translatable("desc.item.armorMod.sensor.anywhere")
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void addDescription(List<Component> tooltip, ItemStack stack, ItemStack armor) {
        tooltip.add(
                Component.translatable("desc.item.armorMod.sensor.installed", stack.getHoverName())
                        .withStyle(ChatFormatting.YELLOW));
    }

    @Override
    public void inventoryTick(
            ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (owner instanceof LivingEntity living) modUpdate(living, ItemStack.EMPTY);
    }

    @Override
    public void modUpdate(LivingEntity entity, ItemStack armor) {
        Level level = entity.level();
        if (level.isClientSide() || level.getGameTime() % CADENCE != 0) return;

        int x = Mth.floor(entity.getX());
        int y = Mth.floor(entity.getEyeY());
        int z = Mth.floor(entity.getZ());

        boolean poison = false;
        boolean explosive = false;
        BlockPos.MutableBlockPos at = new BlockPos.MutableBlockPos();
        for (int i = -REACH; i <= REACH; i++) {
            for (int j = -HEIGHT; j <= HEIGHT; j++) {
                for (int k = -REACH; k <= REACH; k++) {
                    BlockState state =
                            level.getBlockState(
                                    at.set(x + i * STRIDE, y + j * STRIDE, z + k * STRIDE));
                    if (isPoison(state)) poison = true;
                    if (isExplosive(state)) explosive = true;
                }
            }
        }

        if (explosive) return;
        if (poison) {
            level.playSound(
                    null,
                    entity.blockPosition(),
                    ModSounds.TECH_BOOP.get(),
                    SoundSource.PLAYERS,
                    2F,
                    1.5F);
        }
    }

    private static boolean isPoison(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.GAS_ASBESTOS.get()
                || block == ModBlocks.GAS_COAL.get()
                || block == ModBlocks.GAS_RADON.get()
                || block == ModBlocks.GAS_MONOXIDE.get()
                || block == ModBlocks.GAS_RADON_DENSE.get()
                || block == ModBlocks.CHLORINE_GAS.get();
    }

    private static boolean isExplosive(BlockState state) {
        Block block = state.getBlock();
        return block == ModBlocks.GAS_FLAMMABLE.get() || block == ModBlocks.GAS_EXPLOSIVE.get();
    }
}
