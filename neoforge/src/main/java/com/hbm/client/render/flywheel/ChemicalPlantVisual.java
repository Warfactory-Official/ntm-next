// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.FluidStackNTM;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.recipes.loader.GenericRecipe;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.BlockEntityMachineChemicalPlant;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.instance.UvTransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.function.Consumer;
import net.minecraft.util.ARGB;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class ChemicalPlantVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineChemicalPlant>
        implements ShaderLightVisual {
    private static final int FRAME = ResourceManager.chemical_plant.partId("Frame");
    private static final int SLIDER = ResourceManager.chemical_plant.partId("Slider");
    private static final int SPINNER = ResourceManager.chemical_plant.partId("Spinner");
    private static final int FLUID = ResourceManager.chemical_plant.partId("Fluid");
    private static final float BASE_YAW = 90F;
    private static final int NO_TYPE_COLOR = 0x888888;
    private static final Material FLUID_MATERIAL =
            SimpleMaterial.builderOf(Materials.TRANSLUCENT_NO_DEPTH_WRITE)
                    .texture(ResourceManager.chemical_plant_fluid_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.TINY)
                    .transparency(Transparency.ORDER_INDEPENDENT)
                    .writeMask(WriteMask.COLOR)
                    .useLight(true)
                    .light(LightShaders.SMOOTH)
                    .useOverlay(false)
                    .cardinalLightingMode(CardinalLightingMode.OFF)
                    .ambientOcclusion(false)
                    .backfaceCulling(true)
                    .build();
    private static final HFRWavefrontObject MODEL = ResourceManager.chemical_plant;
    private static final Material BODY_MATERIAL =
            MeshPart.litCutout(ResourceManager.chemical_plant_tex);
    private static final MeshPart[] PARTS = {
        MeshPart.obj(MODEL.groups[FRAME], MODEL.smoothing(), BODY_MATERIAL),
        MeshPart.obj(MODEL.groups[SLIDER], MODEL.smoothing(), BODY_MATERIAL),
        MeshPart.obj(MODEL.groups[SPINNER], MODEL.smoothing(), BODY_MATERIAL)
    };
    private static final Model FLUID_PART =
            new SingleMeshModel(
                    PackedQuadMesh.of(MODEL.groups[FLUID], MODEL.smoothing()), FLUID_MATERIAL);
    private final TransformedInstance[] instances;
    private final AABB bodyBounds;
    private final UvTransformedInstance fluid;
    private final Matrix4f[] local = {new Matrix4f(), new Matrix4f(), new Matrix4f()};
    private final Matrix4f fluidPose = new Matrix4f();
    private final Matrix4f world = new Matrix4f();
    private final float[] rgb = new float[3];
    private double lastAnim = Double.NaN;
    private boolean lastFrame;
    private boolean lastProgressing;
    private @Nullable GenericRecipe lastRecipe;
    private int fluidColor;
    private int lastFluidColor;
    private float lastFluidU;
    private float lastFluidV;
    private boolean lastFluidVisible;
    private boolean initialized;

    public ChemicalPlantVisual(
            VisualizationContext context,
            BlockEntityMachineChemicalPlant blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Matrix4f bodyLocal =
                new Matrix4f()
                        .translation(.5F, 0F, .5F)
                        .rotateY(
                                (BASE_YAW
                                                + Facing.yaw(
                                                        BlockMultiblockCore.coreFacing(
                                                                blockEntity.getBlockState()),
                                                        0))
                                        * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(MODEL, "Base", bodyLocal, pos);
        instances = new TransformedInstance[PARTS.length];
        for (int i = 0; i < PARTS.length; i++)
            instances[i] =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, PARTS[i].model())
                            .createInstance();
        fluid =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, FLUID_PART)
                        .createInstance();
        local[0].set(bodyLocal);
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static double sps(double x) {
        return Math.sin(Math.PI / 2D * Math.cos(x));
    }

    private static int sumChannels(FluidStackNTM[] stacks, float[] rgb) {
        for (FluidStackNTM stack : stacks) {
            NTMFluidProperty property = NTMFluidProperties.get(stack.type());
            int color = property == null ? NO_TYPE_COLOR : property.colorARGB();
            rgb[0] += ARGB.red(color);
            rgb[1] += ARGB.green(color);
            rgb[2] += ARGB.blue(color);
        }
        return stacks.length;
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        float anim = Mth.lerp(partialTick, blockEntity.prevAnim, blockEntity.anim);
        boolean animChanged = !initialized || anim != lastAnim;
        boolean frame = blockEntity.frame;
        if (!initialized || frame != lastFrame) {
            write(0, local[0], frame);
            lastFrame = frame;
        }
        if (animChanged) {
            local[1].set(local[0]).translate((float) (sps(anim * .125D) * .375D), 0F, 0F);
            local[2].set(local[0])
                    .translate(.5F, 0F, .5F)
                    .rotateY((float) ((anim * 15D) % 360D) * Mth.DEG_TO_RAD)
                    .translate(-.5F, 0F, -.5F);
            fluidPose.set(local[0]);
            write(1, local[1], true);
            write(2, local[2], true);
            lastAnim = anim;
        }
        boolean progressing = blockEntity.isProgressing;
        GenericRecipe recipe = progressing ? blockEntity.module.getRecipe() : null;
        if (!initialized || progressing != lastProgressing || recipe != lastRecipe) {
            fluidColor = 0;
            if (recipe != null) {
                rgb[0] = rgb[1] = rgb[2] = 0F;
                int colors = sumChannels(recipe.outputFluid, rgb);
                if (colors == 0) colors = sumChannels(recipe.inputFluid, rgb);
                if (colors > 0)
                    fluidColor =
                            ARGB.colorFromFloat(
                                    .5F,
                                    rgb[0] / 255F / colors,
                                    rgb[1] / 255F / colors,
                                    rgb[2] / 255F / colors);
            }
        }
        int color = fluidColor;
        boolean visible = color != 0;
        float u = visible ? -anim / 100F : 0F;
        float v = visible ? (float) (sps(anim * .1D) * .1D - .25D) : 0F;
        if (!initialized
                || visible != lastFluidVisible
                || color != lastFluidColor
                || u != lastFluidU
                || v != lastFluidV) {
            fluid.setVisible(visible);
            if (visible) {
                world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(fluidPose);
                fluid.setTransform(world).light(0).colorArgb(color);
                fluid.uvRegion(u, v, 1F, 1F).setChanged();
            }
            lastFluidVisible = visible;
            lastFluidColor = color;
            lastFluidU = u;
            lastFluidV = v;
        }
        lastProgressing = progressing;
        lastRecipe = recipe;
        initialized = true;
    }

    private void write(int index, Matrix4f pose, boolean visible) {
        TransformedInstance instance = instances[index];
        instance.setVisible(visible);
        if (!visible) return;
        world.translation(visualPos.getX(), visualPos.getY(), visualPos.getZ()).mul(pose);
        instance.setTransform(world).light(0).setChanged();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 3,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (TransformedInstance instance : instances) consumer.accept(instance);
    }

    @Override
    protected void _delete() {
        for (TransformedInstance instance : instances) instance.delete();
        fluid.delete();
    }
}
