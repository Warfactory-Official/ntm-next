// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.weapon;

import com.hbm.blocks.ModBlocks;
import com.hbm.blocks.generic.BlockSlag;
import com.hbm.entity.projectile.EntityArtilleryRocket;
import com.hbm.explosion.ExplosionChaos;
import com.hbm.explosion.ExplosionLarge;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.explosion.vanillant.ExplosionVNT;
import com.hbm.explosion.vanillant.standard.BlockAllocatorStandard;
import com.hbm.explosion.vanillant.standard.BlockMutatorDebris;
import com.hbm.explosion.vanillant.standard.BlockProcessorStandard;
import com.hbm.explosion.vanillant.standard.EntityProcessorCross;
import com.hbm.explosion.vanillant.standard.PlayerProcessorStandard;
import com.hbm.particle.helper.ExplosionCreator;
import com.hbm.potion.HbmPotion;
import com.hbm.potion.UncurableEffectInstance;
import com.hbm.sound.ModSounds;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.Supplier;
import net.minecraft.ChatFormatting;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;

public class ItemAmmoHIMARS extends Item {

    public final HIMARSRocketType type;

    public ItemAmmoHIMARS(Properties properties, HIMARSRocketType type) {
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
        String desc = "item.hbm.ammo_himars." + type.name().toLowerCase(Locale.US) + ".desc.";
        switch (type) {
            case SMALL -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.BLUE);
            }
            case SMALL_WP -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
                line(adder, desc, 3, ChatFormatting.BLUE);
            }
            case SMALL_HE, SMALL_TB, LARGE, LARGE_TB -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.YELLOW);
                line(adder, desc, 2, ChatFormatting.RED);
            }
            case SMALL_MINI_NUKE, SMALL_LAVA -> {
                line(adder, desc, 0, ChatFormatting.YELLOW);
                line(adder, desc, 1, ChatFormatting.RED);
                line(adder, desc, 2, ChatFormatting.RED);
            }
        }
    }

    private static void line(
            Consumer<Component> adder, String desc, int index, ChatFormatting color) {
        adder.accept(Component.translatable(desc + index).withStyle(color));
    }

    public enum HIMARSRocketType {
        SMALL("standard", "himars_standard", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                standardExplosion(rocket, mop, 20F, 3F, false, SLAG_CRACKED);

                ExplosionCreator.composeEffectStandard(
                        rocket.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        LARGE("single", "himars_single", 1) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                standardExplosion(rocket, mop, 50F, 5F, true, SLAG_CRACKED);
                ExplosionCreator.composeEffectLarge(
                        rocket.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        SMALL_HE("standard_he", "himars_standard_he", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                standardExplosion(rocket, mop, 20F, 3F, true, SLAG_CRACKED);
                ExplosionCreator.composeEffectStandard(
                        rocket.level(),
                        mop.getLocation().x,
                        mop.getLocation().y,
                        mop.getLocation().z);
            }
        },
        SMALL_WP("standard_wp", "himars_standard_wp", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {

                Level level = rocket.level();
                Vec3 hit = mop.getLocation();

                playBoom(rocket);
                standardExplosion(rocket, mop, 20F, 3F, false, SLAG_CRACKED);
                ExplosionLarge.spawnShrapnels(level, hit.x, hit.y, hit.z, 30);
                if (level instanceof ServerLevel server) {
                    ExplosionChaos.igniteAllBlocks(
                            server, (int) hit.x, (int) hit.y, (int) hit.z, 20);
                }

                int radius = 30;
                List<Entity> hits =
                        level.getEntities(
                                rocket,
                                new AABB(
                                        rocket.getX() - radius,
                                        rocket.getY() - radius,
                                        rocket.getZ() - radius,
                                        rocket.getX() + radius,
                                        rocket.getY() + radius,
                                        rocket.getZ() + radius));
                for (Entity e : hits) {
                    e.igniteForSeconds(5F);
                    if (e instanceof LivingEntity living) {
                        living.addEffect(
                                new UncurableEffectInstance(
                                        HbmPotion.phosphorus(), 30 * 20, 0, true, true));
                    }
                }

                ExplosionCreator.composeEffectRBMKMush(level, hit.x, hit.y, hit.z, 15F);
            }
        },
        SMALL_TB("standard_tb", "himars_standard_tb", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                Vec3 hit = mop.getLocation();
                playBoom(rocket);
                standardExplosion(rocket, mop, 20F, 10F, true, SLAG_CRACKED);
                ExplosionLarge.spawnShrapnels(rocket.level(), hit.x, hit.y, hit.z, 30);
                ExplosionCreator.composeEffectRBMKMush(rocket.level(), hit.x, hit.y, hit.z, 20F);
            }
        },
        LARGE_TB("single_tb", "himars_single_tb", 1) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                Vec3 hit = mop.getLocation();
                playBoom(rocket);
                standardExplosion(rocket, mop, 50F, 12F, true, SLAG_CRACKED);
                ExplosionLarge.spawnShrapnels(rocket.level(), hit.x, hit.y, hit.z, 30);
                ExplosionCreator.composeEffectRBMKMush(rocket.level(), hit.x, hit.y, hit.z, 35F);
            }
        },
        SMALL_MINI_NUKE("standard_mini_nuke", "himars_standard_mini_nuke", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                rocket.discard();
                Vec3 impact = behindImpact(rocket, mop);
                ExplosionNukeSmall.explode(
                        rocket.level(),
                        impact.x,
                        impact.y,
                        impact.z,
                        ExplosionNukeSmall.PARAMS_MEDIUM);
            }
        },
        SMALL_LAVA("standard_lava", "himars_standard_lava", 0) {
            @Override
            public void onImpact(EntityArtilleryRocket rocket, HitResult mop) {
                standardExplosion(
                        rocket,
                        mop,
                        20F,
                        3F,
                        true,
                        () -> ModBlocks.VOLCANIC_LAVA_BLOCK.get().defaultBlockState());
            }
        };

        public final String name;

        public final Identifier texture;

        public final int amount;

        public final int modelType;

        HIMARSRocketType(String name, String texture, int type) {
            this.name = name;
            this.texture =
                    Identifier.fromNamespaceAndPath(
                            "hbm", "textures/models/projectiles/" + texture + ".png");
            this.amount = type == 0 ? 6 : 1;
            this.modelType = type;
        }

        public abstract void onImpact(EntityArtilleryRocket rocket, HitResult mop);

        public void onUpdate(EntityArtilleryRocket rocket) {}
    }

    private static final Supplier<BlockState> SLAG_CRACKED =
            () -> ModBlocks.BLOCK_SLAG.get().defaultBlockState().setValue(BlockSlag.CRACKED, true);

    public static HIMARSRocketType byIndex(int index) {
        HIMARSRocketType[] values = HIMARSRocketType.values();
        return values[Math.abs(index) % values.length];
    }

    private static void playBoom(EntityArtilleryRocket rocket) {
        rocket.level()
                .playSound(
                        null,
                        rocket.getX(),
                        rocket.getY(),
                        rocket.getZ(),
                        ModSounds.WEAPON_EXPLOSION_MEDIUM.get(),
                        SoundSource.BLOCKS,
                        20.0F,
                        0.9F + rocket.level().getRandom().nextFloat() * 0.2F);
    }

    private static Vec3 behindImpact(EntityArtilleryRocket rocket, HitResult mop) {
        Vec3 motion = rocket.getDeltaMovement();
        Vec3 vec = motion.lengthSqr() < 1.0e-8 ? Vec3.ZERO : motion.normalize();
        return mop.getLocation().subtract(vec);
    }

    public static void standardExplosion(
            EntityArtilleryRocket rocket,
            HitResult mop,
            float size,
            float rangeMod,
            boolean breaksBlocks,
            Supplier<BlockState> slag) {

        Vec3 impact = behindImpact(rocket, mop);
        ExplosionVNT xnt = new ExplosionVNT(rocket.level(), impact.x, impact.y, impact.z, size);
        if (breaksBlocks) {
            xnt.setBlockAllocator(new BlockAllocatorStandard(48));
            xnt.setBlockProcessor(
                    new BlockProcessorStandard()
                            .setNoDrop()
                            .withBlockEffect(new BlockMutatorDebris(slag)));
        }
        xnt.setEntityProcessor(new EntityProcessorCross(7.5D).withRangeMod(rangeMod));
        xnt.setPlayerProcessor(new PlayerProcessorStandard());
        xnt.explode();
        rocket.discard();
    }
}
