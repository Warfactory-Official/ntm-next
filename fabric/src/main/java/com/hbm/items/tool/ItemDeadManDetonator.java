// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.tool;

import com.hbm.NuclearTech;
import com.hbm.capability.NtmContracts;
import com.hbm.data.ItemData;
import com.hbm.interfaces.IBomb;
import com.hbm.interfaces.IItemEntityUpdate;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;

public class ItemDeadManDetonator extends Item implements IItemEntityUpdate {

    private static final float SWITCH_BLAST = 0.0F;
    private static final float EXPLOSIVE_BLAST = 15.0F;

    private final boolean linked;

    public ItemDeadManDetonator(Properties properties, boolean linked) {
        super(properties);
        this.linked = linked;
    }

    @Override
    public boolean updateDroppedItem(ItemEntity entity) {
        if (!(entity.level() instanceof ServerLevel level)) return false;

        if (linked) {
            Long packed = entity.getItem().get(ModDataComponents.DETONATOR_POS.get());
            if (packed != null) {
                BlockPos target = ItemDetonator.bombTarget(level, BlockPos.of(packed));
                IBomb bomb = NtmContracts.BOMB.at(level, target);
                if (bomb != null) bomb.explode(level, target, entity);
                if (bomb != null && Services.CONFIG.runtime().extendedLogging()) {
                    NuclearTech.LOGGER.info(
                            "[DET] Tried to detonate block at {} / {} / {} by dead man's switch!",
                            target.getX(),
                            target.getY(),
                            target.getZ());
                }
            }
            level.explode(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    SWITCH_BLAST,
                    Level.ExplosionInteraction.BLOCK);
            entity.discard();
            return true;
        }

        if (ItemData.DROP_DEAD_MAN.get()) {
            level.explode(
                    entity,
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    EXPLOSIVE_BLAST,
                    Level.ExplosionInteraction.BLOCK);
            if (Services.CONFIG.runtime().extendedLogging()) {
                NuclearTech.LOGGER.info(
                        "[DET] Detonated dead man's explosive at {} / {} / {}!",
                        (int) entity.getX(),
                        (int) entity.getY(),
                        (int) entity.getZ());
            }
        }
        entity.discard();
        return true;
    }

    public static void onOwnerDeath(ServerPlayer player) {
        ServerLevel level = player.level();
        Inventory inventory = player.getInventory();
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            Long packed =
                    stack.is(ModItems.DETONATOR_DEADMAN.get())
                            ? stack.get(ModDataComponents.DETONATOR_POS.get())
                            : null;
            if (packed == null) continue;
            BlockPos target = ItemDetonator.bombTarget(level, BlockPos.of(packed));
            IBomb bomb = NtmContracts.BOMB.at(level, target);
            if (bomb != null) {
                bomb.explode(level, target, player);
                if (Services.CONFIG.runtime().extendedLogging()) {
                    NuclearTech.LOGGER.info(
                            "[DET] Tried to detonate block at {} / {} / {} by dead man's switch from {}!",
                            target.getX(),
                            target.getY(),
                            target.getZ(),
                            player.getDisplayName().getString());
                }
            }
            inventory.setItem(i, ItemStack.EMPTY);
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Player player = context.getPlayer();
        if (!linked || player == null || !player.isShiftKeyDown()) return InteractionResult.PASS;

        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        context.getItemInHand().set(ModDataComponents.DETONATOR_POS.get(), pos.asLong());
        if (!level.isClientSide()) {
            player.sendSystemMessage(Component.translatable("chat.deadman.positionSet"));
            level.playSound(null, pos, ModSounds.TECH_BOOP.get(), SoundSource.PLAYERS, 2.0F, 1.0F);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        if (linked) {
            adder.accept(Component.translatable("desc.item.deadman.shiftRightClick"));
            adder.accept(Component.translatable("desc.item.deadman.dropToDetonate"));
            Long packed = stack.get(ModDataComponents.DETONATOR_POS.get());
            if (packed == null) {
                adder.accept(Component.translatable("desc.item.deadman.noPositionSet"));
            } else {
                BlockPos pos = BlockPos.of(packed);
                adder.accept(
                        Component.translatable(
                                "desc.item.deadman.setPosTo", pos.getX(), pos.getY(), pos.getZ()));
            }
        } else {
            adder.accept(Component.translatable("desc.item.deadman.explodesWhenDropped"));
        }
        adder.accept(
                Component.literal("[")
                        .append(Component.translatable("trait.drop"))
                        .append("]")
                        .withStyle(ChatFormatting.RED));
    }
}
