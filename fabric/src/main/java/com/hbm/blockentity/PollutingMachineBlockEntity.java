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

package com.hbm.blockentity;

import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.tank.FluidTankNTM;
import com.hbm.inventory.fluid.trait.FluidReleaseType;
import com.hbm.inventory.fluid.trait.PollutingTrait;
import com.hbm.sound.WorldSounds;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;

public abstract class PollutingMachineBlockEntity extends MachineBlockEntity {
    public final FluidTankNTM smoke;
    public final FluidTankNTM smokeLeaded;
    public final FluidTankNTM smokePoison;

    private final FluidTankNTM[] exhaustTanks;

    protected PollutingMachineBlockEntity(
            BlockEntityType<?> type,
            BlockPos pos,
            BlockState state,
            int inventorySize,
            int exhaustCapacity) {
        super(type, pos, state, inventorySize);
        smoke = new FluidTankNTM(NTMFluids.SMOKE, exhaustCapacity);
        smokeLeaded = new FluidTankNTM(NTMFluids.SMOKE_LEADED, exhaustCapacity);
        smokePoison = new FluidTankNTM(NTMFluids.SMOKE_POISON, exhaustCapacity);
        exhaustTanks = new FluidTankNTM[] {smoke, smokeLeaded, smokePoison};
    }

    public final FluidTankNTM[] getSmokeTanks() {
        return exhaustTanks;
    }

    @Override
    public void setLevel(Level level) {
        super.setLevel(level);
        if (level instanceof ServerLevel) {
            smoke.bindOwner(this);
            smokeLeaded.bindOwner(this);
            smokePoison.bindOwner(this);
        }
    }

    public void pollute(PollutionType kind, float amount) {
        FluidTankNTM destination;
        Fluid exhaust;
        switch (kind) {
            case SOOT -> {
                destination = smoke;
                exhaust = NTMFluids.SMOKE;
            }
            case HEAVYMETAL -> {
                destination = smokeLeaded;
                exhaust = NTMFluids.SMOKE_LEADED;
            }
            default -> {
                destination = smokePoison;
                exhaust = NTMFluids.SMOKE_POISON;
            }
        }

        int requested = (int) Math.ceil(amount * 100F);
        int accepted = destination.receive(exhaust, requested);
        if (accepted > 0) {
            setChanged();
        }
        int overflow = requested - accepted;
        if (overflow > 0) {
            PollutionHandler.add(level, worldPosition, kind, overflow / 100F);
            if (level.getRandom().nextInt(3) == 0) {
                WorldSounds.machine(
                        level,
                        null,
                        worldPosition.getX(),
                        worldPosition.getY(),
                        worldPosition.getZ(),
                        SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.BLOCKS,
                        0.1F,
                        1.5F);
            }
        }
    }

    public void pollute(Fluid fluid, FluidReleaseType kind, float ignoredQuantity) {
        if (kind == FluidReleaseType.DISCARD) {
            return;
        }
        var emissions = NTMFluidProperties.getTrait(fluid, PollutingTrait.class);
        if (emissions == null) {
            return;
        }
        for (var channel : emissions.of(kind).entrySet()) {
            pollute(channel.getKey(), channel.getValue());
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
