// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntitySoyuzLauncher;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class SoyuzLauncherVisual extends HbmDynamicBlockEntityVisual<BlockEntitySoyuzLauncher>
        implements ShaderLightVisual {
    private static final float OPEN = 45F;
    private static final int SWING_TICKS = 20;
    private static final double MESH_DROP = 4D;
    private static final double ROCKET_LIFT = 5D;
    private static final double TOWER_PIVOT_Y = 5.5D;
    private static final double TOWER_PIVOT_Z = 5.5D;
    private static final double SUPPORT_PIVOT_Z = -6.5D;
    private static final String[] SINGLE = {
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
    private static final String[] BOOSTERS = {
        "Booster.000", "Booster.001", "Booster.002", "Booster.003"
    };
    private static final String[] BOOSTER_ENGINES = {
        "BoosterEngines.000", "BoosterEngines.001", "BoosterEngines.002", "BoosterEngines.003"
    };
    private static final String[] BOOSTER_SIDES = {
        "BoosterSide.000", "BoosterSide.001", "BoosterSide.002", "BoosterSide.003"
    };
    private static final MeshPart SOYUZ_LAUNCHER_TOWER =
            rawPart(
                    ResourceManager.soyuz_launcher_tower,
                    0,
                    ResourceManager.soyuz_launcher_tower_tex);
    private static final MeshPart SOYUZ_LAUNCHER_SUPPORT =
            rawPart(
                    ResourceManager.soyuz_launcher_support,
                    0,
                    ResourceManager.soyuz_launcher_support_tex);
    private static final PackedQuadMesh[] ROCKET_MESHES = buildRocketMeshes();
    private static final MeshPart[][] ROCKETS = buildRockets();

    static MeshPart[] rocketParts(int skin) {
        return ROCKETS[skin];
    }

    private final Piece tower;
    private final Piece support;
    private final AABB bodyBounds;
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f towerPose = new Matrix4f();
    private final Matrix4f supportPose = new Matrix4f();
    private final Matrix4f rocketPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private @Nullable AABB lastLightBounds;
    private RocketSet rocket;
    private float lastSwing = Float.NaN;
    private int lastRocketType = Integer.MIN_VALUE;
    private boolean initialized;

    public SoyuzLauncherVisual(
            VisualizationContext context, BlockEntitySoyuzLauncher blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyPose = new Matrix4f().translate(.5F, (float) -MESH_DROP, .5F);
        basePose.set(bodyPose);
        HFRWavefrontObject[] body = {
            ResourceManager.soyuz_launcher_legs,
            ResourceManager.soyuz_launcher_table,
            ResourceManager.soyuz_launcher_tower_base,
            ResourceManager.soyuz_launcher_support_base
        };
        AABB extent = new AABB(pos).inflate(1);
        for (HFRWavefrontObject mesh : body)
            extent = extent.minmax(LightBounds.of(mesh.groups[0], bodyPose, pos));
        bodyBounds = extent;
        tower = piece(SOYUZ_LAUNCHER_TOWER);
        support = piece(SOYUZ_LAUNCHER_SUPPORT);
        updateMovingParts(partialTick);
    }

    private static PackedQuadMesh[] buildRocketMeshes() {
        var model = ResourceManager.soyuz;
        var meshes =
                new PackedQuadMesh
                        [SINGLE.length
                                + 1
                                + BOOSTERS.length
                                + BOOSTER_ENGINES.length
                                + BOOSTER_SIDES.length];
        int at = 0;
        for (String name : SINGLE)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        meshes[at++] = PackedQuadMesh.of(model.groups[model.partId("Memento")], model.smoothing());
        for (String name : BOOSTERS)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        for (String name : BOOSTER_ENGINES)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        for (String name : BOOSTER_SIDES)
            meshes[at++] = PackedQuadMesh.of(model.groups[model.partId(name)], model.smoothing());
        return meshes;
    }

    private static MeshPart[][] buildRockets() {
        var result = new MeshPart[ResourceManager.soyuz_skin_tex.length][];
        for (int i = 0; i < result.length; i++) result[i] = buildRocket(i);
        return result;
    }

    private static MeshPart rawPart(HFRWavefrontObject model, int group, Identifier texture) {
        return MeshPart.obj(
                model.groups[group],
                model.smoothing(),
                SimpleMaterial.builderOf(MeshPart.litCutout(texture))
                        .backfaceCulling(false)
                        .build());
    }

    private static MeshPart[] buildRocket(int skinIndex) {
        Identifier[] skin = ResourceManager.soyuz_skin_tex[skinIndex];
        MeshPart[] pieces = new MeshPart[ROCKET_MESHES.length];
        int at = 0;
        for (int i = 0; i < SINGLE.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[i]));
        pieces[at] =
                MeshPart.create(
                        ROCKET_MESHES[at++], MeshPart.litCutout(ResourceManager.soyuz_memento_tex));
        for (int i = 0; i < BOOSTERS.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[9]));
        for (int i = 0; i < BOOSTER_ENGINES.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[7]));
        for (int i = 0; i < BOOSTER_SIDES.length; i++)
            pieces[at] = MeshPart.create(ROCKET_MESHES[at++], MeshPart.litCutout(skin[10]));
        return pieces;
    }

    public static void initModels() {}

    private Piece piece(MeshPart part) {
        TransformedInstance instance =
                instancerProvider()
                        .instancer(InstanceTypes.TRANSFORMED, part.model())
                        .createInstance();
        return new Piece(part, instance);
    }

    private @Nullable RocketSet rocket(int skinIndex) {
        if (skinIndex < 0 || skinIndex >= ROCKETS.length) return null;
        MeshPart[] parts = ROCKETS[skinIndex];
        Piece[] pieces = new Piece[parts.length];
        for (int i = 0; i < parts.length; i++) pieces[i] = piece(parts[i]);
        return new RocketSet(skinIndex, pieces);
    }

    private void syncRocket(int skinIndex) {
        if (rocket != null && rocket.skin == skinIndex) return;
        if (rocket != null) rocket.delete();
        rocket = rocket(skinIndex);
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        int rocketType = blockEntity.rocketType;
        boolean rocketChanged = !initialized || rocketType != lastRocketType;
        if (rocketChanged) syncRocket(rocketType);
        float swing = blockEntity.rocketType >= 0 ? 0F : OPEN;
        if (blockEntity.starting && blockEntity.countdown < SWING_TICKS)
            swing = (SWING_TICKS - blockEntity.countdown + partialTick) * OPEN / SWING_TICKS;
        if (!rocketChanged && Float.compare(swing, lastSwing) == 0) return;
        lastRocketType = rocketType;
        lastSwing = swing;
        initialized = true;

        towerPose
                .set(basePose)
                .translate(0F, (float) TOWER_PIVOT_Y, (float) TOWER_PIVOT_Z)
                .rotateX((swing) * Mth.DEG_TO_RAD)
                .translate(0F, (float) -TOWER_PIVOT_Y, (float) -TOWER_PIVOT_Z);
        write(tower, towerPose);
        LightBounds.resetBounds(lightBoundsAccumulator, bodyBounds);
        LightBounds.includeLightBounds(lightBoundsAccumulator, tower.part.model(), towerPose, pos);
        supportPose
                .set(basePose)
                .translate(0F, (float) TOWER_PIVOT_Y, (float) SUPPORT_PIVOT_Z)
                .rotateX(-(swing) * Mth.DEG_TO_RAD)
                .translate(0F, (float) -TOWER_PIVOT_Y, (float) -SUPPORT_PIVOT_Z);
        write(support, supportPose);
        LightBounds.includeLightBounds(
                lightBoundsAccumulator, support.part.model(), supportPose, pos);
        if (rocket != null) {
            rocketPose.set(basePose).translate(0F, (float) ROCKET_LIFT, 0F);
            for (Piece piece : rocket.pieces) {
                write(piece, rocketPose);
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, piece.part.model(), rocketPose, pos);
            }
        }
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private void write(Piece piece, Matrix4f pose) {
        instancePose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        piece.instance.setTransform(instancePose).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 7,
                                pos.getY() - 4,
                                pos.getZ() - 9,
                                pos.getX() + 8,
                                pos.getY() + 58,
                                pos.getZ() + 10)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(tower.instance);
        consumer.accept(support.instance);
        if (rocket != null) for (Piece piece : rocket.pieces) consumer.accept(piece.instance);
    }

    @Override
    protected void _delete() {
        tower.delete();
        support.delete();
        if (rocket != null) rocket.delete();
    }

    private static final class Piece {
        private final MeshPart part;
        private final TransformedInstance instance;

        private Piece(MeshPart part, TransformedInstance instance) {
            this.part = part;
            this.instance = instance;
        }

        private void delete() {
            instance.delete();
        }
    }

    private static final class RocketSet {
        private final int skin;
        private final Piece[] pieces;

        private RocketSet(int skin, Piece[] pieces) {
            this.skin = skin;
            this.pieces = pieces;
        }

        private void delete() {
            for (Piece piece : pieces) piece.delete();
        }
    }
}
