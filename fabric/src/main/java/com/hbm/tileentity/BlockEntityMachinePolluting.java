// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FT_Polluting;
import com.hbm.inventory.fluid.trait.FluidTrait;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class BlockEntityMachinePolluting extends BlockEntityMachineBase {

    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;
    private final FluidTankNTM[] smokeTanks;

    protected BlockEntityMachinePolluting(
            BlockEntityType<?> type, BlockPos pos, BlockState state, int slots, int buffer) {
        super(type, pos, state, slots);
        smoke = new FluidTankNTM(NTMFluids.SMOKE, buffer);
        smokeLeaded = new FluidTankNTM(NTMFluids.SMOKE_LEADED, buffer);
        smokePoison = new FluidTankNTM(NTMFluids.SMOKE_POISON, buffer);
        smokeTanks = new FluidTankNTM[] {smoke, smokeLeaded, smokePoison};
    }

    public void pollute(PollutionType type, float amount) {
        FluidTankNTM tank =
                type == PollutionType.SOOT
                        ? smoke
                        : type == PollutionType.HEAVYMETAL ? smokeLeaded : smokePoison;

        int fluidAmount = (int) Math.ceil(amount * 100);
        int accepted = tank.fill(tank.getTankType(), fluidAmount, true);
        int overflow = fluidAmount - accepted;
        if (overflow > 0) {
            PollutionHandler.incrementPollution(level, worldPosition, type, overflow / 100F);

            if (level.getRandom().nextInt(3) == 0)
                level.playSound(
                        null,
                        worldPosition,
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.1F,
                        1.5F);
        }
    }

    public void pollute(Fluid type, FluidTrait.FluidReleaseType release, float amount) {
        FT_Polluting trait = NTMFluidProperties.getTrait(type, FT_Polluting.class);
        if (trait == null) return;
        if (release == FluidTrait.FluidReleaseType.VOID) return;

        Map<PollutionType, Float> map =
                release == FluidTrait.FluidReleaseType.BURN
                        ? trait.getBurnMap()
                        : trait.getReleaseMap();

        for (Map.Entry<PollutionType, Float> entry : map.entrySet()) {
            pollute(entry.getKey(), entry.getValue());
        }
    }

    public FluidTankNTM[] getSmokeTanks() {
        return smokeTanks;
    }

    protected long smokeFluidAvailable(Fluid type) {
        for (FluidTankNTM tank : getSmokeTanks()) {
            if (tank.provides(type)) return tank.getFill();
        }
        return 0;
    }

    protected void useUpSmokeFluid(Fluid type, long amount) {
        for (FluidTankNTM tank : getSmokeTanks()) {
            if (tank.provides(type)) {
                tank.setFill((int) Math.max(0, tank.getFill() - amount));
                return;
            }
        }
    }

    @Override
    protected void loadAdditional(ValueInput input) {
        super.loadAdditional(input);
        input.child("smoke0").ifPresent(smoke::deserialize);
        input.child("smoke1").ifPresent(smokeLeaded::deserialize);
        input.child("smoke2").ifPresent(smokePoison::deserialize);
    }

    @Override
    protected void saveAdditional(ValueOutput output) {
        super.saveAdditional(output);
        smoke.serialize(output.child("smoke0"));
        smokeLeaded.serialize(output.child("smoke1"));
        smokePoison.serialize(output.child("smoke2"));
    }
}
