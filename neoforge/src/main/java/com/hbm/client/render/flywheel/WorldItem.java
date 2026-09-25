// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.client.render.FramedItem;
import dev.engine_room.flywheel.api.instance.InstancerProvider;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer;
import dev.engine_room.flywheel.lib.visual.util.ItemStackSlot;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.jspecify.annotations.Nullable;

public final class WorldItem {
    private static final Map<SupportKey, Boolean> SUPPORTED = new ConcurrentHashMap<>();
    private static final Map<ModelKey, Model> MODELS = new ConcurrentHashMap<>();
    private static final Map<Material, Material> LIT = new ConcurrentHashMap<>();

    private final VisualizationContext context;
    private final InstancerProvider instancers;
    private final Level level;
    private final BlockPos lightPos;
    private final int light;
    private final ItemStackSlot slot = new ItemStackSlot();
    private @Nullable ItemDisplayContext slotContext;
    private final ItemStackRenderState scratch = new ItemStackRenderState();
    private final Matrix4f pose = new Matrix4f();
    private ItemStack stack = ItemStack.EMPTY;
    private FramedItem.@Nullable Arm arm;
    private @Nullable TransformedInstance instance;
    private boolean posed;

    public WorldItem(VisualizationContext context, Level level, BlockPos lightPos) {
        this(context, level, lightPos, false);
    }

    public WorldItem(
            VisualizationContext context, Level level, BlockPos lightPos, boolean fullBright) {
        this.context = context;
        this.instancers = context.instancerProvider();
        this.level = level;
        this.lightPos = lightPos;
        light = fullBright ? LightCoordsUtil.FULL_BRIGHT : 0;
    }

    public static void clear() {
        SUPPORTED.clear();
        MODELS.clear();
    }

    public static boolean bakes(ItemStack stack, ItemDisplayContext context) {
        if (stack.isEmpty()) return true;
        return SUPPORTED.computeIfAbsent(
                new SupportKey(stack.getItem(), stack.get(DataComponents.ITEM_MODEL), context),
                key -> {
                    var meshes = BakedModelBufferer.INSTANCE.bufferItem(stack, context, null, 0);
                    return meshes != null && meshes.stackDetermined();
                });
    }

    public static boolean bakesFramed(ItemStack stack) {
        return bakes(stack, ItemDisplayContext.NONE) && bakes(stack, ItemDisplayContext.GROUND);
    }

    public static boolean draws(ItemStack stack, ItemDisplayContext context) {
        return bakes(stack, context) || ItemStackSlot.isVisualized(stack);
    }

    public static boolean drawsFramed(ItemStack stack) {
        return bakesFramed(stack) || ItemStackSlot.isVisualized(stack);
    }

    private static @Nullable Model model(ItemStack stack, ItemDisplayContext context) {
        var meshes = BakedModelBufferer.INSTANCE.bufferItem(stack, context, null, 0);
        if (meshes == null || !meshes.stackDetermined()) {
            SUPPORTED.put(
                    new SupportKey(stack.getItem(), stack.get(DataComponents.ITEM_MODEL), context),
                    false);
            return null;
        }
        return MODELS.computeIfAbsent(
                new ModelKey(
                        context,
                        meshes.identity(),
                        stack.getItem(),
                        stack.get(DataComponents.ITEM_MODEL)),
                key -> {
                    List<Model.ConfiguredMesh> configured = new ArrayList<>();
                    meshes.meshes()
                            .forEach(
                                    (bucket, mesh) -> {
                                        Material material =
                                                ModelUtil.getItemMaterial(
                                                        bucket.layer(), bucket.blocksAtlas());
                                        if (material == null) return;
                                        configured.add(
                                                new Model.ConfiguredMesh(lit(material), mesh));
                                        if (meshes.foil())
                                            configured.add(
                                                    new Model.ConfiguredMesh(
                                                            Materials.GLINT, mesh));
                                    });
                    return new SimpleModel(configured);
                });
    }

    static Material lit(Material source) {
        return LIT.computeIfAbsent(
                source,
                material ->
                        SimpleMaterial.builderOf(material)
                                .light(LightShaders.SMOOTH)
                                .ambientOcclusion(false)
                                .build());
    }

    public boolean set(ItemStack next, ItemDisplayContext context) {
        if (!change(next)) return instance != null || slotContext != null || stack.isEmpty();
        return install(context);
    }

    public FramedItem.@Nullable Arm setFramed(ItemStack next, @Nullable Level level) {
        if (!change(next)) return instance == null && slotContext == null ? null : arm;
        arm = null;
        if (stack.isEmpty() || !drawsFramed(stack)) return null;
        FramedItem.Arm resolved =
                FramedItem.resolve(
                        Minecraft.getInstance().getItemModelResolver(),
                        scratch,
                        stack,
                        level,
                        null,
                        0);
        if (!install(resolved.mesh() ? ItemDisplayContext.GROUND : ItemDisplayContext.NONE))
            return null;
        arm = resolved;
        return arm;
    }

    private boolean change(ItemStack next) {
        if (next.isEmpty()
                ? stack.isEmpty()
                : !stack.isEmpty() && ItemStack.isSameItemSameComponents(next, stack)) {
            return false;
        }
        stack = next.isEmpty() ? ItemStack.EMPTY : next.copyWithCount(1);
        if (instance != null) instance.delete();
        instance = null;
        slot.delete();
        slotContext = null;
        return true;
    }

    private boolean install(ItemDisplayContext context) {
        if (stack.isEmpty()) return true;
        Model model = model(stack, context);
        if (model == null) {
            if (!ItemStackSlot.isVisualized(stack)) return false;
            slotContext = context;
            return true;
        }
        if (!model.meshes().isEmpty()) {
            instance = instancers.instancer(InstanceTypes.TRANSFORMED, model).createInstance();
            posed = false;
        }
        return true;
    }

    public void write(Matrix4fc at, float partialTick) {
        if (slotContext != null) {
            slot.draw(
                    context,
                    stack,
                    slotContext,
                    null,
                    0,
                    at,
                    light != 0 ? light : LightCoordsUtil.getLightCoords(level, lightPos),
                    OverlayTexture.NO_OVERLAY,
                    partialTick);
            return;
        }
        if (instance == null || posed && pose.equals(at)) return;
        pose.set(at);
        instance.setTransform(pose).light(light).setChanged();
        posed = true;
    }

    public void delete() {
        if (instance != null) instance.delete();
        instance = null;
        slot.delete();
    }

    private record SupportKey(Item item, @Nullable Identifier model, ItemDisplayContext context) {}

    private record ModelKey(
            ItemDisplayContext context, Object identity, Item item, @Nullable Identifier model) {}
}
