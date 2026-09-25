// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.handler;

import com.hbm.advancement.HbmCriteria;
import com.hbm.config.BombConfig;
import com.hbm.config.RadiationConfig;
import com.hbm.data.ExplosionData;
import com.hbm.data.ItemData;
import com.hbm.data.RadiationData;
import com.hbm.entity.ModEntities;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.explosion.ExplosionNukeSmall;
import com.hbm.extprop.ContaminationEffect;
import com.hbm.extprop.HbmLivingProps;
import com.hbm.handler.pollution.PollutionHandler;
import com.hbm.handler.pollution.PollutionType;
import com.hbm.handler.radiation.RadiationSystemNT;
import com.hbm.hazard.HazardClass;
import com.hbm.items.weapon.sedna.factory.ConfettiUtil;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.toclient.HbmPlayerSyncPayload;
import com.hbm.packet.toclient.VomitPayload;
import com.hbm.particle.helper.FlameCreator;
import com.hbm.particle.helper.ParticleCreators;
import com.hbm.platform.Services;
import com.hbm.potion.HbmPotion;
import com.hbm.sound.ModSounds;
import com.hbm.util.ArmorRegistry;
import com.hbm.util.BiomeUtil;
import com.hbm.util.ContagionUtil;
import com.hbm.util.ContaminationUtil.ContaminationType;
import com.hbm.util.ContaminationUtil.HazardType;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import com.hbm.world.biome.NtmBiomes;
import java.util.Iterator;
import java.util.List;
import java.util.Random;
import java.util.function.Consumer;
import java.util.function.IntConsumer;
import net.minecraft.SharedConstants;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.animal.cow.Cow;
import net.minecraft.world.entity.animal.cow.MushroomCow;
import net.minecraft.world.entity.animal.equine.Horse;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Creeper;
import net.minecraft.world.entity.npc.villager.Villager;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.biome.Biome;
import net.minecraft.world.level.block.Blocks;
import org.jspecify.annotations.Nullable;

public final class EntityEffectHandler {

    public static final int BLACK_FIRE_DAMAGE_PERIOD = 10;

    private static final int MINUTE = 60 * SharedConstants.TICKS_PER_SECOND;
    private static final int HOUR = 60 * MINUTE;
    public static final int CONTAGION_DURATION = 3 * HOUR;

    public static @Nullable IntConsumer CLIENT_RADIATION_FX;

    public static @Nullable Consumer<Player> CLIENT_CRATER_AURA;

    private EntityEffectHandler() {}

    public static void onUpdate(LivingEntity entity) {
        if (entity.level().isClientSide()) {
            if (entity instanceof Player player) handleClientFX(player);
            return;
        }

        if (entity instanceof ServerPlayer player) PlayerShield.tick(player);

        HbmLivingProps props = HbmLivingProps.peek(entity);

        if (props != null && entity.tickCount % SharedConstants.TICKS_PER_SECOND == 0) {
            props.radBuf = props.radEnv;
            props.radEnv = 0D;
            if (entity instanceof ServerPlayer sp)
                Services.NETWORK.sendTo(new HbmPlayerSyncPayload(props), sp);
        }

        if (props != null) {
            handleBombTimer(entity, props);
            handleContamination(entity, props);
        }
        handleCraterBiomeRadiation(entity);
        handleRadiation(entity);
        if (props != null) {
            handleRadiationEffect(entity, props);
            handleRadiationFX(entity, props);
            handleDigamma(entity, props);
            handleLungDisease(entity, props);
            handleOil(entity, props);
        }
        if (TickPhase.every(entity, 60)) handlePollution(entity);
        if (props != null) handleTemperature(entity, props);
        handleContagion(entity, props);
    }

    private static void handleBombTimer(LivingEntity entity, HbmLivingProps props) {
        if (props.bombTimer <= 0) return;
        props.bombTimer--;
        if (props.bombTimer == 0) {
            ExplosionNukeSmall.explode(
                    entity.level(),
                    entity.getX(),
                    entity.getY(),
                    entity.getZ(),
                    ExplosionNukeSmall.PARAMS_MEDIUM);
        }
    }

