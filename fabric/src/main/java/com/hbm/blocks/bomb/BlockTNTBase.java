// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.bomb;

import com.hbm.api.block.IFuckingExplode;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityTntNtm;
import com.hbm.interfaces.IToolable;
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
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.redstone.Orientation;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public abstract class BlockTNTBase extends TntBlock implements IToolable, IFuckingExplode {

    private final int popFuse;

    protected BlockTNTBase(Properties props, int popFuse) {
        super(props);
        this.popFuse = popFuse;
    }

    @Override
    public void wasExploded(ServerLevel level, BlockPos pos, Explosion explosion) {
        ChainDetonation.spawn(
                level, pos, explosion.getIndirectSourceEntity(), defaultBlockState(), popFuse);
    }

    @Override
    protected void onPlace(
            BlockState state,
            Level level,
            BlockPos pos,
            BlockState oldState,
            boolean movedByPiston) {
        if (oldState.is(state.getBlock())) return;
        if (level.hasNeighborSignal(pos)) {
            if (prime(level, pos, null)) level.removeBlock(pos, false);
        } else {
            checkAndIgnite(level, pos);
        }
    }

    @Override
    protected void neighborChanged(
            BlockState state,
            Level level,
            BlockPos pos,
            Block block,
            @Nullable Orientation orientation,
            boolean movedByPiston) {
        if (level.hasNeighborSignal(pos)) {
            if (prime(level, pos, null)) level.removeBlock(pos, false);
        } else {
            checkAndIgnite(level, pos);
        }
    }

    private void checkAndIgnite(Level level, BlockPos pos) {
        for (Direction dir : Direction.VALUES) {
            if (level.getBlockState(pos.relative(dir)).is(Blocks.FIRE)) {
                if (prime(level, pos, null)) level.removeBlock(pos, false);
                return;
            }
        }
    }

    @Override
    public BlockState playerWillDestroy(
            Level level, BlockPos pos, BlockState state, Player player) {
        if (!level.isClientSide()
                && !player.getAbilities().instabuild
                && state.getValue(UNSTABLE)) {
            prime(level, pos, null);
        }
        this.spawnDestroyParticles(level, player, pos, state);
        if (state.is(BlockTags.GUARDED_BY_PIGLINS) && level instanceof ServerLevel serverLevel) {
            PiglinAi.angerNearbyPiglins(serverLevel, player, false);
        }
        level.gameEvent(GameEvent.BLOCK_DESTROY, pos, GameEvent.Context.of(player, state));
        return state;
    }

    @Override
    protected InteractionResult useItemOn(
            ItemStack itemStack,
            BlockState state,
            Level level,
            BlockPos pos,
            Player player,
            InteractionHand hand,
            BlockHitResult hitResult) {
        if (!itemStack.is(Items.FLINT_AND_STEEL) && !itemStack.is(Items.FIRE_CHARGE)) {
            return super.useItemOn(itemStack, state, level, pos, player, hand, hitResult);
        }
        if (prime(level, pos, player)) {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), 11);
            Item item = itemStack.getItem();
            if (itemStack.is(Items.FLINT_AND_STEEL)) {
                itemStack.hurtAndBreak(1, player, hand.asEquipmentSlot());
            } else {
                itemStack.consume(1, player);
            }
            player.awardStat(Stats.ITEM_USED.get(item));
        } else if (!level.isClientSide() && !TntRule.explodes(level)) {
            player.sendOverlayMessage(Component.translatable("block.minecraft.tnt.disabled"));
            return InteractionResult.PASS;
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected void onProjectileHit(
            Level level, BlockState state, BlockHitResult blockHit, Projectile projectile) {
        if (level instanceof ServerLevel serverLevel) {
            BlockPos pos = blockHit.getBlockPos();
            Entity owner = projectile.getOwner();
            if (projectile.isOnFire()
                    && projectile.mayInteract(serverLevel, pos)
                    && prime(level, pos, owner instanceof LivingEntity living ? living : null)) {
                level.removeBlock(pos, false);
            }
        }
    }

    protected void vanillaBlast(
            Level level, double x, double y, double z, EntityTntNtm entity, float strength) {
        level.explode(
                entity,
                level.damageSources().explosion(entity, entity.getOwner()),
                null,
                x,
                y,
                z,
                strength,
                false,
                Level.ExplosionInteraction.TNT);
    }

    private boolean prime(Level level, BlockPos pos, @Nullable LivingEntity igniter) {
        if (!TntRule.explodes(level)) return false;
        EntityTntNtm tnt =
                new EntityTntNtm(
                        ModEntities.TNT_NTM.get(),
                        level,
                        pos.getX() + 0.5D,
                        pos.getY(),
                        pos.getZ() + 0.5D,
                        igniter,
                        defaultBlockState());
        level.addFreshEntity(tnt);
        level.playSound(
                null,
                tnt.getX(),
                tnt.getY(),
                tnt.getZ(),
                SoundEvents.TNT_PRIMED,
                SoundSource.BLOCKS,
                1.0F,
                1.0F);

        level.gameEvent(igniter, GameEvent.PRIME_FUSE, pos);
        return true;
    }

    @Override
    public boolean onScrew(
            Level level,
            Player player,
            BlockPos pos,
            @Nullable Direction side,
            Vec3 hit,
            ToolType tool) {
        if (tool == ToolType.DEFUSER) {
            if (!level.isClientSide()) {
                level.destroyBlock(pos, false);
                Block.popResource(level, pos, new ItemStack(this));
            }
            return true;
        }
        if (tool != ToolType.SCREWDRIVER) return false;

        if (!level.isClientSide()) {
            BlockState state = level.getBlockState(pos);
            boolean unstable = !state.getValue(UNSTABLE);
            level.setBlock(pos, state.setValue(UNSTABLE, unstable), 3);
            player.sendSystemMessage(
                    Component.translatable(
                                    "desc.block.tnt.igniteOnBreak",
                                    Component.translatable(
                                            unstable
                                                    ? "desc.shared.enabled"
                                                    : "desc.shared.disabled"))
                            .withStyle(unstable ? ChatFormatting.RED : ChatFormatting.GOLD));
        }
        return true;
    }
}
