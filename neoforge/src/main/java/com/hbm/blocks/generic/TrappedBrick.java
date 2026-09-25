// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ITickingBlock;
import com.hbm.blocks.ModBlocks;
import com.hbm.lib.ModDamageTypes;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.BlockEntityTrappedBrick;
import com.mojang.serialization.MapCodec;
import java.util.function.Function;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

public class TrappedBrick extends Block {

    public final Trap trap;
    private final MapCodec<? extends Block> codec;

    public TrappedBrick(Properties props, Trap trap) {
        this(props, trap, p -> new TrappedBrick(p, trap));
    }

    protected <B extends TrappedBrick> TrappedBrick(
            Properties props, Trap trap, Function<Properties, B> factory) {
        super(props);
        this.trap = trap;
        this.codec = simpleCodec(factory);
    }

    @Override
    protected MapCodec<? extends Block> codec() {
        return codec;
    }

    @Override
    public void stepOn(Level level, BlockPos pos, BlockState onState, Entity entity) {
        if (level.isClientSide()
                || trap.type != TrapType.ON_STEP
                || !(entity instanceof Player player)) return;
        if (!(level instanceof ServerLevel server)) return;

        switch (trap) {
            case FIRE -> {
                BlockPos above = pos.above();
                if (level.getBlockState(above).canBeReplaced())
                    level.setBlockAndUpdate(above, Blocks.FIRE.defaultBlockState());
            }
            case SPIKES -> {
                BlockPos above = pos.above();
                if (level.getBlockState(above).canBeReplaced()) {
                    level.setBlockAndUpdate(above, ModBlocks.SPIKES.get().defaultBlockState());
                }
                AABB box =
                        new AABB(
                                pos.getX(),
                                pos.getY() + 1,
                                pos.getZ(),
                                pos.getX() + 1,
                                pos.getY() + 2,
                                pos.getZ() + 1);
                for (Entity e : level.getEntitiesOfClass(Entity.class, box)) {
                    e.hurtServer(server, level.damageSources().source(ModDamageTypes.SPIKES), 10F);
                }
                level.playSound(null, pos, ModSounds.SLICER.get(), SoundSource.BLOCKS, 1.0F, 1.0F);
            }
            case MINE ->
                    level.explode(
                            null,
                            pos.getX() + 0.5,
                            pos.getY() + 1.5,
                            pos.getZ() + 0.5,
                            1F,
                            Level.ExplosionInteraction.NONE);
            case WEB -> {
                BlockPos above = pos.above();
                if (level.getBlockState(above).canBeReplaced())
                    level.setBlockAndUpdate(above, Blocks.COBWEB.defaultBlockState());
            }
            case RAD_CONVERSION -> convertNearby(level, pos, ModBlocks.BRICK_JUNGLE_OOZE.get());
            case MAGIC_CONVERSTION ->
                    convertNearby(level, pos, ModBlocks.BRICK_JUNGLE_MYSTIC.get());
            case SLOWNESS -> player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 300, 2));
            case WEAKNESS -> player.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 300, 2));
            default -> {}
        }

        level.playSound(null, pos, SoundEvents.LEVER_CLICK, SoundSource.BLOCKS, 0.3F, 0.6F);
        level.setBlockAndUpdate(pos, ModBlocks.BRICK_JUNGLE.get().defaultBlockState());
    }

    private void convertNearby(Level level, BlockPos pos, Block target) {
        RandomSource rand = level.getRandom();
        BlockPos.MutableBlockPos p = new BlockPos.MutableBlockPos();
        for (int a = -3; a <= 3; a++) {
            for (int b = -3; b <= 3; b++) {
                for (int c = -3; c <= 3; c++) {
                    if (rand.nextBoolean()) continue;
                    p.set(pos.getX() + a, pos.getY() + b, pos.getZ() + c);
                    Block bl = level.getBlockState(p).getBlock();
                    if (bl == ModBlocks.BRICK_JUNGLE.get()
                            || bl == ModBlocks.BRICK_JUNGLE_CRACKED.get()
                            || bl == ModBlocks.BRICK_JUNGLE_LAVA.get()) {
                        level.setBlockAndUpdate(p, target.defaultBlockState());
                    }
                }
            }
        }
    }

    public static class Detector extends TrappedBrick implements ITickingBlock {

        public Detector(Properties props, Trap trap) {
            super(props, trap, p -> new Detector(p, trap));
        }

        @Override
        public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
            return new BlockEntityTrappedBrick(pos, state);
        }
    }

    public enum TrapType {
        ON_STEP,
        DETECTOR
    }

    public enum Trap {
        FALLING_ROCKS(TrapType.DETECTOR),
        FIRE(TrapType.ON_STEP),
        ARROW(TrapType.DETECTOR),
        SPIKES(TrapType.ON_STEP),
        MINE(TrapType.ON_STEP),
        WEB(TrapType.ON_STEP),
        FLAMING_ARROW(TrapType.DETECTOR),
        PILLAR(TrapType.DETECTOR),
        RAD_CONVERSION(TrapType.ON_STEP),
        MAGIC_CONVERSTION(TrapType.ON_STEP),
        SLOWNESS(TrapType.ON_STEP),
        WEAKNESS(TrapType.ON_STEP),
        POISON_DART(TrapType.DETECTOR),
        ZOMBIE(TrapType.DETECTOR),
        SPIDERS(TrapType.DETECTOR);

        public final TrapType type;

        Trap(TrapType type) {
            this.type = type;
        }
    }
}
