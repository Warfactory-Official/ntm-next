// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.client.render.flywheel.MeshItemVisual;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mags.MagazineSingleTypeBase;
import com.hbm.render.loader.HFRWavefrontObject;
import com.hbm.util.GameTime;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Vector3f;
import org.joml.Vector3fc;
import org.jspecify.annotations.Nullable;

public final class MeshItemRenderer
        implements SpecialModelRenderer<MeshItemRenderer.Argument>,
                ItemVisuals.Factory<MeshItemRenderer.Argument> {

    private static final int NO_PART = -1;
    private static final Vector3fc ZERO = new Vector3f();

    private final HFRWavefrontObject mesh;
    private final int part;
    private final List<List<Placed>> sets;
    private final List<RenderType> skins;
    private final List<Identifier> skinTextures;
    private final boolean cull;
    private final @Nullable SkinSource skinSource;
    private final float[] bounds;
    private final Map<ItemDisplayContext, Clock> clocks;

    private MeshItemRenderer(
            HFRWavefrontObject mesh,
            @Nullable String part,
            List<List<Draw>> sets,
            List<Identifier> skins,
            boolean cull,
            @Nullable SkinSource skinSource,
            Map<ItemDisplayContext, Clock> clocks) {
        this.mesh = mesh;
        this.part = part == null ? NO_PART : mesh.partId(part);
        this.sets =
                sets.stream()
                        .map(
                                set ->
                                        set.stream()
                                                .map(
                                                        draw ->
                                                                new Placed(
                                                                        mesh.partId(draw.part()),
                                                                        draw.offset()))
                                                .toList())
                        .toList();

        this.skins =
                skins.stream()
                        .map(
                                skin ->
                                        cull
                                                ? RenderTypes.entityCutoutCull(skin)
                                                : RenderTypes.entityCutout(skin))
                        .toList();
        this.skinTextures = List.copyOf(skins);
        this.cull = cull;
        this.skinSource = skinSource;
        this.clocks = clocks;
        float[] extents =
                this.sets.isEmpty()
                        ? (part == null ? mesh.getExtents() : mesh.boundsOfParts(part))
                        : boundsOfSets(mesh, this.sets);
        if (extents == null)
            throw new IllegalArgumentException(
                    "mesh names no part " + part + ", so nothing would draw");
        this.bounds = extents;
    }

    private static float @Nullable [] boundsOfSets(
            HFRWavefrontObject mesh, List<List<Placed>> sets) {
        float[] box = null;
        for (List<Placed> set : sets) {
            for (Placed placed : set) {
                float[] part = mesh.boundsOfParts(placed.part());
                if (part == null) continue;
                float[] moved = {
                    part[0] + placed.offset().x(),
                    part[1] + placed.offset().y(),
                    part[2] + placed.offset().z(),
                    part[3] + placed.offset().x(),
                    part[4] + placed.offset().y(),
                    part[5] + placed.offset().z()
                };
                if (box == null) {
                    box = moved;
                    continue;
                }
                for (int i = 0; i < 3; i++) {
                    box[i] = Math.min(box[i], moved[i]);
                    box[i + 3] = Math.max(box[i + 3], moved[i + 3]);
                }
            }
        }
        return box;
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return null;
    }

    @Override
    public void submit(
            @Nullable Argument argument,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            int light,
            int overlay,
            boolean hasFoil,
            int outlineColor) {
        int index = argument == null ? 0 : Math.clamp(argument.skin(), 0, skins.size() - 1);
        RenderType skin = skins.get(index);
        if (sets.isEmpty()) {
            collector.submitCustomGeometry(
                    poseStack,
                    skin,
                    part == NO_PART
                            ? (pose, buffer) -> mesh.render(pose, buffer, light, -1)
                            : (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, part));
            return;
        }
        for (Placed placed : sets.get(Math.clamp(index, 0, sets.size() - 1))) {
            poseStack.pushPose();
            poseStack.translate(placed.offset().x(), placed.offset().y(), placed.offset().z());
            collector.submitCustomGeometry(
                    poseStack,
                    skin,
                    (pose, buffer) -> mesh.renderPart(pose, buffer, light, -1, placed.part()));
            poseStack.popPose();
        }
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        return new MeshItemVisual(ctx, this, skin(stack));
    }

    public int skin(ItemStack stack) {
        return skinSource == null ? 0 : Math.clamp(skinSource.index(stack), 0, skins.size() - 1);
    }

    public void visitParts(int skin, PartVisitor visitor) {
        if (sets.isEmpty()) {
            if (part != NO_PART) visitor.accept(part, ZERO);
            else for (int i = 0; i < mesh.groups.length; i++) visitor.accept(i, ZERO);
            return;
        }
        for (Placed placed : sets.get(Math.clamp(skin, 0, sets.size() - 1)))
            visitor.accept(placed.part(), placed.offset());
    }

    public HFRWavefrontObject mesh() {
        return mesh;
    }

    public Identifier skinTexture(int skin) {
        return skinTextures.get(skin);
    }

    public boolean cull() {
        return cull;
    }

    @FunctionalInterface
    public interface PartVisitor {
        void accept(int part, Vector3fc offset);
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        for (int x = 0; x < 2; x++)
            for (int y = 0; y < 2; y++)
                for (int z = 0; z < 2; z++) {
                    output.accept(
                            new Vector3f(bounds[x * 3], bounds[y * 3 + 1], bounds[z * 3 + 2]));
                }
    }

    private Matrix4fc poseFor(Argument argument, ItemDisplayContext context, Matrix4fc fallback) {
        Clock clock = clocks.get(context);
        if (clock == null) return fallback;
        Vector3fc axis = clock.axis();
        return (fallback != null ? new Matrix4f(fallback) : new Matrix4f())
                .rotate((float) Math.toRadians(argument.spin()), axis.x(), axis.y(), axis.z())
                .mul(clock.post());
    }

    public enum SkinSource implements StringRepresentable {
        MAGAZINE("magazine");

        public static final Codec<SkinSource> CODEC =
                StringRepresentable.fromEnum(SkinSource::values);

        private final String id;

        SkinSource(String id) {
            this.id = id;
        }

        @Override
        public String getSerializedName() {
            return id;
        }

        int index(ItemStack stack) {
            return switch (this) {
                case MAGAZINE -> {
                    ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
                    IMagazine<?> magazine =
                            gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
                    yield magazine instanceof MagazineSingleTypeBase single
                            ? single.acceptedBullets.indexOf(single.getType(stack, null))
                            : 0;
                }
            };
        }
    }

    private record Clock(Vector3fc axis, Matrix4f post) {}

    public record Draw(String part, Vector3fc offset) {
        public static final Codec<Draw> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.STRING.fieldOf("part").forGetter(Draw::part),
                                                ExtraCodecs.VECTOR3F
                                                        .<Vector3fc>xmap(v -> v, Vector3f::new)
                                                        .optionalFieldOf("offset", new Vector3f())
                                                        .forGetter(Draw::offset))
                                        .apply(i, Draw::new));
    }

    private record Placed(int part, Vector3fc offset) {}

    public record Argument(float spin, int skin) {}

    public record Spin(long divisor, long modulus, float rate, Optional<Vector3fc> axis) {
        public static final Codec<Spin> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.LONG
                                                        .optionalFieldOf("divisor", 1L)
                                                        .forGetter(Spin::divisor),
                                                Codec.LONG
                                                        .fieldOf("modulus")
                                                        .forGetter(Spin::modulus),
                                                Codec.FLOAT
                                                        .optionalFieldOf("rate", 1F)
                                                        .forGetter(Spin::rate),
                                                ExtraCodecs.VECTOR3F
                                                        .xmap(v -> v, Vector3f::new)
                                                        .optionalFieldOf("axis")
                                                        .forGetter(Spin::axis))
                                        .apply(i, Spin::new));

        public float degrees() {
            return (float) (GameTime.now() / divisor % modulus) * rate;
        }
    }

    public record Unbaked(
            Identifier base,
            Identifier mesh,
            boolean smooth,
            boolean cull,
            Optional<String> part,
            List<List<Draw>> parts,
            List<Identifier> skins,
            Optional<SkinSource> skinSource,
            Optional<Map<String, Matrix4fc>> suffixes,
            Optional<Spin> spin)
            implements ItemModel.Unbaked {
        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                Identifier.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base),
                                                Identifier.CODEC
                                                        .fieldOf("mesh")
                                                        .forGetter(Unbaked::mesh),
                                                Codec.BOOL
                                                        .optionalFieldOf("smooth", true)
                                                        .forGetter(Unbaked::smooth),
                                                Codec.BOOL
                                                        .optionalFieldOf("cull", true)
                                                        .forGetter(Unbaked::cull),
                                                Codec.STRING
                                                        .optionalFieldOf("part")
                                                        .forGetter(Unbaked::part),
                                                Draw.CODEC
                                                        .listOf()
                                                        .listOf()
                                                        .optionalFieldOf("parts", List.of())
                                                        .forGetter(Unbaked::parts),
                                                Identifier.CODEC
                                                        .listOf()
                                                        .fieldOf("skins")
                                                        .forGetter(Unbaked::skins),
                                                SkinSource.CODEC
                                                        .optionalFieldOf("skin_source")
                                                        .forGetter(Unbaked::skinSource),
                                                Codec.unboundedMap(
                                                                Codec.STRING, ExtraCodecs.MATRIX4F)
                                                        .optionalFieldOf("pose_suffix")
                                                        .forGetter(Unbaked::suffixes),
                                                Spin.CODEC
                                                        .optionalFieldOf("spin")
                                                        .forGetter(Unbaked::spin))
                                        .apply(i, Unbaked::new));

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            HFRWavefrontObject loaded =
                    new HFRWavefrontObject(Minecraft.getInstance().getResourceManager(), mesh);
            if (!smooth) loaded.noSmooth();

            EnumMap<ItemDisplayContext, Clock> clocks = new EnumMap<>(ItemDisplayContext.class);
            Map<String, Matrix4fc> remainder = suffixes.orElse(Map.of());
            if (skins.size() > 1 && skinSource.isEmpty()) {
                throw new IllegalArgumentException("several skins need a skin_source to pick one");
            }
            if (remainder.isEmpty() != spin.isEmpty()) {
                throw new IllegalArgumentException(
                        "a post-clock remainder and a spin declaration must "
                                + "accompany each other");
            }
            if (!remainder.isEmpty()) {
                Vector3fc axis =
                        spin.flatMap(Spin::axis)
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "pose_suffix without spin.axis: a suffix exists only because a clock cut the body"));
                remainder.forEach(
                        (name, matrix) -> {
                            ItemDisplayContext display = null;
                            for (ItemDisplayContext candidate : ItemDisplayContext.values()) {
                                if (candidate.getSerializedName().equals(name)) display = candidate;
                            }
                            if (display == null)
                                throw new IllegalArgumentException(
                                        "unknown display context " + name);
                            clocks.put(display, new Clock(axis, new Matrix4f(matrix)));
                        });
            }

            SkinSource source = skinSource.orElse(null);
            MeshItemRenderer renderer =
                    new MeshItemRenderer(
                            loaded, part.orElse(null), parts, skins, cull, source, clocks);
            Optional<Spin> clock = spin;
            DynamicSpecialWrapper.ArgumentPose<Argument> turn =
                    clocks.isEmpty() ? null : renderer::poseFor;
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, display, owner) ->
                            new Argument(
                                    clock.map(Spin::degrees).orElse(0F),
                                    source == null ? 0 : source.index(stack)),
                    turn,
                    spin.isPresent(),
                    DynamicSpecialWrapper.properties(context, base),
                    Map.of());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
