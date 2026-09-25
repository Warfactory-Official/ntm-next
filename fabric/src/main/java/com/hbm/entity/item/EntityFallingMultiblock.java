// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.item;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.entity.ModEntities;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.special.CarriedBlockItems;
import com.hbm.packet.toclient.FallingMultiblockPayload;
import com.hbm.platform.Services;
import com.mojang.serialization.Codec;
import it.unimi.dsi.fastutil.longs.LongOpenHashSet;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.IntStream;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.storage.TagValueInput;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EntityFallingMultiblock extends Entity implements StoredItems {

    private static final Logger LOGGER = LoggerFactory.getLogger("NTM");
    private static final double GRAVITY = 0.04D;
    private static final double DRAG = 0.98D;
    private static final int RESEND_INTERVAL = 20;

    private final LongOpenHashSet memberPosSet = new LongOpenHashSet();
    private BlockPos originCore = BlockPos.ZERO;

    private long[] members = new long[0];
    private @Nullable CompoundTag carriedBe;
    private @Nullable BlockEntity carriedInventory;
    private @Nullable BlockEntity carriedView;
    private int committedDrop;
    private double fallProgress;
    private double fallVelocity;

    public EntityFallingMultiblock(
            EntityType<? extends EntityFallingMultiblock> type, Level level) {
        super(type, level);
    }

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (carriedBe == null || !(level() instanceof ServerLevel server)) return;
        if (carriedInventory == null) {
            carriedInventory =
                    CarriedBlockItems.load(
                            coreState(), originCore, carriedBe, server.registryAccess());
        }
        if (carriedInventory != null && visitor.visit(carriedInventory)) {
            carriedBe = carriedInventory.saveCustomOnly(server.registryAccess());
        }
    }

    private static int packOffset(int dx, int dy, int dz) {
        return (dx & 0x3F) | ((dy & 0x3FF) << 6) | ((dz & 0x3F) << 16);
    }

    private static int offsetX(int packed) {
        return (packed << 26) >> 26;
    }

    private static int offsetY(int packed) {
        return (packed << 16) >> 22;
    }

    private static int offsetZ(int packed) {
        return (packed << 10) >> 26;
    }

    public static @Nullable EntityFallingMultiblock tryFall(ServerLevel level, BlockPos core) {
        BlockState coreState = level.getBlockState(core);
        if (MultiblockSurface.foldedCore(coreState) == null) return null;

        if (!level.isPositionEntityTicking(core)) return null;

        List<BlockPos> blocks = MultiblockSurface.rigidStructureBlocks(level, core);
        if (blocks.isEmpty()) return null;

        EntityFallingMultiblock entity =
                new EntityFallingMultiblock(ModEntities.FALLING_MULTIBLOCK.get(), level);
        entity.originCore = core.immutable();
        for (BlockPos member : blocks) entity.memberPosSet.add(member.asLong());

        long[] packed = new long[blocks.size()];
        for (int i = 0; i < blocks.size(); i++) {
            BlockPos member = blocks.get(i);
            BlockState state = level.getBlockState(member);
            int offset =
                    packOffset(
                            member.getX() - core.getX(),
                            member.getY() - core.getY(),
                            member.getZ() - core.getZ());
            packed[i] = (long) Block.BLOCK_STATE_REGISTRY.getId(state) << 32 | (offset & 0x3FFFFFL);
        }
        entity.members = packed;

        if (!entity.canStep(level, 1)) return null;

        BlockEntity be = level.getBlockEntity(core);
        if (be != null) entity.carriedBe = be.saveCustomOnly(level.registryAccess());

        BlockMultiblockCore.withoutTeardown(
                () -> {
                    BlockState air = Blocks.AIR.defaultBlockState();
                    for (BlockPos member : blocks) {
                        level.setBlock(
                                member,
                                air,
                                Block.UPDATE_ALL | Block.UPDATE_SKIP_BLOCK_ENTITY_SIDEEFFECTS);
                    }
                });

        entity.setPos(core.getX() + 0.5, core.getY(), core.getZ() + 0.5);
        level.addFreshEntity(entity);
        entity.sendMembers();
        return entity;
    }

    public static int offsetXOf(long packed) {
        return offsetX((int) (packed & 0x3FFFFFL));
    }

    public static int offsetYOf(long packed) {
        return offsetY((int) (packed & 0x3FFFFFL));
    }

    public static int offsetZOf(long packed) {
        return offsetZ((int) (packed & 0x3FFFFFL));
    }

    public static BlockState stateOf(long packed) {
        return Block.BLOCK_STATE_REGISTRY.byId((int) (packed >>> 32));
    }

    private @Nullable BlockState coreState() {
        for (long member : members) {
            if ((member & 0x3FFFFFL) == 0) return stateOf(member);
        }
        return null;
    }

    private boolean canStep(ServerLevel level, int drop) {
        for (long packed : members) {
            BlockPos target = memberAt(packed, drop);
            if (memberPosSet.contains(target.above(drop - 1).asLong())) continue;
            if (target.getY() < level.getMinY()) return false;
            BlockState state = level.getBlockState(target);
            if (!(state.isAir() || (state.canBeReplaced() && !state.liquid()))) return false;
        }
        return true;
    }

    private BlockPos memberAt(long packed, int drop) {
        int offset = (int) (packed & 0x3FFFFFL);
        return originCore.offset(offsetX(offset), offsetY(offset) - drop, offsetZ(offset));
    }

    @Override
    public void tick() {
        super.tick();

        fallVelocity += GRAVITY;
        fallProgress += fallVelocity;
        fallVelocity *= DRAG;

        if (level() instanceof ServerLevel server) {
            while (fallProgress >= 1.0D) {
                if (!canStep(server, committedDrop + 1)) {
                    land(server);
                    discard();
                    return;
                }
                committedDrop++;
                fallProgress -= 1.0D;
            }
        }

        setPos(
                originCore.getX() + 0.5,
                originCore.getY() - committedDrop - fallProgress,
                originCore.getZ() + 0.5);

        if (tickCount % RESEND_INTERVAL == 0) sendMembers();
    }

    private void land(ServerLevel level) {
        BlockState[] states = new BlockState[members.length];
        for (int i = 0; i < members.length; i++) {
            states[i] = Block.BLOCK_STATE_REGISTRY.byId((int) (members[i] >>> 32));
        }

        BlockMultiblockCore.withoutTeardown(
                () -> {
                    for (int i = 0; i < members.length; i++) {
                        level.setBlock(
                                memberAt(members[i], committedDrop), states[i], Block.UPDATE_ALL);
                    }
                });

        BlockPos newCore = originCore.below(committedDrop);
        if (carriedBe != null && level.getBlockEntity(newCore) instanceof BlockEntity be) {
            try (ProblemReporter.ScopedCollector reporter =
                    new ProblemReporter.ScopedCollector(be.problemPath(), LOGGER)) {
                be.loadCustomOnly(
                        TagValueInput.create(reporter, level.registryAccess(), carriedBe));
            }
            be.setChanged();
        }

        BlockMultiblockCore block = MultiblockSurface.foldedCore(level.getBlockState(newCore));
        if (block != null) block.reindexLoadedCells(level, newCore);
    }

    private void sendMembers() {
        if (level() instanceof ServerLevel) {
            Services.NETWORK.sendToAllTracking(
                    new FallingMultiblockPayload(getId(), originCore.asLong(), members, carriedBe),
                    this);
        }
    }

    public void setMembersFromNetwork(
            long[] packedMembers, long corePos, @Nullable CompoundTag carried) {
        this.members = packedMembers;
        this.originCore = BlockPos.of(corePos);
        if (Objects.equals(carriedBe, carried)) return;
        carriedBe = carried;
        carriedView = null;
    }

    public @Nullable BlockEntity carriedBlockEntity(BlockPos at) {
        if (carriedBe == null) return null;
        if (carriedView == null || !carriedView.getBlockPos().equals(at)) {
            carriedView =
                    CarriedBlockItems.load(coreState(), at, carriedBe, level().registryAccess());
            if (carriedView != null) carriedView.setLevel(level());
        }
        return carriedView;
    }

    public long[] membersPacked() {
        return members;
    }

    public BlockPos originCore() {
        return originCore;
    }

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        return false;
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {}

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        originCore = input.read("core", BlockPos.CODEC).orElse(BlockPos.ZERO);
        carriedBe = input.read("be", CompoundTag.CODEC).orElse(null);
        carriedInventory = null;

        int[] offsets =
                input.read("offsets", Codec.INT_STREAM).map(IntStream::toArray).orElse(new int[0]);
        List<BlockState> states = input.read("states", BlockState.CODEC.listOf()).orElse(List.of());
        int n = Math.min(offsets.length, states.size());
        members = new long[n];
        for (int i = 0; i < n; i++) {
            members[i] =
                    (long) Block.BLOCK_STATE_REGISTRY.getId(states.get(i)) << 32
                            | (offsets[i] & 0x3FFFFFL);
        }

        memberPosSet.clear();
        for (long packed : members) memberPosSet.add(memberAt(packed, 0).asLong());
        committedDrop = input.getIntOr("drop", 0);
        fallProgress = input.getDoubleOr("progress", 0.0D);
        fallVelocity = input.getDoubleOr("velocity", 0.0D);
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        output.store("core", BlockPos.CODEC, originCore);
        int[] offsets = new int[members.length];
        List<BlockState> states = new ArrayList<>(members.length);
        for (int i = 0; i < members.length; i++) {
            offsets[i] = (int) (members[i] & 0x3FFFFFL);
            states.add(Block.BLOCK_STATE_REGISTRY.byId((int) (members[i] >>> 32)));
        }
        output.store("offsets", Codec.INT_STREAM, IntStream.of(offsets));
        output.store("states", BlockState.CODEC.listOf(), states);
        if (carriedBe != null) output.store("be", CompoundTag.CODEC, carriedBe);
        output.putInt("drop", committedDrop);
        output.putDouble("progress", fallProgress);
        output.putDouble("velocity", fallVelocity);
    }
}
