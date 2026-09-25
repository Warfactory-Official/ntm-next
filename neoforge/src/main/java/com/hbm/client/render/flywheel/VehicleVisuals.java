// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.model.Meshes;
import com.hbm.entity.ModEntities;
import com.hbm.entity.item.EntityParachuteCrate;
import com.hbm.entity.logic.EntityBomber;
import com.hbm.entity.logic.EntityC130;
import com.hbm.entity.missile.EntityBobmazon;
import com.hbm.entity.missile.EntitySatellitePod;
import com.hbm.entity.missile.EntitySoyuz;
import com.hbm.entity.missile.EntitySoyuzCapsule;
import com.hbm.entity.projectile.EntityBoxcar;
import com.hbm.entity.projectile.EntityBuilding;
import com.hbm.entity.projectile.EntityCoin;
import com.hbm.entity.projectile.EntityDuchessGambit;
import com.hbm.entity.projectile.EntityRBMKDebris;
import com.hbm.entity.projectile.EntityZirnoxDebris;
import com.hbm.entity.train.EntityRailCarBase;
import com.hbm.entity.train.TrainCargoTram;
import com.hbm.entity.train.TrainCargoTramTrailer;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import dev.engine_room.flywheel.lib.visualization.SimpleEntityVisualizer;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import org.joml.Matrix4f;

public final class VehicleVisuals {
    private VehicleVisuals() {}

    public static void initModels() {
        Rigs.init();
    }

