// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.blockentity.machine.BlockEntityTesla;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.packet.PacketWire;
import com.hbm.packet.toclient.TeslaArcPayload;
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

public class EntityTeslaCrab extends EntityCyberCrab {
    public List<Vec3> targets = new ArrayList<>();

    public EntityTeslaCrab(EntityType<? extends EntityTeslaCrab> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityCyberCrab.createAttributes()
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
            List<Vec3> zapped = BlockEntityTesla.zap(server, getX(), getY() + 1D, getZ(), 3D, this);
            if (!zapped.equals(targets)) {
                targets = zapped;
                PacketWire.sendToAllAround(
                        new TeslaArcPayload(this, targets),
                        new TargetPoint(server, getX(), getY(), getZ(), 64));
            }
        }

        super.tick();
    }
}
