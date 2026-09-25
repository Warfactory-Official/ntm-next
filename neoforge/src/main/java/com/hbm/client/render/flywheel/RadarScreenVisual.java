// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.api.entity.RadarEntry;
import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.gui.ScreenMachineRadar;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.machine.BlockEntityMachineRadarScreen;
import com.hbm.util.Facing;
import com.hbm.util.GameTime;
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
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import it.unimi.dsi.fastutil.HashCommon;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;

public final class RadarScreenVisual
        extends HbmDynamicBlockEntityVisual<BlockEntityMachineRadarScreen>
        implements ShaderLightVisual {
    private static final float PLANE_X = .38F;
    private static final long BAR_PERIOD = 56L;
    private static final float BAR_SPEED = 30F;
    private static final float BAR_TOP = 2F;
    private static final float BAR_HEIGHT = .125F;
    private static final float FACE_MIN = -.375F;
    private static final float FACE_MAX = 1.375F;
    private static final double SCOPE_SPAN = .875D;
    private static final double BLIP_HALF = .0625D;
    private static final int STATIC_V = 118;
    private static final int STATIC_ROWS = 81;
    private static final float STATIC_HEIGHT = 40F;
    private static final int BAR_TOP_COLOR = 0x0000FF00;
    private static final int BAR_BOTTOM_COLOR = 0x3200FF00;
    private static final ScreenAssets SCREEN = buildScreenAssets();
    private static final Model BLIP_MODEL = new SingleMeshModel(quad(), blipMaterial());
    private final TransformedInstance bar;
    private final UvTransformedInstance staticPanel;
    private final ArrayList<UvTransformedInstance> blips = new ArrayList<>();
    private final Matrix4f basePose = new Matrix4f();
    private final Matrix4f staticPose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private double[] lastBlipX = new double[0];
    private double[] lastBlipZ = new double[0];
    private int[] lastBlipLevel = new int[0];
    private boolean[] lastBlipVisible = new boolean[0];
    private float lastBarOffset = Float.NaN;
    private int lastStaticV = Integer.MIN_VALUE;
    private boolean lastLinked;
    private boolean initialized;

    public RadarScreenVisual(
            VisualizationContext context,
            BlockEntityMachineRadarScreen blockEntity,
            float partialTick) {
        super(context, blockEntity, partialTick);
        Direction bodyFacing = BlockMultiblockCore.coreFacing(blockState);
        basePose.translate(.5F, 0F, .5F).rotateY(Facing.yaw(bodyFacing, 90) * Mth.DEG_TO_RAD);
        staticPose
                .set(basePose)
                .translate(PLANE_X, .125F, FACE_MIN)
                .scale(1F, 1.75F, FACE_MAX - FACE_MIN);
        bar = instancerProvider().instancer(InstanceTypes.TRANSFORMED, SCREEN.bar).createInstance();
        staticPanel =
                instancerProvider()
                        .instancer(InstanceTypes.UV_TRANSFORMED, SCREEN.panel.model())
                        .createInstance();
        updateMovingParts(partialTick);
    }

    public static void initModels() {}

    private static ScreenAssets buildScreenAssets() {
        Material barMaterial =
                SimpleMaterial.builder()
                        .texture(ResourceManager.white_tex)
                        .mipmap(false)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .backfaceCulling(false)
                        .build();
        Material staticMaterial =
                SimpleMaterial.builderOf(MeshPart.litCutout(ScreenMachineRadar.TEXTURE))
                        .backfaceCulling(false)
                        .build();
        PackedQuadMesh barMesh =
                PackedQuadMesh.builder(1)
                        .normal(0F, 0F, 1F)
                        .vertex(PLANE_X, 0F, FACE_MAX, 0F, 0F, BAR_TOP_COLOR)
                        .vertex(PLANE_X, 0F, FACE_MIN, 0F, 0F, BAR_TOP_COLOR)
                        .vertex(PLANE_X, -BAR_HEIGHT, FACE_MIN, 0F, 0F, BAR_BOTTOM_COLOR)
                        .vertex(PLANE_X, -BAR_HEIGHT, FACE_MAX, 0F, 0F, BAR_BOTTOM_COLOR)
                        .build();
        return new ScreenAssets(
                new MeshPart(new SingleMeshModel(quad(), staticMaterial)),
                new SingleMeshModel(barMesh, barMaterial));
    }

    private static Material blipMaterial() {
        return SimpleMaterial.builder()
                .texture(ScreenMachineRadar.TEXTURE)
                .mipmap(false)
                .cutout(CutoutShaders.ONE_TENTH)
                .light(LightShaders.SMOOTH)
                .ambientOcclusion(false)
                .cardinalLightingMode(CardinalLightingMode.CHUNK)
                .writeMask(WriteMask.COLOR)
                .backfaceCulling(false)
                .build();
    }

    private static PackedQuadMesh quad() {
        return PackedQuadMesh.builder(1)
                .vertex(0F, 1F, 1F, 0F, 1F, -1)
                .vertex(0F, 1F, 0F, 1F, 1F, -1)
                .vertex(0F, 0F, 0F, 1F, 0F, -1)
                .vertex(0F, 0F, 1F, 0F, 0F, -1)
                .build();
    }

    @Override
    protected void frame(Context context) {
        updateMovingParts(context.partialTick());
    }

    public void updateMovingParts(float partialTick) {
        boolean linked = blockEntity.linked;
        float barOffset = ((level.getGameTime() % BAR_PERIOD) + partialTick) / BAR_SPEED;

        int staticV =
                STATIC_V
                        + Math.floorMod(HashCommon.mix(GameTime.now() ^ pos.asLong()), STATIC_ROWS);
        boolean linkedChanged = !initialized || linked != lastLinked;
        boolean barChanged =
                !initialized
                        || Float.floatToIntBits(barOffset) != Float.floatToIntBits(lastBarOffset);
        if (linked) {
            if (linkedChanged) {
                bar.setVisible(true);
                staticPanel.setVisible(false);
            }
            if (barChanged || linkedChanged) {
                localPose.set(basePose).translate(0F, BAR_TOP - barOffset, 0F);
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);
                bar.setTransform(instancePose).light(0).setChanged();
            }
        } else {
            if (linkedChanged) {
                bar.setVisible(false);
                staticPanel.setVisible(true);
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(staticPose);
                staticPanel.setTransform(instancePose).light(0);
            }
            if (linkedChanged || staticV != lastStaticV) {
                staticPanel
                        .uvRegion(216F / 256F, staticV / 256F, 40F / 256F, STATIC_HEIGHT / 256F)
                        .setChanged();
            }
        }

        int count = blockEntity.entries.size();
        while (blips.size() < count)
            blips.add(
                    instancerProvider()
                            .instancer(InstanceTypes.UV_TRANSFORMED, BLIP_MODEL)
                            .createInstance());
        if (lastBlipVisible.length < blips.size()) {
            int size = blips.size();
            lastBlipX = Arrays.copyOf(lastBlipX, size);
            lastBlipZ = Arrays.copyOf(lastBlipZ, size);
            lastBlipLevel = Arrays.copyOf(lastBlipLevel, size);
            lastBlipVisible = Arrays.copyOf(lastBlipVisible, size);
        }
        for (int i = 0; i < blips.size(); i++) {
            UvTransformedInstance instance = blips.get(i);
            if (!linked || i >= count) {
                if (!initialized || lastBlipVisible[i]) instance.setVisible(false);
                lastBlipVisible[i] = false;
                continue;
            }
            RadarEntry entry = blockEntity.entries.get(i);
            double x =
                    (entry.posX - blockEntity.refX)
                            / ((double) blockEntity.range + 1D)
                            * SCOPE_SPAN;
            double z =
                    (entry.posZ - blockEntity.refZ)
                            / ((double) blockEntity.range + 1D)
                            * SCOPE_SPAN;
            boolean changed =
                    !lastBlipVisible[i]
                            || Double.doubleToLongBits(x) != Double.doubleToLongBits(lastBlipX[i])
                            || Double.doubleToLongBits(z) != Double.doubleToLongBits(lastBlipZ[i])
                            || entry.blipLevel != lastBlipLevel[i];
            if (changed) {
                localPose
                        .set(basePose)
                        .translate(
                                PLANE_X,
                                (float) (1F - z - BLIP_HALF),
                                (float) (.5F - x - BLIP_HALF))
                        .scale(1F, (float) (BLIP_HALF * 2D), (float) (BLIP_HALF * 2D));
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(localPose);
                instance.setVisible(true);
                instance.setTransform(instancePose).light(0);
                instance.uvRegion(216F / 256F, entry.blipLevel * 8F / 256F, 8F / 256F, 8F / 256F)
                        .setChanged();
            }
            lastBlipVisible[i] = true;
            lastBlipX[i] = x;
            lastBlipZ[i] = z;
            lastBlipLevel[i] = entry.blipLevel;
        }
        lastBarOffset = barOffset;
        lastStaticV = staticV;
        lastLinked = linked;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return new AABB(
                        pos.getX() - 1,
                        pos.getY(),
                        pos.getZ() - 1,
                        pos.getX() + 2,
                        pos.getY() + 2,
                        pos.getZ() + 2)
                .inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        consumer.accept(staticPanel);
        for (var blip : blips) consumer.accept(blip);
    }

    @Override
    protected void _delete() {
        bar.delete();
        staticPanel.delete();
        for (var blip : blips) blip.delete();
    }

    private record ScreenAssets(MeshPart panel, Model bar) {}
}