    public static void register() {
        ObjEntityVisual.register(ModEntities.BOXCAR.get(), VehicleVisuals::boxcar);
        ObjEntityVisual.register(ModEntities.DUCHESS_GAMBIT.get(), VehicleVisuals::duchessGambit);
        ObjEntityVisual.register(ModEntities.FALLING_BUILDING.get(), VehicleVisuals::building);
        ObjEntityVisual.register(ModEntities.BOBMAZON_DELIVERY.get(), VehicleVisuals::bobmazon);
        ObjEntityVisual.register(ModEntities.COIN.get(), VehicleVisuals::coin);
        ObjEntityVisual.register(ModEntities.CARGO_TRAM.get(), VehicleVisuals::cargoTram);
        ObjEntityVisual.register(
                ModEntities.CARGO_TRAM_TRAILER.get(), VehicleVisuals::cargoTramTrailer);

        SimpleEntityVisualizer.builder(ModEntities.BOAT_RUBBER.get())
                .factory(RubberBoatVisual::new)
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.C130.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(ctx, entity, partialTick, e -> Rigs.C130))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.BOMBER.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(ctx, entity, partialTick, Rigs::bomber))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.SOYUZ.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(ctx, entity, partialTick, Rigs::soyuz))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.SOYUZ_CAPSULE.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(
                                        ctx, entity, partialTick, e -> Rigs.SOYUZ_CAPSULE))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.SATELLITE_POD.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(ctx, entity, partialTick, e -> Rigs.DROPSHIP))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.PARACHUTE_CRATE.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(
                                        ctx, entity, partialTick, e -> Rigs.PARACHUTE_CRATE))
                .apply();

        SimpleEntityVisualizer.builder(ModEntities.RBMK_DEBRIS.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(ctx, entity, partialTick, Rigs::rbmkDebris))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.ZIRNOX_DEBRIS.get())
                .factory(
                        (ctx, entity, partialTick) ->
                                new CraftVisual<>(ctx, entity, partialTick, Rigs::zirnoxDebris))
                .apply();
        SimpleEntityVisualizer.builder(ModEntities.BURNING_FOEQ.get())
                .factory(FOEQVisual::new)
                .apply();
    }

    private static ObjEntityVisual.Spec<EntityBoxcar> boxcar() {
        return ObjEntityVisual.Spec.<EntityBoxcar>of(
                        ResourceManager.boxcar, ResourceManager.boxcar_tex, "Cube_Cube.001")
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.translate(0F, 0F, -1.5F);
                            pose.rotateZ(180F * Mth.DEG_TO_RAD);
                            pose.rotateX(90F * Mth.DEG_TO_RAD);
                        });
    }

    private static ObjEntityVisual.Spec<TrainCargoTram> cargoTram() {
        return ObjEntityVisual.Spec.<TrainCargoTram>of(
                        ResourceManager.train_cargo_tram, ResourceManager.train_tram_tex, "Plane")
                .pose(VehicleVisuals::railCar)
                .unculled();
    }

    private static ObjEntityVisual.Spec<TrainCargoTramTrailer> cargoTramTrailer() {
        return ObjEntityVisual.Spec.<TrainCargoTramTrailer>of(
                        ResourceManager.train_cargo_tram_trailer,
                        ResourceManager.tram_trailer_tex,
                        "Plane")
                .pose(VehicleVisuals::railCar)
                .unculled();
    }

    private static void railCar(Matrix4f pose, EntityRailCarBase car, float partialTick) {
        pose.translate(
                (float)
                        (Mth.lerp(partialTick, car.lastRenderX, car.renderX)
                                - Mth.lerp(partialTick, car.xo, car.getX())),
                (float)
                        (Mth.lerp(partialTick, car.lastRenderY, car.renderY)
                                - Mth.lerp(partialTick, car.yo, car.getY())),
                (float)
                        (Mth.lerp(partialTick, car.lastRenderZ, car.renderZ)
                                - Mth.lerp(partialTick, car.zo, car.getZ())));
        pose.rotateY(-Mth.rotLerp(partialTick, car.yRotO, car.getYRot()) * Mth.DEG_TO_RAD);
        pose.rotateX(-Mth.lerp(partialTick, car.xRotO, car.getXRot()) * Mth.DEG_TO_RAD);
    }

    private static ObjEntityVisual.Spec<EntityDuchessGambit> duchessGambit() {
        return ObjEntityVisual.Spec.<EntityDuchessGambit>of(
                        ResourceManager.duchessgambit,
                        ResourceManager.duchessgambit_tex,
                        "Cube_Cube.001")
                .pose((pose, entity, partialTick) -> pose.translate(0F, 0F, -1F));
    }

    private static ObjEntityVisual.Spec<EntityBuilding> building() {
        return ObjEntityVisual.Spec.<EntityBuilding>of(
                        ResourceManager.building, ResourceManager.building_tex, "Cube")
                .pose(VehicleVisuals::asAuthored)
                .unculled();
    }

    private static ObjEntityVisual.Spec<EntityBobmazon> bobmazon() {
        return ObjEntityVisual.Spec.<EntityBobmazon>of(
                        ResourceManager.miner_rocket, ResourceManager.bobmazon_tex, "Cylinder")
                .pose((pose, entity, partialTick) -> pose.rotateX(180F * Mth.DEG_TO_RAD))
                .unculled();
    }

    private static ObjEntityVisual.Spec<EntityCoin> coin() {
        return ObjEntityVisual.Spec.<EntityCoin>of(Rigs.CHIP, Rigs.CHIP_TEX, "Cylinder")
                .scale(0.125F)
                .pose(
                        (pose, entity, partialTick) -> {
                            pose.rotateY(
                                    -(Mth.lerp(partialTick, entity.yRotO, entity.getYRot()) - 90F)
                                            * Mth.DEG_TO_RAD);
                            pose.rotateZ((entity.tickCount + partialTick) * 45F * Mth.DEG_TO_RAD);
                        });
    }

    private static void asAuthored(Matrix4f pose, Entity entity, float partialTick) {}

    private static final class Rigs {
        private static final float PIVOT = 7F;
        private static final float DIAG = 0.57735026F;

        private static final float[][] C130_PROPS = {
            {10F, 4.2F, -20.5F}, {10F, 4.2F, -11.16F}, {10F, 4.2F, 11.16F}, {10F, 4.2F, 20.5F}
        };
        private static final String[] SOYUZ_SINGLE = {
            "EngineBlock",
            "BottomStage",
            "TopStage",
            "Payload",
            "PayloadBlocks",
            "LES",
            "LESThrusters",
            "MainEngines",
            "SideEngines"
        };
        private static final int SOYUZ_MAIN_ENGINES = 7;
        private static final int SOYUZ_BOOSTER = 9;
        private static final int SOYUZ_BOOSTER_SIDE = 10;

        static final HFRWavefrontObject CHIP =
                Meshes.flatShaded(Meshes.load(Library.id("models/trinkets/chip.obj")));
        static final Identifier CHIP_TEX = Library.id("textures/models/trinkets/chip_gold.png");
        static final CraftVisual.Rig<EntityC130> C130 = c130();
        static final CraftVisual.Rig<EntitySoyuzCapsule> SOYUZ_CAPSULE =
                new CraftVisual.Rig<>(
                        (pose, entity, partialTick) -> swing(pose, entity.level().getGameTime()),
                        List.of(
                                CraftVisual.Part.of(
                                        ResourceManager.soyuz_lander,
                                        "Capsule",
                                        ResourceManager.soyuz_lander_tex),
                                CraftVisual.Part.of(
                                        ResourceManager.soyuz_lander,
                                        "Chute",
                                        ResourceManager.soyuz_chute_tex)));
        static final CraftVisual.Rig<EntitySatellitePod> DROPSHIP = dropship();

        private static CraftVisual.Rig<EntitySatellitePod> dropship() {
            List<CraftVisual.Part<EntitySatellitePod>> parts = new ArrayList<>();
            parts.add(
                    CraftVisual.Part.of(
                            ResourceManager.dropship, "Pod", ResourceManager.dropship_tex));
            for (int i = 0; i < 4; i++) {
                int leg = i;
                parts.add(
                        CraftVisual.Part.<EntitySatellitePod>of(
                                        ResourceManager.dropship,
                                        "Leg",
                                        ResourceManager.dropship_tex)
                                .at(
                                        (pose, entity, partialTick) -> {
                                            float extension =
                                                    entity.prevLegs
                                                            + (entity.legs - entity.prevLegs)
                                                                    * partialTick;
                                            pose.rotateY((float) Math.toRadians(45F + 90F * leg));
                                            pose.translate(0.5F, 1.75F, 0F);
                                            pose.rotateZ(
                                                    (float)
                                                            Math.toRadians(
                                                                    150F * (1F - extension)));
                                            pose.translate(-0.5F, -1.75F, 0F);
                                        }));
            }
            return new CraftVisual.Rig<>(null, List.copyOf(parts));
        }

        static final CraftVisual.Rig<EntityParachuteCrate> PARACHUTE_CRATE =
                new CraftVisual.Rig<>(
                        (pose, entity, partialTick) -> swing(pose, entity.level().getGameTime()),
                        List.of(
                                CraftVisual.Part.of(
                                        ResourceManager.conservecrate,
                                        "Cube.001",
                                        ResourceManager.supply_crate_tex),
                                CraftVisual.Part.<EntityParachuteCrate>of(
                                                ResourceManager.soyuz_lander,
                                                "Chute",
                                                ResourceManager.soyuz_chute_tex)
                                        .at(
                                                (pose, entity, partialTick) ->
                                                        pose.translate(0F, -1F, 0F))));
        private static final List<CraftVisual.Rig<EntitySoyuz>> SOYUZ = soyuzRigs();
        private static final CraftVisual.Rig<EntityBomber>[] BOMBER = bomberRigs();
        private static final CraftVisual.Rig<EntityRBMKDebris>[] RBMK =
                new CraftVisual.Rig[EntityRBMKDebris.DebrisType.VALUES.length];
        private static final CraftVisual.Rig<EntityZirnoxDebris>[] ZIRNOX =
                new CraftVisual.Rig[EntityZirnoxDebris.DebrisType.VALUES.length];

        static {
            for (EntityRBMKDebris.DebrisType type : EntityRBMKDebris.DebrisType.VALUES) {
                RBMK[type.ordinal()] = rbmkRig(type);
            }
            for (EntityZirnoxDebris.DebrisType type : EntityZirnoxDebris.DebrisType.VALUES) {
                ZIRNOX[type.ordinal()] = zirnoxRig(type);
            }
        }

        private Rigs() {}

        static void init() {}

        private static CraftVisual.Rig<EntityBomber>[] bomberRigs() {
            var rigs = new CraftVisual.Rig[9];
            for (int i = 0; i < rigs.length; i++) rigs[i] = bomberRig(i);
            return rigs;
        }

        static CraftVisual.Rig<EntitySoyuz> soyuz(EntitySoyuz entity) {
            return SOYUZ.get(Math.floorMod(entity.getSkin(), SOYUZ.size()));
        }

        static CraftVisual.Rig<EntityBomber> bomber(EntityBomber entity) {
            return BOMBER[entity.getStyle()];
        }

        static CraftVisual.Rig<EntityRBMKDebris> rbmkDebris(EntityRBMKDebris entity) {
            return RBMK[entity.getDebrisType().ordinal()];
        }

        static CraftVisual.Rig<EntityZirnoxDebris> zirnoxDebris(EntityZirnoxDebris entity) {
            return ZIRNOX[entity.getDebrisType().ordinal()];
        }

        private static void swing(Matrix4f pose, double time) {
            pose.translate(0F, PIVOT, 0F);
            pose.rotateZ((float) (Math.sin(time * 0.05D) * 5D) * Mth.DEG_TO_RAD);
            pose.rotateX((float) (Math.sin(time * 0.05D + Math.PI * 0.5D) * 5D) * Mth.DEG_TO_RAD);
            pose.translate(0F, -PIVOT, 0F);
        }

        private static void tumble(Matrix4f pose, int idSpin, float rot) {
            pose.translate(0F, 0.125F, 0F);
            pose.rotateY(idSpin * Mth.DEG_TO_RAD);
            pose.rotate(rot * Mth.DEG_TO_RAD, DIAG, DIAG, DIAG);
        }

        private static CraftVisual.Rig<EntityC130> c130() {
            List<CraftVisual.Part<EntityC130>> parts = new ArrayList<>();
            parts.add(
                    CraftVisual.Part.of(ResourceManager.c130, "Plane", ResourceManager.c130_0_tex));
            for (int i = 0; i < C130_PROPS.length; i++) {
                float px = C130_PROPS[i][0];
                float py = C130_PROPS[i][1];
                float pz = C130_PROPS[i][2];
                parts.add(
                        CraftVisual.Part.<EntityC130>of(
                                        ResourceManager.c130,
                                        "Prop" + (i + 1),
                                        ResourceManager.c130_0_tex)
                                .at(
                                        (pose, entity, partialTick) -> {
                                            pose.translate(px, py, pz);
                                            pose.rotateX(
                                                    (float) (GameTime.now() * 15D % 360D)
                                                            * Mth.DEG_TO_RAD);
                                            pose.translate(-px, -py, -pz);
                                        }));
            }
            return new CraftVisual.Rig<>(
                    (pose, entity, partialTick) -> {
                        pose.rotateY(
                                (Mth.rotLerp(partialTick, entity.renderYawO, entity.renderYaw)
                                                - 90F)
                                        * Mth.DEG_TO_RAD);
                        pose.rotateZ(90F * Mth.DEG_TO_RAD);
                        pose.rotateZ(
                                Mth.lerp(partialTick, entity.renderPitchO, entity.renderPitch)
                                        * Mth.DEG_TO_RAD);
                    },
                    List.copyOf(parts));
        }

        private static CraftVisual.Rig<EntityBomber> bomberRig(int style) {
            Identifier texture =
                    switch (style) {
                        case 2 -> ResourceManager.dornier_2_tex;
                        case 4 -> ResourceManager.dornier_4_tex;
                        case 5 -> ResourceManager.b29_0_tex;
                        case 6 -> ResourceManager.b29_1_tex;
                        case 7 -> ResourceManager.b29_2_tex;
                        case 8 -> ResourceManager.b29_3_tex;
                        default -> ResourceManager.dornier_1_tex;
                    };
            CraftVisual.Part<EntityBomber> airframe =
                    style >= 0 && style <= 4
                            ? CraftVisual.Part.<EntityBomber>of(
                                            ResourceManager.dornier, "Cube", texture)
                                    .at(
                                            (pose, entity, partialTick) -> {
                                                pose.scale(5F);
                                                pose.rotateY(-90F * Mth.DEG_TO_RAD);
                                            })
                            : CraftVisual.Part.<EntityBomber>of(
                                            ResourceManager.b29, "Cube", texture)
                                    .at(
                                            (pose, entity, partialTick) -> {
                                                pose.scale(30F / 3.1F);
                                                pose.rotateY(180F * Mth.DEG_TO_RAD);
                                            });
            return CraftVisual.Rig.unculled(
                    (pose, entity, partialTick) -> {
                        pose.rotateY(
                                (Mth.rotLerp(partialTick, entity.renderYawO, entity.renderYaw)
                                                - 90F)
                                        * Mth.DEG_TO_RAD);
                        pose.rotateZ(90F * Mth.DEG_TO_RAD);
                        pose.rotateZ(
                                Mth.lerp(partialTick, entity.renderPitchO, entity.renderPitch)
                                        * Mth.DEG_TO_RAD);
                        pose.rotateX(
                                (float) Math.sin((entity.tickCount + partialTick) * 0.05D)
                                        * 10F
                                        * Mth.DEG_TO_RAD);
                    },
                    List.of(airframe));
        }

        private static List<CraftVisual.Rig<EntitySoyuz>> soyuzRigs() {
            List<CraftVisual.Rig<EntitySoyuz>> rigs = new ArrayList<>();
            for (Identifier[] skin : ResourceManager.soyuz_skin_tex) {
                List<CraftVisual.Part<EntitySoyuz>> parts = new ArrayList<>();
                for (int i = 0; i < SOYUZ_SINGLE.length; i++) {
                    parts.add(CraftVisual.Part.of(ResourceManager.soyuz, SOYUZ_SINGLE[i], skin[i]));
                }
                parts.add(
                        CraftVisual.Part.of(
                                ResourceManager.soyuz,
                                "Memento",
                                ResourceManager.soyuz_memento_tex));
                for (int i = 0; i < 4; i++) {
                    String tag = ".00" + i;
                    parts.add(
                            CraftVisual.Part.of(
                                    ResourceManager.soyuz, "Booster" + tag, skin[SOYUZ_BOOSTER]));
                    parts.add(
                            CraftVisual.Part.of(
                                    ResourceManager.soyuz,
                                    "BoosterEngines" + tag,
                                    skin[SOYUZ_MAIN_ENGINES]));
                    parts.add(
                            CraftVisual.Part.of(
                                    ResourceManager.soyuz,
                                    "BoosterSide" + tag,
                                    skin[SOYUZ_BOOSTER_SIDE]));
                }
                rigs.add(new CraftVisual.Rig<EntitySoyuz>(null, List.copyOf(parts)));
            }
            return List.copyOf(rigs);
        }

        private static CraftVisual.Rig<EntityRBMKDebris> rbmkRig(EntityRBMKDebris.DebrisType type) {
            CraftVisual.Part<EntityRBMKDebris> chunk =
                    switch (type) {
                        case BLANK ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_blank,
                                        "Column",
                                        ResourceManager.deb_blank_tex);
                        case ELEMENT ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_element,
                                        "Column",
                                        ResourceManager.deb_element_tex);
                        case FUEL ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_fuel,
                                        "Rods",
                                        ResourceManager.deb_fuel_tex);
                        case ROD ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_rod,
                                        "Lid",
                                        ResourceManager.deb_control_tex);
                        case GRAPHITE ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_graphite,
                                        "Cube_Cube.001",
                                        ResourceManager.deb_graphite_tex);
                        case LID ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_lid,
                                        "Lid",
                                        ResourceManager.deb_lid_tex);
                    };
            return new CraftVisual.Rig<>(
                    (pose, entity, partialTick) ->
                            tumble(
                                    pose,
                                    entity.getId() % 360,
                                    Mth.lerp(partialTick, entity.lastRot, entity.rot)),
                    List.of(chunk));
        }

        private static CraftVisual.Rig<EntityZirnoxDebris> zirnoxRig(
                EntityZirnoxDebris.DebrisType type) {
            CraftVisual.Part<EntityZirnoxDebris> chunk =
                    switch (type) {
                        case BLANK ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_zirnox_blank,
                                        "Plane",
                                        ResourceManager.zirnox_tex);
                        case ELEMENT ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_zirnox_element,
                                        "Cube_Cube.001",
                                        ResourceManager.zirnox_deb_element_tex);
                        case SHRAPNEL ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_zirnox_shrapnel,
                                        "Plane",
                                        ResourceManager.zirnox_tex);
                        case GRAPHITE ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_graphite,
                                        "Cube_Cube.001",
                                        ResourceManager.deb_graphite_tex);
                        case CONCRETE ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_zirnox_concrete,
                                        "Plane",
                                        ResourceManager.zirnox_destroyed_tex);
                        case EXCHANGER ->
                                CraftVisual.Part.of(
                                        ResourceManager.deb_zirnox_exchanger,
                                        "Plane",
                                        ResourceManager.zirnox_tex);
                    };
            return CraftVisual.Rig.unculled(
                    (pose, entity, partialTick) ->
                            tumble(
                                    pose,
                                    entity.getId() % 360,
                                    Mth.lerp(partialTick, entity.lastRot, entity.rot)),
                    List.of(chunk));
        }
    }
}
