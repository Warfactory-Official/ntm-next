/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.blocks.bomb;

import com.hbm.api.block.ExplosiveBlock;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.TntNtmEntity;
import com.hbm.interfaces.ToolInteractionHandler;
import com.hbm.interfaces.ToolType;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.tags.BlockTags;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockBehaviour.Properties;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.pattern.BlockInWorld;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BaseTNTBlock extends TntBlock
        implements ToolInteractionHandler, ExplosiveBlock {

    private final int popFuse;

    protected BaseTNTBlock(Properties properties, int popFuse) {
        super(properties);
        this.popFuse = popFuse;
    }

    private boolean primeCharge(
            Level level, BlockPos pos, @Nullable LivingEntity igniter, ItemStack ignitionItem) {
        if (!TntRule.explodes(level)) {
            return false;
        }
        if (igniter instanceof Player player
                && player.gameMode() == GameType.ADVENTURE
                && !ignitionItem.canBreakBlockInAdventureMode(
                        new BlockInWorld(level, pos, false))) {
            return false;
        }

        var primed =
                new TntNtmEntity(
                        ModEntities.TNT_NTM.get(),
                        level,
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D,
                        igniter,
                        defaultBlockState());
        level.addFreshEntity(primed);
        level.playSound(
                null,
                primed.getX(),
                primed.getY(),
                primed.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1F,
                1F);
        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        return true;
    }

    private void igniteFromSurroundings(Level level, BlockPos pos) {
        if (level.isClientSide()) {
            return;
        }

        boolean ignition = level.hasNeighborSignal(pos);
        if (!ignition) {
            var neighbor = new BlockPos.MutableBlockPos();
            for (var direction : Direction.VALUES) {
                neighbor.setWithOffset(pos, direction);
                if (level.getBlockState(neighbor).is(Blocks.FIRE)) {
                    ignition = true;
                    break;
                }
            }
        }
        if (ignition && primeCharge(level, pos, null, ItemStack.EMPTY)) {
            level.removeBlock(pos, false);
        }
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (!oldState.is(state.getBlock())) {
            igniteFromSurroundings(level, pos);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block changedBlock,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        igniteFromSurroundings(level, pos);
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && state.getValue(UNSTABLE)) {
            primeCharge(level, pos, null, ItemStack.EMPTY);
        }

        spawnDestroyByEntityParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS) && level instanceof ServerLevel server) {
            PiglinAi.angerNearbyPiglins(server, player, false);
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        ChainDetonation.spawn(
                level, pos, explosion.getIndirectSourceEntity(), defaultBlockState(), popFuse);
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack stack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hit) {
        if (!stack.is(Items.FLINT_AND_STEEL) && !stack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(stack, state, level, pos, player, hand, hit);
        }

        if (primeCharge(level, pos, player, stack)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            var item = stack.getItem();
            if (stack.is(Items.FLINT_AND_STEEL)) {
                stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                stack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (level instanceof ServerLevel && !TntRule.explodes(level)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(
            Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        if (level instanceof ServerLevel server) {
            var pos = hit.getBlockPos();
            var owner = projectile.getOwner();
            if (!projectile.isOnFire() || !projectile.mayInteract(server, pos)) {
                return;
            }

            LivingEntity igniter = owner instanceof LivingEntity living ? living : null;
            var ignitionItem = ItemStack.EMPTY;
            if (projectile instanceof AbstractArrow arrow) {
                ignitionItem = arrow.getPickupItemStackOrigin();
            }
            if (primeCharge(level, pos, igniter, ignitionItem)) {
                level.removeBlock(pos, false);
            }
        }
    }

    @Override
    public boolean onToolUse(
            Level level,
            @Nullable Player player,
            BlockPos pos,
            @Nullable Direction face,
            Vec3 hit,
            ToolType tool) {
        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.destroyBlock(pos, false);
                popResource(level, pos, new ItemStack(this));
            }
            return true;
        }
        if (tool == ToolType.SCREWDRIVER) {
            if (!level.isClientSide()) {
                var state = level.getBlockState(pos).cycle(UNSTABLE);
                level.setBlock(pos, state, Block.UPDATE_ALL);
                if (player != null) {
                    boolean enabled = state.getValue(UNSTABLE);
                    var status =
                            Component.translatable(
                                    enabled ? "desc.shared.enabled" : "desc.shared.disabled");
                    var message =
                            Component.translatable("desc.block.tnt.igniteOnBreak", status)
                                    .withStyle(enabled ? ChatFormatting.RED : ChatFormatting.GOLD);
                    player.sendSystemMessage(message);
                }
            }
            return true;
        }
        return false;
    }

    protected void vanillaBlast(
            Level level, double x, double y, double z, TntNtmEntity entity, float strength) {
        var damageSource = level.damageSources().explosion(entity, entity.getOwner());
        level.explode(
                entity,
                damageSource,
                null,
                x,
                y,
                z,
                strength,
                false,
                Level.ExplosionInteraction.TNT);
    }
}
