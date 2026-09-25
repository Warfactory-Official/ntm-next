// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.explosion.vanillant.standard;

import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.interfaces.IBlockMutator;
import com.hbm.explosion.vanillant.interfaces.IBlockProcessor;
import com.hbm.platform.Services;
import java.util.HashSet;
import java.util.Iterator;
import java.util.function.BiConsumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Explosion;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.Vec3;

public class BlockProcessorStandard implements IBlockProcessor {

    private static final BiConsumer<ItemStack, BlockPos> SWALLOW = (stack, pos) -> {};

    protected IBlockMutator convert;
    private boolean noDrop;
    private boolean allDrop;
    private int fortune;

    public BlockProcessorStandard() {}

    public BlockProcessorStandard withBlockEffect(IBlockMutator convert) {
        this.convert = convert;
        return this;
    }

    @Override
    public Explosion.BlockInteraction interaction() {
        return allDrop
                ? Explosion.BlockInteraction.DESTROY
                : Explosion.BlockInteraction.DESTROY_WITH_DECAY;
    }

    @Override
    public void process(
            ExplosionVNT explosion,
            Level world,
            double x,
            double y,
            double z,
            HashSet<BlockPos> affectedBlocks) {

        if (!(world instanceof ServerLevel level)) return;

        ItemStack tool = ItemStack.EMPTY;
        if (fortune > 0 && !noDrop) {
            Holder<Enchantment> enchantment =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.FORTUNE);
            tool = new ItemStack(Items.STICK);
            tool.enchant(enchantment, fortune);
        }

        BiConsumer<ItemStack, BlockPos> drops =
                noDrop || !tool.isEmpty()
                        ? SWALLOW
                        : (stack, pos) -> Block.popResource(level, pos, stack);

        Iterator<BlockPos> iterator = affectedBlocks.iterator();

        while (iterator.hasNext()) {
            BlockPos pos = iterator.next();
            BlockState state = world.getBlockState(pos);

            if (state.isAir()) {
                iterator.remove();
                continue;
            }

            if (!tool.isEmpty()
                    && Services.PLATFORM.canDropFromExplosion(level, pos, state, explosion)) {
                dropWithFortune(level, pos, state, explosion, tool);
            }
            state.onExplosionHit(level, pos, explosion, drops);

            if (convert != null) convert.mutatePre(explosion, state, pos);
        }

        if (convert != null) {
            for (BlockPos pos : affectedBlocks) {
                if (world.getBlockState(pos).isAir()) {
                    convert.mutatePost(explosion, pos);
                }
            }
        }
    }

    private static void dropWithFortune(
            ServerLevel level,
            BlockPos pos,
            BlockState state,
            ExplosionVNT explosion,
            ItemStack tool) {
        BlockEntity blockEntity = state.hasBlockEntity() ? level.getBlockEntity(pos) : null;
        LootParams.Builder params =
                new LootParams.Builder(level)
                        .withParameter(LootContextParams.ORIGIN, Vec3.atCenterOf(pos))
                        .withParameter(LootContextParams.TOOL, tool)
                        .withOptionalParameter(LootContextParams.BLOCK_ENTITY, blockEntity)
                        .withOptionalParameter(
                                LootContextParams.THIS_ENTITY, explosion.getDirectSourceEntity());
        state.getDrops(params).forEach(stack -> Block.popResource(level, pos, stack));
    }

    public BlockProcessorStandard setNoDrop() {
        this.noDrop = true;
        return this;
    }

    public BlockProcessorStandard setAllDrop() {
        this.allDrop = true;
        return this;
    }

    public BlockProcessorStandard setFortune(int fortune) {
        this.fortune = fortune;
        return this;
    }
}
