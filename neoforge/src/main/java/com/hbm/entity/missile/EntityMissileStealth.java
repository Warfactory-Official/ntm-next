// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.missile;

import com.hbm.entity.ModEntities;
import com.hbm.inventory.material.MaterialShapes;
import com.hbm.inventory.material.Mats;
import com.hbm.items.ModItems;
import com.hbm.items.machine.EnumAshType;
import com.hbm.items.special.Autogen;
import com.hbm.particle.helper.ExplosionCreator;
import java.util.List;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.BlockHitResult;

public class EntityMissileStealth extends EntityMissileBaseNT {

    public EntityMissileStealth(EntityType<? extends EntityMissileStealth> type, Level level) {
        super(type, level);
    }

    public EntityMissileStealth(Level level) {
        super(ModEntities.MISSILE_STEALTH.get(), level);
    }

    @Override
    public void onMissileImpact(BlockHitResult mop) {
        explodeStandard(20F, 24, false);
        ExplosionCreator.composeEffectStandard(level(), getX(), getY(), getZ());
    }

    @Override
    public List<ItemStack> getDebris() {

        return List.of(autogen(MaterialShapes.BOLT, Mats.MAT_STEEL, 4));
    }

    @Override
    public ItemStack getDebrisRareDrop() {

        return ModItems.POWDER_ASH.stack(EnumAshType.MISC);
    }

    @Override
    public ItemStack getMissileItemForInfo() {
        return new ItemStack(ModItems.MISSILE_STEALTH);
    }

    @Override
    public boolean canBeSeenBy(Object radar) {
        return false;
    }
}
