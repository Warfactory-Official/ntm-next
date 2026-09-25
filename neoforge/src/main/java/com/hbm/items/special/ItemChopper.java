// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.special;

import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

public class ItemChopper extends Item {

    private final Supplier<EntityType<? extends Mob>> type;
    private final int launch;
    private final Consumer<Mob> spawned;
    private final String[] tooltip;

    public ItemChopper(Properties properties, Supplier<EntityType<? extends Mob>> type) {
        this(properties, type, 0, mob -> {});
    }

    public ItemChopper(
            Properties properties,
            Supplier<EntityType<? extends Mob>> type,
            int launch,
            Consumer<Mob> spawned,
            String... tooltip) {
        super(properties);
        this.type = type;
        this.launch = launch;
        this.spawned = spawned;
        this.tooltip = tooltip;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {

        EntityType<? extends Mob> type = this.type.get();
        if (!type.canSpawn(context.getLevel())) return InteractionResult.FAIL;

        if (!(context.getLevel() instanceof ServerLevel level)) return InteractionResult.SUCCESS;

        BlockPos clicked = context.getClickedPos();
        Direction face = context.getClickedFace();
        BlockPos pos =
                level.getBlockState(clicked).getCollisionShape(level, clicked).isEmpty()
                        ? clicked
                        : clicked.relative(face);

        return spawn(
                type,
                level,
                context.getItemInHand(),
                context.getPlayer(),
                pos,
                true,
                !Objects.equals(clicked, pos) && face == Direction.UP);
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {

        ItemStack stack = player.getItemInHand(hand);
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.SOURCE_ONLY);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        EntityType<? extends Mob> type = this.type.get();
        if (!type.canSpawn(level)) return InteractionResult.FAIL;
        if (!(level instanceof ServerLevel serverLevel)) return InteractionResult.SUCCESS;

        BlockPos pos = hit.getBlockPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LiquidBlock))
            return InteractionResult.PASS;
        if (!level.mayInteract(player, pos)
                || !player.mayUseItemAt(pos, hit.getDirection(), stack)) {
            return InteractionResult.FAIL;
        }

        InteractionResult result = spawn(type, serverLevel, stack, player, pos, false, false);
        if (result == InteractionResult.SUCCESS) player.awardStat(Stats.ITEM_USED.get(this));
        return result;
    }

    private InteractionResult spawn(
            EntityType<? extends Mob> type,
            ServerLevel level,
            ItemStack stack,
            Player player,
            BlockPos pos,
            boolean tryMoveDown,
            boolean movedUp) {

        BlockPos spawnPos = pos.above(this.launch);
        Mob mob =
                type.spawn(
                        level,
                        stack,
                        player,
                        spawnPos,
                        EntitySpawnReason.SPAWN_ITEM_USE,
                        tryMoveDown && this.launch == 0,
                        movedUp);
        if (mob == null) return InteractionResult.FAIL;

        this.spawned.accept(mob);
        stack.consume(1, player);
        level.gameEvent(player, GameEvent.ENTITY_PLACE, spawnPos);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        for (String line : this.tooltip) adder.accept(Component.literal(line));
    }
}
