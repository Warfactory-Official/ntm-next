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

package com.hbm.entity.mob;

import com.hbm.blockentity.machine.TeslaDischarge;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.layout.TeslaArcLayout;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityDimensions;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

public class TeslaCrabEntity extends CyberCrabEntity {
    public List<Vec3> targets = new ArrayList<>();

    public TeslaCrabEntity(EntityType<? extends TeslaCrabEntity> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return CyberCrabEntity.createAttributes()
                .add(Attributes.MAX_HEALTH, 10.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.5D);
    }

    @Override
    protected EntityDimensions getDefaultDimensions(Pose pose) {
        return EntityDimensions.fixed(0.75F, 1.25F);
    }

    @Override
    public void tick() {
        if (level() instanceof ServerLevel server) {
            List<Vec3> zapped =
                    TeslaDischarge.discharge(server, getX(), getY() + 1D, getZ(), 3D, this);
            if (!zapped.equals(targets)) {
                targets = zapped;
                PacketWire.sendToAllAround(
                        TeslaArcLayout.encode(getId(), targets),
                        new TargetPoint(server, getX(), getY(), getZ(), 64));
            }
        }

        super.tick();
    }
}
