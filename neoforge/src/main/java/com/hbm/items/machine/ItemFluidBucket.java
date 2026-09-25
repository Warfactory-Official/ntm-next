// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.blocks.fluid.ClassicFluid;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.advancements.triggers.CriteriaTriggers;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BucketItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemUtils;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.gameevent.GameEvent;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import org.jspecify.annotations.Nullable;

public class ItemFluidBucket extends Item implements IFluidContainerItem {

    public static final int VOLUME = 1000;

    private static volatile @Nullable Map<Fluid, ClassicFluid> placed;

    public ItemFluidBucket(Properties properties) {
        super(properties);
    }

    public static ItemStack make(Fluid fluid) {
        ItemStack stack = new ItemStack(ModItems.FLUID_BUCKET);
        stack.set(ModDataComponents.FLUID_CONTENT.get(), new FluidStackNTM(fluid, VOLUME));
        return stack;
    }

    public static Fluid getFluid(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT).type();
    }

    @Override
    public boolean canStore(Fluid type) {
        return type == NTMFluids.WATZ_MUD
                || type == NTMFluids.SCHRABIDIC
                || type == NTMFluids.SULFURIC_ACID;
    }

    @Override
    public int capacity(ItemStack stack) {
        return VOLUME;
    }

    @Override
    public FluidStackNTM getContent(ItemStack stack) {
        return stack.getOrDefault(ModDataComponents.FLUID_CONTENT.get(), EMPTY_CONTENT);
    }

    @Override
    public void setContent(ItemStack stack, FluidStackNTM content) {
        if (content == null || content.type() == Fluids.EMPTY || content.amount() <= 0) {
            stack.remove(ModDataComponents.FLUID_CONTENT.get());
            return;
        }
        stack.set(
                ModDataComponents.FLUID_CONTENT.get(),
                new FluidStackNTM(content.type(), VOLUME, content.pressure()));
    }

    @Override
    public int fill(ItemStack stack, Fluid type, int amount, int pressure) {
        if (type == null || type == Fluids.EMPTY || amount < VOLUME) return 0;
        if (!canStore(type)) return 0;
        if (getContent(stack).type() != Fluids.EMPTY) return 0;
        setContent(stack, new FluidStackNTM(type, VOLUME, pressure));
        return VOLUME;
    }

    @Override
    public int drain(ItemStack stack, Fluid type, int amount, int pressure) {
        FluidStackNTM held = getContent(stack);
        if (held.type() != type
                || held.pressure() != pressure
                || held.amount() <= 0
                || amount < VOLUME) return 0;
        setContent(stack, EMPTY_CONTENT);
        return VOLUME;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        ClassicFluid fluid = placedFluid(getFluid(stack));
        if (fluid == null) return InteractionResult.PASS;
        BlockHitResult hit = getPlayerPOVHitResult(level, player, ClipContext.Fluid.NONE);
        if (hit.getType() != HitResult.Type.BLOCK) return InteractionResult.PASS;
        BlockPos place = hit.getBlockPos().relative(hit.getDirection());
        if (!level.mayInteract(player, hit.getBlockPos())
                || !player.mayUseItemAt(place, hit.getDirection(), stack)
                || !emptyContents(player, level, place, hit, fluid)) {
            return InteractionResult.FAIL;
        }
        if (player instanceof ServerPlayer serverPlayer) {
            CriteriaTriggers.PLACED_BLOCK.trigger(serverPlayer, place, stack);
        }
        player.awardStat(Stats.ITEM_USED.get(this));
        return InteractionResult.SUCCESS.heldItemTransformedTo(
                ItemUtils.createFilledResult(
                        stack, player, BucketItem.getEmptySuccessItem(stack, player)));
    }

    private static boolean emptyContents(
            @Nullable LivingEntity user,
            Level level,
            BlockPos pos,
            @Nullable BlockHitResult hit,
            ClassicFluid fluid) {
        BlockState state = level.getBlockState(pos);
        boolean mayReplace = state.canBeReplaced(fluid);
        boolean shiftKeyDown = user != null && user.isShiftKeyDown();
        if (!state.isAir() && !(mayReplace && (!shiftKeyDown || hit == null))) {
            return hit != null
                    && emptyContents(
                            user,
                            level,
                            hit.getBlockPos().relative(hit.getDirection()),
                            null,
                            fluid);
        }
        if (!level.isClientSide() && mayReplace && !state.liquid()) level.destroyBlock(pos, true);
        if (!level.setBlock(
                        pos,
                        fluid.defaultFluidState().createLegacyBlock(),
                        Block.UPDATE_ALL_IMMEDIATE)
                && !state.getFluidState().isSource()) {
            return false;
        }
        level.playSound(user, pos, SoundEvents.BUCKET_EMPTY, SoundSource.BLOCKS, 1.0F, 1.0F);
        level.gameEvent(user, GameEvent.FLUID_PLACE, pos);
        return true;
    }

    private static @Nullable ClassicFluid placedFluid(Fluid token) {
        Map<Fluid, ClassicFluid> map = placed;
        if (map == null) {
            map = new IdentityHashMap<>();
            for (Fluid fluid : BuiltInRegistries.FLUID) {
                if (!(fluid instanceof ClassicFluid classic)
                        || !fluid.isSource(fluid.defaultFluidState())) continue;
                ItemStack bucket = classic.spec().bucket().get();
                if (!bucket.is(ModItems.FLUID_BUCKET.get())) continue;
                ClassicFluid prior = map.put(getFluid(bucket), classic);
                if (prior != null) {
                    throw new IllegalStateException(
                            getFluid(bucket) + " buckets from " + prior + " and " + classic);
                }
            }
            placed = map;
        }
        return map.get(token);
    }

    @Override
    public Component getName(ItemStack stack) {
        Fluid fluid = getFluid(stack);
        if (fluid == Fluids.EMPTY) return Component.translatable("item.hbm.fluid_bucket.empty");
        return Component.translatable(
                "item.hbm.fluid_bucket", NTMFluidProperties.getDisplayName(fluid));
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {

        if (getFluid(stack) == Fluids.EMPTY) return;
        String s = VOLUME + " mB";
        if (stack.getCount() > 1) s = stack.getCount() + "x " + s;
        adder.accept(Component.literal(s).withStyle(ChatFormatting.GRAY));
    }
}
