// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineHephaestus;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class HephaestusVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineHephaestus>
        implements ShaderLightVisual {
    private static final Identifier COBBLESTONE =
            Identifier.withDefaultNamespace("textures/block/cobblestone.png");
    private static final HFRWavefrontObject MODEL = ResourceManager.hephaestus;
    private static final Material ROTOR_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.hephaestus_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final Material ACTIVE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT)
                    .texture(ResourceManager.hephaestus_lava_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.NONE)
                    .useLight(false)
                    .useOverlay(false)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .build();
    private static final MeshPart ROTOR_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Rotor")], MODEL.smoothing(), ROTOR_MATERIAL);
    private static final MeshPart IDLE_CORE_PART =
            MeshPart.obj(
                    MODEL.groups[MODEL.partId("Core")], false, MeshPart.litCutout(COBBLESTONE));
    private static final MeshPart ACTIVE_CORE_PART =
            MeshPart.obj(MODEL.groups[MODEL.partId("Core")], false, ACTIVE_MATERIAL);
    private final TransformedInstance[] rotors = new TransformedInstance[3];
    private final UvTransformedInstance idleCore;
    private final UvTransformedInstance activeCore;
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Matrix4f corePose = new Matrix4f().translation(.5F, 0F, .5F);
    private final Matrix4f basePose = new Matrix4f().translation(.5F, 0F, .5F);
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB rawBodyBounds;
    private float lastRotation = Float.NaN;
    private boolean lastActive;
    private boolean initialized;
    private @Nullable AABB lastLightBounds;

    public HephaestusVisual(
            VisualizationContext context,
            BlockEntityMachineHephaestus blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        rawBodyBounds = new AABB(pos).minmax(LightBounds.of(MODEL, "Main", basePose, pos));
        var rotorInstancer =
                instancerProvider().instancer(InstanceTypes.TRANSFORMED, ROTOR_PART.model());
        for (int i = 0; i < rotors.length; i++) rotors[i] = rotorInstancer.createInstance();
        idleCore =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, IDLE_CORE_PART.model())
                        .createInstance();
        activeCore =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, ACTIVE_CORE_PART.model())
                        .createInstance();
        writeFrame(partialTick);
    }

    public static void initModels() {}

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        float rotation = Mth.lerp(partialTick, blockEntity.prevRot, blockEntity.rot);
        boolean active = blockEntity.bufferedHeat > 0;
        boolean rotationChanged = !initialized || Float.compare(rotation, lastRotation) != 0;
        boolean activeChanged = !initialized || active != lastActive;
        if (!rotationChanged && !activeChanged) return;
        lastRotation = rotation;
        lastActive = active;
        initialized = true;
        LightBounds.resetBounds(lightBoundsAccumulator, rawBodyBounds);
        for (int i = 0; i < rotors.length; i++) {
            localPose.set(basePose).rotateY((rotation + i * 120F) * Mth.DEG_TO_RAD);
            if (rotationChanged) {
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);
                rotors[i].setTransform(instancePose).light(0).setChanged();
            }
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, ROTOR_PART.model(), localPose, pos);
        }

        if (activeChanged) {
            idleCore.setVisible(!active);
            activeCore.setVisible(active);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(corePose);
            if (active)
                activeCore
                        .setTransform(instancePose)
                        .colorArgb(0xFFFFFFFF)
                        .light(LightCoordsUtil.FULL_BRIGHT);
            else idleCore.setTransform(instancePose).colorArgb(0xFF808080).light(0);
        }
        UvTransformedInstance core = active ? activeCore : idleCore;
        core.uvRegion(0F, rotation / 20F, .5F, .5F);
        core.setChanged();
        if (!active)
            LightBounds.includeLightBounds(
                    lightBoundsAccumulator, IDLE_CORE_PART.model(), corePose, pos);
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return rawBodyBounds.minmax(
                new AABB(
                                pos.getX() - 3,
                                pos.getY(),
                                pos.getZ() - 3,
                                pos.getX() + 4,
                                pos.getY() + 12,
                                pos.getZ() + 4)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var rotor : rotors) consumer.accept(rotor);
        consumer.accept(idleCore);
    }

    @Override
    protected void _delete() {
        for (var rotor : rotors) rotor.delete();
        idleCore.delete();
        activeCore.delete();
    }
}
