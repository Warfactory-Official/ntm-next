// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.armor;

import com.hbm.extprop.HbmLivingProps;
import com.hbm.extprop.HbmPlayerProps;
import com.hbm.handler.ArmorModHandler;
import com.hbm.handler.ArmorUtil;
import com.hbm.handler.threading.TargetPoint;
import com.hbm.items.ModDataComponents;
import com.hbm.items.ModItems;
import com.hbm.items.tool.ItemGeigerCounter;
import com.hbm.lib.Library;
import com.hbm.lib.ModDamageTypes;
import com.hbm.packet.toclient.JetpackParticlePayload;
import com.hbm.platform.Services;
import com.hbm.sound.ModSounds;
import com.hbm.util.ContaminationUtil;
import com.hbm.util.TickPhase;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.SharedConstants;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier.Operation;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

public final class ArmorSuitEffects {

    private static final double HARD_LANDING_RANGE = 3D;

    private static final float HARD_LANDING_THRESHOLD = 10F;

    private static final double STEP_STRIDE = 1D / 0.6D;

    private static final Map<ServerPlayer, GaitState> GAIT = new WeakHashMap<>();

    private ArmorSuitEffects() {}

    public static void init() {
        Services.SERVER.onServerTickPost(ArmorSuitEffects::tick);
    }

    private static void tick(MinecraftServer server) {
        for (ServerPlayer player : server.getPlayerList().getPlayers()) {
            HbmPlayerProps props = HbmPlayerProps.getData(player);
            if (props.plinkCooldown > 0) props.plinkCooldown--;
            if (wearsAny(player, ModArmorItem.Suit.ASBESTOS)) player.clearFire();
            ItemStack no9 = player.getItemBySlot(EquipmentSlot.HEAD);
            if (no9.getItem() == ModItems.NO9.get()) {
                lamp(player, no9);
                int blackLung = HbmLivingProps.getBlackLung(player);
                if (blackLung > HbmLivingProps.maxBlacklung * .9D)
                    HbmLivingProps.setBlackLung(player, (int) (HbmLivingProps.maxBlacklung * .9D));
                if (HbmLivingProps.getBlackLung(player) >= HbmLivingProps.maxBlacklung * .25D)
                    HbmLivingProps.setBlackLung(player, HbmLivingProps.getBlackLung(player) - 1);
            }

            ArmorFullSetBonus bonus = activeBonus(player);
            if (bonus != null) {
                applyEffects(player, bonus);
                if (bonus.drain() > 0 && !player.isCreative()) drain(player, bonus.drain());
            }
            applyStepHeight(player, bonus == null ? 0 : bonus.stepSize());

            handleGait(player, bonus);

            if (hasFullSet(player, ModArmorItem.Suit.DNS))
                applySprintSpeed(player, DNS_SPEED_ID, 0.25D);
            else removeSprintSpeed(player, DNS_SPEED_ID);
            Jetpack jetpack = jetpackKind(player);
            if (jetpack != null) jetpackServerTick(player, jetpack);
            if (wingsKind(player) != null) wingsServerTick(player);

            bjBrownout(player);

            ArmorDash.tick(player, 0F, 0F);

            if (hasEnvsuitSwim(player)) {
                applySprintSpeed(player, ENVSUIT_SPEED_ID, 0.1D);
                envsuitServerTick(player);
            } else {
                removeSprintSpeed(player, ENVSUIT_SPEED_ID);
            }
        }
    }

    private static void applyEffects(ServerPlayer player, ArmorFullSetBonus bonus) {
        for (ArmorFullSetBonus.EffectBonus effect : bonus.effects()) {
            player.addEffect(
                    new MobEffectInstance(
                            effect.effect(),
                            SharedConstants.TICKS_PER_SECOND + 1,
                            effect.amplifier(),
                            true,
                            false));
        }
    }

    private static void handleGait(ServerPlayer player, @Nullable ArmorFullSetBonus bonus) {
        GaitState state = GAIT.computeIfAbsent(player, p -> new GaitState());
        boolean onGround = player.onGround();
        SoundEvent step = bonus == null ? null : bonus.step();

        if (bonus != null) {
            if (state.onGround && !onGround && player.getDeltaMovement().y > 0.1D) {
                playAt(player, bonus.jump(), 0.5F);
            }

            if (!state.onGround && onGround) {

                if (state.fallDistance > 0.5D) playAt(player, bonus.fall(), 0.5F);
                if (bonus.hardLanding() && state.fallDistance > HARD_LANDING_THRESHOLD)
                    hardLanding(player);
            }

            if (bonus.geigerSound()) geigerClick(player);
        }

        if (onGround && step != null) {
            double dx = player.getX() - state.prevX;
            double dz = player.getZ() - state.prevZ;
            state.stepAccum += Math.sqrt(dx * dx + dz * dz);
            if (state.stepAccum > STEP_STRIDE) {
                state.stepAccum = 0D;
                playAt(player, step, 0.25F);
            }
        } else {
            state.stepAccum = 0D;
        }

        state.onGround = onGround;
        state.fallDistance = player.fallDistance;
        state.prevX = player.getX();
        state.prevZ = player.getZ();
    }

