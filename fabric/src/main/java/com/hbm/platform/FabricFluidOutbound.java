// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.platform;

import com.hbm.api.fluidmk2.FluidCaps;
import com.hbm.api.fluidmk2.FluidFace;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.capability.ForeignFluidStaging;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.function.Supplier;
import net.fabricmc.fabric.api.lookup.v1.block.BlockApiLookup;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidStorage;
import net.fabricmc.fabric.api.transfer.v1.fluid.FluidVariant;
import net.fabricmc.fabric.api.transfer.v1.storage.Storage;
import net.fabricmc.fabric.api.transfer.v1.storage.StorageView;
import net.fabricmc.fabric.api.transfer.v1.transaction.TransactionContext;
import net.fabricmc.fabric.api.transfer.v1.transaction.base.SnapshotParticipant;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.material.Fluid;
import org.jspecify.annotations.Nullable;

final class FabricFluidOutbound {

    private static final long DROPLETS_PER_MB = 81L;

    private FabricFluidOutbound() {}

    static void register(FabricCapabilityService caps, List<Supplier<? extends Block>> blocks) {
        if (blocks.isEmpty()) return;
        caps.registerForBlocks(
                FluidStorage.SIDED,
                (level, pos, state, be, side) -> faceAt(level, pos, side),
                blocks.stream().map(Supplier::get).toArray(Block[]::new));
    }

    private static @Nullable Storage<FluidVariant> faceAt(
            Level level, BlockPos pos, @Nullable Direction side) {
        if (side == null) return null;
        IFluidHandlerMK2 machine = resolve(FluidCaps.RECEIVER, level, pos, side, null);
        if (machine == null) machine = resolve(FluidCaps.PROVIDER, level, pos, side, null);

        if (!(machine instanceof BlockEntity owner)) return null;

        if (ForeignFluidStaging.tanksOf(machine).length == 0) return null;
        return Staging.of(owner, machine).face(pos, side);
    }

    private static @Nullable IFluidHandlerMK2 resolve(
            BlockApiLookup<IFluidHandlerMK2, FluidFace> token,
            Level level,
            BlockPos pos,
            Direction side,
            @Nullable Fluid fluid) {
        return Services.CAPS.find(token, level, pos, FluidFace.of(side, fluid));
    }

    private static final class Staging extends SnapshotParticipant<ForeignFluidStaging.Saved> {

        private static final Map<BlockEntity, WeakReference<Staging>> ALL = new WeakHashMap<>();

        private final BlockEntity be;
        private final IFluidHandlerMK2 machine;
        private final ForeignFluidStaging staging;
        private final Map<Direction, Face> faces = new EnumMap<>(Direction.class);

        private Staging(BlockEntity be, IFluidHandlerMK2 machine, int tanks) {
            this.be = be;
            this.machine = machine;
            this.staging = new ForeignFluidStaging(machine, be, tanks);
        }

        static synchronized Staging of(BlockEntity be, IFluidHandlerMK2 machine) {

            WeakReference<Staging> ref = ALL.get(be);
            Staging staging = ref == null ? null : ref.get();
            if (staging == null || staging.staging.tanks() != machine.getAllTanks().length) {
                ALL.put(
                        be,
                        new WeakReference<>(
                                staging = new Staging(be, machine, machine.getAllTanks().length)));
            }
            return staging;
        }

        Face face(BlockPos pos, Direction side) {

            return faces.computeIfAbsent(side, s -> new Face(this, pos.immutable(), s));
        }

        @Override
        protected ForeignFluidStaging.Saved createSnapshot() {
            return staging.save();
        }

        @Override
        protected void readSnapshot(ForeignFluidStaging.Saved snapshot) {
            staging.restore(snapshot);
        }

        @Override
        protected void onFinalCommit() {
            staging.commit();
        }
    }

    private static final class Face implements Storage<FluidVariant> {

        private final Staging owner;
        private final BlockPos pos;
        private final Direction side;
        private final List<StorageView<FluidVariant>> views = new ArrayList<>();

        Face(Staging owner, BlockPos pos, Direction side) {
            this.owner = owner;
            this.pos = pos;
            this.side = side;
        }

