/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.client.render.ParasiteMaggotRenderState;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.DeliveryDroneEntity;
import com.hbm.entity.item.DroneBaseEntity;
import com.hbm.entity.item.RequestDroneEntity;
import com.hbm.entity.mob.CreeperBaseEntity;
import com.hbm.entity.mob.CyberCrabEntity;
import com.hbm.entity.mob.DuckEntity;
import com.hbm.entity.mob.FBIDroneEntity;
import com.hbm.entity.mob.MaskManEntity;
import com.hbm.entity.mob.ParasiteMaggotEntity;
import com.hbm.entity.mob.PigeonEntity;
import com.hbm.entity.mob.PlasticBagEntity;
import com.hbm.entity.mob.QuackosEntity;
import com.hbm.entity.mob.RADBeastEntity;
import com.hbm.entity.mob.TaintCrabEntity;
import com.hbm.entity.mob.TeslaCrabEntity;
import com.hbm.entity.mob.UFOEntity;
import com.hbm.entity.mob.botprime.BOTPrimeBodyEntity;
import com.hbm.entity.mob.botprime.BOTPrimeHeadEntity;
import com.hbm.entity.mob.botprime.WormBaseNTEntity;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.RegistryUtil;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.part.ModelTrees;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import java.util.function.Predicate;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.animal.chicken.AdultChickenModel;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.model.monster.blaze.BlazeModel;
import net.minecraft.client.model.monster.creeper.CreeperModel;
import net.minecraft.client.model.monster.silverfish.SilverfishModel;
import net.minecraft.client.renderer.entity.state.ChickenRenderState;
import net.minecraft.client.renderer.entity.state.CreeperRenderState;
import net.minecraft.client.renderer.entity.state.LivingEntityRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

public final class MobVisuals {
    private MobVisuals() {}

    private static <T extends Entity> Predicate<T> always() {
        return entity -> true;
    }

    private static <T extends CreeperBaseEntity> Predicate<T> uncharged() {
        return entity -> !entity.isPowered();
    }

    private static <V> Supplier<V> lazy(Supplier<V> source) {
        return new Supplier<>() {
            private volatile @Nullable V value;

            @Override
            public V get() {
                V local = value;
                if (local == null) {
                    synchronized (this) {
                        local = value;
                        if (local == null) value = local = source.get();
                    }
                }
                return local;
            }
        };
    }

    private static Supplier<Material> cutout(Supplier<Identifier> texture) {
        return cutout(texture, false);
    }

    private static Supplier<Material> cutout(Supplier<Identifier> texture, boolean cull) {
        return lazy(
                () ->
                        SimpleMaterial.builderOf(Materials.CUTOUT)
                                .texture(texture.get())
                                .mipmap(false)
                                .cutout(CutoutShaders.ONE_TENTH)
                                .cardinalLightingMode(CardinalLightingMode.ENTITY)
                                .backfaceCulling(cull)
                                .build());
    }

    private static Supplier<ModelPart> bake(ModelLayerLocation layer) {
        return () -> Minecraft.getInstance().getEntityModels().bakeLayer(layer);
    }

    private static <T extends Entity> void obj(
            SimpleEntityVisualizer.Builder<T> builder,
            Supplier<MobObjVisual.Spec<T>> spec,
            Predicate<T> instanced) {
        Predicate<T> drawn = unvanished(instanced);
        var binding = new ObjBinding<T>();
        FlywheelResources.onInitialize(
                () -> binding.prepared = MobObjVisual.prepare(spec.get().instanced(drawn)));
        builder.factory(
                        (ctx, entity, partialTick) ->
                                new MobObjVisual<>(ctx, entity, partialTick, binding.prepared))
                .skipVanillaRender(drawn)
                .apply();
    }

    private static <T extends LivingEntity, S extends LivingEntityRenderState> void model(
            SimpleEntityVisualizer.Builder<T> builder,
            Supplier<MobModelVisual.Spec<T, S>> spec,
            Predicate<T> instanced) {
        Predicate<T> drawn = unvanished(instanced);
        Supplier<MobModelVisual.Spec<T, S>> lazy = lazy(() -> spec.get().instanced(drawn));
        builder.factory(
                        (ctx, entity, partialTick) ->
                                new MobModelVisual<>(ctx, entity, partialTick, lazy.get()))
                .skipVanillaRender(drawn)
                .apply();
    }

