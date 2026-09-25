// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine;

import com.hbm.api.energymk2.IBatteryItem;
import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.energymk2.ItemEnergyTransfer;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuMachineTurbofan;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Combustible.FuelGrade;
import com.hbm.inventory.fluid.trait.FT_Combustible;
import com.hbm.inventory.fluid.trait.FluidTrait.FluidReleaseType;
import com.hbm.items.ModItems;
import com.hbm.items.machine.FluidIdentifierItem;
import com.hbm.items.machine.upgrade.ItemMachineUpgrade;
import com.hbm.items.machine.upgrade.UpgradeManager;
import com.hbm.items.machine.upgrade.UpgradeType;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.GasFlamePayload;
import com.hbm.particle.ParticleGiblet;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.platform.Services;
import com.hbm.sound.AudioSystem;
import com.hbm.sound.AudioWrapper;
import com.hbm.sound.ModSounds;
import com.hbm.tileentity.AudioLoop;
import com.hbm.tileentity.BlockEntityMachinePolluting;
import com.hbm.tileentity.IFluidCopiable;
import com.hbm.tileentity.IUpgradeInfoProvider;
import com.hbm.util.TickPhase;
import io.netty.buffer.ByteBuf;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class BlockEntityMachineTurbofan extends BlockEntityMachinePolluting
        implements AudioLoop,
                IEnergyHandlerMK2,
                FluidTankEndpoint,
                MenuProvider,
                IUpgradeInfoProvider,
                IFluidCopiable,
                SyncUnitSchema {

    public static final int SLOT_FLUID_IN = 0;
    public static final int SLOT_CONTAINER_OUT = 1;
    public static final int SLOT_UPGRADE = 2;
    public static final int SLOT_BATTERY = 3;
    public static final int SLOT_FLUID_ID = 4;
    public static final int SLOT_COUNT = 5;

    public static final long MAX_POWER = 1_000_000L;
    public static final int FLUID_CAP = 24_000;
    public static final int SMOKE_CAP = 150;

    public static final int PONY_AFTERBURNER = 100;

    private static final int[] VALID_UPGRADES =
            IUpgradeInfoProvider.upgradeCaps(UpgradeType.AFTERBURN, 3);
    public static @Nullable Consumer<BlockEntityMachineTurbofan> CLIENT_SOUND;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM tank = new FluidTankNTM(NTMFluids.KEROSENE, FLUID_CAP);

    @SyncField(units = 1L << 5)
    public final FluidTankNTM blood = new FluidTankNTM(NTMFluids.BLOOD, FLUID_CAP);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;
    private final FluidTankNTM[] all;
    private final UpgradeManager upgradeManager = new UpgradeManager(this);

    @SyncField(units = 1L << 0)
    public long power;

    @SyncField(units = 1L << 1)
    public int afterburner;

    @SyncField(units = 1L << 2)
    public boolean wasOn;

    @SyncField(units = 1L << 3)
    public boolean showBlood;

    private int output;
    private int consumption;

    public float spin;
    public float lastSpin;
    public int momentum;

    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityMachineTurbofan(BlockPos pos, BlockState state) {
        super(ModBlockEntities.MACHINE_TURBOFAN.get(), pos, state, SLOT_COUNT, SMOKE_CAP);
        receiving = new FluidTankNTM[] {tank};
        sending = new FluidTankNTM[] {blood, smoke, smokeLeaded, smokePoison};
        all = new FluidTankNTM[] {tank, blood, smoke, smokeLeaded, smokePoison};
    }

    public static long getHEFromFuel(@Nullable Fluid type) {
        FT_Combustible fuel = NTMFluidProperties.getTrait(type, FT_Combustible.class);
        if (fuel == null || fuel.getGrade() != FuelGrade.AERO) return 0;
        return fuel.getCombustionEnergy() / 1_000L;
    }

    @Override
    public void tickServer() {
        output = 0;
        consumption = 0;

        boolean changed = tank.setType(SLOT_FLUID_ID, SLOT_FLUID_ID, inventory);
        changed |= tank.loadTank(SLOT_FLUID_IN, SLOT_CONTAINER_OUT, inventory);
        if (changed) setChanged();

        wasOn = false;

        upgradeManager.scan(this, SLOT_UPGRADE, SLOT_UPGRADE);
        afterburner = upgradeManager.getLevel(UpgradeType.AFTERBURN);
        if (inventory.get(SLOT_UPGRADE).is(ModItems.FLAME_PONY.get()))
            afterburner = PONY_AFTERBURNER;

        boolean redstone = blockedByRedstone();

        long burnValue = redstone ? 0 : getHEFromFuel(tank.getTankType());
        int amountToBurn = Math.min(1 + afterburner, tank.getFill());

        if (!redstone && amountToBurn > 0) {
            wasOn = true;
            tank.setFill(tank.getFill() - amountToBurn);
            output = (int) (burnValue * amountToBurn * (1 + Math.min(afterburner / 3D, 4)));
            power += output;
            consumption = amountToBurn;

            if (TickPhase.every(this, 20))
                pollute(tank.getTankType(), FluidReleaseType.BURN, amountToBurn * 5F);
        }

        power -= ItemEnergyTransfer.insert(this, SLOT_BATTERY, power, false);

        if (burnValue > 0 && amountToBurn > 0) blast((ServerLevel) level);

        if (power > MAX_POWER) power = MAX_POWER;

        flush.provide((ServerLevel) level, this);

        networkPackNT(150);
    }

    private boolean blockedByRedstone() {
        Direction facing = BlockMultiblockCore.coreFacing(getBlockState());
        Direction back = facing.getOpposite();
        Direction rot = facing.getClockWise().getOpposite();
        var chunks = ((ServerLevel) level).getChunkSource();
        BlockPos port = worldPosition.relative(facing, 2);

        if (chunks.getChunkNow(port.getX() >> 4, port.getZ() >> 4) != null
                && level.hasNeighborSignal(port)) return true;
        port = port.relative(rot);
        if (chunks.getChunkNow(port.getX() >> 4, port.getZ() >> 4) != null
                && level.hasNeighborSignal(port)) return true;
        port = worldPosition.relative(back, 2);
        if (chunks.getChunkNow(port.getX() >> 4, port.getZ() >> 4) != null
                && level.hasNeighborSignal(port)) return true;
        port = port.relative(rot);
        return chunks.getChunkNow(port.getX() >> 4, port.getZ() >> 4) != null
                && level.hasNeighborSignal(port);
    }

    private void blast(ServerLevel level) {
        Direction thrust = BlockMultiblockCore.coreFacing(getBlockState()).getClockWise();
        Vec3 push = new Vec3(-thrust.getStepX() * 0.2, 0, -thrust.getStepZ() * 0.2);
        if (afterburner > 0) emitAfterburnerEffects(level, thrust);

        for (Entity entity :
                level.getEntities((Entity) null, box(thrust, -3.5, -19.5), e -> true)) {
            if (afterburner > 0) {
                entity.igniteForSeconds(5);
                entity.hurtServer(level, level.damageSources().onFire(), 5F);
            }
            shove(entity, push);
        }

        for (Entity entity : level.getEntities((Entity) null, box(thrust, 3.5, 8.5), e -> true)) {
            shove(entity, push);
        }

        for (Entity entity : level.getEntities((Entity) null, box(thrust, 3.5, 3.75), e -> true)) {
            entity.hurtServer(level, level.damageSources().source(ModDamageTypes.BLENDER), 1000F);
            entity.makeStuckInBlock(
                    Blocks.COBWEB.defaultBlockState(), new Vec3(0.25D, 0.05D, 0.25D));

            if (!entity.isAlive() && entity instanceof LivingEntity) {
                ParticleCreators.giblets(level, entity, ParticleGiblet.TYPE_MEAT, 5);
                level.playSound(
                        null,
                        entity.getX(),
                        entity.getY(),
                        entity.getZ(),
                        SoundEvents.ZOMBIE_BREAK_WOODEN_DOOR,
                        SoundSource.BLOCKS,
                        2.0F,
                        0.95F + level.getRandom().nextFloat() * 0.2F);
                blood.setFill(Math.min(blood.getFill() + 50, blood.getMaxFill()));
                showBlood = true;
            }
        }
    }

    private void emitAfterburnerEffects(ServerLevel level, Direction thrust) {
        RandomSource random = level.getRandom();
        for (int i = 0; i < 2; i++) {
            double speed = 2 + random.nextDouble() * 3;
            double deviation = random.nextGaussian() * 0.2;
            double x = worldPosition.getX() + 0.5 - thrust.getStepX() * (3 - i);
            double y = worldPosition.getY() + 1.5;
            double z = worldPosition.getZ() + 0.5 - thrust.getStepZ() * (3 - i);
            Services.NETWORK.sendToAllAround(
                    new GasFlamePayload(
                            x,
                            y,
                            z,
                            -thrust.getStepX() * speed + deviation,
                            0,
                            -thrust.getStepZ() * speed + deviation,
                            8F),
                    new TargetPoint(
                            level,
                            worldPosition.getX(),
                            worldPosition.getY(),
                            worldPosition.getZ(),
                            150));
        }

        if (afterburner <= 90) return;
        if (random.nextInt(30) == 0) {
            level.playSound(
                    null,
                    worldPosition.getX() + 0.5,
                    worldPosition.getY() + 1.5,
                    worldPosition.getZ() + 0.5,
                    ModSounds.BLOCK_DAMAGE.get(),
                    SoundSource.BLOCKS,
                    3F,
                    0.95F + random.nextFloat() * 0.2F);
        }

        Direction across = thrust.getClockWise();
        double motionY = 0.1 * random.nextDouble();
        double x =
                worldPosition.getX()
                        + 0.5
                        + thrust.getStepX() * (random.nextDouble() * 4 - 2)
                        + across.getStepX() * (random.nextDouble() * 2 - 1);
        double y = worldPosition.getY() + 1 + random.nextDouble() * 2;
        double z =
                worldPosition.getZ()
                        + 0.5
                        - thrust.getStepZ() * (random.nextDouble() * 4 - 2)
                        + across.getStepZ() * (random.nextDouble() * 2 - 1);
        Services.NETWORK.sendToAllAround(
                new GasFlamePayload(x, y, z, 0, motionY, 0, 4F),
                new TargetPoint(
                        level,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        150));
    }

    private AABB box(Direction thrust, double near, double far) {
        Direction across = thrust.getClockWise();
        double x = worldPosition.getX() + 0.5;
        double z = worldPosition.getZ() + 0.5;
        double minX = x + thrust.getStepX() * near - across.getStepX() * 1.5;
        double maxX = x + thrust.getStepX() * far + across.getStepX() * 1.5;
        double minZ = z + thrust.getStepZ() * near - across.getStepZ() * 1.5;
        double maxZ = z + thrust.getStepZ() * far + across.getStepZ() * 1.5;
        return new AABB(
                Math.min(minX, maxX),
                worldPosition.getY(),
                Math.min(minZ, maxZ),
                Math.max(minX, maxX),
                worldPosition.getY() + 3,
                Math.max(minZ, maxZ));
    }

    private static void shove(Entity entity, Vec3 push) {
        entity.push(push.x, push.y, push.z);
        if (entity instanceof ServerPlayer player) player.hurtMarked = true;
    }

    @Override
    public void tickClient() {
        lastSpin = spin;

        if (wasOn) {
            if (momentum < 100) momentum++;
        } else if (momentum > 0) {
            momentum--;
        }

        spin += momentum / 2;

        if (spin >= 360) {
            spin -= 360F;
            lastSpin -= 360F;
        }

        if (CLIENT_SOUND != null) CLIENT_SOUND.accept(this);
    }

    @Override
    public AudioWrapper createAudioLoop() {
        return AudioSystem.getLoopedSound(
                ModSounds.TURBOFAN_LOOP.get(),
                SoundSource.BLOCKS,
                worldPosition.getX(),
                worldPosition.getY(),
                worldPosition.getZ(),
                1.0F,
                50F,
                1.0F,
                20);
    }

    @Override
    public long getPower() {
        return power;
    }

    @Override
    public void setPower(long p) {
        power = Math.max(0L, Math.min(p, MAX_POWER));
    }

    @Override
    public long getMaxPower() {
        return MAX_POWER;
    }

    @Override
    public FluidTankNTM[] getReceivingTanks() {
        return receiving;
    }

    @Override
    public FluidTankNTM[] getSendingTanks() {
        return sending;
    }

    @Override
    public FluidTankNTM[] getAllTanks() {
        return all;
    }

    @Override
    public int[] getValidUpgrades() {
        return VALID_UPGRADES;
    }

    @Override
    public boolean canPlaceItem(int slot, ItemStack stack) {
        return switch (slot) {
            case SLOT_FLUID_IN -> FluidTankNTM.isFluidContainer(stack);
            case SLOT_UPGRADE ->
                    stack.getItem() instanceof ItemMachineUpgrade
                            || stack.is(ModItems.FLAME_PONY.get());
            case SLOT_BATTERY -> IBatteryItem.isBattery(stack);
            case SLOT_FLUID_ID -> stack.getItem() instanceof FluidIdentifierItem;
            default -> false;
        };
    }

    private void writeAfterburner(ByteBuf output) {
        output.writeByte(afterburner);
    }

    private void readAfterburner(ByteBuf input) {
        afterburner = input.readByte();
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.machineTurbofan");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public AbstractContainerMenu createMenu(
            int containerId, Inventory playerInventory, Player player) {
        return new MenuMachineTurbofan(containerId, playerInventory, this);
    }

    @Override
    public @Nullable FluidTankNTM getTankToPaste() {
        return tank;
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.getLong("power").ifPresent(v -> power = v);
        showBlood = input.getBooleanOr("showBlood", false);
        input.child("tank").ifPresent(tank::deserialize);
        input.child("blood").ifPresent(blood::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        output.putLong("power", power);
        output.putBoolean("showBlood", showBlood);
        tank.serialize(output.child("tank"));
        blood.serialize(output.child("blood"));
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x3fL;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 0 -> output.writeLong(this.power);
            case 1 -> writeAfterburner(output);
            case 2 -> output.writeBoolean(this.wasOn);
            case 3 -> output.writeBoolean(this.showBlood);
            case 4 -> this.tank.packetSerialize(output);
            case 5 -> this.blood.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 0 -> this.power = input.readLong();
            case 1 -> readAfterburner(input);
            case 2 -> this.wasOn = input.readBoolean();
            case 3 -> this.showBlood = input.readBoolean();
            case 4 -> this.tank.packetDeserialize(input);
            case 5 -> this.blood.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
