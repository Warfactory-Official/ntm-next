// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.animloader.AnimatedModel;
import com.hbm.animloader.Animation;
import com.hbm.animloader.AnimationWrapper;
import com.hbm.main.ResourceManager;
import com.hbm.render.anim.BusAnimationSequence;
import com.hbm.render.anim.HbmAnimations;
import com.hbm.tileentity.BlockEntityDoorGeneric;
import com.hbm.tileentity.DoorDecl;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Mesh;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.ClipTransformedInstance;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class DoorVisual extends HbmDynamicBlockEntityVisual<BlockEntityDoorGeneric>
        implements ShaderLightVisual, AnimatedModel.NodeVisitor {
    private static final int NONE = 0, HALFSPACE = 1, SLAB = 2;
    private static final DoorAssets SILO_HATCH = new DoorAssets(DoorDecl.SILO_HATCH);
    private static final DoorAssets SILO_HATCH_LARGE = new DoorAssets(DoorDecl.SILO_HATCH_LARGE);
    private static final DoorAssets FIRE_DOOR = new DoorAssets(DoorDecl.FIRE_DOOR);
    private static final DoorAssets SECURE_ACCESS_DOOR =
            new DoorAssets(DoorDecl.SECURE_ACCESS_DOOR);
    private static final DoorAssets QE_SLIDING = new DoorAssets(DoorDecl.QE_SLIDING);
    private static final DoorAssets CARGO_DOOR = new DoorAssets(DoorDecl.CARGO_DOOR);
    private static final DoorAssets WATER_DOOR = new DoorAssets(DoorDecl.WATER_DOOR);
    private static final DoorAssets VAULT_DOOR = new DoorAssets(DoorDecl.VAULT_DOOR);
    private static final DoorAssets SLIDING_SEAL_DOOR = new DoorAssets(DoorDecl.SLIDING_SEAL_DOOR);
    private static final DoorAssets QE_CONTAINMENT = new DoorAssets(DoorDecl.QE_CONTAINMENT);
    private static final DoorAssets ROUND_AIRLOCK_DOOR =
            new DoorAssets(DoorDecl.ROUND_AIRLOCK_DOOR);
    private static final DoorAssets SLIDE_DOOR = new DoorAssets(DoorDecl.SLIDE_DOOR);
    private static final DoorAssets LARGE_VEHICLE_DOOR =
            new DoorAssets(DoorDecl.LARGE_VEHICLE_DOOR);
    private static final DoorAssets TRANSITION_SEAL = new DoorAssets(DoorDecl.TRANSITION_SEAL);
    private static final DoorAssets[] ASSETS = {
        SILO_HATCH,
        SILO_HATCH_LARGE,
        FIRE_DOOR,
        SECURE_ACCESS_DOOR,
        QE_SLIDING,
        CARGO_DOOR,
        WATER_DOOR,
        VAULT_DOOR,
        SLIDING_SEAL_DOOR,
        QE_CONTAINMENT,
        ROUND_AIRLOCK_DOOR,
        SLIDE_DOOR,
        LARGE_VEHICLE_DOOR,
        TRANSITION_SEAL
    };
    private final DoorDecl declaration;
    private final DoorAssets assets;
    private final String[] partNames;
    private final TransformedInstance[] instances;
    private final Model[] models;
    private final boolean[] blended;
    private final Matrix4f[] applied;
    private final boolean[] valid, shown;
    private final int[] clips;
    private final float[] clipValues;
    private final Matrix4f base = new Matrix4f(), bodyPose = new Matrix4f();
    private final Matrix4f partPose = new Matrix4f(), secondPose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f(),
            boundsPose = new Matrix4f(),
            worldPose = new Matrix4f();
    private final float[] origin = new float[3],
            rotation = new float[3],
            translation = new float[3];
    private final double[] lightBounds = new double[6];
    private final AABB bodyBounds;
    private final AnimatedModel.Mesh @Nullable [] armatureMeshes;
    private final AnimatedModel.@Nullable Walk walk;
    private final @Nullable AnimationWrapper playback;
    private HbmAnimations.@Nullable Animation animation;
    private @Nullable BusAnimationSequence firstBus, secondBus;
    private int firstDimension, secondDimension, duration;
    private int skin = Integer.MIN_VALUE, nodeIndex;
    private byte lastState = -1;
    private long lastTime = Long.MIN_VALUE;
    private double lastFirst = Double.NaN, lastSecond = Double.NaN;
    private boolean initialized;
    private AABB renderBounds;
    private @Nullable AABB lastLightBounds;

    public DoorVisual(
            VisualizationContext context, BlockEntityDoorGeneric blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        declaration = blockEntity.getDoorType();
        assets = assets(declaration);
        partNames = declaration.getDynamicParts();
        base.translation(.5F, 0, .5F)
                .rotateY(Facing.yaw(blockEntity.facing(), 90) * Mth.DEG_TO_RAD);
        float[] offset = declaration.getStaticOffset();
        bodyPose.set(base)
                .rotateY(declaration.getStaticYaw() * Mth.DEG_TO_RAD)
                .translate(offset[0], offset[1], offset[2]);
        var source = declaration.getModel();
        AABB extent = new AABB(pos).inflate(1);
        for (String name : declaration.getStaticParts())
            extent = extent.minmax(LightBounds.of(source, name, bodyPose, pos));
        bodyBounds = extent;
        renderBounds = bodyBounds;
        if (declaration == DoorDecl.TRANSITION_SEAL) {
            armatureMeshes = assets.armatureMeshes;
            walk = new AnimatedModel.Walk();
            playback =
                    new AnimationWrapper(0, Seal.ANIMATION).onEnd(AnimationWrapper.EndResult.STAY);
        } else {
            armatureMeshes = null;
            walk = null;
            playback = null;
        }
        int count = armatureMeshes == null ? partNames.length : armatureMeshes.length;
        instances = new TransformedInstance[count];
        models = new Model[count];
        blended = new boolean[count];
        applied = new Matrix4f[count];
        valid = new boolean[count];
        shown = new boolean[count];
        clips = new int[count];
        clipValues = new float[count * 7];
        for (int i = 0; i < count; i++) {
            applied[i] = new Matrix4f();
            clips[i] = clip(declaration, partNames, i);
        }
        installSkins(blockEntity.getSkinIndex());
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    public static void reloadTextures() {
        for (DoorAssets assets : ASSETS) assets.bindTextures();
    }

    private static DoorAssets assets(DoorDecl declaration) {
        if (declaration == DoorDecl.SILO_HATCH) return SILO_HATCH;
        if (declaration == DoorDecl.SILO_HATCH_LARGE) return SILO_HATCH_LARGE;
        if (declaration == DoorDecl.FIRE_DOOR) return FIRE_DOOR;
        if (declaration == DoorDecl.SECURE_ACCESS_DOOR) return SECURE_ACCESS_DOOR;
        if (declaration == DoorDecl.QE_SLIDING) return QE_SLIDING;
        if (declaration == DoorDecl.CARGO_DOOR) return CARGO_DOOR;
        if (declaration == DoorDecl.WATER_DOOR) return WATER_DOOR;
        if (declaration == DoorDecl.VAULT_DOOR) return VAULT_DOOR;
        if (declaration == DoorDecl.SLIDING_SEAL_DOOR) return SLIDING_SEAL_DOOR;
        if (declaration == DoorDecl.QE_CONTAINMENT) return QE_CONTAINMENT;
        if (declaration == DoorDecl.ROUND_AIRLOCK_DOOR) return ROUND_AIRLOCK_DOOR;
        if (declaration == DoorDecl.SLIDE_DOOR) return SLIDE_DOOR;
        if (declaration == DoorDecl.LARGE_VEHICLE_DOOR) return LARGE_VEHICLE_DOOR;
        if (declaration == DoorDecl.TRANSITION_SEAL) return TRANSITION_SEAL;
        throw new IllegalArgumentException();
    }

    private static int clip(DoorDecl declaration, String[] names, int index) {
        if (declaration == DoorDecl.ROUND_AIRLOCK_DOOR
                || declaration == DoorDecl.LARGE_VEHICLE_DOOR) return SLAB;
        if (declaration == DoorDecl.QE_CONTAINMENT || declaration == DoorDecl.SLIDING_SEAL_DOOR)
            return HALFSPACE;
        if (declaration == DoorDecl.SLIDE_DOOR)
            return names[index].endsWith("Lock") ? HALFSPACE : SLAB;
        return NONE;
    }

    private static void collectMeshes(AnimatedModel node, ArrayList<AnimatedModel.Mesh> meshes) {
        if (node.mesh() != null) meshes.add(node.mesh());
        for (var child : node.children) collectMeshes(child, meshes);
    }

    private static Meshes bind(
            Mesh mesh, Identifier texture, int clip, boolean cull, boolean blended) {
        var material =
                SimpleMaterial.builderOf(
                                switch (clip) {
                                    case HALFSPACE -> Materials.CUTOUT_CLIP_HALFSPACE;
                                    case SLAB -> Materials.CUTOUT_CLIP_SLAB;
                                    default -> Materials.CUTOUT;
                                })
                        .texture(texture)
                        .mipmap(false)
                        .backfaceCulling(cull)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .transparency(
                                blended ? Transparency.ORDER_INDEPENDENT : Transparency.OPAQUE)
                        .writeMask(blended ? WriteMask.COLOR : WriteMask.COLOR_DEPTH);
        if (clip == NONE) material.cutout(CutoutShaders.ONE_TENTH);
        var built = material.build();
        return new Meshes(
                blended ? new SingleMeshModel(mesh, built) : MeshPart.create(mesh, built).model());
    }

    private void installSkins(int skin) {
        this.skin = skin;
        for (int i = 0; i < instances.length; i++) {
            if (instances[i] != null) instances[i].delete();
            var meshes = assets.moving[Math.floorMod(skin, assets.skinCount)][i];
            models[i] = meshes.model;
            blended[i] =
                    meshes.model.meshes().getFirst().material().transparency()
                            != Transparency.OPAQUE;
            instances[i] =
                    clips[i] == NONE
                            ? instancerProvider()
                                    .instancer(InstanceTypes.TRANSFORMED, meshes.model)
                                    .createInstance()
                            : instancerProvider()
                                    .instancer(InstanceTypes.CLIP_TRANSFORMED, meshes.model)
                                    .createInstance();
        }
        Arrays.fill(valid, false);
        initialized = false;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        if (skin != blockEntity.getSkinIndex()) installSkins(blockEntity.getSkinIndex());
        byte state = blockEntity.state;
        if (armatureMeshes != null) {
            long now = level.getGameTime() * 50L + (long) (partialTick * 50F);
            long elapsed =
                    state > BlockEntityDoorGeneric.STATE_OPEN
                            ? Math.clamp(now - blockEntity.animStartTime, 0, Seal.ANIMATION.length)
                            : 0;
            if (initialized && elapsed == lastTime && state == lastState) return;
            lastTime = elapsed;
            lastState = state;
            initialized = true;
            playback.startTime = now - elapsed;
            playback.reverse =
                    state == BlockEntityDoorGeneric.STATE_OPEN
                            || state == BlockEntityDoorGeneric.STATE_CLOSING;
            playback.prevFrame = 0;
            walk.root().translation(0, 0, .5F);
            nodeIndex = 0;
            LightBounds.resetBounds(lightBounds, bodyBounds);
            Seal.MODEL.walk(now, playback, walk, this);
            publishBounds();
            return;
        }
        double first, second;
        if (declaration == DoorDecl.SILO_HATCH || declaration == DoorDecl.SILO_HATCH_LARGE) {
            first =
                    switch (state) {
                        case BlockEntityDoorGeneric.STATE_OPENING ->
                                Math.min(
                                        declaration.timeToOpen(),
                                        blockEntity.openTicks + partialTick);
                        case BlockEntityDoorGeneric.STATE_CLOSING ->
                                Math.max(0, blockEntity.openTicks - partialTick);
                        default -> blockEntity.openTicks;
                    };
            second = 0;
        } else {
            var current = blockEntity.currentAnimation;
            if (current != animation) {
                animation = current;
                if (current != null) {
                    String firstName = "DOOR", secondName = "LOCK";
                    firstDimension = 1;
                    secondDimension = 0;
                    if (declaration == DoorDecl.WATER_DOOR) {
                        secondName = "BOLT";
                        secondDimension = 2;
                    } else if (declaration == DoorDecl.VAULT_DOOR) {
                        firstName = "PULL";
                        firstDimension = 2;
                        secondName = "SLIDE";
                    } else if (declaration == DoorDecl.CARGO_DOOR) {
                        firstName = "TOP";
                        secondName = "BOT";
                        secondDimension = 1;
                    }
                    firstBus = current.animation.getBus(firstName);
                    secondBus = current.animation.getBus(secondName);
                    duration = current.animation.getDuration();
                }
            }
            if (current == null)
                first = second = state == BlockEntityDoorGeneric.STATE_OPEN ? 1 : 0;
            else {
                long now = level.getGameTime() * 50L + (long) (partialTick * 50F);
                int time = (int) Math.clamp(now - current.startMillis, 0, duration);
                first = firstBus == null ? 0 : firstBus.value(firstDimension, time);
                second = secondBus == null ? 0 : secondBus.value(secondDimension, time);
            }
        }
        if (initialized && first == lastFirst && second == lastSecond) return;
        lastFirst = first;
        lastSecond = second;
        initialized = true;
        LightBounds.resetBounds(lightBounds, bodyBounds);
        poseParts(first, second);
        publishBounds();
    }

    private void poseParts(double first, double second) {
        partPose.identity();
        if (declaration == DoorDecl.FIRE_DOOR) {
            partPose.rotateY(Mth.HALF_PI)
                    .translate(-.5F, (float) Mth.clamp(first * 2.75, 0, 2.75), 0);
            write(0, partPose);
        } else if (declaration == DoorDecl.SECURE_ACCESS_DOOR) {
            partPose.translate(0, 1 + (float) Mth.clamp(first * 3.5, 0, 3.5), 0);
            write(0, partPose);
        } else if (declaration == DoorDecl.CARGO_DOOR) {
            partPose.translation(0, (float) Mth.clamp(first, 0, 1), 0);
            write(0, partPose);
            partPose.translation(0, (float) Mth.clamp(second, 0, 1) * 2, 0);
            write(1, partPose);
        } else if (declaration == DoorDecl.WATER_DOOR) {
            partPose.translate(.375F, 0, 0)
                    .rotateY(Mth.HALF_PI)
                    .translate(-1.1875F, 0, 0)
                    .rotateY((float) (-first * 120) * Mth.DEG_TO_RAD)
                    .translate(1.1875F, 0, 0);
            write(0, partPose);
            secondPose.set(partPose).translate((float) (-.4 * second), 0, 0);
            write(1, secondPose);
            wheel(2, 2.28125F, second);
            wheel(3, .71875F, second);
        } else if (declaration == DoorDecl.VAULT_DOOR) {
            float slide = (float) (second * 5);
            partPose.translate((float) -first, 0, 0)
                    .translate(0, 0, slide)
                    .translate(0, 2.5F, 0)
                    .rotateX((float) (360D * (second * 5) / (4.25D * Math.PI)) * Mth.DEG_TO_RAD)
                    .translate(0, -2.5F, 0);
            write(0, partPose);
            write(1, partPose);
        } else if (declaration == DoorDecl.QE_SLIDING) {
            float travel = (float) Mth.clamp(first * .95, 0, .95);
            partPose.translation(.53125F, .001F, .5F + travel);
            write(0, partPose);
            partPose.translation(.53125F, .001F, .5F - travel);
            write(1, partPose);
        } else if (declaration == DoorDecl.SLIDING_SEAL_DOOR) {
            float travel = (float) (DoorDecl.smoothstep((float) Mth.clamp(first, 0, 1)) * .9);
            partPose.translation(.5F, 0, 0);
            write(0, partPose, 0, 0, travel, 0, 0, 1, .5001F, false);
        } else if (declaration == DoorDecl.QE_CONTAINMENT) {
            partPose.translation(.25F, 0, 0);
            write(0, partPose, 0, (float) Mth.clamp(first * 2.25, 0, 2.25), 0, 0, 1, 0, 3, false);
        } else if (declaration == DoorDecl.ROUND_AIRLOCK_DOOR) {
            float travel = (float) Mth.clamp(first * 1.5, 0, 1.5);
            partPose.translation(0, 0, .5F);
            write(0, partPose, 0, 0, travel, 0, 0, 1, 1.999F, false);
            write(1, partPose, 0, 0, -travel, 0, 0, 1, 1.999F, false);
        } else if (declaration == DoorDecl.LARGE_VEHICLE_DOOR) {
            float travel = (float) Mth.clamp(first * 3, 0, 3);
            partPose.rotateY(Mth.HALF_PI);
            write(0, partPose, -travel, 0, 0, 1, 0, 0, 3.4375F, false);
            write(1, partPose, travel, 0, 0, 1, 0, 0, 3.4375F, false);
        } else if (declaration == DoorDecl.SLIDE_DOOR) {
            float travel = (float) Mth.clamp(first * 2.125, 0, 2.125);
            blastLeaf(0, travel, second * 90);
            blastLeaf(2, -travel, second * 90);
        } else {
            for (int i = 0; i < partNames.length; i++) {
                declaration.getOrigin(partNames[i], origin);
                declaration.getRotation(partNames[i], (float) first, rotation);
                declaration.getTranslation(partNames[i], (float) first, false, translation);
                partPose.translation(origin[0], origin[1], origin[2]);
                if (rotation[0] != 0) partPose.rotateX(rotation[0] * Mth.DEG_TO_RAD);
                if (rotation[1] != 0) partPose.rotateY(rotation[1] * Mth.DEG_TO_RAD);
                if (rotation[2] != 0) partPose.rotateZ(rotation[2] * Mth.DEG_TO_RAD);
                partPose.translate(
                        -origin[0] + translation[0],
                        -origin[1] + translation[1],
                        -origin[2] + translation[2]);
                write(i, partPose);
            }
        }
    }

    private void wheel(int index, float y, double turn) {
        secondPose
                .set(partPose)
                .translate(.40625F, y, 0)
                .rotateZ((float) (turn * 360) * Mth.DEG_TO_RAD)
                .translate(-.40625F, -y, 0);
        write(index, secondPose);
    }

    private void blastLeaf(int index, float travel, double lock) {
        partPose.identity();
        write(index, partPose, 0, 0, travel, 0, 0, 1, 2.5F, false);
        float swing = (float) Math.toRadians(90 + lock), side = travel < 0 ? -1 : 1;
        partPose.translation(0, 0, travel).translate(0, 1.8125F, 0).rotateX(swing);
        write(
                index + 1,
                partPose,
                0,
                -1.8125F,
                0,
                0,
                side * (float) Math.sin(swing),
                side * (float) Math.cos(swing),
                2.5F - Math.abs(travel),
                false);
    }

    private void write(int index, Matrix4fc pose) {
        write(index, pose, 0, 0, 0, 0, 0, 0, 0, false);
    }

    private void write(
            int index,
            Matrix4fc pose,
            float sx,
            float sy,
            float sz,
            float nx,
            float ny,
            float nz,
            float threshold,
            boolean hidden) {
        var instance = instances[index];
        if (hidden) {
            if (!valid[index] || shown[index]) {
                instance.setVisible(false);
                shown[index] = false;
                valid[index] = true;
            }
            return;
        }
        localPose.set(base).mul(pose);
        boundsPose.set(localPose).translate(sx, sy, sz);
        LightBounds.includeLightBounds(lightBounds, models[index], boundsPose, pos);
        int at = index * 7;
        if (valid[index]
                && shown[index]
                && applied[index].equals(localPose)
                && clipValues[at] == sx
                && clipValues[at + 1] == sy
                && clipValues[at + 2] == sz
                && clipValues[at + 3] == nx
                && clipValues[at + 4] == ny
                && clipValues[at + 5] == nz
                && clipValues[at + 6] == threshold) return;
        instance.setVisible(true);
        worldPose.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(localPose);
        instance.setTransform(worldPose).light(0);
        if (instance instanceof ClipTransformedInstance clipped)
            clipped.setSlide(sx, sy, sz).setPlane(nx, ny, nz, threshold);
        instance.setChanged();
        applied[index].set(localPose);
        shown[index] = true;
        valid[index] = true;
        clipValues[at] = sx;
        clipValues[at + 1] = sy;
        clipValues[at + 2] = sz;
        clipValues[at + 3] = nx;
        clipValues[at + 4] = ny;
        clipValues[at + 5] = nz;
        clipValues[at + 6] = threshold;
    }

    @Override
    public void node(AnimatedModel.Mesh mesh, Matrix4fc pose, boolean hidden) {
        assert armatureMeshes[nodeIndex] == mesh;
        write(nodeIndex++, pose, 0, 0, 0, 0, 0, 0, 0, hidden);
    }

    private void publishBounds() {
        AABB next = LightBounds.sectionBounds(lightBounds, renderBounds);
        if (next != renderBounds) {
            renderBounds = next;
            refreshVisibleBounds();
        }
        lastLightBounds = LightBounds.sections(lightSections, lightBounds, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return renderBounds;
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (int i = 0; i < instances.length; i++) if (!blended[i]) consumer.accept(instances[i]);
    }

    @Override
    protected void _delete() {
        for (var instance : instances) instance.delete();
    }

    private record Meshes(Model model) {}

    private static final class DoorAssets {
        final DoorDecl declaration;
        final int skinCount;
        final Mesh[] movingMeshes;
        final AnimatedModel.Mesh @Nullable [] armatureMeshes;
        final Meshes[][] moving;
        final Meshes[][][] movingVariants;

        DoorAssets(DoorDecl declaration) {
            this.declaration = declaration;
            skinCount = Math.max(1, declaration.getSkinCount());
            var source = declaration.getModel();
            if (declaration == DoorDecl.TRANSITION_SEAL) {
                var parts = new ArrayList<AnimatedModel.Mesh>();
                collectMeshes(Seal.MODEL, parts);
                armatureMeshes = parts.toArray(AnimatedModel.Mesh[]::new);
                movingMeshes = new Mesh[armatureMeshes.length];
                for (int i = 0; i < movingMeshes.length; i++)
                    movingMeshes[i] = PackedQuadMesh.of(armatureMeshes[i]);
            } else {
                armatureMeshes = null;
                movingMeshes = new Mesh[declaration.getDynamicParts().length];
                int[] ids = declaration.getDynamicPartIds();
                for (int i = 0; i < movingMeshes.length; i++)
                    movingMeshes[i] = PackedQuadMesh.of(source.groups[ids[i]], source.smoothing());
            }
            moving = new Meshes[skinCount][movingMeshes.length];
            movingVariants = new Meshes[skinCount][movingMeshes.length][2];
            buildBindings();
            bindTextures();
        }

        void buildBindings() {
            boolean cull =
                    declaration != DoorDecl.ROUND_AIRLOCK_DOOR
                            && declaration != DoorDecl.LARGE_VEHICLE_DOOR
                            && declaration != DoorDecl.QE_SLIDING
                            && declaration != DoorDecl.SLIDE_DOOR;
            String[] movingNames = declaration.getDynamicParts();
            for (int skin = 0; skin < skinCount; skin++) {
                for (int i = 0; i < movingMeshes.length; i++) {
                    Identifier texture =
                            armatureMeshes != null
                                    ? ResourceManager.transition_seal_tex
                                    : declaration.getTextureForPart(skin, movingNames[i]);
                    int clip = clip(declaration, movingNames, i);
                    movingVariants[skin][i][0] = bind(movingMeshes[i], texture, clip, cull, false);
                    movingVariants[skin][i][1] = bind(movingMeshes[i], texture, clip, cull, true);
                }
            }
        }

        void bindTextures() {
            boolean blend =
                    declaration == DoorDecl.SILO_HATCH
                            || declaration == DoorDecl.SILO_HATCH_LARGE
                            || declaration == DoorDecl.TRANSITION_SEAL;
            String[] movingNames = declaration.getDynamicParts();
            for (int skin = 0; skin < skinCount; skin++) {
                for (int i = 0; i < movingMeshes.length; i++) {
                    Identifier texture =
                            armatureMeshes != null
                                    ? ResourceManager.transition_seal_tex
                                    : declaration.getTextureForPart(skin, movingNames[i]);
                    moving[skin][i] =
                            movingVariants[skin][i][
                                    blend && VisualTextures.translucentTexture(texture) ? 1 : 0];
                }
            }
        }
    }

    private static final class Seal {
        static final AnimatedModel MODEL = ResourceManager.transition_seal();
        static final Animation ANIMATION = ResourceManager.transition_seal_anim();
    }
}
