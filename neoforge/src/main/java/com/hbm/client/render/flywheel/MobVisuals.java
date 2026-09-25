// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.VanishedEntities;
import com.hbm.client.render.ParasiteMaggotRenderState;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityDeliveryDrone;
import com.hbm.entity.item.EntityDroneBase;
import com.hbm.entity.item.EntityRequestDrone;
import com.hbm.entity.mob.EntityCreeperBase;
import com.hbm.entity.mob.EntityCyberCrab;
import com.hbm.entity.mob.EntityDuck;
import com.hbm.entity.mob.EntityFBIDrone;
import com.hbm.entity.mob.EntityMaskMan;
import com.hbm.entity.mob.EntityParasiteMaggot;
import com.hbm.entity.mob.EntityPigeon;
import com.hbm.entity.mob.EntityPlasticBag;
import com.hbm.entity.mob.EntityQuackos;
import com.hbm.entity.mob.EntityRADBeast;
import com.hbm.entity.mob.EntityTaintCrab;
import com.hbm.entity.mob.EntityTeslaCrab;
import com.hbm.entity.mob.EntityUFO;
import com.hbm.entity.mob.botprime.EntityBOTPrimeBody;
import com.hbm.entity.mob.botprime.EntityBOTPrimeHead;
import com.hbm.entity.mob.botprime.EntityWormBaseNT;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
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
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.Mob;
import org.jspecify.annotations.Nullable;

public final class MobVisuals {

    private MobVisuals() {}

    private static <T extends Entity> Predicate<T> always() {
        return entity -> true;
    }