    private static void handleOil(LivingEntity entity, HbmLivingProps props) {
        if (props.oil <= 0) return;
        if (entity.isOnFire()) {
            props.oil = 0;
            entity.level()
                    .explode(
                            null,
                            entity.getX(),
                            entity.getY() + entity.getBbHeight() / 2,
                            entity.getZ(),
                            3F,
                            false,
                            Level.ExplosionInteraction.BLOCK);
        } else {
            props.oil--;
        }
        if (entity.tickCount % 5 == 0) {
            ParticleCreators.sweat(
                    entity.level(), entity, Blocks.COAL_BLOCK.defaultBlockState(), 1);
        }
    }

    public static void handlePollution(LivingEntity entity) {
        BlockPos eye = BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ());

        if (RadiationData.ENABLE_POISON.get()
                && !ArmorRegistry.hasProtection(
                        entity, EquipmentSlot.HEAD, HazardClass.GAS_BLISTERING)) {
            float poison = PollutionHandler.getPollution(entity.level(), eye, PollutionType.POISON);
            if (poison > 10) {
                if (poison < 25) entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 0));
                else if (poison < 50)
                    entity.addEffect(new MobEffectInstance(MobEffects.POISON, 100, 1));
                else entity.addEffect(new MobEffectInstance(MobEffects.WITHER, 100, 2));
            }
        }

        if (RadiationData.ENABLE_LEAD_POISONING.get()
                && !ArmorRegistry.hasProtection(
                        entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_FINE)) {
            float metal =
                    PollutionHandler.getPollution(entity.level(), eye, PollutionType.HEAVYMETAL);

            if (metal > 25)
                entity.addEffect(new MobEffectInstance(HbmPotion.lead(), 100, metal < 50 ? 0 : 2));
        }
    }

    private static void handleContamination(LivingEntity entity, HbmLivingProps props) {
        List<ContaminationEffect> cont = props.getCont();
        if (cont.isEmpty()) return;
        Iterator<ContaminationEffect> it = cont.iterator();
        while (it.hasNext()) {
            ContaminationEffect con = it.next();
            ContaminationUtil.contaminate(
                    entity,
                    HazardType.RADIATION,
                    con.ignoreArmor ? ContaminationType.RAD_BYPASS : ContaminationType.CREATIVE,
                    con.getRad());
            con.time--;
            if (con.time <= 0) it.remove();
        }
    }

    private static void handleCraterBiomeRadiation(LivingEntity entity) {
        Holder<Biome> biome =
                entity.level() instanceof ServerLevel server
                        ? BiomeUtil.biomeAt(server, entity.blockPosition())
                        : entity.level().getBiome(entity.blockPosition());
        float radiation = craterDose(biome, entity.isInWaterOrRain());
        if (radiation > 0F) {
            ContaminationUtil.contaminate(
                    entity,
                    HazardType.RADIATION,
                    ContaminationType.CREATIVE,
                    radiation / SharedConstants.TICKS_PER_SECOND);
        }
    }

    public static float craterDose(Holder<Biome> biome, boolean wet) {
        float radiation = 0F;
        if (biome.is(NtmBiomes.CRATER_OUTER))
            radiation = ExplosionData.CRATER_BIOME_OUTER_RAD.get().floatValue();
        if (biome.is(NtmBiomes.CRATER))
            radiation = ExplosionData.CRATER_BIOME_RAD.get().floatValue();
        if (biome.is(NtmBiomes.CRATER_INNER))
            radiation = ExplosionData.CRATER_BIOME_INNER_RAD.get().floatValue();
        if (wet) radiation *= ExplosionData.CRATER_BIOME_WATER_MULT.get().floatValue();
        return radiation;
    }

    private static void handleRadiation(LivingEntity entity) {
        if (ContaminationUtil.isRadImmune(entity)) return;
        if (!(entity.level() instanceof ServerLevel server)) return;

        double rad = RadiationSystemNT.doseAt(server, entity.blockPosition());
        if (rad > 0D) {
            ContaminationUtil.contaminate(
                    entity,
                    HazardType.RADIATION,
                    ContaminationType.CREATIVE,
                    rad / SharedConstants.TICKS_PER_SECOND);
        }
    }

    private static void handleRadiationEffect(LivingEntity entity, HbmLivingProps props) {
        if (!RadiationData.ENABLE_CONTAMINATION.get()) return;
        if (entity.isDeadOrDying()) return;
        if (!(entity.level() instanceof ServerLevel world)) return;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return;

        double eRad = props.radiation();
        if (eRad <= 0D) return;
        DamageSource radSource = world.damageSources().source(ModDamageTypes.RADIATION);
        if (entity.isInvulnerableTo(world, radSource)) return;

        if (eRad >= 200D && entity.getClass() == Creeper.class) {
            if (world.getRandom().nextInt(3) != 0) {
                entity.hurtServer(world, radSource, 100F);
                return;
            }
            if (replaceWith(entity, ModEntities.CREEPER_NUCLEAR.get())) return;
        } else if (eRad >= 50D && entity instanceof Cow && !(entity instanceof MushroomCow)) {
            if (replaceWith(entity, EntityTypes.MOOSHROOM)) return;
        } else if (eRad >= 500D && entity instanceof Villager) {
            if (replaceWith(entity, EntityTypes.ZOMBIE_VILLAGER)) return;
        } else if (eRad >= 800D && entity instanceof Horse) {
            if (replaceWith(entity, EntityTypes.ZOMBIE_HORSE)) return;
        } else if (eRad >= 200D && entity.getClass() == EntityDuck.class) {
            if (replaceWith(entity, ModEntities.QUACKOS.get())) return;
        }

        if (eRad < 200D || ContaminationUtil.isRadImmune(entity)) return;

        if (eRad >= 1000D) {
            entity.hurtServer(world, radSource, 1000F);
            HbmLivingProps.setRadiation(entity, 0D);
            if (entity.getHealth() > 0F && !(entity instanceof Player)) entity.setHealth(0F);
            if (entity instanceof ServerPlayer player && entity.getHealth() <= 0F) {
                HbmCriteria.radiation(player, eRad, true);
            }
            return;
        }

        int rng = world.getRandom().nextInt(21000);
        if (eRad >= 800D) {
            if (rng % 300 == 0)
                entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 5 * 30, 0));
            if (rng % 300 == 50)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, 10 * SharedConstants.TICKS_PER_SECOND, 2));
            if (rng % 300 == 100)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.WEAKNESS, 10 * SharedConstants.TICKS_PER_SECOND, 2));
            if (rng % 500 == 0)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.POISON, 3 * SharedConstants.TICKS_PER_SECOND, 2));
            if (rng % 700 == 0)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.WITHER, 3 * SharedConstants.TICKS_PER_SECOND, 1));
        } else if (eRad >= 600D) {
            if (rng % 300 == 0)
                entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 5 * 30, 0));
            if (rng % 300 == 50)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, 10 * SharedConstants.TICKS_PER_SECOND, 2));
            if (rng % 300 == 100)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.WEAKNESS, 10 * SharedConstants.TICKS_PER_SECOND, 2));
            if (rng % 500 == 0)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.POISON, 3 * SharedConstants.TICKS_PER_SECOND, 1));
        } else if (eRad >= 400D) {
            if (rng % 300 == 0)
                entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 5 * 30, 0));
            if (rng % 500 == 50)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.SLOWNESS, 5 * SharedConstants.TICKS_PER_SECOND, 0));
            if (rng % 300 == 100)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.WEAKNESS, 5 * SharedConstants.TICKS_PER_SECOND, 1));
        } else {
            if (rng % 300 == 0)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.NAUSEA, 5 * SharedConstants.TICKS_PER_SECOND, 0));
            if (rng % 500 == 0)
                entity.addEffect(
                        new MobEffectInstance(
                                MobEffects.WEAKNESS, 5 * SharedConstants.TICKS_PER_SECOND, 0));

            if (entity instanceof ServerPlayer player) HbmCriteria.radiation(player, eRad, false);
        }
    }

    private static void handleRadiationFX(LivingEntity entity, HbmLivingProps props) {
        if (ContaminationUtil.isRadImmune(entity)) return;
        if (!(entity.level() instanceof ServerLevel world)) return;
        if (entity instanceof Player p && (p.isCreative() || p.isSpectator())) return;

        double rad = props.radiation();
        if (rad <= 200D) return;

        Random offsets = new Random(entity.getId());
        int r600 = offsets.nextInt(600);
        int r1200 = offsets.nextInt(1200);
        long time = world.getGameTime();

        if (rad > 600D) {
            if ((time + r600) % 600 < 20 && canVomit(entity)) {
                ParticleCreators.vomit(world, entity, VomitPayload.MODE_BLOOD, 25);
                if ((time + r600) % 600 == 1) retch(world, entity);
            }
        } else if ((time + r1200) % 1200 < 20 && canVomit(entity)) {
            ParticleCreators.vomit(world, entity, VomitPayload.MODE_NORMAL, 15);
            if ((time + r1200) % 1200 == 1) retch(world, entity);
        }

        if (rad > 900D && (time + offsets.nextInt(10)) % 10 == 0) {
            ParticleCreators.sweat(world, entity, Blocks.REDSTONE_BLOCK.defaultBlockState(), 1);
        }
    }

    private static void handleDigamma(LivingEntity entity, HbmLivingProps props) {
        if (props.digamma < 0.01D) return;
        int chance = Math.max(10 - (int) props.digamma, 1);
        if (chance == 1 || entity.getRandom().nextInt(chance) == 0) {
            ParticleCreators.sweat(entity.level(), entity, Blocks.SOUL_SAND.defaultBlockState(), 1);
        }
    }

    private static void handleClientFX(Player player) {
        if (player.level().getBiome(player.blockPosition()).is(NtmBiomes.CRATER)
                || player.level().getBiome(player.blockPosition()).is(NtmBiomes.CRATER_INNER)) {
            if (CLIENT_CRATER_AURA != null) CLIENT_CRATER_AURA.accept(player);
        }

        HbmLivingProps props = HbmLivingProps.peek(player);
        if (props == null || CLIENT_RADIATION_FX == null) return;
        double radiation = props.radiation;
        if (radiation > 600D)
            CLIENT_RADIATION_FX.accept(radiation > 900D ? 4 : radiation > 800D ? 2 : 1);
    }

    private static void handleContagion(LivingEntity entity, @Nullable HbmLivingProps props) {
        if (!ItemData.ENABLE_MKU.get()) return;
        if (!(entity.level() instanceof ServerLevel world)) return;
        if (!ItemData.ENABLE_MKU.get()) return;

        RandomSource rand = entity.getRandom();
        int contagion = props != null ? props.contagion : 0;

        if (entity instanceof Player player) {
            ItemStack stack = player.getInventory().getItem(rand.nextInt(Inventory.INVENTORY_SIZE));
            if (rand.nextInt(100) == 0) {
                stack =
                        player.getItemBySlot(
                                ArmorUtil.ARMOR_SLOTS[rand.nextInt(ArmorUtil.ARMOR_SLOTS.length)]);
            }

            if (!stack.isEmpty() && stack.getMaxStackSize() == 1) {
                if (contagion > 0) {
                    ContagionUtil.taint(stack);
                } else if (ContagionUtil.isContagious(stack)
                        && !ContagionUtil.isProtected(player)) {
                    HbmLivingProps.getData(player).contagion = CONTAGION_DURATION;
                }
            }
        }

        if (contagion <= 0) return;
        assert props != null;
        props.contagion = contagion - 1;

        if (contagion < CONTAGION_DURATION - 5 * MINUTE
                && contagion % SharedConstants.TICKS_PER_SECOND == 0) {
            double range = entity.isInWaterOrRain() ? 16D : 2D;
            for (Entity other : world.getEntities(entity, entity.getBoundingBox().inflate(range))) {
                if (other instanceof LivingEntity living) {
                    HbmLivingProps otherProps = HbmLivingProps.getData(living);
                    if (otherProps.contagion <= 0 && !ContagionUtil.isProtected(living)) {
                        otherProps.contagion = CONTAGION_DURATION;
                    }
                }
                if (other instanceof ItemEntity item) ContagionUtil.taint(item.getItem());
            }
        }

        if (contagion < 2 * HOUR && rand.nextInt(1000) == 0) {
            entity.addEffect(
                    new MobEffectInstance(MobEffects.NAUSEA, SharedConstants.TICKS_PER_SECOND, 0));
        }
        if (contagion < HOUR && rand.nextInt(100) == 0) {
            entity.addEffect(
                    new MobEffectInstance(
                            MobEffects.NAUSEA, 3 * SharedConstants.TICKS_PER_SECOND, 0));
            entity.addEffect(
                    new MobEffectInstance(
                            MobEffects.WEAKNESS, 15 * SharedConstants.TICKS_PER_SECOND, 4));
        }

        DamageSource mku = world.damageSources().source(ModDamageTypes.MKU);
        if (contagion < 30 * MINUTE && rand.nextInt(400) == 0) entity.hurtServer(world, mku, 1F);
        if (contagion < 5 * MINUTE && rand.nextInt(100) == 0) entity.hurtServer(world, mku, 2F);

        if (contagion < 30 * MINUTE
                && (contagion + entity.getId()) % 200 < 20
                && canVomit(entity)) {
            ParticleCreators.vomit(world, entity, VomitPayload.MODE_BLOOD, 25);

            if ((contagion + entity.getId()) % 200 == 19) playVomit(world, entity);
        }

        if (props.contagion == 0) entity.hurtServer(world, mku, 1000F);
    }

    private static boolean canVomit(LivingEntity entity) {
        return entity.getType().getCategory() != MobCategory.WATER_CREATURE;
    }

    private static void retch(ServerLevel world, LivingEntity entity) {
        playVomit(world, entity);
        entity.addEffect(new MobEffectInstance(MobEffects.HUNGER, 60, 19));
    }

    private static void playVomit(ServerLevel world, LivingEntity entity) {
        world.playSound(
                null,
                entity.getX(),
                entity.getY(),
                entity.getZ(),
                ModSounds.PLAYER_VOMIT.get(),
                SoundSource.PLAYERS,
                1.0F,
                1.0F);
    }

    private static void handleLungDisease(LivingEntity entity, HbmLivingProps props) {
        if (!(entity.level() instanceof ServerLevel)) return;
        if (entity instanceof Player p && p.isCreative()) {
            HbmLivingProps.setBlackLung(entity, 0);
            HbmLivingProps.setAsbestos(entity, 0);
            return;
        }

        int bl = props.blackLung();
        if (bl > 0 && bl < HbmLivingProps.maxBlacklung * 0.5D) {
            HbmLivingProps.setBlackLung(entity, bl - 1);
        }

        double blacklung = Math.min(props.blackLung(), HbmLivingProps.maxBlacklung);
        double asbestos = Math.min(props.asbestos(), HbmLivingProps.maxAsbestos);
        double soot =
                entity instanceof Player
                                && !ArmorRegistry.hasProtection(
                                        entity, EquipmentSlot.HEAD, HazardClass.PARTICLE_COARSE)
                        ? PollutionHandler.getPollution(
                                entity.level(),
                                BlockPos.containing(entity.getX(), entity.getEyeY(), entity.getZ()),
                                PollutionType.SOOT)
                        : 0D;
        boolean coughs =
                blacklung / HbmLivingProps.maxBlacklung > 0.25D
                        || asbestos / HbmLivingProps.maxAsbestos > 0.25D
                        || soot > 30;
        if (!coughs) return;

        double blacklungDelta = 1D - (blacklung / (double) HbmLivingProps.maxBlacklung);
        double asbestosDelta = 1D - (asbestos / (double) HbmLivingProps.maxAsbestos);
        double sootDelta = 1D - Math.min(soot / 100, 1D);
        double total = 1D - (blacklungDelta * asbestosDelta);

        if (total > 0.75D) entity.addEffect(new MobEffectInstance(MobEffects.WEAKNESS, 100, 2));
        if (total > 0.95D) entity.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 100, 0));

        total = 1D - (blacklungDelta * asbestosDelta * sootDelta);
        int freq = Math.max((int) (1000 - 950 * total), 20);
        if (entity.level().getGameTime() % freq == entity.getId() % freq) {
            entity.level()
                    .playSound(
                            null,
                            entity.getX(),
                            entity.getY(),
                            entity.getZ(),
                            ModSounds.PLAYER_COUGH.get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F);
            double blacklungShare = blacklung / HbmLivingProps.maxBlacklung;
            if (asbestos / HbmLivingProps.maxAsbestos > 0.75D || blacklungShare > 0.75D) {
                ParticleCreators.vomit(entity.level(), entity, VomitPayload.MODE_BLOOD, 5);
            }
            if (blacklungShare > 0.5D) {
                ParticleCreators.vomit(
                        entity.level(),
                        entity,
                        VomitPayload.MODE_SMOKE,
                        blacklungShare > 0.8D ? 50 : 10);
            }
        }
    }

    private static void handleTemperature(LivingEntity entity, HbmLivingProps p) {
        if (!entity.isAlive()) return;
        if (!(entity.level() instanceof ServerLevel server)) return;
        DamageSource fireSrc = entity.damageSources().onFire();
        int phase = entity.tickCount + entity.getId();

        if (entity.fireImmune()) {
            p.fire = 0;
            p.phosphorus = 0;
        }
        if (entity.isInWaterOrRain()) {
            p.fire = 0;
        }

        if (p.fire > 0) {
            p.fire--;
            if (phase % 15 == 0) fizz(server, entity);
            if (phase % 40 == 0) entity.hurtServer(server, fireSrc, 2F);
            flame(server, entity, FlameCreator.META_FIRE);
        }
        if (p.phosphorus > 0) {
            p.phosphorus--;
            if (phase % 15 == 0) fizz(server, entity);
            if (phase % 40 == 0) entity.hurtServer(server, fireSrc, 5F);
            flame(server, entity, FlameCreator.META_FIRE);
        }
        if (p.balefire > 0) {
            p.balefire--;
            if (phase % 15 == 0) fizz(server, entity);
            ContaminationUtil.contaminate(
                    entity, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            if (phase % SharedConstants.TICKS_PER_SECOND == 0)
                entity.hurtServer(server, fireSrc, 5F);
            flame(server, entity, FlameCreator.META_BALEFIRE);
        }
        if (p.blackFire > 0) {
            p.blackFire--;
            if (phase % 10 == 0) fizz(server, entity);
            ContaminationUtil.contaminate(
                    entity, HazardType.RADIATION, ContaminationType.CREATIVE, 5F);
            if (phase % BLACK_FIRE_DAMAGE_PERIOD == 0) entity.hurtServer(server, fireSrc, 10F);
            flame(server, entity, FlameCreator.META_BLACK);
        }

        if (p.fire > 0 || p.phosphorus > 0 || p.balefire > 0 || p.blackFire > 0) {
            if (!entity.isAlive()) ConfettiUtil.decideConfetti(entity, fireSrc);
        }
    }

    private static void fizz(ServerLevel server, LivingEntity entity) {
        float pitch = 1.5F + entity.getRandom().nextFloat() * 0.5F;
        server.playSound(
                null,
                entity.getX(),
                entity.getY() + entity.getBbHeight() / 2D,
                entity.getZ(),
                SoundEvents.FIRE_EXTINGUISH,
                entity.getSoundSource(),
                1F,
                pitch);
    }

    private static void flame(ServerLevel server, LivingEntity entity, int meta) {
        RandomSource rand = entity.getRandom();
        double width = entity.getBbWidth();
        double x = entity.getX() - width / 2D + width * rand.nextDouble();
        double y = entity.getY() + rand.nextDouble() * entity.getBbHeight();
        double z = entity.getZ() - width / 2D + width * rand.nextDouble();
        FlameCreator.composeEffect(server, x, y, z, meta);
    }

    private static boolean replaceWith(LivingEntity src, EntityType<?> type) {
        if (!(src.level() instanceof ServerLevel server)) return false;
        Entity replacement = type.create(server, EntitySpawnReason.CONVERSION);
        if (replacement == null) return false;
        replacement.snapTo(src.getX(), src.getY(), src.getZ(), src.getYRot(), src.getXRot());
        if (server.addFreshEntity(replacement)) {
            src.discard();
            return true;
        }
        return false;
    }
}
