// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.entity.grenade.EntityGrenadeUniversal;
import com.hbm.items.weapon.grenade.ItemGrenadeFilling.EnumGrenadeFilling;
import com.hbm.items.weapon.grenade.ItemGrenadeFuze.EnumGrenadeFuze;
import com.hbm.items.weapon.grenade.ItemGrenadeShell.EnumGrenadeShell;
import com.hbm.items.weapon.grenade.ItemGrenadeUniversal;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;

public class EntityFBIDrone extends EntityUFOBase {

    private int attackCooldown;

    public EntityFBIDrone(EntityType<? extends EntityFBIDrone> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Mob.createMobAttributes()
                .add(Attributes.MAX_HEALTH, 35.0D)
                .add(Attributes.FOLLOW_RANGE, 100.0D);
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        if (this.courseChangeCooldown > 0) this.courseChangeCooldown--;
        if (this.scanCooldown > 0) this.scanCooldown--;
        if (this.attackCooldown > 0) this.attackCooldown--;

        if (this.target != null && this.attackCooldown <= 0) {
            double dx = getX() - this.target.getX();
            double dy = getY() - this.target.getY();
            double dz = getZ() - this.target.getZ();

            if (Math.abs(dx) < 5 && Math.abs(dz) < 5 && dy > 3) {
                this.attackCooldown = 60;

                EntityGrenadeUniversal grenade =
                        new EntityGrenadeUniversal(
                                level,
                                this,
                                ItemGrenadeUniversal.make(
                                        EnumGrenadeShell.FRAG,
                                        EnumGrenadeFilling.HE,
                                        EnumGrenadeFuze.S7));
                grenade.setPos(getX(), getY(), getZ());
                level.addFreshEntity(grenade);
            }
        }

        if (this.courseChangeCooldown > 0) approachPosition(this.target == null ? 0.25D : 0.5D);
    }

    @Override
    protected int getScanRange() {
        return 100;
    }

    @Override
    protected int targetHeightOffset() {
        return 7 + random.nextInt(4);
    }

    @Override
    protected int wanderHeightOffset() {
        return 7 + random.nextInt(4);
    }
}
