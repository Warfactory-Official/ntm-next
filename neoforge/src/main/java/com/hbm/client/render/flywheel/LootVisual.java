// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BlockLoot;
import com.hbm.client.render.RenderLoot;
import com.hbm.items.ModItems;
import com.hbm.items.armor.ModArmorItem;
import com.hbm.items.weapon.sedna.factory.GunFactory.EnumAmmo;
import com.hbm.lib.Library;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.GroupObject;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.engine_room.flywheel.api.instance.Instance;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.DepthTest;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.visual.ShaderLightVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.instance.InstanceTypes;
import dev.engine_room.flywheel.lib.instance.TransformedInstance;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.joml.Matrix4f;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

public final class LootVisual extends HbmDynamicBlockEntityVisual<BlockLoot.TileEntityLoot>
        implements ShaderLightVisual {
    private static final Identifier MINI_NUKE_TEXTURE =
            Library.id("textures/models/projectiles/mini_nuke.png");
    private static final Assets ASSETS = buildAssets();
    private final List<Placed> placed = new ArrayList<>();
    private final List<PlacedStack> stacks = new ArrayList<>();
    private final Matrix4f instancePose = new Matrix4f();
    private final double[] lightBoundsAccumulator = new double[6];
    private final AABB anchorBounds;
    private List<EntryKey> keys = List.of();
    private BlockLoot.TileEntityLoot.Entry[] trackedEntries = new BlockLoot.TileEntityLoot.Entry[0];
    private @Nullable AABB lastLightBounds;

    public LootVisual(
            VisualizationContext context, BlockLoot.TileEntityLoot blockEntity, float partialTick) {
        super(context, blockEntity, partialTick);
        anchorBounds = new AABB(pos);
        writeFrame(partialTick);
    }

    private static Assets buildAssets() {
        HFRWavefrontObject projectile = ResourceManager.projectiles;
        int miniNuke = projectile.partId("MiniNuke");
        List<DrawSpec> nuke =
                List.of(
                        spec(
                                projectile,
                                miniNuke,
                                MeshPart.litCutout(MINI_NUKE_TEXTURE),
                                new Matrix4f().scale(.5F).translate(1F, .5F, 1F),
                                true));

        HFRWavefrontObject maresleg = ResourceManager.maresleg;
        ArrayList<DrawSpec> shotgunParts = new ArrayList<>();
        Matrix4f shotgunPose =
                new Matrix4f()
                        .scale(.125F)
                        .translate(3F, 0F, 0F)
                        .rotateY(radians(25F))
                        .rotateX(radians(90F))
                        .rotateY(radians(90F));
        var shotgunMaterial = MeshPart.litCutout(ResourceManager.maresleg_tex);
        for (int i = 0; i < maresleg.groups.length; i++)
            shotgunParts.add(spec(maresleg, i, shotgunMaterial, shotgunPose, false));
        List<DrawSpec> shotgun = List.copyOf(shotgunParts);
        List<DrawSpec>[] trenchmaster =
                armor(
                        ResourceManager.armor_trenchmaster,
                        ResourceManager.trenchmaster_helmet_tex,
                        ResourceManager.trenchmaster_chest_tex,
                        ResourceManager.trenchmaster_arm_tex,
                        ResourceManager.trenchmaster_leg_tex,
                        "Light");
        List<DrawSpec>[] ncrpa =
                armor(
                        ResourceManager.armor_ncr,
                        ResourceManager.ncrpa_helmet_tex,
                        ResourceManager.ncrpa_chest_tex,
                        ResourceManager.ncrpa_arm_tex,
                        ResourceManager.ncrpa_leg_tex,
                        "Eyes");
        return new Assets(nuke, shotgun, trenchmaster, ncrpa);
    }

    public static void initModels() {}

    public static boolean vanillaNeeded(BlockLoot.TileEntityLoot be) {
        for (var entry : be.items) {
            ItemStack stack = entry.stack();
            if (specs(stack) == null
                    && !WorldSprite.resolvedSprite(stack)
                    && !WorldItem.draws(stack, ItemDisplayContext.FIXED)) {
                return true;
            }
        }
        return false;
    }

    private static float radians(float degrees) {
        return degrees * ((float) Math.PI / 180F);
    }

    @SuppressWarnings("unchecked")
    private static List<DrawSpec>[] armor(
            HFRWavefrontObject mesh,
            Identifier helmetTexture,
            Identifier chestTexture,
            Identifier armTexture,
            Identifier legTexture,
            String glowName) {
        Matrix4f body =
                new Matrix4f().translate(.5F, 1.5F, .5F).scale(.0625F).rotateX(radians(180F));
        Matrix4f arms = new Matrix4f(body).rotateX(radians(-3F));
        Matrix4f rightLeg = new Matrix4f(body).rotateX(radians(-.1F));
        Material helmet =
                SimpleMaterial.builderOf(Materials.TRANSLUCENT)
                        .texture(helmetTexture)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .transparency(Transparency.ORDER_INDEPENDENT)
                        .writeMask(WriteMask.COLOR)
                        .depthTest(DepthTest.LEQUAL)
                        .light(LightShaders.SMOOTH)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.CHUNK)
                        .backfaceCulling(true)
                        .build();
        Material glow =
                SimpleMaterial.builderOf(Materials.CUTOUT)
                        .texture(helmetTexture)
                        .mipmap(false)
                        .cutout(CutoutShaders.ONE_TENTH)
                        .light(LightShaders.NONE)
                        .useLight(false)
                        .useOverlay(false)
                        .ambientOcclusion(false)
                        .cardinalLightingMode(CardinalLightingMode.OFF)
                        .backfaceCulling(true)
                        .build();
        Material chest = MeshPart.litCutout(chestTexture);
        Material armsMaterial = MeshPart.litCutout(armTexture);
        Material legs = MeshPart.litCutout(legTexture);
        List<DrawSpec>[] result = new List[4];
        int helmetPart = mesh.partId("Helmet");
        int glowPart = mesh.partId(glowName);
        result[0] =
                List.of(
                        spec(mesh, helmetPart, helmet, body, false),
                        spec(mesh, glowPart, glow, body, false, true));
        result[1] =
                List.of(
                        spec(mesh, mesh.partId("Chest"), chest, body, false),
                        spec(mesh, mesh.partId("LeftArm"), armsMaterial, arms, false),
                        spec(mesh, mesh.partId("RightArm"), armsMaterial, arms, false));
        result[2] =
                List.of(
                        spec(mesh, mesh.partId("LeftLeg"), legs, body, false),
                        spec(mesh, mesh.partId("RightLeg"), legs, rightLeg, false));
        result[3] =
                List.of(
                        spec(mesh, mesh.partId("LeftBoot"), legs, body, false),
                        spec(mesh, mesh.partId("RightBoot"), legs, rightLeg, false));
        return result;
    }

    private static DrawSpec spec(
            HFRWavefrontObject mesh,
            int part,
            Material material,
            Matrix4f transform,
            boolean smooth) {
        return spec(mesh, part, material, transform, smooth, false);
    }

    private static DrawSpec spec(
            HFRWavefrontObject mesh,
            int part,
            Material material,
            Matrix4f transform,
            boolean smooth,
            boolean fixedLight) {
        GroupObject group = mesh.groups[part];
        return new DrawSpec(
                MeshPart.obj(smooth ? group : group.flatShaded(), true, material),
                new Matrix4f(transform),
                fixedLight,
                material.transparency() == Transparency.OPAQUE && !fixedLight,
                transformedBounds(group, transform));
    }

    private static AABB transformedBounds(GroupObject group, Matrix4f transform) {
        float minX = Float.MAX_VALUE, minY = Float.MAX_VALUE, minZ = Float.MAX_VALUE;
        float maxX = -Float.MAX_VALUE, maxY = -Float.MAX_VALUE, maxZ = -Float.MAX_VALUE;
        Vector3f point = new Vector3f();
        float[] vertices = group.quads(true);
        for (int i = 0; i < vertices.length; i += GroupObject.STRIDE) {
            transform.transformPosition(point.set(vertices[i], vertices[i + 1], vertices[i + 2]));
            minX = Math.min(minX, point.x);
            minY = Math.min(minY, point.y);
            minZ = Math.min(minZ, point.z);
            maxX = Math.max(maxX, point.x);
            maxY = Math.max(maxY, point.y);
            maxZ = Math.max(maxZ, point.z);
        }
        return new AABB(minX, minY, minZ, maxX, maxY, maxZ);
    }

    private static @Nullable List<DrawSpec> specs(ItemStack stack) {
        EnumAmmo ammo = ModItems.AMMO_STANDARD.typeOf(stack);
        if (ammo != null
                && ammo.ordinal() >= EnumAmmo.NUKE_STANDARD.ordinal()
                && ammo.ordinal() <= EnumAmmo.NUKE_HIVE.ordinal()) return ASSETS.nuke;
        if (stack.is(ModItems.GUN_MARESLEG.get())) return ASSETS.shotgun;
        if (!(stack.getItem() instanceof ModArmorItem armor)) return null;
        List<DrawSpec>[] family =
                switch (armor.suit()) {
                    case TRENCHMASTER -> ASSETS.trenchmaster;
                    case NCRPA -> ASSETS.ncrpa;
                    default -> null;
                };
        if (family == null) return null;
        int slot =
                switch (stack.get(DataComponents.EQUIPPABLE).slot()) {
                    case HEAD -> 0;
                    case CHEST -> 1;
                    case LEGS -> 2;
                    case FEET -> 3;
                    default -> throw new AssertionError();
                };
        return family[slot];
    }

    @Override
    protected void trackExtent() {
        List<BlockLoot.TileEntityLoot.Entry> source = blockEntity.items;
        boolean same = source.size() == trackedEntries.length;
        for (int i = 0; same && i < trackedEntries.length; i++)
            same = source.get(i) == trackedEntries[i];
        if (same) return;
        trackedEntries = source.toArray(new BlockLoot.TileEntityLoot.Entry[0]);
        refreshVisibleBounds();
    }

    @Override
    protected void frame(Context context) {
        writeFrame(context.partialTick());
    }

    private void writeFrame(float partialTick) {
        if (!syncEntries()) {
            for (var stack : stacks) stack.write(partialTick);
            return;
        }
        LightBounds.resetBounds(lightBoundsAccumulator, anchorBounds);
        for (var draw : placed) draw.write();
        for (var draw : placed)
            if (!draw.spec.fixedLight) {
                LightBounds.includeLightBounds(
                        lightBoundsAccumulator, draw.spec.part.model(), draw.localPose, pos);
            }
        for (var stack : stacks) {
            LightBounds.includeLightBounds(lightBoundsAccumulator, stack.extent);
            stack.write(partialTick);
        }
        lastLightBounds =
                LightBounds.sections(lightSections, lightBoundsAccumulator, lastLightBounds);
    }

    private boolean syncEntries() {
        List<BlockLoot.TileEntityLoot.Entry> source = blockEntity.items;
        if (source.size() == keys.size()) {
            boolean same = true;
            for (int i = 0; i < source.size(); i++) {
                EntryKey key = keys.get(i);
                BlockLoot.TileEntityLoot.Entry entry = source.get(i);
                if (key.x != entry.x()
                        || key.y != entry.y()
                        || key.z != entry.z()
                        || !ItemStack.isSameItemSameComponents(key.stack, entry.stack())) {
                    same = false;
                    break;
                }
            }
            if (same) return false;
        }
        for (var draw : placed) draw.delete();
        placed.clear();
        for (var stack : stacks) stack.delete();
        stacks.clear();
        ArrayList<EntryKey> next = new ArrayList<>(source.size());
        for (var entry : source) {
            next.add(new EntryKey(entry.stack().copy(), entry.x(), entry.y(), entry.z()));
            List<DrawSpec> specs = specs(entry.stack());
            if (specs != null)
                for (var spec : specs)
                    placed.add(new Placed(entry.x(), entry.y(), entry.z(), spec));
            else stacks.add(new PlacedStack(entry.x(), entry.y(), entry.z(), entry.stack()));
        }
        keys = List.copyOf(next);
        return true;
    }

    @Override
    protected AABB getRenderBoundingBox() {
        AABB bounds = new AABB(pos);
        for (var entry : blockEntity.items) {
            List<DrawSpec> specs = specs(entry.stack());
            if (specs == null) {
                AABB fixed =
                        RenderLoot.fixedModel(
                                Minecraft.getInstance().getItemModelResolver(),
                                new ItemStackRenderState(),
                                entry.stack(),
                                level);
                bounds =
                        bounds.minmax(
                                RenderLoot.stackBounds(fixed)
                                        .move(
                                                pos.getX() + entry.x(),
                                                pos.getY() + entry.y(),
                                                pos.getZ() + entry.z()));
            } else
                for (var spec : specs) {
                    bounds =
                            bounds.minmax(
                                    spec.bounds.move(
                                            pos.getX() + entry.x(),
                                            pos.getY() + entry.y(),
                                            pos.getZ() + entry.z()));
                }
        }
        return bounds.inflate(1);
    }

    @Override
    public void collectCrumblingInstances(Consumer<Instance> consumer) {
        for (var draw : placed) if (draw.spec.crumbles) consumer.accept(draw.instance);
    }

    @Override
    protected void _delete() {
        for (var draw : placed) draw.delete();
        placed.clear();
        for (var stack : stacks) stack.delete();
        stacks.clear();
    }

    private record Assets(
            List<DrawSpec> nuke,
            List<DrawSpec> shotgun,
            List<DrawSpec>[] trenchmaster,
            List<DrawSpec>[] ncrpa) {}

    private record DrawSpec(
            MeshPart part, Matrix4f transform, boolean fixedLight, boolean crumbles, AABB bounds) {}

    private record EntryKey(ItemStack stack, double x, double y, double z) {}

    private final class PlacedStack {
        private final ItemStack stack;
        private final WorldSprite sprite = new WorldSprite(instancerProvider());
        private final Matrix4f spritePose = new Matrix4f();
        private final Matrix4f modelPose = new Matrix4f();
        private final AABB extent;
        private @Nullable WorldItem model;

        private PlacedStack(double x, double y, double z, ItemStack stack) {
            this.stack = stack;
            sprite.set(stack);
            spritePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .translate((float) x, (float) y, (float) z)
                    .translate(.25F, 0F, .25F)
                    .scale(.5F)
                    .rotateX(radians(90F));
            AABB fixed =
                    RenderLoot.fixedModel(
                            Minecraft.getInstance().getItemModelResolver(),
                            new ItemStackRenderState(),
                            stack,
                            level);
            PoseStack poses = new PoseStack();
            poses.translate(visualPos.getX() + x, visualPos.getY() + y, visualPos.getZ() + z);
            RenderLoot.fixedView(poses, fixed);
            modelPose.set(poses.last().pose());
            extent =
                    RenderLoot.stackBounds(fixed)
                            .move(pos.getX() + x, pos.getY() + y, pos.getZ() + z);
        }

        private void write(float partialTick) {
            sprite.write(spritePose);
            if (model == null) {
                if (!sprite.notSprite()) return;
                model = new WorldItem(visualizationContext, level, pos);
                model.set(stack, ItemDisplayContext.FIXED);
            }
            model.write(modelPose, partialTick);
        }

        private void delete() {
            sprite.delete();
            if (model != null) model.delete();
        }
    }

    private final class Placed {
        private final double x, y, z;
        private final DrawSpec spec;
        private final TransformedInstance instance;
        private final Matrix4f localPose = new Matrix4f();

        private Placed(double x, double y, double z, DrawSpec spec) {
            this.x = x;
            this.y = y;
            this.z = z;
            this.spec = spec;
            instance =
                    instancerProvider()
                            .instancer(InstanceTypes.TRANSFORMED, spec.part.model())
                            .createInstance();
        }

        private void write() {
            localPose.identity().translate((float) x, (float) y, (float) z).mul(spec.transform);
            instancePose
                    .translation(visualPos.getX(), visualPos.getY(), visualPos.getZ())
                    .mul(localPose);
            instance.setTransform(instancePose)
                    .light(spec.fixedLight ? LightCoordsUtil.FULL_BRIGHT : 0)
                    .setChanged();
        }

        private void delete() {
            instance.delete();
        }
    }
}
