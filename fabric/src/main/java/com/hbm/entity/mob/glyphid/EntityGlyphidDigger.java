// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: Contributors to Hbm's Nuclear Tech Mod
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.entity.mob.glyphid;

import com.hbm.blocks.multiblock.BlockMultiblockCell;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.entity.mob.glyphid.GlyphidStats.StatBundle;
import com.hbm.entity.projectile.EntityRubble;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.platform.Services;
import com.hbm.registration.IRegistrar;
import com.hbm.registration.Reg;
import com.hbm.util.MobUtil;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.tags.EntityTypeTags;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MobCategory;
import net.minecraft.world.entity.ai.attributes.AttributeSupplier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.Vec3;
import org.jspecify.annotations.Nullable;

public class EntityGlyphidDigger extends EntityGlyphid {

    public static Reg.@Nullable EntityHandle<EntityGlyphidDigger> TYPE;

    public static void register(IRegistrar r) {
        TYPE =
                Reg.entity(
                        "entity_glyphid_digger",
                        () ->
                                EntityType.Builder.<EntityGlyphidDigger>of(
                                                EntityGlyphidDigger::new, MobCategory.MONSTER)
                                        .noLootTable()
                                        .sized(1.75F, 1F)
                                        .clientTrackingRange(5)
                                        .updateInterval(3)
                                        .build(
                                                ResourceKey.create(
                                                        Registries.ENTITY_TYPE,
                                                        Library.id("entity_glyphid_digger"))));
        r.registerLivingAttributes(TYPE, EntityGlyphidDigger::createAttributes);
    }

    public static AttributeSupplier.Builder createAttributes() {
        return EntityGlyphid.createAttributes()
                .add(Attributes.MAX_HEALTH, GlyphidStats.getStats().getDigger().health)
                .add(
                        Attributes.MOVEMENT_SPEED,
                        MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getDigger().speed))
                .add(Attributes.ATTACK_DAMAGE, GlyphidStats.getStats().getDigger().damage);
    }

    protected @Nullable LivingEntity lastTarget;
    protected double lastX;
    protected double lastY;
    protected double lastZ;
    public int timer = 0;

    public EntityGlyphidDigger(EntityType<? extends EntityGlyphidDigger> type, Level level) {
        super(type, level);
    }

    @Override
    public Identifier getSkin() {
        return ResourceManager.glyphid_digger_tex;
    }

    @Override
    public double getGlyphidScale() {
        return 1.3D;
    }

    @Override
    protected void applyEntityAttributes() {
        super.applyEntityAttributes();
        getAttribute(Attributes.MAX_HEALTH)
                .setBaseValue(GlyphidStats.getStats().getDigger().health);
        getAttribute(Attributes.MOVEMENT_SPEED)
                .setBaseValue(MobUtil.oldAiWalkSpeed(GlyphidStats.getStats().getDigger().speed));
        getAttribute(Attributes.ATTACK_DAMAGE)
                .setBaseValue(GlyphidStats.getStats().getDigger().damage);
    }

    @Override
    public StatBundle getStats() {
        return GlyphidStats.getStats().statsDigger;
    }

    @Override
    protected void customServerAiStep(ServerLevel level) {
        super.customServerAiStep(level);

        LivingEntity target = getTarget();
        if (target != null && isAlive()) {

            lastX = target.getX();
            lastY = target.getY();
            lastZ = target.getZ();

            if (--timer <= 0) {
                groundSlam();
                timer = 120;
            }
        }
    }

    public void groundSlam() {
        LivingEntity target = getTarget();
        if (!level().isClientSide() && target != null && distanceTo(target) < 30) {

            boolean topAttack = false;

            int l = 6;
            float part = -1F / 16F;

            int bugX = (int) getX();
            int bugY = (int) getY();
            int bugZ = (int) getZ();

            Vec3 vec0 = getLookAngle();

            List<int[]> list = Library.getBlockPosInPath(bugX, bugY, bugZ, l, vec0);

            for (int i = 0; i < 8; i++) {
                vec0 = vec0.yRot(part);
                list.addAll(Library.getBlockPosInPath(bugX, bugY - 1, bugZ, l, vec0));
            }

            double velX = target.getX() - lastX;
            double velY = target.getY() - lastY;
            double velZ = target.getZ() - lastZ;

            if (lastTarget != target) {
                velX = velY = velZ = 0;
            }

            if (distanceTo(target) > 20) {
                topAttack = true;
            }

            int prediction = 60;
            double dx = target.getX() - getX() + velX * prediction;
            double dy =
                    (target.getY() + target.getBbHeight() / 2) - (getY() + 1) + velY * prediction;
            double dz = target.getZ() - getZ() + velZ * prediction;
            double len = Math.sqrt(dx * dx + dy * dy + dz * dz);
            if (len < 3) return;
            double targetYaw = -Math.atan2(dx, dz);

            double x = Math.sqrt(dx * dx + dz * dz);
            double y = dy;
            double v0 = 1.2;
            double v02 = v0 * v0;
            double g = 0.03D;
            double upperLower = topAttack ? 1 : -1;
            double targetPitch =
                    Math.atan(
                            (v02
                                            + Math.sqrt(v02 * v02 - g * (g * x * x + 2 * y * v02))
                                                    * upperLower)
                                    / (g * x));
            Vec3 fireVec = null;
            if (!Double.isNaN(targetPitch)) {

                fireVec = new Vec3(v0, 0, 0);
                fireVec = fireVec.zRot((float) -targetPitch);
                fireVec = fireVec.yRot((float) -(targetYaw + Math.PI * 0.5));
            }

            for (int[] ints : list) {

                int x1 = ints[0];
                int y1 = ints[1];
                int z1 = ints[2];

                BlockPos pos = new BlockPos(x1, y1, z1);
                BlockState state = level().getBlockState(pos);
                Block b = state.getBlock();
                float k = b.getExplosionResistance();

                if (k < concreteResistance()
                        && state.isSolidRender()
                        && !(b instanceof BlockMultiblockCore)
                        && !(b instanceof BlockMultiblockCell)
                        && level().getBlockEntity(pos) == null
                        && level() instanceof ServerLevel server
                        && Services.PLATFORM.canEntityDestroyBlock(server, pos, state, this)) {

                    EntityRubble rubble = new EntityRubble(level(), x1 + 0.5, y1 + 2, z1 + 0.5);
                    rubble.setBlockState(state);

                    if (fireVec != null) {
                        rubble.shoot(
                                fireVec.x, fireVec.y, fireVec.z, (float) v0, random.nextFloat());
                    }

                    level().addFreshEntity(rubble);

                    level().setBlockAndUpdate(pos, Blocks.AIR.defaultBlockState());
                }
            }
        }
    }

    private float concreteResistance() {
        return BuiltInRegistries.BLOCK
                .getOptional(Library.id("concrete"))
                .map(Block::getExplosionResistance)
                .orElse(140.0F);
    }

    @Override
    public boolean isArmorBroken(float amount) {
        return random.nextInt(100) <= Math.min(Math.pow(amount * 0.25, 2), 100);
    }

    @Override
    protected boolean canDig() {
        return true;
    }
}
