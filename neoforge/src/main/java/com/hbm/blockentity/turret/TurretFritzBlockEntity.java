/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-FileCopyrightText: HbmMods (The Bobcat)
 * SPDX-License-Identifier: LGPL-3.0-only
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 * Copyright (C) HbmMods (The Bobcat). All rights reserved.
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS FILE HEADER.
 *
 * This code is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License version 3 only,
 * as published by the Free Software Foundation.
 * This code is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Lesser General Public
 * License for more details.
 * 
 * You should have received a copy of the GNU Lesser General Public License
 * along with this code. If not, see <https://www.gnu.org/licenses/>.
 *
 * Under GPLv3 §7(c) and §7(e), these additional terms apply to LGPL-covered
 * material authored by movblock:
 *
 * - the license grants no rights to the name "NTM NEXT";
 * - a modified version must not be named NTM NEXT or presented as the
 *   official release, and must be marked as different from it.
 *
 * Accurately identifying a work as derived from NTM NEXT does not violate
 * these additional terms, provided it is clearly distinguished from the
 * official release and does not imply endorsement. Ports are encouraged to
 * acknowledge NTM NEXT as their upstream project and provide a link to it.
 */

package com.hbm.blockentity.turret;

import com.hbm.api.fluidmk2.PlainFluidReceiver;
import com.hbm.blockentity.FluidSettingsHandler;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.BulletBaseMK4Entity;
import com.hbm.inventory.container.TurretBaseMenu;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankAccess;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.CombustibleTrait;
import com.hbm.inventory.fluid.trait.FlammableTrait;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIconItem;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.sound.ModSounds;
import com.hbm.sound.WorldSounds;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class TurretFritzBlockEntity extends TurretBaseNTBlockEntity
        implements FluidTankAccess, PlainFluidReceiver, FluidSettingsHandler {

    private static final int TANK_CAPACITY = 16000;
    private static final int FUEL_PER_SHOT = 2;
    private static final int DIESEL_PER_ITEM = 1000;
    private static final float HEAT_PER_DAMAGE = 500_000F;
    private static final float MAX_DAMAGE = 20F;
    private static final float FLAME_SPREAD = 0.05F;
    private static final int SLOT_SERVICE = 9;
    private static final int[] AUTOMATION_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};
    private static final int TANK_UNIT = 11;

    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.DIESEL, TANK_CAPACITY);

    public TurretFritzBlockEntity(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_FRITZ.get(), pos, state);
    }

    @Override
    protected @Nullable List<BulletConfig> getAmmoList() {
        return null;
    }

    @Override
    public List<ItemStack> getAmmoTypesForDisplay() {
        if (ammoStacks != null) return ammoStacks;
        List<ItemStack> stacks = new ArrayList<>();
        stacks.add(ModItems.AMMO_STANDARD.stack(EnumAmmo.FLAME_DIESEL));
        for (Fluid fluid : NTMFluids.displayOrder()) {
            if (NTMFluidProperties.hasTrait(fluid, CombustibleTrait.class)
                    && NTMFluidProperties.hasTrait(fluid, FT_Liquid.class))
                stacks.add(FluidIconItem.make(fluid));
        }
        ammoStacks = List.copyOf(stacks);
        return ammoStacks;
    }

    @Override
    public void tickServer() {
        super.tickServer();

        boolean changed = tank.setType(SLOT_SERVICE, SLOT_SERVICE, inventory);
        for (int slot = SLOT_AMMO_FIRST; slot < SLOT_SERVICE; slot++) {
            changed |= tank.loadTank(slot, SLOT_SERVICE, inventory);
        }
        for (int slot = SLOT_AMMO_FIRST; slot <= SLOT_AMMO_LAST; slot++) {
            if (!ModItems.AMMO_STANDARD.is(getItem(slot), EnumAmmo.FLAME_DIESEL)) continue;
            if (!tank.accepts(NTMFluids.DIESEL)
                    || tank.getFill() + DIESEL_PER_ITEM > tank.getMaxFill()) continue;
            tank.receive(NTMFluids.DIESEL, DIESEL_PER_ITEM);
            removeItem(slot, 1);
            changed = true;
        }
        if (changed) markChanged();
    }

    @Override
    public void updateFiringTick() {
        Fluid fuel = tank.getDeclaredFluid();
        FlammableTrait flame = NTMFluidProperties.getTrait(fuel, FlammableTrait.class);
        if (flame == null
                || !NTMFluidProperties.hasTrait(fuel, FT_Liquid.class)
                || tank.getFill() < FUEL_PER_SHOT) return;

        tank.setFill(tank.getFill() - FUEL_PER_SHOT);
        markChanged();

        float damage = Math.min((float) flame.heatPerBucket() / HEAT_PER_DAMAGE, MAX_DAMAGE);
        BulletConfig config =
                fuel == NTMFluids.BALEFIRE_FUEL
                        ? XFactoryFlamer.flame_nograv_bf
                        : XFactoryFlamer.flame_nograv;
        Vec3 tip = barrelTip();
        var shot =
                new BulletBaseMK4Entity(
                        level,
                        config,
                        damage,
                        FLAME_SPREAD,
                        (float) rotationYaw,
                        (float) rotationPitch);
        shot.snapTo(tip.x, tip.y, tip.z, shot.getYRot(), shot.getXRot());
        level.addFreshEntity(shot);

        WorldSounds.machine(
                level,
                null,
                worldPosition,
                ModSounds.FLAMETHROWER_SHOOT.get(),
                SoundSource.PLAYERS,
                2.0F,
                1F + level.getRandom().nextFloat() * 0.5F);
    }

    @Override
    protected @Nullable Fluid connectorFluid() {
        return tank.getDeclaredFluid();
    }

    @Override
    public boolean canReceiveFluid(Fluid type) {
        return tank.joins(type);
    }

    @Override
    public void forEachTank(Consumer<FluidTankNTM> visitor) {
        visitor.accept(tank);
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        return pressure == 0 && tank.accepts(type) ? tank.getMaxFill() - tank.getFill() : 0L;
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0 || !tank.accepts(type)) return amount;
        int added = tank.receive(type, (int) Math.min(amount, Integer.MAX_VALUE));
        if (added > 0) markChanged();
        return amount - added;
    }

    @Override
    public int tankCount() {
        return 1;
    }

    @Override
    public FluidTankNTM tank(int index) {
        if (index != 0) throw new IndexOutOfBoundsException(index);
        return tank;
    }

    @Override
    public FluidTankNTM pasteTank() {
        return tank;
    }

    @Override
    public int[] getSlotsForFace(Direction side) {
        return AUTOMATION_SLOTS;
    }

    @Override
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getDetectorRange() {
        return 48D;
    }

    @Override
    public double getDetectorGrace() {
        return 2D;
    }

    @Override
    public double getTurretElevation() {
        return 45D;
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15D;
    }

    @Override
    public double getBarrelLength() {
        return 2.25D;
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretFritz");
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new TurretBaseMenu(containerId, playerInventory, this);
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("diesel").ifPresent(tank::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tank.serialize(output.child("diesel"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << TANK_UNIT;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        if (unit == TANK_UNIT) tank.packetSerialize(output);
        else super.writeSyncUnit(unit, output);
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        if (unit == TANK_UNIT) tank.packetDeserialize(input);
        else super.readSyncUnit(unit, input);
    }

    static {
        com.hbm.packet.SyncBindings.field(TurretFritzBlockEntity.class, "tank", 3, 1L << TANK_UNIT);
    }
}
