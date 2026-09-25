// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.rbmk;

import com.hbm.api.control.IControlReceiver;
import com.hbm.api.fluidmk2.FluidFlushOutputs;
import com.hbm.api.fluidmk2.FluidTankEndpoint;
import com.hbm.api.fluidmk2.FlushLanes;
import com.hbm.api.redstoneoverradio.IRORValueProvider;
import com.hbm.blocks.ModBlockEntities;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.inventory.container.MenuRBMKBoiler;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.packet.SyncField;
import com.hbm.packet.SyncUnitSchema;
import com.hbm.packet.toclient.RbmkJetPayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import io.netty.buffer.ByteBuf;
import net.minecraft.core.BlockPos;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import org.jspecify.annotations.Nullable;

public class BlockEntityRBMKBoiler extends BlockEntityRBMKBase
        implements FluidTankEndpoint,
                IControlReceiver,
                MenuProvider,
                SyncUnitSchema,
                IRORValueProvider {
    @SyncField(units = 1L << 5)
    public final FluidTankNTM feed = new FluidTankNTM(NTMFluids.WATER, 10000);

    private final FluidTankNTM[] receiving;
    private final FluidTankNTM[] sending;

    @SyncField(units = 1L << 4)
    public final FluidTankNTM steam = new FluidTankNTM(NTMFluids.STEAM, 1000000);

    public int consumption;
    public int output;
    private int ventDelay;
    private final FluidFlushOutputs flush = new FluidFlushOutputs();

    public BlockEntityRBMKBoiler(BlockPos pos, BlockState state) {
        super(ModBlockEntities.RBMK_BOILER.get(), pos, state, 0);
        receiving = new FluidTankNTM[] {feed};
        sending = new FluidTankNTM[] {steam};
    }

    public static double getHeatFromSteam(@Nullable Fluid type) {
        if (type == NTMFluids.STEAM) return 100D;
        if (type == NTMFluids.HOTSTEAM) return 300D;
        if (type == NTMFluids.SUPERHOTSTEAM) return 450D;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 600D;
        return 0D;
    }

    public static double getFactorFromSteam(@Nullable Fluid type) {
        if (type == NTMFluids.STEAM) return 1D;
        if (type == NTMFluids.HOTSTEAM) return 10D;
        if (type == NTMFluids.SUPERHOTSTEAM) return 100D;
        if (type == NTMFluids.ULTRAHOTSTEAM) return 1000D;
        return 0D;
    }

    @Override
    public void tickServer() {
        this.consumption = 0;
        this.output = 0;
        if (this.ventDelay > 0) this.ventDelay--;

        double heatCap = getHeatFromSteam(steam.getTankType());
        double heatProvided = this.heat - heatCap;

        if (heatProvided > 0) {
            double heatPerMbWater = RBMKConfig.getBoilerHeatConsumption(getLevel());
            double steamFactor = getFactorFromSteam(steam.getTankType());
            int waterUsed;
            int steamProduced;

            if (steam.getTankType() == NTMFluids.ULTRAHOTSTEAM) {
                steamProduced =
                        (int) Math.floor((heatProvided / heatPerMbWater) * 100D / steamFactor);
                waterUsed = (int) Math.floor(steamProduced / 100D * steamFactor);
                if (feed.getFill() < waterUsed) {
                    steamProduced = (int) Math.floor(feed.getFill() * 100D / steamFactor);
                    waterUsed = (int) Math.floor(steamProduced / 100D * steamFactor);
                }
            } else {
                waterUsed = (int) Math.floor(heatProvided / heatPerMbWater);
                waterUsed = Math.min(waterUsed, feed.getFill());
                steamProduced = (int) Math.floor((waterUsed * 100D) / steamFactor);
            }

            this.consumption = waterUsed;
            this.output = steamProduced;

            feed.setFill(feed.getFill() - waterUsed);
            if (steam.getFill() + steamProduced > steam.getMaxFill()) {
                steam.setFill(steam.getMaxFill());
                if (this.ventDelay <= 0) {
                    vent();
                    this.ventDelay = 20 + level.getRandom().nextInt(10);
                    level.playSound(
                            null,
                            worldPosition.getX(),
                            worldPosition.getY() + RBMKConfig.getColumnHeight(level),
                            worldPosition.getZ(),
                            ModSounds.STEAM_ENGINE_OPERATE.get(),
                            SoundSource.BLOCKS,
                            2F,
                            1F + level.getRandom().nextFloat() * 0.25F);
                }
            } else {
                steam.setFill(steam.getFill() + steamProduced);
            }

            this.heat -= waterUsed * heatPerMbWater;
        }

        flush.provide((ServerLevel) level, this);
        super.tickServer();
    }

    @Override
    public void declareFlush(FlushLanes out) {
        out.add(steam, COLUMN_OUTPUTS);
    }

    private void vent() {
        ServerLevel server = (ServerLevel) level;
        RandomSource rand = server.getRandom();
        double px = worldPosition.getX() + 0.25D + rand.nextInt(2) * 0.5D;
        double py = worldPosition.getY() + RBMKConfig.getColumnHeight(server);
        double pz = worldPosition.getZ() + 0.25D + rand.nextInt(2) * 0.5D;
        Services.NETWORK.sendToAllAround(
                RbmkJetPayload.steam(px, py, pz),
                new TargetPoint(
                        server,
                        worldPosition.getX() + 0.5D,
                        worldPosition.getY() + 1D,
                        worldPosition.getZ() + 0.5D,
                        100));
    }

    @Override
    public void onMelt(int reduce) {
        int count = 1 + level.getRandom().nextInt(2);
        for (int i = 0; i < count; i++) spawnDebris(EntityRBMKDebris.DebrisType.BLANK);
        super.onMelt(reduce);
    }

    public void cyceCompressor() {
        if (heat > 50 && feed.getFill() > 0) return;

        Fluid type = steam.getTankType();

        int fill = steam.getFill();
        if (type == NTMFluids.STEAM) {
            steam.setTankType(NTMFluids.HOTSTEAM);
            steam.setFill(fill / 10);
        } else if (type == NTMFluids.HOTSTEAM) {
            steam.setTankType(NTMFluids.SUPERHOTSTEAM);
            steam.setFill(fill / 10);
        } else if (type == NTMFluids.SUPERHOTSTEAM) {
            steam.setTankType(NTMFluids.ULTRAHOTSTEAM);
            steam.setFill(fill / 10);
        } else if (type == NTMFluids.ULTRAHOTSTEAM) {
            steam.setTankType(NTMFluids.STEAM);
            steam.setFill(Math.min((long) fill * 1000, steam.getMaxFill()));
        }
        markChanged();
    }

    @Override
    public boolean hasPermission(Player player) {
        return player.getEyePosition()
                        .distanceToSqr(
                                worldPosition.getX(), worldPosition.getY(), worldPosition.getZ())
                < 20 * 20;
    }

    @Override
    public void receiveControl(CompoundTag data) {
        if (data.getBooleanOr("compression", false)) cyceCompressor();
    }

    @Override
    public RBMKColumnType getConsoleType() {
        return RBMKColumnType.BOILER;
    }

    @Override
    public RBMKColumn getConsoleData(RBMKColumn reuse) {
        RBMKColumn.BoilerColumn data = (RBMKColumn.BoilerColumn) super.getConsoleData(reuse);
        data.water = feed.getFill();
        data.maxWater = feed.getMaxFill();
        data.steam = steam.getFill();
        data.maxSteam = steam.getMaxFill();
        data.steamType = RBMKColumn.fluidId(steam.getTankType());
        return data;
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
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("feed").ifPresent(feed::deserialize);
        input.child("steam").ifPresent(steam::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        feed.serialize(output.child("feed"));
        steam.serialize(output.child("steam"));
    }

    @Override
    public void writeDiagnostics(CompoundTag tag) {
        super.writeDiagnostics(tag);
        writeDiagnostics(tag, "feed", feed);
        writeDiagnostics(tag, "steam", steam);
    }

    @Override
    public String[] getFunctionInfo() {
        return new String[] {
            PREFIX_VALUE + "feed", PREFIX_VALUE + "steam", PREFIX_VALUE + "consumption"
        };
    }

    @Override
    public @Nullable String provideRORValue(String name) {
        if ((PREFIX_VALUE + "feed").equals(name)) return "" + feed.getFill();
        if ((PREFIX_VALUE + "steam").equals(name)) return "" + steam.getFill();
        if ((PREFIX_VALUE + "consumption").equals(name)) return "" + consumption;
        return null;
    }

    @Override
    protected Component getDefaultName() {
        return Component.translatable("container.rbmkBoiler");
    }

    @Override
    public Component getDisplayName() {
        return getName();
    }

    @Override
    public @Nullable AbstractContainerMenu createMenu(int id, Inventory inv, Player player) {
        return new MenuRBMKBoiler(id, inv, this);
    }

    @Override
    public long syncUnitMask() {
        return super.syncUnitMask() | 0x30L;
    }

    @Override
    public void writeSyncUnit(int unit, ByteBuf output) {
        switch (unit) {
            case 4 -> this.steam.packetSerialize(output);
            case 5 -> this.feed.packetSerialize(output);
            default -> super.writeSyncUnit(unit, output);
        }
    }

    @Override
    public void readSyncUnit(int unit, ByteBuf input) {
        switch (unit) {
            case 4 -> this.steam.packetDeserialize(input);
            case 5 -> this.feed.packetDeserialize(input);
            default -> super.readSyncUnit(unit, input);
        }
    }
}
