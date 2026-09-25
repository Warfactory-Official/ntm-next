// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.turret;

import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.EntityBulletBaseMK4;
import com.hbm.inventory.container.MenuTurretBase;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FT_Flammable;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Liquid;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.machine.ItemFluidIcon;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.items.weapon.sedna.factory.XFactoryFlamer;
import com.hbm.packet.SyncField;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.IFluidCopiable;
import io.netty.buffer.ByteBuf;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityTurretFritz extends BlockEntityTurretBaseNT
        implements IFluidHandlerMK2, IFluidCopiable {

    private static final int[] FRITZ_SLOTS = {1, 2, 3, 4, 5, 6, 7, 8};

    @SyncField(units = 1L << 11)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.DIESEL, 16000);

    public BlockEntityTurretFritz(BlockPos pos, BlockState state) {
        super(ModBlockEntities.TURRET_FRITZ.get(), pos, state);
    }

    @Override
    public Component getName() {
        return Component.translatable("container.turretFritz");
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

        for (Fluid fluid : BuiltInRegistries.FLUID) {
            if (fluid == Fluids.EMPTY || !fluid.isSource(fluid.defaultFluidState())) continue;
            if (NTMFluidProperties.hasTrait(fluid, FT_Combustible.class)
                    && NTMFluidProperties.hasTrait(fluid, FT_Liquid.class)) {
                stacks.add(ItemFluidIcon.make(fluid));
            }
        }

        ammoStacks = stacks;
        return ammoStacks;
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
    public long getMaxPower() {
        return 10000;
    }

    @Override
    public double getBarrelLength() {
        return 2.25D;
    }

    @Override
    protected @Nullable Fluid connectorFluid() {
        return tank.getTankType();
    }

    @Override
    public double getAcceptableInaccuracy() {
        return 15;
    }

    @Override
    public void updateFiringTick() {
        Fluid type = tank.getTankType();
        if (type == null
                || !NTMFluidProperties.hasTrait(type, FT_Flammable.class)
                || !NTMFluidProperties.hasTrait(type, FT_Liquid.class)
                || tank.getFill() < 2) return;

        FT_Flammable trait = NTMFluidProperties.getTrait(type, FT_Flammable.class);
        tank.setFill(tank.getFill() - 2);

        Vec3 tip = barrelTip();
        float damage = Math.min((float) (trait.getHeatEnergy() / 500_000F), 20F);
        EntityBulletBaseMK4 proj =
                new EntityBulletBaseMK4(
                        level,
                        type == NTMFluids.BALEFIRE_FUEL
                                ? XFactoryFlamer.flame_nograv_bf
                                : XFactoryFlamer.flame_nograv,
                        damage,
                        0.05F,
                        (float) rotationYaw,
                        (float) rotationPitch);
        proj.snapTo(tip.x, tip.y, tip.z, proj.getYRot(), proj.getXRot());
        level.addFreshEntity(proj);

        level.playSound(
                null,
                worldPosition,
                ModSounds.FLAMETHROWER_SHOOT.get(),
                SoundSource.BLOCKS,
                2F,
                1F + level.getRandom().nextFloat() * 0.5F);
    }

    @Override
    public void tickServer() {
        super.tickServer();

        tank.setType(9, 9, inventory);
        for (int i = 1; i < 9; i++) tank.loadTank(i, 9, inventory);

        for (int i = SLOT_AMMO_FIRST; i <= SLOT_AMMO_LAST; i++) {
            ItemStack stack = getItem(i);
            if (ModItems.AMMO_STANDARD.is(stack, EnumAmmo.FLAME_DIESEL)) {
                if (tank.accepts(NTMFluids.DIESEL) && tank.getFill() + 1000 <= tank.getMaxFill()) {
                    tank.receive(NTMFluids.DIESEL, 1000);
                    removeItem(i, 1);
                }
            }
        }
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
    public int[] getSlotsForFace(Direction side) {
        return FRITZ_SLOTS;
    }

    @Override
    public long getDemand(Fluid type, int pressure) {
        if (pressure != 0 || !tank.accepts(type)) return 0;
        return tank.getMaxFill() - tank.getFill();
    }

    @Override
    public long transferFluid(Fluid type, int pressure, long amount) {
        if (pressure != 0 || !tank.accepts(type)) return amount;
        long headroom = tank.getMaxFill() - tank.getFill();
        long accepted = tank.receive(type, (int) Math.min(headroom, amount));
        return amount - accepted;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return new FluidTankNTM[] {tank};
    }

    @Override
    public FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuTurretBase(containerId, playerInventory, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 1L << 11;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 11 -> this.tank.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 11 -> this.tank.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
