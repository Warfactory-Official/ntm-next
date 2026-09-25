// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.blocks.ModBlockEntities;
import com.hbm.config.BombConfig;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityCloudFleijaRainbow;
import com.hbm.entity.logic.EntityNukeExplosionMK3;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.inventory.IGUIProvider;
import com.hbm.inventory.container.MenuCore;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.items.machine.ItemCatalyst;
import com.hbm.items.special.ItemAMSCore;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.saveddata.satellites.SatelliteRayEvents;
import com.hbm.tileentity.BlockEntityMachineBase;
import io.netty.buffer.ByteBuf;
import java.util.Iterator;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ClipContext;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.phys.shapes.CollisionContext;

public class BlockEntityCore extends BlockEntityMachineBase
        implements IGUIProvider, SyncUnitSchema {

    public static final int SLOT_CATALYST_A = 0;
    public static final int SLOT_CORE = 1;
    public static final int SLOT_CATALYST_B = 2;
    public static final int SLOT_COUNT = 3;

    public static final int TANK_CAPACITY = 128_000;

    public static final int JOULES_PER_MB = 1000;
    public static final int JOULES_PER_HEAT = 10_000;
    private static final int SUPPRESSION_RANGE = 300;
    private static final int MELTDOWN_RADIATION = 100;

    @SyncField(units = 1L << 0)
    public final FluidTankNTM[] tanks = {
        new FluidTankNTM(NTMFluids.DEUTERIUM, TANK_CAPACITY),
        new FluidTankNTM(NTMFluids.TRITIUM, TANK_CAPACITY)
    };

    @SyncField(units = 1L << 1)
    public int field;

    @SyncField(units = 1L << 2)
    public int heat;

    @SyncField(units = 1L << 3)
    public int color;

    @SyncField(units = 1L << 4)
    public boolean meltdownTick;

    private boolean lastTickValid;
    private int consumption;
    private int prevConsumption;

    public BlockEntityCore(BlockPos pos, BlockState state) {
        super(ModBlockEntities.CORE_CORE.get(), pos, state, SLOT_COUNT);
    }

    public static float getFuelEfficiency(Fluid type) {
        if (type == NTMFluids.HYDROGEN) return 1.0F;
        if (type == NTMFluids.DEUTERIUM) return 1.5F;
        if (type == NTMFluids.TRITIUM) return 1.7F;
        if (type == NTMFluids.OXYGEN) return 1.2F;
        if (type == NTMFluids.PEROXIDE) return 1.4F;
        if (type == NTMFluids.XENON) return 1.5F;
        if (type == NTMFluids.SAS3) return 2.0F;
        if (type == NTMFluids.BALEFIRE_FUEL) return 2.5F;
        if (type == NTMFluids.AMAT) return 2.2F;
        if (type == NTMFluids.ASCHRAB) return 2.7F;
        return 0;
    }

    private static int calcAvgHex(int h1, int h2) {
        int r = ((((h1 & 0xFF0000) >> 16) + ((h2 & 0xFF0000) >> 16)) / 2) << 16;
        int g = ((((h1 & 0x00FF00) >> 8) + ((h2 & 0x00FF00) >> 8)) / 2) << 8;
        int b = (((h1 & 0x0000FF) + (h2 & 0x0000FF)) / 2);
        return r | g | b;
    }

    @Override
    public void tickServer() {
        this.prevConsumption = this.consumption;
        this.consumption = 0;
        this.meltdownTick = false;

        int chunkX = worldPosition.getX() >> 4;
        int chunkZ = worldPosition.getZ() >> 4;
        var source = level.getChunkSource();
        this.lastTickValid =
                source.hasChunk(chunkX, chunkZ)
                        && source.hasChunk(chunkX + 1, chunkZ + 1)
                        && source.hasChunk(chunkX + 1, chunkZ - 1)
                        && source.hasChunk(chunkX - 1, chunkZ + 1)
                        && source.hasChunk(chunkX - 1, chunkZ - 1);

        if (lastTickValid && heat > 0 && heat >= field) detonate();

        ItemStack a = inventory.get(SLOT_CATALYST_A);
        ItemStack b = inventory.get(SLOT_CATALYST_B);
        color =
                a.getItem() instanceof ItemCatalyst && b.getItem() instanceof ItemCatalyst
                        ? calcAvgHex(ItemCatalyst.getColor(a), ItemCatalyst.getColor(b))
                        : 0;

        if (heat > 0) {
            radiation();
            if (level.getGameTime() % 100 == 0) {
                SatelliteRayEvents.report(
                        (ServerLevel) level,
                        worldPosition,
                        SatelliteRayEvents.HIGH_ENERGY_PARTICLES,
                        200);
            }
        }

        networkPackNT(250);

        heat = 0;
        if (lastTickValid && field > 0) field -= 1;

        setChanged();
    }

    public static int blastSize(int fill, int heat, int max) {
        return (int) Math.max(Math.min((long) fill * heat * 10 / max, 1000L), 50L);
    }

    private void detonate() {
        int size =
                blastSize(
                        tanks[0].getFill() + tanks[1].getFill(),
                        heat,
                        tanks[0].getMaxFill() + tanks[1].getMaxFill());

        if (!canExplode()) {
            meltdownTick = true;
            RadiationSystemNT.incrementRad((ServerLevel) level, worldPosition, MELTDOWN_RADIATION);
            return;
        }

        EntityNukeExplosionMK3 blast =
                new EntityNukeExplosionMK3(ModEntities.NUKE_EXPLOSION_MK3.get(), level);
        blast.setPos(
                worldPosition.getX() + 0.5, worldPosition.getY() + 0.5, worldPosition.getZ() + 0.5);
        blast.destructionRange = size;
        blast.speed = BombConfig.blastSpeed;
        blast.coefficient = 1.0F;
        level.addFreshEntity(blast);

        level.playSound(
                null,
                worldPosition,
                SoundEvents.GENERIC_EXPLODE.value(),
                SoundSource.BLOCKS,
                100000.0F,
                1.0F);

        level.addFreshEntity(
                EntityCloudFleijaRainbow.statFac(
                        level,
                        size,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ()));
    }

    private boolean canExplode() {
        Iterator<Map.Entry<EntityNukeExplosionMK3.ATEntry, Long>> it =
                EntityNukeExplosionMK3.at.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<EntityNukeExplosionMK3.ATEntry, Long> next = it.next();
            if (next.getValue() < level.getGameTime()) {
                it.remove();
                continue;
            }
            EntityNukeExplosionMK3.ATEntry entry = next.getKey();
            if (entry.dim() != level.dimension()) continue;
            Vec3 vec =
                    new Vec3(
                            worldPosition.getX() + 0.5 - entry.x(),
                            worldPosition.getY() + 0.5 - entry.y(),
                            worldPosition.getZ() + 0.5 - entry.z());
            if (vec.length() < SUPPRESSION_RANGE) return false;
        }
        return true;
    }

    private void radiation() {
        ServerLevel server = (ServerLevel) level;
        double scale = meltdownTick ? 5 : 3;
        double range = meltdownTick ? 50 : 10;
        double x = worldPosition.getX() + 0.5;
        double y = worldPosition.getY() + 0.5;
        double z = worldPosition.getZ() + 0.5;

        DamageSource ams = server.damageSources().source(ModDamageTypes.AMS);
        for (Entity e :
                server.getEntities(
                        (Entity) null, new AABB(x, y, z, x, y, z).inflate(range), e -> true)) {
            if (e instanceof Player player && ArmorUtil.checkForHazmat(player)) continue;
            if (isObstructed(server, x, y + 6, z, e.getX(), e.getY() + e.getEyeHeight(), e.getZ()))
                continue;
            e.hurtServer(server, ams, 1000);
            e.igniteForSeconds(3);
        }

        DamageSource core = server.damageSources().source(ModDamageTypes.AMS_CORE);
        for (Entity e :
                server.getEntities(
                        (Entity) null, new AABB(x, y, z, x, y, z).inflate(scale), e -> true)) {
            if (e instanceof Player player && ArmorUtil.checkForHaz2(player)) continue;
            e.hurtServer(server, core, 10000);
        }
    }

    private static boolean isObstructed(
            ServerLevel level, double x, double y, double z, double ex, double ey, double ez) {
        ClipContext ctx =
                new ClipContext(
                        new Vec3(x, y, z),
                        new Vec3(ex, ey, ez),
                        ClipContext.Block.OUTLINE,
                        ClipContext.Fluid.NONE,
                        CollisionContext.empty());
        return level.clip(ctx).getType() != HitResult.Type.MISS;
    }

    public long burn(long joules) {
        if (!isReady()) return joules;

        int demand = (int) Math.ceil((double) joules / JOULES_PER_MB);
        if (tanks[0].getFill() < demand || tanks[1].getFill() < demand) return joules;

        this.consumption += demand;
        heat += (int) Math.ceil((double) joules / JOULES_PER_HEAT);

        tanks[0].setFill(tanks[0].getFill() - demand);
        tanks[1].setFill(tanks[1].getFill() - demand);

        return (long)
                (joules
                        * getCoreMultiplier()
                        * getFuelEfficiency(tanks[0].getTankType())
                        * getFuelEfficiency(tanks[1].getTankType()));
    }

    public boolean isReady() {
        if (!lastTickValid) return false;
        if (getCoreMultiplier() == 0) return false;
        if (color == 0) return false;
        return !(getFuelEfficiency(tanks[0].getTankType()) <= 0)
                && !(getFuelEfficiency(tanks[1].getTankType()) <= 0);
    }

    public int getCoreMultiplier() {
        return ItemAMSCore.getMultiplier(inventory.get(SLOT_CORE));
    }

    public int getFieldScaled(int i) {
        return (field * i) / 100;
    }

    public int getHeatScaled(int i) {
        return (heat * i) / 100;
    }

    public int getPrevConsumption() {
        return prevConsumption;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_CATALYST_A, SLOT_CATALYST_B -> stack.getItem() instanceof ItemCatalyst;
            case SLOT_CORE -> stack.getItem() instanceof ItemAMSCore;
            default -> false;
        };
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("fuel1").ifPresent(tanks[0]::deserialize);
        input.child("fuel2").ifPresent(tanks[1]::deserialize);
        this.field = input.getIntOr("field", 0);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        tanks[0].serialize(output.child("fuel1"));
        tanks[1].serialize(output.child("fuel2"));
        output.putInt("field", field);
    }

    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory inventory, Player player) {
        return new MenuCore(containerId, inventory, this);
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.dfcCore");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    private void writeTanks(ByteBuf output) {
        for (int i = 0; i < 2; i++) tanks[i].packetSerialize(output);
    }

    private void readTanks(ByteBuf input) {
        for (int i = 0; i < 2; i++) tanks[i].packetDeserialize(input);
    }

    @Override
    public long syncUnitMask() {
        return 0x1fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> writeTanks(output);
            case 1 -> output.writeInt(this.field);
            case 2 -> output.writeInt(this.heat);
            case 3 -> output.writeInt(this.color);
            case 4 -> output.writeBoolean(this.meltdownTick);
            default -> throw new IllegalArgumentException();
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> readTanks(input);
            case 1 -> this.field = input.readInt();
            case 2 -> this.heat = input.readInt();
            case 3 -> this.color = input.readInt();
            case 4 -> this.meltdownTick = input.readBoolean();
            default -> throw new IllegalArgumentException();
        }
    }
}