    private static <T extends EntityCreeperBase> Predicate<T> uncharged() {
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

    private static <T extends EntityDroneBase> MobObjVisual.Spec<T> drone(
            Supplier<Identifier> texture) {
        return MobObjVisual.Spec.of(
                () -> ResourceManager.delivery_drone,
                cutout(texture),
                MobObjVisual.Part.of("Drone"),
                MobObjVisual.Part.when("Crate", entity -> entity.getAppearance() == 1),
                MobObjVisual.Part.when("Barrel", entity -> entity.getAppearance() == 2));
    }

    private static MobObjVisual.Spec<EntityDeliveryDrone> deliveryDrone() {
        Supplier<Material> express = cutout(() -> ResourceManager.delivery_drone_express_tex);
        return MobObjVisual.Spec.of(
                () -> ResourceManager.delivery_drone,
                cutout(() -> ResourceManager.delivery_drone_tex),
                MobObjVisual.Part.when("Drone", entity -> !entity.isExpress()),
                MobObjVisual.Part.when(
                        "Crate", entity -> !entity.isExpress() && entity.getAppearance() == 1),
                MobObjVisual.Part.when(
                        "Barrel", entity -> !entity.isExpress() && entity.getAppearance() == 2),
                MobObjVisual.Part.<EntityDeliveryDrone>when("Drone", EntityDeliveryDrone::isExpress)
                        .material(express),
                MobObjVisual.Part.<EntityDeliveryDrone>when(
                                "Crate",
                                entity -> entity.isExpress() && entity.getAppearance() == 1)
                        .material(express),
                MobObjVisual.Part.<EntityDeliveryDrone>when(
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

    private static <T extends EntityWormBaseNT> MobObjVisual.Spec<T> worm(
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

    private static MobObjVisual.Spec<EntityUFO> ufo() {
        return MobObjVisual.Spec.<EntityUFO>of(
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

    private static MobObjVisual.Spec<EntityMaskMan> maskMan() {
        return MobObjVisual.Spec.<EntityMaskMan>of(
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

    private static ObjEntityVisual.Pose<EntityMaskMan> maskManHead() {
        return (pose, entity, partialTick) -> {
            pose.translate(0.5F, 4F, 0F);
            pose.rotateY(
                    -(Mth.rotLerp(partialTick, entity.yHeadRotO, entity.getYHeadRot())
                                    - Mth.rotLerp(partialTick, entity.yBodyRotO, entity.yBodyRot))
                            * Mth.DEG_TO_RAD);
        };
    }

    private static boolean maskManWounded(EntityMaskMan entity) {
        return entity.getHealth() < entity.getMaxHealth() / 2F;
    }

    private static ObjEntityVisual.Pose<EntityMaskMan> limb(
            float x, float y, float z, float swingMod) {
        return (pose, entity, partialTick) -> {
            pose.translate(x, y, z);
            pose.rotateZ(maskManSwing(entity, partialTick) * swingMod * Mth.DEG_TO_RAD);
        };
    }

    private static float maskManSwing(EntityMaskMan entity, float partialTick) {
        float swing = entity.walkAnimation.position(partialTick);
        float amount = entity.walkAnimation.speed(partialTick) * 0.5F;
        return (float) Math.toDegrees(Mth.cos(swing / 2F + (float) Math.PI) * 1.4F * amount);
    }

    private static <T extends EntityCreeperBase>
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

    private static <T extends EntityDuck> Supplier<MobModelVisual.Spec<T, ChickenRenderState>> duck(
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

    private static Supplier<MobModelVisual.Spec<EntityParasiteMaggot, ParasiteMaggotRenderState>>
            maggot() {
        Supplier<Material> material =
                cutout(() -> Library.id("textures/entity/parasite_maggot.png"));
        return () ->
                MobModelVisual.Spec.<EntityParasiteMaggot, ParasiteMaggotRenderState>of(
                                () -> ModelTrees.of(ModelLayers.SILVERFISH, material.get()),
                                bake(ModelLayers.SILVERFISH),
                                SilverfishModel::new,
                                ParasiteMaggotRenderState::new)
                        .flipDegrees(180F)
                        .shadow(0.3F, 1F);
    }

    private static Supplier<MobModelVisual.Spec<EntityRADBeast, LivingEntityRenderState>>
            radBeast() {
        Supplier<Material> material = cutout(() -> ResourceManager.radbeast_tex);
        return () ->
                MobModelVisual.Spec.<EntityRADBeast, LivingEntityRenderState>of(
                                () -> ModelTrees.of(ModelLayers.BLAZE, material.get()),
                                bake(ModelLayers.BLAZE),
                                BlazeModel::new,
                                LivingEntityRenderState::new)
                        .asFullBright()
                        .shadow(0.5F, 1F);
    }

    public static void register() {
        obj(
                SimpleEntityVisualizer.builder(ModEntities.DELIVERY_DRONE.get()),
                MobVisuals::deliveryDrone,
                always());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.REQUEST_DRONE.get()),
                () ->
                        MobVisuals.<EntityRequestDrone>drone(
                                () -> ResourceManager.delivery_drone_request_tex),
                always());

        obj(
                SimpleEntityVisualizer.builder(ModEntities.FBI_DRONE.get()),
                () ->
                        MobObjVisual.Spec.<EntityFBIDrone>of(
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
                        MobObjVisual.Spec.<EntityPlasticBag>of(
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
                                                    (Mth.lerp(
                                                                            partialTick,
                                                                            entity.xRotO,
                                                                            entity.getXRot())
                                                                    - 90F)
                                                            * Mth.DEG_TO_RAD);
                                        }),
                always());

        obj(
                SimpleEntityVisualizer.builder(ModEntities.TESLA_CRAB.get()),
                () ->
                        MobVisuals.<EntityTeslaCrab>crab(
                                () -> ResourceManager.teslacrab,
                                () -> ResourceManager.teslacrab_tex,
                                "Front",
                                "Back",
                                0F),
                entity -> entity.targets.isEmpty());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.TAINT_CRAB.get()),
                () ->
                        MobVisuals.<EntityTaintCrab>crab(
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
                MobVisuals.<EntityDuck>duck(1F, 0.3F),
                always());
        model(
                SimpleEntityVisualizer.builder(ModEntities.QUACKOS.get()),
                MobVisuals.<EntityQuackos>duck(25F, 7.5F),
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
                        MobVisuals.<EntityBOTPrimeHead>worm(
                                () -> ResourceManager.bot_prime_head,
                                () -> ResourceManager.mark_zero_head_tex),
                always());
        obj(
                SimpleEntityVisualizer.builder(ModEntities.BALLS_O_TRON_SEG.get()),
                () ->
                        MobVisuals.<EntityBOTPrimeBody>worm(
                                () -> ResourceManager.bot_prime_body,
                                () -> ResourceManager.mark_zero_body_tex),
                always());

        obj(
                SimpleEntityVisualizer.builder(ModEntities.UFO.get()),
                MobVisuals::ufo,
                entity -> !entity.getBeam());

        Predicate<EntityCyberCrab> crabDrawn = unvanished(always());
        SimpleEntityVisualizer.builder(ModEntities.CYBER_CRAB.get())
                .factory(
                        (ctx, entity, partialTick) -> new CyberCrabVisual(ctx, entity, partialTick))
                .skipVanillaRender(crabDrawn)
                .apply();
        Predicate<EntityPigeon> pigeonDrawn = unvanished(always());
        SimpleEntityVisualizer.builder(ModEntities.PIGEON.get())
                .factory((ctx, entity, partialTick) -> new PigeonVisual(ctx, entity, partialTick))
                .skipVanillaRender(pigeonDrawn)
                .apply();
    }

    private static final class ObjBinding<T extends Entity> {
        MobObjVisual.Prepared<T> prepared;
    }
}
