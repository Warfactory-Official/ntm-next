// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.integration.top;

import com.hbm.api.energymk2.IEnergyHandlerMK2;
import com.hbm.api.fluidmk2.IFluidHandlerMK2;
import com.hbm.blocks.multiblock.MultiblockSurface;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.lib.Library;
import java.util.function.Function;
import mcjty.theoneprobe.Tools;
import mcjty.theoneprobe.api.Color;
import mcjty.theoneprobe.api.CompoundText;
import mcjty.theoneprobe.api.IProbeConfig;
import mcjty.theoneprobe.api.IProbeHitData;
import mcjty.theoneprobe.api.IProbeInfo;
import mcjty.theoneprobe.api.IProbeInfoProvider;
import mcjty.theoneprobe.api.ITheOneProbe;
import mcjty.theoneprobe.api.NumberFormat;
import mcjty.theoneprobe.api.ProbeMode;
import mcjty.theoneprobe.api.TextStyleClass;
import mcjty.theoneprobe.apiimpl.elements.ElementProgress;
import mcjty.theoneprobe.config.Config;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.neoforged.neoforge.fluids.FluidStack;
import org.jspecify.annotations.Nullable;

public final class NTMTopPlugin
        implements IProbeInfoProvider, Function<ITheOneProbe, @Nullable Void> {

    private static final Component HE = Component.literal("HE");
    private static final Component MB = Component.literal("mB");

    @Override
    public @Nullable Void apply(ITheOneProbe probe) {
        probe.registerProvider(this);
        probe.registerBlockDisplayOverride(
                (mode, info, player, level, state, data) ->
                        MultiblockSurface.isFoldedCell(state)
                                && MultiblockSurface.coreOfFoldedCell(level, data.getPos(), state)
                                        == null);
        return null;
    }

    @Override
    public Identifier getID() {
        return Library.id("storage");
    }

    @Override
    public void addProbeInfo(
            ProbeMode mode,
            IProbeInfo info,
            Player player,
            Level level,
            BlockState state,
            IProbeHitData data) {
        BlockPos pos =
                MultiblockSurface.isFoldedCell(state)
                        ? MultiblockSurface.coreOfFoldedCell(level, data.getPos(), state)
                        : data.getPos();
        if (pos == null) return;
        BlockEntity owner = level.getBlockEntity(pos);
        IProbeConfig config = Config.getRealConfig();
        if (config.getRFMode() > 0
                && owner instanceof IEnergyHandlerMK2 machine
                && machine.getMaxPower() > 0) {
            energy(info, config.getRFMode(), machine.getPower(), machine.getMaxPower());
        }
        if (Tools.show(mode, config.getShowTankSetting())
                && config.getTankMode() > 0
                && owner instanceof IFluidHandlerMK2 machine) {
            for (FluidTankNTM tank : machine.getAllTanks()) {
                Fluid fluid = tank.getFluid();
                if (fluid == null || tank.getFill() <= 0) continue;
                tank(
                        info,
                        config.getTankMode(),
                        new FluidStack(fluid, tank.getFill()),
                        tank.getMaxFill());
            }
        }
    }

    private static void energy(IProbeInfo info, int rfMode, long power, long maxPower) {
        if (rfMode == 1) {
            info.progress(
                    power,
                    maxPower,
                    info.defaultProgressStyle()
                            .suffix(HE)
                            .filledColor(Config.rfbarFilledColor)
                            .alternateFilledColor(Config.rfbarAlternateFilledColor)
                            .borderColor(Config.rfbarBorderColor)
                            .numberFormat(Config.rfFormat.get()));
        } else {
            info.text(
                    CompoundText.create()
                            .style(TextStyleClass.PROGRESS)
                            .text(ElementProgress.format(power, Config.rfFormat.get(), HE)));
        }
    }

    private static void tank(IProbeInfo info, int tankMode, FluidStack stack, int capacity) {
        int contents = stack.getAmount();
        if (tankMode == 1) {
            Color color =
                    stack.getFluid() == Fluids.LAVA
                            ? new Color(255, 139, 27)
                            : new Color(0xffffffff);
            MutableComponent text =
                    Component.empty()
                            .append(ElementProgress.format(contents, Config.tankFormat.get(), MB))
                            .append("/")
                            .append(ElementProgress.format(capacity, Config.tankFormat.get(), MB));
            info.tankSimple(
                    capacity,
                    stack,
                    info.defaultProgressStyle()
                            .numberFormat(NumberFormat.NONE)
                            .borderlessColor(color, color.darker().darker())
                            .prefix(stack.getHoverName().copy().append(": "))
                            .suffix(text));
        } else {
            info.text(
                    CompoundText.create()
                            .style(TextStyleClass.NAME)
                            .text("Liquid:")
                            .info(stack.getFluidType().getDescriptionId(stack)));
            if (tankMode == 2) {
                info.progress(
                        contents,
                        capacity,
                        info.defaultProgressStyle()
                                .suffix(MB)
                                .filledColor(Config.tankbarFilledColor)
                                .alternateFilledColor(Config.tankbarAlternateFilledColor)
                                .borderColor(Config.tankbarBorderColor)
                                .numberFormat(Config.tankFormat.get()));
            } else {
                info.text(
                        CompoundText.create()
                                .style(TextStyleClass.PROGRESS)
                                .text(
                                        ElementProgress.format(
                                                contents, Config.tankFormat.get(), MB)));
            }
        }
    }
}
