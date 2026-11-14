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

package com.hbm.blockentity.machine.dfc;

import com.hbm.blockentity.MachineBlockEntity;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.effect.CloudFleijaRainbowEntity;
import com.hbm.entity.logic.NukeExplosionMK3Entity;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.container.CoreMenu;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.CatalystItem;
import com.hbm.items.special.AMSCoreItem;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.util.LineOfSight;
import com.hbm.util.SectionAccess;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.core.SectionPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;

public class DfcChamberBlockEntity extends MachineBlockEntity
        implements MenuProvider, FluidTankAccess, SyncUnitSchema {
    public static final int SLOT_CATALYST_A = 0, SLOT_CORE = 1, SLOT_CATALYST_B = 2, SLOT_COUNT = 3;

    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.DEUTERIUM, 128_000), new FluidTankNTM(NTMFluids.TRITIUM, 128_000)
    };
    public int field;
    public int heat;
    public int color;
    public boolean meltdownTick;
    private boolean surroundingsReady;
    private int consumed, previousConsumption;

    public DfcChamberBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_CORE.get(), pos, state, SLOT_COUNT);
        for (var tank : tanks) tank.bindOwner(this);
    }

    public static float fuelCoefficient(Fluid fluid) {
        return fluid == null ? 0 : FuelFactors.BY_ID[BuiltInRegistries.FLUID.getId(fluid)];
    }

    private static final class FuelFactors {
        static final float[] BY_ID = create();

        private static float[] create() {
            var factors = new float[BuiltInRegistries.FLUID.size()];
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.HYDROGEN)] = 1.0F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.DEUTERIUM)] = 1.5F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.TRITIUM)] = 1.7F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.OXYGEN)] = 1.2F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.PEROXIDE)] = 1.4F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.XENON)] = 1.5F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.SAS3)] = 2.0F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.BALEFIRE_FUEL)] = 2.5F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.AMAT)] = 2.2F;
            factors[BuiltInRegistries.FLUID.getId(NTMFluids.ASCHRAB)] = 2.7F;
            return factors;
        }
    }

    public long amplify(long incoming) {
        int multiplier = AMSCoreItem.getMultiplier(getItem(SLOT_CORE));
        float first = fuelCoefficient(tanks[0].getTankType());
        float second = fuelCoefficient(tanks[1].getTankType());
        if (!surroundingsReady || multiplier == 0 || color == 0 || first <= 0 || second <= 0)
            return incoming;
        int fuel = (int) Math.ceil((double) incoming / 1_000);
        if (tanks[0].getFill() < fuel || tanks[1].getFill() < fuel) return incoming;
        tanks[0].setFill(tanks[0].getFill() - fuel);
        tanks[1].setFill(tanks[1].getFill() - fuel);
        consumed += fuel;
        hbm$syncSet$DfcChamberBlockEntity$heat(
                (int) (heat + ((int) Math.ceil((double) incoming / 10_000))));
        setChanged();
        return (long) (incoming * multiplier * first * second);
    }

    @Override
    public void tickServer() {
        var server = (ServerLevel) level;
        previousConsumption = consumed;
        consumed = 0;
        hbm$syncSet$DfcChamberBlockEntity$meltdownTick(false);
        int cx = worldPosition.getX() >> 4, cz = worldPosition.getZ() >> 4;
        surroundingsReady =
                server.hasChunk(cx, cz)
                        && server.hasChunk(cx - 1, cz - 1)
                        && server.hasChunk(cx - 1, cz + 1)
                        && server.hasChunk(cx + 1, cz - 1)
                        && server.hasChunk(cx + 1, cz + 1);
        if (surroundingsReady && heat > 0 && heat >= field) containmentFailure(server);
        var first = getItem(SLOT_CATALYST_A);
        var second = getItem(SLOT_CATALYST_B);
        if (first.getItem() instanceof CatalystItem a
                && second.getItem() instanceof CatalystItem b) {
            int ca = a.getColor(), cb = b.getColor();
            hbm$syncSet$DfcChamberBlockEntity$color(
                    (((ca >> 16 & 255) + (cb >> 16 & 255)) / 2 << 16)
                            | (((ca >> 8 & 255) + (cb >> 8 & 255)) / 2 << 8)
                            | ((ca & 255) + (cb & 255)) / 2);
        } else hbm$syncSet$DfcChamberBlockEntity$color(0);
        if (heat > 0) {
            irradiate(server);
            if (server.getGameTime() % 100 == 0) {
                SatelliteRayEvents.report(
                        server, worldPosition, SatelliteRayEvents.HIGH_ENERGY_PARTICLES, 200);
            }
        }
        networkPackNT(250);
        hbm$syncSet$DfcChamberBlockEntity$heat(0);
        if (surroundingsReady && field > 0) {
            hbm$syncSet$DfcChamberBlockEntity$field((int) (field - 1));
            setChanged();
        }
    }

    private void containmentFailure(ServerLevel server) {
        double x = worldPosition.getX() + .5,
                y = worldPosition.getY() + .5,
                z = worldPosition.getZ() + .5;
        var claims = NukeExplosionMK3Entity.at.entrySet().iterator();
        while (claims.hasNext()) {
            var claim = claims.next();
            if (claim.getValue() < server.getGameTime()) {
                claims.remove();
                continue;
            }
            var pos = claim.getKey();
            if (pos.dim() != server.dimension()) continue;
            double dx = x - pos.x(), dy = y - pos.y(), dz = z - pos.z();
            if ((float) Math.sqrt(dx * dx + dy * dy + dz * dz) < 300)
                hbm$syncSet$DfcChamberBlockEntity$meltdownTick(true);
        }
        if (meltdownTick) {
            RadiationSystemNT.incrementRad(server, worldPosition, 100);
            return;
        }
        int radius =
                blastRadius(
                        tanks[0].getFill() + tanks[1].getFill(),
                        heat,
                        tanks[0].getMaxFill() + tanks[1].getMaxFill());
        server.addFreshEntity(NukeExplosionMK3Entity.statFacFleija(server, x, y, z, radius));
        server.playSound(
                null,
                worldPosition,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                100_000,
                1);
        server.addFreshEntity(
                CloudFleijaRainbowEntity.statFac(
                        server,
                        radius,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ()));
    }

    public static int blastRadius(int fill, int heat, int capacity) {
        return (int) Math.clamp((long) fill * heat * 10 / capacity, 50L, 1000L);
    }

    private void irradiate(ServerLevel server) {
        double x = worldPosition.getX() + .5,
                y = worldPosition.getY() + .5,
                z = worldPosition.getZ() + .5;
        int radius = meltdownTick ? 50 : 10;
        var wideDamage = server.damageSources().source(ModDamageTypes.AMS);
        var entities =
                server.getEntities(
                        (Entity) null,
                        new AABB(
                                x - radius,
                                y - radius,
                                z - radius,
                                x + radius,
                                y + radius,
                                z + radius));
        var batch = new LineOfSight.Batch(server, new SectionAccess.LoadingReader(server));
        batch.reset(x, y + 6, z);
        int[] slots = new int[entities.size()];
        for (int i = 0; i < slots.length; i++) {
            var entity = entities.get(i);
            slots[i] =
                    hazmat(entity)
                            ? -1
                            : batch.add(
                                    entity.getX(),
                                    entity.getEyeY(),
                                    entity.getZ(),
                                    SectionPos.asLong(entity.blockPosition()));
        }
        for (int i = 0; i < slots.length; i++) {
            var entity = entities.get(i);
            if (hazmat(entity)) continue;
            double ex = entity.getX(), ey = entity.getEyeY(), ez = entity.getZ();
            if (slots[i] < 0 ? batch.blocked(ex, ey, ez) : batch.blocked(slots[i], ex, ey, ez))
                continue;
            entity.hurtServer(server, wideDamage, 1000);
            entity.igniteForSeconds(3);
        }
        radius = meltdownTick ? 5 : 3;
        var closeDamage = server.damageSources().source(ModDamageTypes.AMS_CORE);
        for (var entity :
                server.getEntities(
                        (Entity) null,
                        new AABB(
                                x - radius,
                                y - radius,
                                z - radius,
                                x + radius,
                                y + radius,
                                z + radius))) {
            if (entity instanceof Player player && ArmorUtil.checkForHaz2(player)) continue;
            entity.hurtServer(server, closeDamage, 10_000);
        }
    }

    private static boolean hazmat(Entity entity) {
        return entity instanceof Player player && ArmorUtil.checkForHazmat(player);
    }

    public int getFieldScaled(int scale) {
        return field * scale / 100;
    }

    public int getHeatScaled(int scale) {
        return heat * scale / 100;
    }

    @Override
    public int tankCount() {
        return 2;
    }

    @Override
    public FluidTankNTM tank(int index) {
        return tanks[index];
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_CATALYST_A, SLOT_CATALYST_B -> stack.getItem() instanceof CatalystItem;
            case SLOT_CORE -> stack.getItem() instanceof AMSCoreItem;
            default -> false;
        };
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcCore");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(int id, Inventory inventory, Player player) {
        return new CoreMenu(id, inventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("fuel1").ifPresent(tanks[0]::deserialize);
        input.child("fuel2").ifPresent(tanks[1]::deserialize);
        hbm$syncSet$DfcChamberBlockEntity$field(input.getIntOr("field", 0));
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("fuel1"));
        tanks[1].serialize(output.child("fuel2"));
        output.putInt("field", field);
    }

    @Override
    public long syncUnitMask() {
        return 31;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> {
                tanks[0].packetSerialize(output);
                tanks[1].packetSerialize(output);
            }
            case 1 -> output.writeInt(field);
            case 2 -> output.writeInt(heat);
            case 3 -> output.writeInt(color);
            case 4 -> output.writeBoolean(meltdownTick);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> {
                tanks[0].packetDeserialize(input);
                tanks[1].packetDeserialize(input);
            }
            case 1 -> hbm$syncSet$DfcChamberBlockEntity$field(input.readInt());
            case 2 -> hbm$syncSet$DfcChamberBlockEntity$heat(input.readInt());
            case 3 -> hbm$syncSet$DfcChamberBlockEntity$color(input.readInt());
            case 4 -> hbm$syncSet$DfcChamberBlockEntity$meltdownTick(input.readBoolean());
            default -> throw new IllegalArgumentException();
        }
    }

    public final FluidTankNTM hbm$syncArraySet$DfcChamberBlockEntity$tanks(
            FluidTankNTM[] array, int index, FluidTankNTM next) {
        if (array[index] != next) {
            if (array == this.tanks && syncBound())
                com.hbm.packet.SyncBindings.unbind(this, array[index]);
            array[index] = next;
            if (array == this.tanks) {
                syncArrayChanged(array, index, 3, 1);
            }
        }
        return next;
    }

    public final void hbm$syncSet$DfcChamberBlockEntity$field(int next) {
        if (this.field != next) {
            this.field = next;
            if ((2) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 2);
        }
    }

    public final void hbm$syncSet$DfcChamberBlockEntity$heat(int next) {
        if (this.heat != next) {
            this.heat = next;
            if ((4) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 4);
        }
    }

    public final void hbm$syncSet$DfcChamberBlockEntity$color(int next) {
        if (this.color != next) {
            this.color = next;
            if ((8) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 8);
        }
    }

    public final void hbm$syncSet$DfcChamberBlockEntity$meltdownTick(boolean next) {
        if (this.meltdownTick != next) {
            this.meltdownTick = next;
            if ((16) == 0L) syncChanged(3);
            else syncUnitsChanged(3, 16);
        }
    }

    static {
        com.hbm.packet.SyncBindings.field(DfcChamberBlockEntity.class, "tanks", 3, 1);
    }
}
