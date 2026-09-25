// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.projectile;

import com.hbm.api.entity.IRadarDetectable;
import com.hbm.interfaces.StoredItems;
import com.hbm.items.weapon.ItemAmmoArty;
import com.hbm.sound.ModSounds;
import com.hbm.util.ChunkUtil;
import com.hbm.util.DamageResistanceHandler;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import net.minecraft.world.phys.EntityHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class EntityArtilleryShell extends EntityThrowableInterp
        implements IRadarDetectable, StoredItems {

    @Override
    public boolean hurtServer(ServerLevel level, DamageSource source, float amount) {
        if (amount >= 250F
                && DamageResistanceHandler.CATEGORY_ENERGY.equals(
                        DamageResistanceHandler.typeToCategory(source))) discard();
        return false;
    }

    private static final EntityDataAccessor<Integer> TYPE =
            SynchedEntityData.defineId(EntityArtilleryShell.class, EntityDataSerializers.INT);

    private double targetX;
    private double targetY;
    private double targetZ;
    private boolean shouldWhistle = false;
    private boolean didWhistle = false;

    private ItemStack cargo = ItemStack.EMPTY;

    @Override
    public void visitStoredItems(Visitor visitor) {
        if (visitor.visit(cargo) && cargo.isEmpty()) cargo = ItemStack.EMPTY;
    }

    public EntityArtilleryShell(EntityType<? extends EntityArtilleryShell> type, Level level) {
        super(type, level);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(TYPE, 0);
    }

    @Override
    public boolean shouldRenderAtSqrDistance(double distance) {
        return true;
    }

    public EntityArtilleryShell setType(int type) {
        this.entityData.set(TYPE, type);
        return this;
    }

    public int getShellType() {
        return this.entityData.get(TYPE);
    }

    public ItemAmmoArty.ArtilleryShellType getShell() {
        return ItemAmmoArty.byIndex(getShellType());
    }

    public double[] getTarget() {
        return new double[] {this.targetX, this.targetY, this.targetZ};
    }

    public void setTarget(double x, double y, double z) {
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
    }

    public double getTargetHeight() {
        return this.targetY;
    }

    public void setWhistle(boolean whistle) {
        this.shouldWhistle = whistle;
    }

    public boolean getWhistle() {
        return this.shouldWhistle;
    }

    public boolean didWhistle() {
        return this.didWhistle;
    }

    @Override
    public void tick() {

        super.tick();

        if (this.isRemoved()) return;

        if (!this.level().isClientSide()) {

            if (!this.didWhistle && this.shouldWhistle) {
                Vec3 motion = this.getDeltaMovement();
                double speed = Math.sqrt(motion.x * motion.x + motion.z * motion.z);
                double deltaX = this.getX() - this.targetX;
                double deltaZ = this.getZ() - this.targetZ;
                double dist = Math.sqrt(deltaX * deltaX + deltaZ * deltaZ);

                if (speed * 18 > dist) {
                    this.level()
                            .playSound(
                                    null,
                                    this.targetX,
                                    this.targetY,
                                    this.targetZ,
                                    ModSounds.MORTAR_WHISTLE.get(),
                                    SoundSource.BLOCKS,
                                    15.0F,
                                    0.9F + this.random.nextFloat() * 0.2F);
                    this.didWhistle = true;
                }
            }

            ChunkUtil.holdOwnChunk(this);

            this.getShell().onUpdate(this);
        } else {

            Vec3 backlog = this.getInterpolation().position().subtract(this.position());
            if (backlog.length() < 0.2) {
                this.level()
                        .addParticle(
                                ParticleTypes.SMOKE,
                                this.getX(),
                                this.getY() + 0.5,
                                this.getZ(),
                                0.0,
                                0.1,
                                0.0);
            }
        }
    }

    @Override
    protected void onImpact(HitResult mop) {

        if (!this.level().isClientSide()) {

            if (mop.getType() == HitResult.Type.ENTITY
                    && ((EntityHitResult) mop).getEntity() instanceof EntityArtilleryShell) return;
            this.getShell().onImpact(this, mop);
        }
    }

    @Override
    public RadarTargetType getTargetType() {
        return RadarTargetType.ARTILLERY;
    }

    @Override
    protected float getAirDrag() {
        return 1.0F;
    }

    @Override
    public double getGravityVelocity() {
        return 9.81 * 0.05;
    }

    @Override
    protected int groundDespawn() {
        return !this.cargo.isEmpty() ? 0 : 1200;
    }

    @Override
    public boolean isPickable() {
        return true;
    }

    public void setCargo(ItemStack stack) {
        this.cargo = stack;
    }

    @Override
    public InteractionResult interact(Player player, InteractionHand hand, Vec3 location) {

        if (!this.level().isClientSide()) {
            player.getInventory().placeItemBackInInventory(this.cargo.copy());
            this.discard();
        }

        return InteractionResult.PASS;
    }

    @Override
    protected void addAdditionalSaveData(ValueOutput output) {
        super.addAdditionalSaveData(output);

        output.putInt("type", this.entityData.get(TYPE));
        output.putBoolean("shouldWhistle", this.shouldWhistle);
        output.putBoolean("didWhistle", this.didWhistle);
        output.putDouble("targetX", this.targetX);
        output.putDouble("targetY", this.targetY);
        output.putDouble("targetZ", this.targetZ);

        if (!this.cargo.isEmpty()) output.store("cargo", ItemStack.OPTIONAL_CODEC, this.cargo);
    }

    @Override
    protected void readAdditionalSaveData(ValueInput input) {
        super.readAdditionalSaveData(input);

        this.entityData.set(TYPE, input.getIntOr("type", 0));
        this.shouldWhistle = input.getBooleanOr("shouldWhistle", false);
        this.didWhistle = input.getBooleanOr("didWhistle", false);
        this.targetX = input.getDoubleOr("targetX", 0);
        this.targetY = input.getDoubleOr("targetY", 0);
        this.targetZ = input.getDoubleOr("targetZ", 0);

        this.setCargo(input.read("cargo", ItemStack.OPTIONAL_CODEC).orElse(ItemStack.EMPTY));
    }
}