    static <T extends Entity> Predicate<T> unvanished(Predicate<T> instanced) {
        return entity -> instanced.test(entity) && !VanishedEntities.isVanished(entity);
    }

    private static double firstDouble(long seed) {
        long mask = (1L << 48) - 1;
        long state = (seed ^ 0x5DEECE66DL) & mask;
        state = (state * 0x5DEECE66DL + 0xBL) & mask;
        long high = state >>> (48 - 26);
        state = (state * 0x5DEECE66DL + 0xBL) & mask;
        long low = state >>> (48 - 27);
        return ((high << 27) + low) * 0x1.0p-53;
    }

    private static float legSwing(Mob entity, float partialTick) {
        return -(Mth.cos(entity.walkAnimation.position(partialTick) * 0.6662F * 2F) * 0.4F)
                * entity.walkAnimation.speed(partialTick)
                * 57.3F;
    }

    private static <T extends DroneBaseEntity> MobObjVisual.Spec<T> drone(
            Supplier<Identifier> texture) {
        return MobObjVisual.Spec.of(
                () -> ResourceManager.delivery_drone,
                cutout(texture),
                MobObjVisual.Part.of("Drone"),
                MobObjVisual.Part.when("Crate", entity -> entity.getAppearance() == 1),
                MobObjVisual.Part.when("Barrel", entity -> entity.getAppearance() == 2));
    }

    private static MobObjVisual.Spec<DeliveryDroneEntity> deliveryDrone() {
        Supplier<Material> express = cutout(() -> ResourceManager.delivery_drone_express_tex);
        return MobObjVisual.Spec.of(
                () -> ResourceManager.delivery_drone,
                cutout(() -> ResourceManager.delivery_drone_tex),
                MobObjVisual.Part.when("Drone", entity -> !entity.isExpress()),
                MobObjVisual.Part.when(
                        "Crate", entity -> !entity.isExpress() && entity.getAppearance() == 1),
                MobObjVisual.Part.when(
                        "Barrel", entity -> !entity.isExpress() && entity.getAppearance() == 2),
                MobObjVisual.Part.<DeliveryDroneEntity>when("Drone", DeliveryDroneEntity::isExpress)
                        .material(express),
                MobObjVisual.Part.<DeliveryDroneEntity>when(
                                "Crate",
                                entity -> entity.isExpress() && entity.getAppearance() == 1)
                        .material(express),
                MobObjVisual.Part.<DeliveryDroneEntity>when(
                                "Barrel",
                                entity -> entity.isExpress() && entity.getAppearance() == 2)
                        .material(express));
    }

