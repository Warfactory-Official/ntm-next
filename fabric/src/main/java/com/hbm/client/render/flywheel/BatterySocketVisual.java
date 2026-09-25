// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.FramedItem;
import com.hbm.client.render.RenderBatterySocket;
import com.hbm.items.machine.EnumBatteryPack;
import com.hbm.items.machine.ItemBatteryCreative;
import com.hbm.items.machine.ItemBatteryPack;
import com.hbm.items.machine.ItemBatterySC;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.render.util.BeamPronter.EnumWaveType;
import com.hbm.tileentity.machine.storage.BlockEntityBatterySocket;
import com.hbm.util.Facing;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.Random;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.util.Mth;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;

public final class BatterySocketVisual extends HbmDynamicBlockEntityVisual<BlockEntityBatterySocket>
        implements ShaderLightVisual {
    private static final double UNICORN_SCALE = .75D;
    private static final float UNICORN_DEGREES_PER_TICK = 25F;
    private static final double ARC_OFFSET = .4375D;
    private static final double ARC_HEIGHT = 1.1875D;
    private static final int ARC_OUTER = 0x404040;
    private static final int ARC_INNER = 0x002040;
    private static final int ARC_COUNT = 8;
    private static final HFRWavefrontObject SOCKET_MODEL = ResourceManager.battery_socket;
    private static final HFRWavefrontObject HORSE_MODEL = ResourceManager.horse;
    private static final Material SOCKET_MATERIAL =
            MeshPart.litCutout(ResourceManager.battery_socket_tex);
    private static final Material HORSE_MATERIAL =
            SimpleMaterial.builderOf(Materials.CUTOUT_NO_CULL)
                    .texture(ResourceManager.horse_sunburst_tex)
                    .mipmap(false)
                    .cutout(CutoutShaders.ONE_TENTH)
                    .light(LightShaders.SMOOTH)
                    .ambientOcclusion(false)
                    .cardinalLightingMode(CardinalLightingMode.CHUNK)
                    .build();
    private static final MeshPart SUPPORTS_PART =
            MeshPart.obj(
                    SOCKET_MODEL.groups[SOCKET_MODEL.partId("Supports")],
                    SOCKET_MODEL.smoothing(),
                    SOCKET_MATERIAL);
    private static final String[] HORSE_NAMES = {
        "Body",
        "Head",
        "Mane",
        "NoseFemale",
        "HornPointy",
        "LeftFrontLeg",
        "RightFrontLeg",
        "LeftBackLeg",
        "RightBackLeg",
        "Tail"
    };
    private static final MeshPart[] BATTERY_PARTS = buildBatteryParts();
    private static final MeshPart[] HORSE_PARTS = buildHorseParts();
    private final AABB bodyBounds;
    private final PartVisual supports;
    private final PartVisual[] batteries;
    private final PartVisual[] horse;
    private final WorldItem item;
    private final PoseStack itemPoses = new PoseStack();
    private final BeamVisual[] arcs = new BeamVisual[ARC_COUNT];
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f horsePose = new Matrix4f();
    private final Matrix4f arcPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private final Vec3[] arcSkeletons = {
        new Vec3(-ARC_OFFSET, ARC_HEIGHT, -ARC_OFFSET),
        new Vec3(-ARC_OFFSET, ARC_HEIGHT, ARC_OFFSET),
        new Vec3(ARC_OFFSET, ARC_HEIGHT, -ARC_OFFSET),
        new Vec3(ARC_OFFSET, ARC_HEIGHT, ARC_OFFSET)
    };
    private final Random arcRandom = new Random();
    private int lastBody = -1;
    private boolean lastFrame;
    private long lastArcBucket = Long.MIN_VALUE;
    private int lastArcPhase = Integer.MIN_VALUE;
    private int lastArcUsed;
    private boolean lastUnicorn;
    private boolean lastGeneric;
    private boolean initialized;

    public BatterySocketVisual(
            VisualizationContext context, BlockEntityBatterySocket blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        Direction bodyFacing = blockEntity.getBlockState().getValue(BlockMultiblockCore.FACING);
        rootPose.translation(.5F, 0F, .5F)
                .rotateY(Facing.yaw(bodyFacing, 90) * Mth.DEG_TO_RAD)
                .translate(-.5F, 0F, .5F);
        bodyBounds = LightBounds.of(SOCKET_MODEL, "Socket", rootPose, pos);
        supports = new PartVisual(SUPPORTS_PART);
        batteries = new PartVisual[BATTERY_PARTS.length];
        for (int i = 0; i < batteries.length; i++) batteries[i] = new PartVisual(BATTERY_PARTS[i]);
        horse = new PartVisual[HORSE_NAMES.length];
        for (int i = 0; i < horse.length; i++) horse[i] = new PartVisual(HORSE_PARTS[i]);
        item = new WorldItem(context, level, pos);
        for (int i = 0; i < arcs.length; i++)
            arcs[i] = new BeamVisual(context, level, pos, false, 3, 1F);
        arcPose.set(rootPose).translate(0F, .75F, 0F);
        writeFrame(partialTick);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockEntityBatterySocket be) {
        ItemStack stack = be.syncStack;
        if (stack.isEmpty()
                || stack.getItem() instanceof ItemBatteryPack
                || stack.getItem() instanceof ItemBatterySC
                || stack.getItem() instanceof ItemBatteryCreative) return false;
        return !WorldItem.drawsFramed(stack);
    }

    private static MeshPart[] buildBatteryParts() {
        int bodyId = SOCKET_MODEL.partId("Battery");
        int capacitorId = SOCKET_MODEL.partId("Capacitor");
        MeshPart[] parts = new MeshPart[EnumBatteryPack.VALUES.length + 1];
        for (int i = 0; i < EnumBatteryPack.VALUES.length; i++) {
            EnumBatteryPack tier = EnumBatteryPack.VALUES[i];
            parts[i] =
                    MeshPart.obj(
                            SOCKET_MODEL.groups[tier.isCapacitor() ? capacitorId : bodyId],
                            SOCKET_MODEL.smoothing(),
                            MeshPart.litCutout(
                                    Library.id(
                                            "textures/block/models/machines/"
                                                    + tier.tex
                                                    + ".png")));
        }
        parts[parts.length - 1] =
                MeshPart.obj(
                        SOCKET_MODEL.groups[bodyId],
                        SOCKET_MODEL.smoothing(),
                        MeshPart.litCutout(ResourceManager.battery_sc_tex));
        return parts;
    }

    private static MeshPart[] buildHorseParts() {
        MeshPart[] parts = new MeshPart[HORSE_NAMES.length];
        for (int i = 0; i < parts.length; i++)
            parts[i] =
                    MeshPart.obj(
                            HORSE_MODEL.groups[HORSE_MODEL.partId(HORSE_NAMES[i])],
                            HORSE_MODEL.smoothing(),
                            HORSE_MATERIAL);
        return parts;
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        int body = -1;
        ItemStack stack = blockEntity.syncStack;
        if (stack.getItem() instanceof ItemBatteryPack pack) body = pack.tier.ordinal();
        else if (stack.getItem() instanceof ItemBatterySC) body = batteries.length - 1;
        boolean frameChanged = !initialized || blockEntity.frame != lastFrame;
        boolean bodyChanged = !initialized || body != lastBody;
        if (frameChanged) supports.write(blockEntity.frame, rootPose);
        if (bodyChanged) {
            for (int i = 0; i < batteries.length; i++) batteries[i].write(i == body, rootPose);
        }

        boolean unicorn = stack.getItem() instanceof ItemBatteryCreative;
        boolean generic = !stack.isEmpty() && body < 0 && !unicorn;
        float spin = (float) (level.getGameTime() % 360L) + partialTick;
        FramedItem.Arm arm = item.setFramed(generic ? stack : ItemStack.EMPTY, level);
        if (arm != null) {
            itemPoses.setIdentity();
            itemPoses.translate(visualPos.getX(), visualPos.getY(), visualPos.getZ());
            itemPoses.mulPose(rootPose);
            RenderBatterySocket.itemPose(itemPoses, spin, arm);
            item.write(itemPoses.last().pose(), partialTick);
        }
        if (unicorn) {
            horsePose
                    .set(rootPose)
                    .scale((float) UNICORN_SCALE)
                    .rotateY(-spin * UNICORN_DEGREES_PER_TICK * Mth.DEG_TO_RAD);
            for (var part : horse) part.write(true, horsePose);
        } else if (initialized && lastUnicorn) {
            for (var part : horse) part.write(false, horsePose);
        } else if (!initialized) {
            for (var part : horse) part.write(false, horsePose);
        }

        long bucket = level.getGameTime() / 5L;
        int phase = (int) (level.getGameTime() % 20L);
        boolean arcChanged =
                !initialized
                        || unicorn != lastUnicorn
                        || generic != lastGeneric
                        || bucket != lastArcBucket
                        || phase != lastArcPhase;
        if (arcChanged) {
            int used = 0;
            if (unicorn || generic) {
                arcRandom.setSeed(bucket);
                arcRandom.nextBoolean();
                for (int i = 0; i < 4; i++) {
                    if (arcRandom.nextInt(4) != 0) continue;
                    arcs[used++].update(
                            arcPose,
                            arcSkeletons[i],
                            EnumWaveType.RANDOM,
                            phase,
                            15,
                            .0625F,
                            .025F,
                            ARC_OUTER,
                            ARC_INNER);
                    arcs[used++].update(
                            arcPose,
                            arcSkeletons[i],
                            EnumWaveType.RANDOM,
                            phase,
                            1,
                            0F,
                            .025F,
                            ARC_OUTER,
                            ARC_INNER);
                }
            }
            for (int i = used; i < lastArcUsed; i++) arcs[i].hide();
            lastArcUsed = used;
            lastArcBucket = bucket;
            lastArcPhase = phase;
        }
        lastBody = body;
        lastFrame = blockEntity.frame;
        lastUnicorn = unicorn;
        lastGeneric = generic;
        initialized = true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 1,
                                pos.getY(),
                                pos.getZ() - 1,
                                pos.getX() + 2,
                                pos.getY() + 2,
                                pos.getZ() + 2)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        supports.collect(consumer);
        for (var battery : batteries) battery.collect(consumer);
        for (var part : horse) part.collect(consumer);
    }

    @Override
    protected void _delete() {
        supports.delete();
        for (var battery : batteries) battery.delete();
        for (var part : horse) part.delete();
        item.delete();
        for (var arc : arcs) arc.delete();
    }

    private final class PartVisual {
        private final TransformedInstance instance;
        private boolean lastShown;
        private boolean initialized;

        private PartVisual(MeshPart part) {
            instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, part.model())
                            .createInstance();
        }

        private void write(boolean shown, Matrix4f local) {
            if (initialized && shown == lastShown) {
                if (!shown) return;
                if (local == rootPose) return;
            }
            instance.setVisible(shown);
            if (!shown) {
                lastShown = false;
                initialized = true;
                return;
            }
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(local);
            instance.setTransform(instancePose).light(0).setChanged();
            lastShown = shown;
            initialized = true;
        }

        private void collect(Consumer<Instance> consumer) {
            consumer.accept(instance);
        }

        private void delete() {
            instance.delete();
        }
    }
}
