// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blocks.generic;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.ModBlocks;
import com.hbm.packet.SyncBindings;
import com.hbm.packet.SyncEnvelope;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncList;
import com.hbm.packet.SyncSource;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.tileentity.FoldedCoreResident;
import com.hbm.tileentity.GraphResident;
import com.hbm.tileentity.Synced;
import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.HolderLookup;
import net.minecraft.core.particles.ItemParticleOption;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelAccessor;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.Shapes;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jspecify.annotations.Nullable;

public class BlockLoot extends Block implements EntityBlock {

    private static final int PARTICLES_PER_ENTRY = 6;

    private static final VoxelShape SHAPE = Shapes.box(0D, 0D, 0D, 1D, 0.0625D, 1D);

    public BlockLoot(Properties props) {
        super(props);
    }

    public static void place(LevelAccessor level, BlockPos pos, List<TileEntityLoot.Entry> items) {
        level.setBlock(pos, ModBlocks.LOOT.get().defaultBlockState(), Block.UPDATE_CLIENTS);
        if (level.getBlockEntity(pos) instanceof TileEntityLoot pile) {
            pile.items.addAll(items);
            pile.setChanged();
        }
    }

    @Override
    protected VoxelShape getShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected VoxelShape getCollisionShape(
            BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected InteractionResult useWithoutItem(
            BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (level.isClientSide()) return InteractionResult.SUCCESS;
        if (player.isShiftKeyDown()) return InteractionResult.PASS;
        level.removeBlock(pos, false);
        return InteractionResult.SUCCESS;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TileEntityLoot(pos, state);
    }

    public static class TileEntityLoot extends BlockEntity
            implements Synced, SyncUnitSchema, GraphResident, FoldedCoreResident {

        private static final String ITEMS = "items";

        @SyncField(units = 1L)
        public final List<Entry> items = new SyncList<>();

        public TileEntityLoot(BlockPos pos, BlockState state) {
            super(ModBlockEntities.NTM_LOOT.get(), pos, state);
        }

        @Override
        public void preRemoveSideEffects(BlockPos pos, BlockState state) {
            if (!(level instanceof ServerLevel server)) return;
            for (Entry entry : items) {
                server.sendParticles(
                        new ItemParticleOption(
                                ParticleTypes.ITEM,
                                ItemStackTemplate.fromNonEmptyStack(entry.stack())),
                        pos.getX() + entry.x() + 0.25D,
                        pos.getY() + entry.y(),
                        pos.getZ() + entry.z() + 0.25D,
                        PARTICLES_PER_ENTRY,
                        0.1D,
                        0.05D,
                        0.1D,
                        0.05D);
                server.addFreshEntity(
                        new ItemEntity(
                                server,
                                pos.getX() + 0.5,
                                pos.getY(),
                                pos.getZ() + 0.5,
                                entry.stack().copy()));
            }
            items.clear();
        }

        public TileEntityLoot addItem(ItemStack stack, double x, double y, double z) {
            if (stack.isEmpty()) return this;
            items.add(new Entry(stack, x, y, z));
            setChanged();
            return this;
        }

        @Override
        public long syncUnitMask() {
            return 1L;
        }

        @Override
        public void writeSyncUnit(int unit, ByteBuf output) {
            if (unit != 0) throw new IllegalArgumentException();
            output.writeInt(items.size());
            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(output, level.registryAccess());
            for (Entry e : items) {
                ItemStack.OPTIONAL_STREAM_CODEC.encode(buf, e.stack());
                buf.writeDouble(e.x());
                buf.writeDouble(e.y());
                buf.writeDouble(e.z());
            }
        }

        @Override
        public void readSyncUnit(int unit, ByteBuf input) {
            if (unit != 0) throw new IllegalArgumentException();
            int count = input.readInt();
            if (count < 0 || count > input.readableBytes() / 25)
                throw new DecoderException("Invalid loot entry count");
            RegistryFriendlyByteBuf buf =
                    new RegistryFriendlyByteBuf(input, level.registryAccess());
            items.clear();
            for (int i = 0; i < count; i++) {
                ItemStack stack = ItemStack.OPTIONAL_STREAM_CODEC.decode(buf);
                double x = buf.readDouble();
                double y = buf.readDouble();
                double z = buf.readDouble();
                if (!stack.isEmpty()) items.add(new Entry(stack, x, y, z));
            }
        }

        @Override
        protected void loadAdditional(ValueInput input) {
            super.loadAdditional(input);
            items.clear();
            input.listOrEmpty(ITEMS, Entry.CODEC).forEach(items::add);
        }

        @Override
        protected void saveAdditional(ValueOutput output) {
            super.saveAdditional(output);
            ValueOutput.TypedOutputList<Entry> list = output.list(ITEMS, Entry.CODEC);
            for (Entry e : items) list.add(e);
        }

        public static CompoundTag tagOf(List<Entry> items, HolderLookup.Provider registries) {
            CompoundTag tag = new CompoundTag();
            tag.put(
                    ITEMS,
                    Entry.CODEC
                            .listOf()
                            .encodeStart(
                                    registries.createSerializationContext(NbtOps.INSTANCE), items)
                            .getOrThrow());
            return tag;
        }

        public record Entry(ItemStack stack, double x, double y, double z) implements SyncEnvelope {
            @Override
            public void bindSyncChildren(SyncSource owner, int mask) {
                SyncBindings.bind(owner, stack, mask);
            }

            @Override
            public void unbindSyncChildren(SyncSource owner) {
                SyncBindings.unbind(owner, stack);
            }

            public static final Codec<Entry> CODEC =
                    RecordCodecBuilder.create(
                            i ->
                                    i.group(
                                                    ItemStack.CODEC
                                                            .fieldOf("item")
                                                            .forGetter(Entry::stack),
                                                    Codec.DOUBLE.fieldOf("x").forGetter(Entry::x),
                                                    Codec.DOUBLE.fieldOf("y").forGetter(Entry::y),
                                                    Codec.DOUBLE.fieldOf("z").forGetter(Entry::z))
                                            .apply(i, Entry::new));
        }
    }
}