    private static void playAt(ServerPlayer player, @Nullable SoundEvent sound, float volume) {
        if (sound == null) return;
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        sound,
                        SoundSource.PLAYERS,
                        volume,
                        1.0F);
    }

    private static void geigerClick(ServerPlayer player) {
        if (!TickPhase.every(player, 5)) return;
        if (player.getInventory().contains(s -> s.is(ModItems.GEIGER_COUNTER.get()))) return;

        double rads = ContaminationUtil.getNoNeutronPlayerRads(player);
        int idx = ItemGeigerCounter.pickGeiger(rads, player.level().getRandom());
        if (idx > 0) {
            player.level()
                    .playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.GEIGER[idx - 1].get(),
                            SoundSource.PLAYERS,
                            1.0F,
                            1.0F);
        }
    }

    private static void hardLanding(ServerPlayer player) {
        ServerLevel level = player.level();
        DamageSource source = level.damageSources().source(ModDamageTypes.SHOCKWAVE, player);

        for (Entity e :
                level.getEntities(
                        player,
                        player.getBoundingBox().inflate(HARD_LANDING_RANGE, 0D, HARD_LANDING_RANGE),
                        e -> !(e instanceof ItemEntity))) {
            double dx = player.getX() - e.getX();
            double dz = player.getZ() - e.getZ();
            double dist = Math.sqrt(dx * dx + dz * dz);
            if (dist >= HARD_LANDING_RANGE) continue;

            double intensity = HARD_LANDING_RANGE - dist;
            e.setDeltaMovement(
                    e.getDeltaMovement()
                            .add(dx * intensity * -2D, 0.1D * intensity, dz * intensity * -2D));
            e.hurtServer(level, source, (float) (intensity * 10D));
        }
    }

    @Nullable
    private static ArmorFullSetBonus activeBonus(ServerPlayer player) {
        if (!(player.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate))
            return null;
        return hasFullSet(player, plate.suit()) ? ArmorFullSetBonus.get(plate.suit()) : null;
    }

    private static void lamp(ServerPlayer player, ItemStack helmet) {
        boolean on = HbmPlayerProps.getData(player).enableHUD;
        if (on == helmet.getOrDefault(ModDataComponents.NO9_LAMP.get(), false)) return;
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        on ? SoundEvents.FLINTANDSTEEL_USE : SoundEvents.FIRE_EXTINGUISH,
                        SoundSource.PLAYERS,
                        on ? 1F : .5F,
                        on ? 1.5F : 2F);
        helmet.set(ModDataComponents.NO9_LAMP.get(), on);
    }

    public static boolean hasFullSet(LivingEntity entity, ModArmorItem.Suit suit) {
        return hasFullSet(entity, suit, true);
    }

    public static boolean hasFullSet(
            LivingEntity entity, ModArmorItem.Suit suit, boolean requireCharge) {
        if (!(entity.getItemBySlot(EquipmentSlot.CHEST).getItem() instanceof ModArmorItem plate)
                || plate.suit() != suit) return false;

        ArmorFullSetBonus bonus = ArmorFullSetBonus.get(suit);
        boolean noHelmet = bonus != null && bonus.noHelmet();
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            if (noHelmet && slot == EquipmentSlot.HEAD) continue;
            ItemStack stack = entity.getItemBySlot(slot);
            if (!(stack.getItem() instanceof ModArmorItem item)
                    || !item.suit().sharesMaterial(suit)) return false;
            if (requireCharge && !item.isEnabled(stack)) return false;
        }
        return true;
    }

    public static boolean wearsAny(LivingEntity entity, ModArmorItem.Suit suit) {
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            if (entity.getItemBySlot(slot).getItem() instanceof ModArmorItem item
                    && item.suit() == suit) return true;
        }
        return false;
    }

    private static void drain(ServerPlayer player, long amount) {
        boolean fuelTick = TickPhase.every(player, 10);
        for (EquipmentSlot slot : ArmorUtil.ARMOR_SLOTS) {
            ItemStack stack = player.getItemBySlot(slot);
            if (stack.getItem() instanceof ModArmorItemFueled fueled) {
                if (fuelTick) fueled.setFill(stack, fueled.getFill(stack) - (int) amount);
            } else if (stack.getItem() instanceof ModArmorItem armor) {
                armor.setCharge(stack, armor.getCharge(stack) - amount);
            }
        }
    }

    private static final Identifier DNS_SPEED_ID = Library.id("dns_speed");

    private static final Identifier ENVSUIT_SPEED_ID = Library.id("envsuit_speed");
    private static final Identifier STEP_HEIGHT_ID = Library.id("suit_step_height");

    private static void applySprintSpeed(ServerPlayer player, Identifier id, double amount) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed == null) return;
        boolean present = speed.getModifier(id) != null;
        if (player.isSprinting()) {
            if (!present)
                speed.addTransientModifier(new AttributeModifier(id, amount, Operation.ADD_VALUE));
        } else if (present) {
            speed.removeModifier(id);
        }
    }

    private static void removeSprintSpeed(ServerPlayer player, Identifier id) {
        AttributeInstance speed = player.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null && speed.getModifier(id) != null) speed.removeModifier(id);
    }

    private static void applyStepHeight(ServerPlayer player, int stepSize) {
        AttributeInstance step = player.getAttribute(Attributes.STEP_HEIGHT);
        if (step == null) return;
        AttributeModifier present = step.getModifier(STEP_HEIGHT_ID);
        if (stepSize <= 0) {
            if (present != null) step.removeModifier(STEP_HEIGHT_ID);
            return;
        }
        double amount = stepSize - step.getBaseValue();
        if (present != null && present.amount() == amount) return;
        if (present != null) step.removeModifier(STEP_HEIGHT_ID);
        step.addTransientModifier(
                new AttributeModifier(STEP_HEIGHT_ID, amount, Operation.ADD_VALUE));
    }

    public static @Nullable Jetpack jetpackKind(Player player) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest.is(ModItems.DNS_PLATE.get()) && hasFullSet(player, ModArmorItem.Suit.DNS))
            return Jetpack.DNS;
        if (chest.is(ModItems.BJ_PLATE_JETPACK.get()) && hasFullSet(player, ModArmorItem.Suit.BJ))
            return Jetpack.BJ;
        ItemStack pack = wornPack(player);
        return pack.getItem() instanceof ItemJetpack jetpack && jetpack.getFuel(pack) > 0
                ? jetpack.kind
                : null;
    }

    public static ItemStack wornPack(Player player) {
        return wornPlateMod(player, ItemJetpack.class);
    }

    public static ItemWings.@Nullable Kind wingsKind(Player player) {
        return wornPlateMod(player, ItemWings.class).getItem() instanceof ItemWings wings
                ? wings.kind
                : null;
    }

    private static ItemStack wornPlateMod(Player player, Class<? extends ItemArmorMod> type) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (type.isInstance(chest.getItem())) return chest;
        ItemStack mod = ArmorModHandler.pryMod(chest, ArmorModHandler.PLATE_ONLY);
        return type.isInstance(mod.getItem()) ? mod : ItemStack.EMPTY;
    }

    public static void storeWornPack(Player player, ItemStack pack) {
        ItemStack chest = player.getItemBySlot(EquipmentSlot.CHEST);
        if (chest != pack) ArmorModHandler.applyMod(chest, pack);
    }

    public enum Jetpack {
        DNS(0.6D, 0.2D, 0D, 0D, 0, 0, 0F),
        BJ(0.4D, 0.1D, 0D, 0D, 0, 0, 0F),

        FLY(0.4D, 0.1D, 0D, 0D, 5, 5, 1.5F),
        VECTOR(0.4D, 0.1D, 0.1D, 2D, 3, 3, 1.5F),
        BOOST(0.6D, 0.1D, 0.25D, 5D, 1, 1, 1.0F),

        BRAKE(0.4D, 0.1D, 0D, 0D, 5, 10, 1.5F);

        public final double ceiling;
        public final double thrust;

        public final double lookThrust;
        public final double speedCap;

        public final int burnRate;
        public final int hoverBurnRate;
        public final float pitch;

        Jetpack(
                double ceiling,
                double thrust,
                double lookThrust,
                double speedCap,
                int burnRate,
                int hoverBurnRate,
                float pitch) {
            this.ceiling = ceiling;
            this.thrust = thrust;
            this.lookThrust = lookThrust;
            this.speedCap = speedCap;
            this.burnRate = burnRate;
            this.hoverBurnRate = hoverBurnRate;
            this.pitch = pitch;
        }

        public boolean isMod() {
            return burnRate > 0;
        }
    }

    public static boolean hasEnvsuitSwim(Player player) {
        return player.getItemBySlot(EquipmentSlot.CHEST).is(ModItems.ENVSUIT_PLATE.get())
                && hasFullSet(player, ModArmorItem.Suit.ENVSUIT);
    }

    public static boolean heldAloft(ServerPlayer player) {
        Jetpack kind = jetpackKind(player);
        if (kind == Jetpack.DNS || kind == Jetpack.BJ || kind == Jetpack.BRAKE) return true;
        if (kind != null && thrusting(player, kind)) return true;
        return wingsKind(player) != null && !player.onGround();
    }

    private static boolean thrusting(ServerPlayer player, Jetpack kind) {
        return HbmPlayerProps.getData(player).isJetpackActive()
                && !(kind == Jetpack.BRAKE && player.isShiftKeyDown());
    }

    private static void jetpackServerTick(ServerPlayer player, Jetpack kind) {
        HbmPlayerProps props = HbmPlayerProps.getData(player);
        boolean thrusting = thrusting(player, kind);
        boolean gliding =
                (kind == Jetpack.DNS || kind == Jetpack.BRAKE)
                        && !thrusting
                        && (!player.isShiftKeyDown()
                                || kind == Jetpack.BRAKE && props.isJetpackActive())
                        && !player.onGround()
                        && props.enableBackpack;

        if (thrusting || gliding) {
            int plume =
                    switch (kind) {
                        case DNS -> JetpackParticlePayload.DNS;
                        case BJ -> JetpackParticlePayload.BJ;
                        case FLY, BRAKE -> JetpackParticlePayload.REGULAR;
                        case VECTOR, BOOST -> JetpackParticlePayload.VECTOR;
                    };
            Services.NETWORK.sendToAllAround(
                    new JetpackParticlePayload(player.getId(), plume),
                    new TargetPoint(
                            player.level(), player.getX(), player.getY(), player.getZ(), 100D));
        }

        if (!thrusting && !gliding) return;

        if (kind.lookThrust == 0D
                || player.getLookAngle().y > 0D
                        && player.getDeltaMovement().length() < kind.speedCap)
            player.fallDistance = 0;
        if (kind.isMod()) {
            ItemStack pack = wornPack(player);
            if (pack.getItem() instanceof ItemJetpack jetpack) {
                jetpack.burn(
                        pack, player.tickCount, thrusting ? kind.burnRate : kind.hoverBurnRate);
                storeWornPack(player, pack);
            }

            player.level()
                    .playSound(
                            null,
                            player.getX(),
                            player.getY(),
                            player.getZ(),
                            ModSounds.FLAMETHROWER_SHOOT.get(),
                            SoundSource.PLAYERS,
                            0.25F,
                            kind.pitch);
            return;
        }
        player.level()
                .playSound(
                        null,
                        player.getX(),
                        player.getY(),
                        player.getZ(),
                        ModSounds.JETPACK_THRUST.get(),
                        SoundSource.PLAYERS,
                        0.125F,
                        1.5F);
    }

    private static void wingsServerTick(ServerPlayer player) {
        if (!player.onGround()) player.fallDistance = 0;
    }

    private static void bjBrownout(ServerPlayer player) {
        if (!hasFullSet(player, ModArmorItem.Suit.BJ, false)) return;
        if (hasFullSet(player, ModArmorItem.Suit.BJ)) return;

        ItemStack helmet = player.getItemBySlot(EquipmentSlot.HEAD);
        player.setItemSlot(EquipmentSlot.HEAD, ItemStack.EMPTY);
        player.getInventory().placeItemBackInInventory(helmet);

        player.hurtServer(
                player.level(), player.level().damageSources().source(ModDamageTypes.LUNAR), 1000F);
    }

    private static void envsuitServerTick(ServerPlayer player) {
        if (player.isInWater()) {
            player.setAirSupply(300);
            player.addEffect(
                    new MobEffectInstance(
                            MobEffects.NIGHT_VISION,
                            15 * SharedConstants.TICKS_PER_SECOND,
                            0,
                            true,
                            false));
        } else {
            player.removeEffect(MobEffects.NIGHT_VISION);
        }
    }

    private static final class GaitState {
        boolean onGround;
        double fallDistance;
        double prevX;
        double prevZ;
        double stepAccum;
    }
}