    private static <T extends Mob> MobObjVisual.Spec<T> crab(
            Supplier<HFRWavefrontObject> mesh,
            Supplier<Identifier> texture,
            String left,
            String right,
            float baseYaw) {
        return MobObjVisual.Spec.<T>of(
                        mesh,
                        cutout(texture),
                        MobObjVisual.Part.of("Body"),
                        MobObjVisual.Part.at(
                                left,
                                (pose, entity, partialTick) ->
                                        pose.rotateY(
                                                legSwing(entity, partialTick) * Mth.DEG_TO_RAD)),
                        MobObjVisual.Part.at(
                                right,
                                (pose, entity, partialTick) ->
                                        pose.rotateY(
                                                -legSwing(entity, partialTick) * Mth.DEG_TO_RAD)))
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.rotateY(
                                    (180F
                                                    - Mth.rotLerp(
                                                            partialTick,
                                                            entity.yBodyRotO,
                                                            entity.yBodyRot))
                                            * Mth.DEG_TO_RAD);
                            pose.rotateY(baseYaw * Mth.DEG_TO_RAD);
                        });
    }

    private static <T extends WormBaseNTEntity> MobObjVisual.Spec<T> worm(
            Supplier<HFRWavefrontObject> mesh, Supplier<Identifier> texture) {
        return MobObjVisual.Spec.<T>of(mesh, cutout(texture), MobObjVisual.Part.of("Cylinder"))
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.rotateY(
                                    (Mth.rotLerp(partialTick, entity.yRotO, entity.getYRot()) - 90F)
                                            * Mth.DEG_TO_RAD);
                            pose.rotateZ(
                                    (Mth.lerp(partialTick, entity.xRotO, entity.getXRot()) - 90F)
                                            * Mth.DEG_TO_RAD);
                        });
    }

    private static MobObjVisual.Spec<UFOEntity> ufo() {
        return MobObjVisual.Spec.<UFOEntity>of(
                        () -> ResourceManager.ufo,
                        cutout(() -> ResourceManager.ufo_tex, true),
                        MobObjVisual.Part.of("Circle_Circle.001"))
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.translate(0F, 1F, 0F);
                            pose.rotateY(
                                    (float) ((entity.tickCount + partialTick) * 5D % 360D)
                                            * Mth.DEG_TO_RAD);
                            pose.scale(2F);
                        });
    }

    private static MobObjVisual.Spec<MaskManEntity> maskMan() {
        return MobObjVisual.Spec.<MaskManEntity>of(
                        () -> ResourceManager.maskman,
                        cutout(() -> ResourceManager.maskman_tex),
                        MobObjVisual.Part.of("Torso"),
                        MobObjVisual.Part.at("LLeg", limb(-0.5F, 1.75F, -0.5F, 1F)),
                        MobObjVisual.Part.at("RLeg", limb(-0.5F, 1.75F, 0.5F, -1F)),
                        MobObjVisual.Part.at("LArm", limb(-0.5F, 3.75F, -1.5F, 0.25F)),
                        MobObjVisual.Part.at("RArm", limb(-0.5F, 3.75F, 1.5F, -0.25F)),
                        MobObjVisual.Part.at("Head", maskManHead())
                                .visible(entity -> !maskManWounded(entity)),
                        MobObjVisual.Part.at("Skull", maskManHead())
                                .visible(MobVisuals::maskManWounded),
                        MobObjVisual.Part.at("IOU", maskManHead())
                                .visible(MobVisuals::maskManWounded)
                                .material(cutout(() -> ResourceManager.iou_tex)))
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.rotateY(
                                    (270F
                                                    - Mth.rotLerp(
                                                            partialTick,
                                                            entity.yBodyRotO,
                                                            entity.yBodyRot))
                                            * Mth.DEG_TO_RAD);
                            pose.rotateX(
                                    maskManSwing(entity, partialTick) * -0.1F * Mth.DEG_TO_RAD);
                        });
    }

    private static ObjEntityVisual.Pose<MaskManEntity> maskManHead() {
        return (pose, entity, partialTick) -> {
            pose.translate(0.5F, 4F, 0F);
            pose.rotateY(
                    -(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.getYHeadRot())
                                    - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot))
                            * Mth.DEG_TO_RAD);
        };
    }

    private static boolean maskManWounded(MaskManEntity entity) {
        return entity.getHealth() < entity.getMaxHealth() / 2F;
    }

    private static ObjEntityVisual.Pose<MaskManEntity> limb(
            float x, float y, float z, float swingMod) {
        return (pose, entity, partialTick) -> {
            pose.translate(x, y, z);
            pose.rotateZ(maskManSwing(entity, partialTick) * swingMod * Mth.DEG_TO_RAD);
        };
    }

    private static float maskManSwing(MaskManEntity entity, float partialTick) {
        float swing = entity.walkAnimation.position(partialTick);
        float amount = entity.walkAnimation.speed(partialTick) * 0.5F;
        return (float) Math.toDegrees(Mth.cos(swing / 2F + (float) Math.PI) * 1.4F * amount);
    }

    private static <T extends CreeperBaseEntity>
            Supplier<MobModelVisual.Spec<T, CreeperRenderState>> creeper(
                    Supplier<Identifier> texture, float swellMod) {
        Supplier<Material> material = cutout(texture);
        return () ->
                MobModelVisual.Spec.<T, CreeperRenderState>of(
                                () -> ModelTrees.of(ModelLayers.CREEPER, material.get()),
                                bake(ModelLayers.CREEPER),
                                CreeperModel::new,
                                CreeperRenderState::new)
                        .extractor(
                                (entity, state, partialTick) -> {
                                    state.swelling = entity.getSwelling(partialTick);
                                    state.isPowered = entity.isPowered();
                                })
                        .scale(
                                (pose, state) -> {
                                    float swell = state.swelling;
                                    float wobble = 1F + Mth.sin(swell * 100F) * swell * 0.01F;
                                    swell = Mth.clamp(swell, 0F, 1F);
                                    swell *= swell;
                                    swell *= swell;
                                    swell *= swellMod;
                                    pose.scale(
                                            (1F + swell * 0.4F) * wobble,
                                            (1F + swell * 0.1F) / wobble,
                                            (1F + swell * 0.4F) * wobble);
                                })
                        .whiteOverlay(
                                state ->
                                        (int) (state.swelling * 10F) % 2 == 0
                                                ? 0F
                                                : Mth.clamp(state.swelling, 0.5F, 1F))
                        .shadow(0.5F, 1F);
    }

    private static <T extends DuckEntity> Supplier<MobModelVisual.Spec<T, ChickenRenderState>> duck(
            float scale, float shadow) {
        Supplier<Material> material = cutout(() -> ResourceManager.duck_tex);
        return () ->
                MobModelVisual.Spec.<T, ChickenRenderState>of(
                                () -> ModelTrees.of(ModelLayers.CHICKEN, material.get()),
                                bake(ModelLayers.CHICKEN),
                                AdultChickenModel::new,
                                ChickenRenderState::new)
                        .extractor(
                                (entity, state, partialTick) -> {
                                    state.flap = Mth.lerp(partialTick, entity.oFlap, entity.flap);
                                    state.flapSpeed =
                                            Mth.lerp(
                                                    partialTick,
                                                    entity.oFlapSpeed,
                                                    entity.flapSpeed);
                                })
                        .scale(
                                (pose, state) -> {
                                    if (scale != 1F) pose.scale(scale);
                                })
                        .shadow(shadow, 1F);
    }

    private static Supplier<MobModelVisual.Spec<ParasiteMaggotEntity, ParasiteMaggotRenderState>>
            maggot() {
        Supplier<Material> material =
                cutout(() -> RegistryUtil.id("textures/entity/parasite_maggot.png"));
        return () ->
                MobModelVisual.Spec.<ParasiteMaggotEntity, ParasiteMaggotRenderState>of(
                                () -> ModelTrees.of(ModelLayers.SILVERFISH, material.get()),
                                bake(ModelLayers.SILVERFISH),
                                SilverfishModel::new,
                                ParasiteMaggotRenderState::new)
                        .flipDegrees(180F)
                        .shadow(0.3F, 1F);
    }

    private static Supplier<MobModelVisual.Spec<RADBeastEntity, LivingEntityRenderState>>
            radBeast() {
        Supplier<Material> material = cutout(() -> ResourceManager.radbeast_tex);
        return () ->
                MobModelVisual.Spec.<RADBeastEntity, LivingEntityRenderState>of(
                                () -> ModelTrees.of(ModelLayers.BLAZE, material.get()),
                                bake(ModelLayers.BLAZE),
                                BlazeModel::new,
                                LivingEntityRenderState::new)
                        .asFullBright()
                        .shadow(0.5F, 1F);
    }

    public static <T extends CreeperBaseEntity> void creeper(
            EntityType<T> type, Supplier<Identifier> texture, float swellMod) {
        model(SimpleEntityVisualizer.builder(type), creeper(texture, swellMod), uncharged());
    }

    public static void register() {
        obj(
                SimpleEntityVisualizer.builder(ModEntities.DELIVERY_DRONE.get()),
                MobVisuals::deliveryDrone,
                always());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.REQUEST_DRONE.get()),
                () ->
                        MobVisuals.<RequestDroneEntity>drone(
                                () -> ResourceManager.delivery_drone_request_tex),
                always());

        obj(
                SimpleEntityVisualizer.builder(ModEntities.FBI_DRONE.get()),
                () ->
                        MobObjVisual.Spec.<FBIDroneEntity>of(
                                        () -> ResourceManager.quadcopter,
                                        cutout(() -> ResourceManager.quadcopter_tex),
                                        MobObjVisual.Part.of("Cube_Cube.001"))
                                .pose(
                                        (pose, entity, partialTick) -> {
                                            pose.translate(0F, 0.25F, 0F);
                                            pose.rotateY(
                                                    (float) (firstDouble(entity.getId()) * 360D)
                                                            * Mth.DEG_TO_RAD);
                                        }),
                always());

        obj(
                SimpleEntityVisualizer.builder(ModEntities.PLASTIC_BAG.get()),
                () ->
                        MobObjVisual.Spec.<PlasticBagEntity>of(
                                        () -> ResourceManager.plasticbag,
                                        cutout(() -> ResourceManager.plasticbag_tex),
                                        MobObjVisual.Part.of("Cube_Cube.001"))
                                .pose(
                                        (pose, entity, partialTick) -> {
                                            pose.rotateY(
                                                    (Mth.rotLerp(
                                                                            partialTick,
                                                                            entity.yRotO,
                                                                            entity.getYRot())
                                                                    + 90F)
                                                            * Mth.DEG_TO_RAD);
                                            pose.rotateZ(
                                                    Mth.lerp(
                                                                    partialTick,
                                                                    entity.xBodyRotO,
                                                                    entity.xBodyRot)
                                                            * Mth.DEG_TO_RAD);
                                        }),
                always());

        obj(
                SimpleEntityVisualizer.builder(ModEntities.TESLA_CRAB.get()),
                () ->
                        MobVisuals.<TeslaCrabEntity>crab(
                                () -> ResourceManager.teslacrab,
                                () -> ResourceManager.teslacrab_tex,
                                "Front",
                                "Back",
                                0F),
                entity -> entity.targets.isEmpty());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.TAINT_CRAB.get()),
                () ->
                        MobVisuals.<TaintCrabEntity>crab(
                                () -> ResourceManager.taintcrab,
                                () -> ResourceManager.taintcrab_tex,
                                "Legs1",
                                "Legs2",
                                90F),
                entity -> entity.targets.isEmpty());

        model(
                SimpleEntityVisualizer.builder(ModEntities.CREEPER_NUCLEAR.get()),
                creeper(() -> ResourceManager.creeper_nuclear_tex, 5F),
                uncharged());
        model(
                SimpleEntityVisualizer.builder(ModEntities.CREEPER_TAINTED.get()),
                creeper(() -> ResourceManager.creeper_tainted_tex, 1F),
                uncharged());
        model(
                SimpleEntityVisualizer.builder(ModEntities.CREEPER_PHOSGENE.get()),
                creeper(() -> ResourceManager.creeper_phosgene_tex, 1F),
                uncharged());
        model(
                SimpleEntityVisualizer.builder(ModEntities.CREEPER_VOLATILE.get()),
                creeper(() -> ResourceManager.creeper_volatile_tex, 1F),
                uncharged());
        model(
                SimpleEntityVisualizer.builder(ModEntities.CREEPER_GOLD.get()),
                creeper(() -> ResourceManager.creeper_gold_tex, 1F),
                uncharged());

        model(
                SimpleEntityVisualizer.builder(ModEntities.DUCK.get()),
                MobVisuals.<DuckEntity>duck(1F, 0.3F),
                always());
        model(
                SimpleEntityVisualizer.builder(ModEntities.QUACKOS.get()),
                MobVisuals.<QuackosEntity>duck(25F, 7.5F),
                always());

        model(
                SimpleEntityVisualizer.builder(ModEntities.PARASITE_MAGGOT.get()),
                maggot(),
                always());
        model(
                SimpleEntityVisualizer.builder(ModEntities.RAD_BEAST.get()),
                radBeast(),
                entity -> entity.getUnfortunateSoul() == null);
        obj(
                SimpleEntityVisualizer.builder(ModEntities.MASK_MAN.get()),
                MobVisuals::maskMan,
                always());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.BALLS_O_TRON.get()),
                () ->
                        MobVisuals.<BOTPrimeHeadEntity>worm(
                                () -> ResourceManager.bot_prime_head,
                                () -> ResourceManager.mark_zero_head_tex),
                always());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.BALLS_O_TRON_SEG.get()),
                () ->
                        MobVisuals.<BOTPrimeBodyEntity>worm(
                                () -> ResourceManager.bot_prime_body,
                                () -> ResourceManager.mark_zero_body_tex),
                always());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.UFO.get()),
                MobVisuals::ufo,
                entity -> !entity.getBeam());

        Predicate<CyberCrabEntity> crabDrawn = unvanished(always());
        SimpleEntityVisualizer.builder(ModEntities.CYBER_CRAB.get())
                .factory(
                        (ctx, entity, partialTick) -> new CyberCrabVisual(ctx, entity, partialTick))
                .skipVanillaRender(crabDrawn)
                .apply();
        Predicate<PigeonEntity> pigeonDrawn = unvanished(always());
        SimpleEntityVisualizer.builder(ModEntities.PIGEON.get())
                .factory((ctx, entity, partialTick) -> new PigeonVisual(ctx, entity, partialTick))
                .skipVanillaRender(pigeonDrawn)
                .apply();
    }

    private static final class ObjBinding<T extends Entity> {
        MobObjVisual.Prepared<T> prepared;
    }
}
