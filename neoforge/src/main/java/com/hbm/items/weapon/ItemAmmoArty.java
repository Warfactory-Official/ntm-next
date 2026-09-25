// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSlag;
import com.hbm.config.BombConfig;
import com.hbm.data.ExplosionData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.effect.EntityMist;
import com.hbm.entity.effect.EntityNukeTorex;
import com.hbm.entity.logic.EntityNukeExplosionMK5;
import com.hbm.entity.projectile.EntityArtilleryShell;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.items.ModDataComponents;
import com.hbm.particle.SpentCasing.CasingType;
import com.hbm.particle.SpentCasing;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemAmmoArty extends Item {

    private static final SpentCasing SIXTEEN_INCH_CASE =
            new SpentCasing(CasingType.STRAIGHT)
                    .setScale(15F, 15F, 10F)
                    .setupSmoke(1F, 1D, 200, 60)
                    .setMaxAge(300)
                    .setBounceMotion(1F, 0.5F);

    public final ArtilleryShellType type;

    public ItemAmmoArty(Properties properties, ArtilleryShellType type) {
        super(properties);
        this.type = type;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        String desc = "item.hbm.ammo_arty." + type.name().toLowerCase(Locale.US) + ".desc.";
        switch (type) {
            case NORMAL -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.BLUE);
            }
            case CLASSIC -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.BLUE);
            }
            case EXPLOSIVE -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case PHOSPHORUS -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
                line(adder, desc, 3, ChatFormatting.BLUE);
            }
            case PHOSPHORUS_MULTI -> line(adder, desc, 0, ChatFormatting.RED);
            case MINI_NUKE -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.RED);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case MINI_NUKE_MULTI -> line(adder, desc, 0, ChatFormatting.RED);
            case NUKE -> {
                line(adder, desc, 0, ChatFormatting.RED);
                line(adder, desc, 1, ChatFormatting.RED);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case CARGO -> {
                var cargo = stack.get(ModDataComponents.ARTY_CARGO.get());
                if (cargo != null) {
                    adder.accept(
                            cargo.create().getHoverName().copy().withStyle(ChatFormatting.YELLOW));
                } else {
                    line(adder, desc, 0, ChatFormatting.RED);
                }
            }
            default -> {}
        }
    }

    private static void line(
            Consumer<Component> adder, String desc, int index, ChatFormatting color) {
        adder.accept(Component.translatable(desc + index).withStyle(color));
    }

    public enum ArtilleryShellType {
        NORMAL("ammo_arty", SpentCasing.COLOR_CASE_16INCH) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                standardExplosion(shell, mop, 10F, 3F, false);

                ExplosionCreator.composeEffectSmall(
                        shell.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        CLASSIC("ammo_arty_classic", SpentCasing.COLOR_CASE_16INCH) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                standardExplosion(shell, mop, 15F, 5F, false);
                ExplosionCreator.composeEffectStandard(
                        shell.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        EXPLOSIVE("ammo_arty_he", SpentCasing.COLOR_CASE_16INCH) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                standardExplosion(shell, mop, 15F, 3F, true);
                ExplosionCreator.composeEffectStandard(
                        shell.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },

        MINI_NUKE("ammo_arty_mini_nuke", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                shell.discard();
                Vec3 impact = behindImpact(shell, mop);
                ExplosionNukeSmall.explode(
                        shell.level(),
                        impact.x,
                        impact.y,
                        impact.z,
                        ExplosionNukeSmall.PARAMS_MEDIUM);
            }
        },

        NUKE("ammo_arty_nuke", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                Vec3 hit = mop.getLocation();
                shell.level()
                        .addFreshEntity(
                                EntityNukeExplosionMK5.statFac(
                                        shell.level(),
                                        ExplosionData.MISSILE_RADIUS.get(),
                                        hit.x,
                                        hit.y,
                                        hit.z));
                EntityNukeTorex.statFac(
                        shell.level(), hit.x, hit.y, hit.z, ExplosionData.MISSILE_RADIUS.get());
                shell.discard();
            }
        },

        PHOSPHORUS("ammo_arty_phosphorus", SpentCasing.COLOR_CASE_16INCH_PHOS) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {

                Level level = shell.level();
                Vec3 hit = mop.getLocation();

                level.playSound(
                        null,
                        shell.getX(),
                        shell.getY(),
                        shell.getZ(),
                        ModSounds.WEAPON_EXPLOSION_MEDIUM.get(),
                        SoundSource.BLOCKS,
                        20.0F,
                        0.9F + level.getRandom().nextFloat() * 0.2F);
                standardExplosion(shell, mop, 10F, 3F, false);
                ExplosionLarge.spawnShrapnels(level, hit.x, hit.y, hit.z, 15);
                if (level instanceof ServerLevel server) {
                    ExplosionChaos.igniteAllBlocks(
                            server, (int) hit.x, (int) hit.y, (int) hit.z, 12);
                }

                int radius = 15;
                List<Entity> hits =
                        level.getEntities(
                                shell,
                                new AABB(
                                        shell.getX() - radius,
                                        shell.getY() - radius,
                                        shell.getZ() - radius,
                                        shell.getX() + radius,
                                        shell.getY() + radius,
                                        shell.getZ() + radius));
                for (Entity e : hits) {
                    e.igniteForSeconds(5F);
                    if (e instanceof LivingEntity living) {
                        living.addEffect(
                                new UncurableEffectInstance(
                                        HbmPotion.phosphorus(), 30 * 20, 0, true, true));
                    }
                }

                ExplosionCreator.composeEffectRBMKMush(level, hit.x, hit.y, hit.z, 10F);
            }
        },

        MINI_NUKE_MULTI("ammo_arty_mini_nuke_multi", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                MINI_NUKE.onImpact(shell, mop);
            }

            @Override
            public void onUpdate(EntityArtilleryShell shell) {
                standardCluster(shell, MINI_NUKE, 5, 300, 5);
            }
        },
        PHOSPHORUS_MULTI("ammo_arty_phosphorus_multi", SpentCasing.COLOR_CASE_16INCH_PHOS) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                PHOSPHORUS.onImpact(shell, mop);
            }

            @Override
            public void onUpdate(EntityArtilleryShell shell) {
                standardCluster(shell, PHOSPHORUS, 10, 300, 5);
            }
        },

        CARGO("ammo_arty_cargo", SpentCasing.COLOR_CASE_16INCH) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                if (mop instanceof BlockHitResult block) {
                    shell.setPos(mop.getLocation().x, mop.getLocation().y, mop.getLocation().z);
                    shell.getStuck(block.getBlockPos(), block.getDirection().ordinal());
                }
            }
        },

        CHLORINE("ammo_arty_chlorine", SpentCasing.COLOR_CASE_16INCH) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                shell.discard();
                Vec3 impact = behindImpact(shell, mop);
                shell.level()
                        .explode(
                                shell,
                                impact.x,
                                impact.y,
                                impact.z,
                                5F,
                                Level.ExplosionInteraction.NONE);

                EntityMist mist = new EntityMist(shell.level());
                mist.setType(NTMFluids.CHLORINE);
                mist.setPos(impact.x, impact.y - 3, impact.z);
                mist.setArea(15, 7.5F);
                shell.level().addFreshEntity(mist);

                pollute(shell, mop, PollutionType.HEAVYMETAL, 5F);
            }
        },
        PHOSGENE("ammo_arty_phosgene", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                shell.discard();
                Vec3 impact = behindImpact(shell, mop);
                shell.level()
                        .explode(
                                shell,
                                impact.x,
                                impact.y,
                                impact.z,
                                5F,
                                Level.ExplosionInteraction.NONE);

                gasCloud(shell, impact, NTMFluids.PHOSGENE, 3, 15D, 15, 10);

                pollute(shell, mop, PollutionType.HEAVYMETAL, 10F);
                pollute(shell, mop, PollutionType.POISON, 15F);
            }
        },
        MUSTARD("ammo_arty_mustard_gas", SpentCasing.COLOR_CASE_16INCH_NUKE) {
            @Override
            public void onImpact(EntityArtilleryShell shell, HitResult mop) {
                shell.discard();
                Vec3 impact = behindImpact(shell, mop);
                shell.level()
                        .explode(
                                shell,
                                impact.x,
                                impact.y,
                                impact.z,
                                5F,
                                Level.ExplosionInteraction.NONE);

                gasCloud(shell, impact, NTMFluids.MUSTARDGAS, 5, 25D, 20, 10);

                pollute(shell, mop, PollutionType.HEAVYMETAL, 15F);
                pollute(shell, mop, PollutionType.POISON, 30F);
            }
        };

        public final String name;
        public final SpentCasing casing;

        ArtilleryShellType(String name, int casingColor) {
            this.name = name;
            this.casing = SIXTEEN_INCH_CASE.clone().register(name).setColor(casingColor);
        }

        public abstract void onImpact(EntityArtilleryShell shell, HitResult mop);

        public void onUpdate(EntityArtilleryShell shell) {}
    }

    public static ArtilleryShellType byIndex(int index) {
        ArtilleryShellType[] values = ArtilleryShellType.values();
        return values[Math.abs(index) % values.length];
    }

    private static Vec3 behindImpact(EntityArtilleryShell shell, HitResult mop) {
        Vec3 motion = shell.getDeltaMovement();
        Vec3 vec = motion.lengthSqr() < 1.0e-8 ? Vec3.ZERO : motion.normalize();
        return mop.getLocation().subtract(vec);
    }

    private static void pollute(
            EntityArtilleryShell shell, HitResult mop, PollutionType type, float amount) {
        PollutionHandler.incrementPollution(
                shell.level(), BlockPos.containing(mop.getLocation()), type, amount);
    }

    private static void gasCloud(
            EntityArtilleryShell shell,
            Vec3 impact,
            Fluid fluid,
            int count,
            double spread,
            float width,
            float height) {
        for (int i = 0; i < count; i++) {
            EntityMist mist = new EntityMist(shell.level());
            mist.setType(fluid);
            double x = impact.x;
            double z = impact.z;
            if (i > 0) {
                x += shell.getRandom().nextGaussian() * spread;
                z += shell.getRandom().nextGaussian() * spread;
            }
            mist.setPos(x, impact.y - 5, z);
            mist.setArea(width, height);
            shell.level().addFreshEntity(mist);
        }
    }

    public static void standardExplosion(
            EntityArtilleryShell shell,
            HitResult mop,
            float size,
            float rangeMod,
            boolean breaksBlocks) {

        Vec3 impact = behindImpact(shell, mop);
        ExplosionVNT xnt = new ExplosionVNT(shell.level(), impact.x, impact.y, impact.z, size);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(
                    new BlockProcessorStandard()
                            .setNoDrop()
                            .withBlockEffect(
                                    new BlockMutatorDebris(
                                            () ->
                                                    ModBlocks.BLOCK_SLAG
                                                            .get()
                                                            .defaultBlockState()
                                                            .setValue(BlockSlag.CRACKED, true))));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        shell.discard();
    }

    public static void standardCluster(
            EntityArtilleryShell shell,
            ArtilleryShellType clusterType,
            int amount,
            double splitHeight,
            double deviation) {

        if (!shell.getWhistle() || shell.getDeltaMovement().y > 0) return;
        if (shell.getTargetHeight() + splitHeight < shell.getY()) return;

        shell.discard();

        for (int i = 0; i < amount; i++) {
            EntityArtilleryShell cluster =
                    new EntityArtilleryShell(ModEntities.ARTILLERY_SHELL.get(), shell.level());
            cluster.setType(clusterType.ordinal());
            Vec3 motion = shell.getDeltaMovement();
            cluster.setDeltaMovement(
                    i == 0 ? motion.x : motion.x + shell.getRandom().nextGaussian() * deviation,
                    motion.y,
                    i == 0 ? motion.z : motion.z + shell.getRandom().nextGaussian() * deviation);
            cluster.snapTo(
                    shell.getX(), shell.getY(), shell.getZ(), shell.getYRot(), shell.getXRot());
            double[] target = shell.getTarget();
            cluster.setTarget(target[0], target[1], target[2]);
            cluster.setWhistle(shell.getWhistle() && !shell.didWhistle());
            shell.level().addFreshEntity(cluster);
        }
    }
}
