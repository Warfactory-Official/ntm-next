// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.multiblock.BlockMultiblockCore;
import com.hbm.client.render.PadMissiles;
import com.hbm.items.ModItems;
import com.hbm.main.ResourceManager;
import com.hbm.tileentity.bomb.BlockEntityLaunchPad;
import com.hbm.util.Facing;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import java.util.function.Consumer;
import net.minecraft.resources.Identifier;
import net.minecraft.util.Mth;
import net.minecraft.world.item.Item;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.jspecify.annotations.Nullable;

public final class LaunchPadVisual extends HbmDynamicBlockEntityVisual<BlockEntityLaunchPad>
        implements ShaderLightVisual {
    private static final Item[] MISSILE_ITEMS = PadMissiles.table().keySet().toArray(Item[]::new);
    private static final MeshPart[][] MISSILE_PARTS = buildMissileParts();
    private final AABB bodyBounds;
    private final Matrix4f rootPose = new Matrix4f();
    private final Matrix4f localPose = new Matrix4f();
    private final Matrix4f instancePose = new Matrix4f();
    private @Nullable Item currentItem;
    private @Nullable MissileDraw currentDraw;

    public LaunchPadVisual(
            VisualizationContext context, BlockEntityLaunchPad blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        rootPose.translate(.5F, 0F, .5F)
                .rotateY(
                        Facing.yaw(BlockMultiblockCore.coreFacing(blockState), 90)
                                * Mth.DEG_TO_RAD);
        bodyBounds = LightBounds.of(ResourceManager.launch_pad_silo, "Plane", rootPose, pos);
        writeFrame();
    }

    private static MeshPart[][] buildMissileParts() {
        var parts = new MeshPart[MISSILE_ITEMS.length][];
        for (int i = 0; i < parts.length; i++) {
            Item item = MISSILE_ITEMS[i];
            Identifier texture = textureFor(item);
            if (texture != null) {
                var missile = PadMissiles.table().get(item);
                parts[i] = MeshPart.objParts(missile.mesh(), MeshPart.litCutout(texture));
            }
        }
        return parts;
    }

    static @Nullable MeshPart[] missileParts(Item item) {
        for (int i = 0; i < MISSILE_ITEMS.length; i++)
            if (MISSILE_ITEMS[i] == item) return MISSILE_PARTS[i];
        return null;
    }

    public static void initModels() {}

    static @Nullable Identifier textureFor(Item item) {
        if (item == ModItems.MISSILE_TEST.get()) return ResourceManager.missileMicroTest_tex;
        if (item == ModItems.MISSILE_TAINT.get()) return ResourceManager.missileMicroTaint_tex;
        if (item == ModItems.MISSILE_MICRO.get()) return ResourceManager.missileMicro_tex;
        if (item == ModItems.MISSILE_BHOLE.get()) return ResourceManager.missileMicroBHole_tex;
        if (item == ModItems.MISSILE_SCHRABIDIUM.get())
            return ResourceManager.missileMicroSchrab_tex;
        if (item == ModItems.MISSILE_EMP.get()) return ResourceManager.missileMicroEMP_tex;
        if (item == ModItems.MISSILE_STEALTH.get()) return ResourceManager.missileStealth_tex;
        if (item == ModItems.MISSILE_GENERIC.get()) return ResourceManager.missileV2_HE_tex;
        if (item == ModItems.MISSILE_INCENDIARY.get()) return ResourceManager.missileV2_IN_tex;
        if (item == ModItems.MISSILE_CLUSTER.get()) return ResourceManager.missileV2_CL_tex;
        if (item == ModItems.MISSILE_BUSTER.get()) return ResourceManager.missileV2_BU_tex;
        if (item == ModItems.MISSILE_DECOY.get()) return ResourceManager.missileV2_decoy_tex;
        if (item == ModItems.MISSILE_ANTI_BALLISTIC.get()) return ResourceManager.missileAA_tex;
        if (item == ModItems.MISSILE_STRONG.get()) return ResourceManager.missileStrong_HE_tex;
        if (item == ModItems.MISSILE_INCENDIARY_STRONG.get())
            return ResourceManager.missileStrong_IN_tex;
        if (item == ModItems.MISSILE_CLUSTER_STRONG.get())
            return ResourceManager.missileStrong_CL_tex;
        if (item == ModItems.MISSILE_BUSTER_STRONG.get())
            return ResourceManager.missileStrong_BU_tex;
        if (item == ModItems.MISSILE_EMP_STRONG.get()) return ResourceManager.missileStrong_EMP_tex;
        if (item == ModItems.MISSILE_BURST.get()) return ResourceManager.missileHuge_HE_tex;
        if (item == ModItems.MISSILE_INFERNO.get()) return ResourceManager.missileHuge_IN_tex;
        if (item == ModItems.MISSILE_RAIN.get()) return ResourceManager.missileHuge_CL_tex;
        if (item == ModItems.MISSILE_DRILL.get()) return ResourceManager.missileHuge_BU_tex;
        if (item == ModItems.MISSILE_NUCLEAR.get()) return ResourceManager.missileNuclear_tex;
        if (item == ModItems.MISSILE_NUCLEAR_CLUSTER.get()) return ResourceManager.missileMIRV_tex;
        if (item == ModItems.MISSILE_VOLCANO.get()) return ResourceManager.missileVolcano_tex;
        if (item == ModItems.MISSILE_DOOMSDAY.get()) return ResourceManager.missileDoomsday_tex;
        if (item == ModItems.MISSILE_DOOMSDAY_RUSTED.get())
            return ResourceManager.missileDoomsdayRusted_tex;
        if (item == ModItems.MISSILE_SHUTTLE.get()) return ResourceManager.missileShuttle_tex;
        return null;
    }

    @Override
    protected void frame(Context context) {
        writeFrame();
    }

    private void writeFrame() {
        Item item = blockEntity.loadedMissile;
        boolean itemChanged = item != currentItem;
        if (itemChanged) {
            if (currentDraw != null) currentDraw.delete();
            currentItem = item;
            PadMissiles.Missile missile = item == null ? null : PadMissiles.table().get(item);
            MeshPart[] parts = item == null ? null : missileParts(item);
            currentDraw = missile == null || parts == null ? null : new MissileDraw(missile, parts);
        }
        MissileDraw draw = currentDraw;
        if (draw == null || !itemChanged) return;

        localPose.set(rootPose).translate(0F, 1F, 0F);
        if (draw.scale != 1F) localPose.scale(draw.scale);
        draw.write(localPose);
    }

    @Override
    protected AABB getRenderBoundingBox() {
        return bodyBounds.minmax(
                new AABB(
                                pos.getX() - 2,
                                pos.getY(),
                                pos.getZ() - 2,
                                pos.getX() + 3,
                                pos.getY() + 15,
                                pos.getZ() + 3)
                        .inflate(1));
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        if (currentDraw != null) currentDraw.collect(consumer);
    }

    @Override
    protected void _delete() {
        if (currentDraw != null) currentDraw.delete();
    }

    private final class MissileDraw {
        private final float scale;
        private final TransformedInstance[] instances;

        private MissileDraw(PadMissiles.Missile missile, MeshPart[] parts) {
            scale = missile.scale();
            instances = new TransformedInstance[parts.length];
            for (int i = 0; i < parts.length; i++)
                instances[i] =
                        instancerProvider()
                                .instancer(InstanceTypes.TRANSFORMED, parts[i].model())
                                .createInstance();
        }

        private void write(Matrix4f pose) {
            for (int i = 0; i < instances.length; i++) {
                instancePose
                        .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                        .mul(pose);
                instances[i].setTransform(instancePose).light(0).setChanged();
            }
        }

        private void collect(Consumer<Instance> consumer) {
            for (var instance : instances) consumer.accept(instance);
        }

        private void delete() {
            for (var instance : instances) instance.delete();
        }
    }
}
