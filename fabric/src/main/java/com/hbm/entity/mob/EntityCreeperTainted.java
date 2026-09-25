// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.bomb.BlockTaint;
import com.hbm.data.ItemData;
import com.hbm.interfaces.IRadiationImmune;
import com.hbm.platform.Services;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

public class EntityCreeperTainted extends EntityCreeperBase implements IRadiationImmune {

    public EntityCreeperTainted(EntityType<? extends EntityCreeperTainted> type, Level level) {
        super(type, level);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return Creeper.createAttributes()
                .add(Attributes.MAX_HEALTH, 15.0D)
                .add(Attributes.MOVEMENT_SPEED, 0.35D);
    }

    @Override
    public void tick() {
        super.tick();

        if (isAlive() && getHealth() < getMaxHealth() && tickCount % 10 == 0) heal(1.0F);
    }

    @Override
    protected void dropCustomDeathLoot(
            ServerLevel level, DamageSource source, boolean killedByPlayer) {
        super.dropCustomDeathLoot(level, source, killedByPlayer);

        int looting = 0;
        if (source.getEntity() instanceof LivingEntity attacker) {
            Holder<Enchantment> lootingEnchant =
                    level.registryAccess()
                            .lookupOrThrow(Registries.ENCHANTMENT)
                            .getOrThrow(Enchantments.LOOTING);
            looting = EnchantmentHelper.getEnchantmentLevel(lootingEnchant, attacker);
        }

        int amount = random.nextInt(3) + (looting > 0 ? random.nextInt(looting + 1) : 0);
        for (int i = 0; i < amount; i++) spawnAtLocation(level, new ItemStack(Items.TNT));
    }

    @Override
    protected void detonate(ServerLevel level) {
        level.explode(this, getX(), getY(), getZ(), 5.0F, Level.ExplosionInteraction.NONE);

        if (!Services.PLATFORM.canEntityGrief(level, this)) return;

        boolean trails = ItemData.TAINT_TRAILS.get();

        if (isPowered()) {
            scatter(level, 255, 15, 7, trails ? 0 : 5, 3);
        } else {
            scatter(level, 85, 7, 3, trails ? 4 : 10, trails ? 3 : 6);
        }
    }

    private void scatter(
            ServerLevel level, int count, int spread, int offset, int ageBase, int ageSpread) {
        for (int i = 0; i < count; i++) {
            BlockPos pos =
                    new BlockPos(
                            random.nextInt(spread) + (int) getX() - offset,
                            random.nextInt(spread) + (int) getY() - offset,
                            random.nextInt(spread) + (int) getZ() - offset);

            BlockState state = level.getBlockState(pos);
            if (!state.isSolidRender() || state.isAir()) continue;

            level.setBlock(
                    pos,
                    ModBlocks.TAINT
                            .get()
                            .defaultBlockState()
                            .setValue(BlockTaint.AGE, random.nextInt(ageSpread) + ageBase),
                    Block.UPDATE_CLIENTS);
        }
    }
}
