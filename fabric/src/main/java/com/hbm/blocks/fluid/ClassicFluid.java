// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.fluid;

import com.hbm.registration.RegistryHandle;
import java.util.Optional;
import java.util.function.Supplier;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.tags.FluidTags;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.InsideBlockEffectType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.material.FlowingFluid;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.FluidState;
import net.minecraft.world.level.material.LavaFluid;
import org.jspecify.annotations.Nullable;

public abstract class ClassicFluid extends FlowingFluid {

    public enum Physics {
        WATER(FluidTags.WATER),
        LAVA(FluidTags.LAVA),

        NONE(null);

        public final @Nullable TagKey<Fluid> tag;

        Physics(@Nullable TagKey<Fluid> tag) {
            this.tag = tag;
        }
    }

    public record Spec(
            Physics physics,
            int quanta,
            int tickDelay,
            int density,
            boolean displacesLiquids,
            Identifier sprite,
            Supplier<? extends BlockFluidClassicBase> block,
            Supplier<ItemStack> bucket) {}

    public record Pair(
            RegistryHandle<? extends ClassicFluid> source,
            RegistryHandle<? extends ClassicFluid> flowing) {}

    private final Spec spec;
    private final Supplier<? extends Fluid> source;
    private final Supplier<? extends Fluid> flowing;

    protected ClassicFluid(
            Spec spec, Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing) {
        this.spec = spec;
        this.source = source;
        this.flowing = flowing;
    }

    public Spec spec() {
        return spec;
    }

    @Override
    public Fluid getSource() {
        return source.get();
    }

    @Override
    public Fluid getFlowing() {
        return flowing.get();
    }

    @Override
    public boolean isSame(Fluid other) {
        return other == getSource() || other == getFlowing();
    }

    @Override
    public Item getBucket() {
        ItemStack bucket = spec.bucket.get();
        return bucket.getComponentsPatch().isEmpty() ? bucket.getItem() : Items.AIR;
    }

    @Override
    public Optional<SoundEvent> getPickupSound() {
        return Optional.of(
                spec.physics == Physics.LAVA
                        ? SoundEvents.BUCKET_FILL_LAVA
                        : SoundEvents.BUCKET_FILL);
    }

    @Override
    protected boolean canConvertToSource(ServerLevel level) {
        return false;
    }

    @Override
    protected void beforeDestroyingBlock(LevelAccessor level, BlockPos pos, BlockState state) {
        Block.dropResources(
                state, level, pos, state.hasBlockEntity() ? level.getBlockEntity(pos) : null);
    }

    @Override
    protected int getSlopeFindDistance(LevelReader level) {
        return 4;
    }

    @Override
    protected int getDropOff(LevelReader level) {
        return 8 / spec.quanta;
    }

    @Override
    public int getTickDelay(LevelReader level) {
        return spec.tickDelay;
    }

    @Override
    protected float getExplosionResistance() {
        return spec.block.get().getExplosionResistance();
    }

    @Override
    protected BlockState createLegacyBlock(FluidState state) {
        return spec.block
                .get()
                .defaultBlockState()
                .setValue(LiquidBlock.LEVEL, getLegacyLevel(state));
    }

    @Override
    protected boolean canBeReplacedWith(
            FluidState state, BlockGetter level, BlockPos pos, Fluid other, Direction direction) {
        return switch (spec.physics) {
            case WATER -> direction == Direction.DOWN && !other.is(FluidTags.WATER);
            case LAVA ->
                    state.getHeight(level, pos) >= LavaFluid.MIN_LEVEL_CUTOFF
                            && other.is(FluidTags.WATER);

            case NONE ->
                    !(other instanceof ClassicFluid sibling) || sibling.spec.density > spec.density;
        };
    }

    @Override
    protected void spreadTo(
            LevelAccessor level,
            BlockPos pos,
            BlockState state,
            Direction direction,
            FluidState target) {
        FluidState present = state.getFluidState();
        if (!spec.displacesLiquids && !present.isEmpty() && !isSame(present.getType())) return;
        super.spreadTo(level, pos, state, direction, target);
    }

    @Override
    public void tick(
            ServerLevel level, BlockPos pos, BlockState blockState, FluidState fluidState) {
        BlockFluidClassicBase block = spec.block.get();
        BlockPos below = pos.below();
        BlockState under = level.getBlockState(below);
        if (under.blocksMotion() && block.displaces(under)) {
            level.setBlock(below, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
        }
        super.tick(level, pos, blockState, fluidState);
        BlockState after = level.getBlockState(pos);
        if (after.is(block)) block.flowTick(after, level, pos, level.getRandom());
    }

    @Override
    protected void entityInside(
            Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier effects) {
        switch (spec.physics) {
            case WATER -> effects.apply(InsideBlockEffectType.EXTINGUISH);
            case LAVA -> {
                effects.apply(InsideBlockEffectType.CLEAR_FREEZE);
                effects.apply(InsideBlockEffectType.LAVA_IGNITE);
                effects.runAfter(InsideBlockEffectType.LAVA_IGNITE, Entity::lavaHurt);
            }
            case NONE -> {}
        }
    }

    public static class Source extends ClassicFluid {

        public Source(
                Spec spec, Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing) {
            super(spec, source, flowing);
        }

        @Override
        public int getAmount(FluidState state) {
            return 8;
        }

        @Override
        public boolean isSource(FluidState state) {
            return true;
        }
    }

    public static class Flowing extends ClassicFluid {

        public Flowing(
                Spec spec, Supplier<? extends Fluid> source, Supplier<? extends Fluid> flowing) {
            super(spec, source, flowing);
        }

        @Override
        protected void createFluidStateDefinition(
                StateDefinition.Builder<Fluid, FluidState> builder) {
            super.createFluidStateDefinition(builder);
            builder.add(LEVEL);
        }

        @Override
        public int getAmount(FluidState state) {
            return state.getValue(LEVEL);
        }

        @Override
        public boolean isSource(FluidState state) {
            return false;
        }
    }
}
