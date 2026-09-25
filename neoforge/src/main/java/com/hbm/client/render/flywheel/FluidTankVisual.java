// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.inventory.fluid.NTMFluidProperties;
import com.hbm.inventory.fluid.NTMFluidProperty;
import com.hbm.inventory.fluid.NTMFluids;
import com.hbm.inventory.fluid.trait.FT_Corrosive;
import com.hbm.inventory.fluid.trait.FluidTraitSimple.FT_Amat;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.tileentity.machine.storage.BlockEntityMachineFluidTank;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.level.material.Fluid;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class FluidTankVisual extends HbmDynamicBlockEntityVisual<BlockEntityMachineFluidTank>
        implements ShaderLightVisual {
    private static final Identifier NONE_TEXTURE =
            Library.id("textures/block/models/tank/tank_none.png");
    private static final HFRWavefrontObject[] SOURCES = {
        ResourceManager.fluidtank, ResourceManager.fluidtank_exploded
    };
    private static final PackedQuadMesh[] TANK_MESH = {
        PackedQuadMesh.of(SOURCES[0].groups[SOURCES[0].partId("Tank")], SOURCES[0].smoothing()),
        PackedQuadMesh.of(SOURCES[1].groups[SOURCES[1].partId("Tank")], SOURCES[1].smoothing())
    };
    private static final Map<Fluid, Identifier> TEXTURES = new ConcurrentHashMap<>();
    private static final Map<BodyKey, Model> MODELS = new ConcurrentHashMap<>();

    private final HazardDiamondVisual hazards;
    private final Matrix4f base = new Matrix4f();
    private final Matrix4f[] hazardPoses = {new Matrix4f(), new Matrix4f()};
    private @Nullable TransformedInstance body;
    private AABB bodyBounds;
    private @Nullable BodyKey bodyKey;
    private @Nullable Fluid bodyFluid;
    private @Nullable AABB lastLightBounds;
    private @Nullable NTMFluidProperty lastProperty;
    private boolean initialized;

    public FluidTankVisual(
            VisualizationContext context,
            BlockEntityMachineFluidTank blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        hazards = new HazardDiamondVisual(context, level, pos, 2);
        base.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(blockState.getValue(BlockMultiblockCore.FACING), 180)
                                * Mth.DEG_TO_RAD);
        bodyFluid = blockEntity.tank.getTankType();
        installBody(new BodyKey(texture(bodyFluid), blockEntity.isDamaged()));
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    public static void reloadTextures() {
        TEXTURES.clear();
    }

    private static Identifier texture(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return NONE_TEXTURE;
        return TEXTURES.computeIfAbsent(fluid, FluidTankVisual::bodyTexture);
    }

    private static Identifier bodyTexture(@Nullable Fluid fluid) {
        if (fluid == null || fluid == Fluids.EMPTY) return NONE_TEXTURE;
        FT_Corrosive corrosive = NTMFluidProperties.getTrait(fluid, FT_Corrosive.class);
        boolean danger =
                NTMFluidProperties.hasTrait(fluid, FT_Amat.class)
                        || corrosive != null && corrosive.isHighlyCorrosive();
        String name = danger ? "danger" : NTMFluids.spritePath(fluid);
        Identifier texture = Library.id("textures/models/tank/tank_" + name + ".png");
        return Minecraft.getInstance().getResourceManager().getResource(texture).isPresent()
                ? texture
                : NONE_TEXTURE;
    }

    private static Model model(BodyKey key) {
        return new SingleMeshModel(
                TANK_MESH[key.damaged() ? 1 : 0],
                SimpleMaterial.builderOf(MeshPart.litCutout(key.texture()))
                        .backfaceCulling(false)
                        .build());
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        Fluid type = blockEntity.tank.getTankType();
        boolean damaged = blockEntity.isDamaged();
        boolean bodyChanged = type != bodyFluid || damaged != bodyKey.damaged();
        if (bodyChanged) {
            bodyFluid = type;
            var key = new BodyKey(texture(type), damaged);
            if (!key.equals(bodyKey)) installBody(key);
        }

        NTMFluidProperty prop = type == null ? null : NTMFluidProperties.get(type);
        if (initialized && !bodyChanged && prop == lastProperty) return;
        lastProperty = prop;
        lastLightBounds = LightBounds.sections(lightSections, bodyBounds, lastLightBounds);
        if (prop == null) {
            hazards.hide(0);
            hazards.hide(1);
            initialized = true;
            return;
        }
        for (int side = -1; side <= 1; side += 2) {
            int index = side == -1 ? 0 : 1;
            hazardPoses[index]
                    .set(base)
                    .translate(.25F * side, .5F, 1.501F * side)
                    .rotateY((-90F * side) * Mth.DEG_TO_RAD)
                    .scale(1F, .375F, .375F);
            hazards.update(
                    index,
                    hazardPoses[index],
                    prop.nfpaHealth(),
                    prop.nfpaFlame(),
                    prop.nfpaReact(),
                    prop.symbol());
        }
        initialized = true;
    }

    private void installBody(BodyKey key) {
        if (body != null) body.delete();
        body =
                instancerProvider()
                        .instancer(
                                InstanceTypes.TRANSFORMED,
                                MODELS.computeIfAbsent(key, FluidTankVisual::model))
                        .createInstance();
        var world =
                new Matrix4f()
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(base);
        body.setTransform(world).light(0).setChanged();
        var source = SOURCES[key.damaged() ? 1 : 0];
        bodyBounds =
                LightBounds.of(source, "Tank", base, pos)
                        .minmax(LightBounds.of(source, "Frame", base, pos));
        if (key.damaged())
            bodyBounds = bodyBounds.minmax(LightBounds.of(source, "TankInner", base, pos));
        bodyKey = key;
        refreshVisibleBounds();
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                        pos.getX() - 3,
                        pos.getY(),
                        pos.getZ() - 3,
                        pos.getX() + 4,
                        pos.getY() + 4,
                        pos.getZ() + 4));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(body);
    }

    @Override
    protected void _delete() {
        body.delete();
        hazards.delete();
    }

    private record BodyKey(Identifier texture, boolean damaged) {}
}