        private @Nullable Level level() {
            return owner.be.isRemoved() ? null : owner.be.getLevel();
        }

        private @Nullable IFluidHandlerMK2 half(
                BlockApiLookup<IFluidHandlerMK2, FluidFace> token, @Nullable Fluid fluid) {
            Level level = level();
            return level == null ? null : resolve(token, level, pos, side, fluid);
        }

        @Override
        public boolean supportsInsertion() {
            return half(FluidCaps.RECEIVER, null) != null;
        }

        @Override
        public boolean supportsExtraction() {
            return half(FluidCaps.PROVIDER, null) != null;
        }

        @Override
        public long insert(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return move(resource, maxAmount, transaction, true);
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            return move(resource, maxAmount, transaction, false);
        }

        private long move(
                FluidVariant resource, long maxAmount, TransactionContext transaction, boolean in) {
            if (resource.isBlank() || maxAmount < DROPLETS_PER_MB) return 0L;
            Fluid fluid = resource.getFluid();
            IFluidHandlerMK2 half = half(in ? FluidCaps.RECEIVER : FluidCaps.PROVIDER, fluid);
            if (half == null) return 0L;
            FluidTankNTM[] tanks = owner.machine.getAllTanks();
            if (tanks.length != owner.staging.tanks()) return 0L;
            long budget = maxAmount / DROPLETS_PER_MB;
            owner.updateSnapshots(transaction);
            long moved = 0L;
            for (int i = 0; i < tanks.length && budget > 0L; i++) {
                long step =
                        in
                                ? owner.staging.stageInsert(half, tanks, i, fluid, budget)
                                : owner.staging.stageExtract(half, tanks, i, fluid, budget);
                moved += step;
                budget -= step;
            }
            return moved * DROPLETS_PER_MB;
        }

        @Override
        public Iterator<StorageView<FluidVariant>> iterator() {
            int tanks = owner.machine.getAllTanks().length;
            while (views.size() < tanks) views.add(new TankView(this, views.size()));
            return views.subList(0, Math.min(tanks, views.size())).iterator();
        }
    }

    private record TankView(Face face, int index) implements StorageView<FluidVariant> {

        private @Nullable FluidTankNTM tank() {
            FluidTankNTM[] tanks = face.owner.machine.getAllTanks();
            return index < tanks.length && tanks.length == face.owner.staging.tanks()
                    ? tanks[index]
                    : null;
        }

        @Override
        public long extract(FluidVariant resource, long maxAmount, TransactionContext transaction) {
            if (resource.isBlank() || maxAmount < DROPLETS_PER_MB) return 0L;
            FluidTankNTM[] tanks = face.owner.machine.getAllTanks();
            if (index >= tanks.length || tanks.length != face.owner.staging.tanks()) return 0L;
            Fluid fluid = resource.getFluid();
            IFluidHandlerMK2 provider = face.half(FluidCaps.PROVIDER, fluid);
            if (provider == null) return 0L;
            face.owner.updateSnapshots(transaction);
            return face.owner.staging.stageExtract(
                            provider, tanks, index, fluid, maxAmount / DROPLETS_PER_MB)
                    * DROPLETS_PER_MB;
        }

        @Override
        public boolean isResourceBlank() {
            return getResource().isBlank();
        }

        @Override
        public FluidVariant getResource() {
            FluidTankNTM[] tanks = face.owner.machine.getAllTanks();
            if (index >= tanks.length || tanks.length != face.owner.staging.tanks())
                return FluidVariant.blank();
            Fluid fluid = face.owner.staging.resource(tanks, index);
            return fluid.isSame(net.minecraft.world.level.material.Fluids.EMPTY)
                    ? FluidVariant.blank()
                    : FluidVariant.of(fluid);
        }

        @Override
        public long getAmount() {
            FluidTankNTM[] tanks = face.owner.machine.getAllTanks();
            if (index >= tanks.length || tanks.length != face.owner.staging.tanks()) return 0L;
            return face.owner.staging.amountMb(tanks, index) * DROPLETS_PER_MB;
        }

        @Override
        public long getCapacity() {
            FluidTankNTM tank = tank();
            return tank == null ? 0L : (long) tank.getMaxFill() * DROPLETS_PER_MB;
        }
    }
}
