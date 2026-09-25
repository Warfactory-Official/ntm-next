// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.client.render.flywheel.GunItemVisual;
import com.hbm.client.render.flywheel.GunRestVisual;
import com.hbm.client.render.flywheel.ItemVisuals;
import com.hbm.items.weapon.sedna.AkimboGhost;
import com.hbm.items.weapon.sedna.BulletConfig;
import com.hbm.items.weapon.sedna.ItemGunBaseNT;
import com.hbm.items.weapon.sedna.impl.ItemGunNI4NI;
import com.hbm.items.weapon.sedna.mags.IMagazine;
import com.hbm.items.weapon.sedna.mods.XWeaponModManager;
import com.hbm.main.ResourceManager;
import com.hbm.render.item.weapon.sedna.ItemRenderWeaponBase;
import com.hbm.render.item.weapon.sedna.MuzzleFlash;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import dev.engine_room.flywheel.api.visual.ItemStackVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import java.lang.Math;
import java.util.*;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.item.ItemModel;
import net.minecraft.client.renderer.item.ModelRenderProperties;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.special.SpecialModelRenderer;
import net.minecraft.client.resources.model.ResolvableModel;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ExtraCodecs;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.joml.*;
import org.jspecify.annotations.Nullable;

public class GunItemRenderer
        implements SpecialModelRenderer<GunItemRenderer.Argument>,
                ItemVisuals.Factory<GunItemRenderer.Argument> {

    public static final Matrix4fc MOD_TABLE_BASE =
            new Matrix4f().scale(-5F).rotate((float) Math.toRadians(90), 0F, 1F, 0F);
    private static final Matrix4f IDENTITY = new Matrix4f();
    public static final int BALEFIRE_LAYERS = 3;
    private static final UpgradeState NO_UPGRADES = new UpgradeState(List.of());
    private final List<BakedDraw> draws;
    private final List<BakedDraw> guiDraws;
    private final List<BakedDraw> entityDraws;
    private final List<BakedDraw> equippedDraws;
    private final List<BakedDraw> akimboDraws;
    private final List<BakedMuzzle> equippedMuzzles;
    private final List<BakedMuzzle> akimboMuzzles;
    private final List<Map<ItemDisplayContext, Matrix4f>> poseVariants;

    private final List<Matrix4fc> modTableVariants;
    private final int equippedShotConfig;
    private final int akimboShotConfig;
    private final List<Vector3fc> extents;
    private final List<PosePredicate> posePredicates = new ArrayList<>();

    private GunItemRenderer(
            List<BakedDraw> draws,
            List<BakedDraw> guiDraws,
            List<BakedDraw> entityDraws,
            List<BakedDraw> equippedDraws,
            List<BakedDraw> akimboDraws,
            List<BakedMuzzle> equippedMuzzles,
            List<BakedMuzzle> akimboMuzzles,
            List<Map<ItemDisplayContext, Matrix4f>> poseVariants,
            List<Matrix4fc> modTableVariants,
            int equippedShotConfig,
            int akimboShotConfig) {
        this.draws = draws;
        this.guiDraws = guiDraws;
        this.entityDraws = entityDraws;
        this.equippedDraws = equippedDraws;
        this.akimboDraws = akimboDraws;
        this.equippedMuzzles = equippedMuzzles;
        this.akimboMuzzles = akimboMuzzles;
        this.poseVariants = poseVariants;
        this.modTableVariants = modTableVariants;
        this.equippedShotConfig = equippedShotConfig;
        this.akimboShotConfig = akimboShotConfig;
        this.extents = extentsOf(entityDraws);
    }

    private static List<Vector3fc> extentsOf(List<BakedDraw> entityDraws) {
        List<Vector3fc> corners = new ArrayList<>();
        for (BakedDraw draw : entityDraws) {
            float[] bounds = draw.model().boundsOfParts(draw.parts());
            if (bounds == null) continue;
            for (int xi = 0; xi < 2; xi++)
                for (int yi = 0; yi < 2; yi++)
                    for (int zi = 0; zi < 2; zi++) {
                        corners.add(
                                draw.ops()
                                        .transformPosition(
                                                new Vector3f(
                                                        bounds[xi * 3],
                                                        bounds[yi * 3 + 1],
                                                        bounds[zi * 3 + 2])));
                    }
        }
        return List.copyOf(corners);
    }

    private static UpgradeState upgradeState(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return NO_UPGRADES;
        List<List<Integer>> configurations = new ArrayList<>(gun.getConfigCount());
        for (int config = 0; config < gun.getConfigCount(); config++) {
            int[] ids =
                    ItemGunBaseNT.getValueIntArray(stack, XWeaponModManager.KEY_MOD_LIST + config);
            List<Integer> snapshot = new ArrayList<>(ids.length);
            for (int id : ids) snapshot.add(id);
            configurations.add(List.copyOf(snapshot));
        }
        return new UpgradeState(List.copyOf(configurations));
    }

    private static boolean loaded(ItemStack stack) {
        if (!(stack.getItem() instanceof ItemGunBaseNT gun)) return false;
        IMagazine<?> magazine = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);

        return magazine != null && magazine.getAmount(stack, null) > 0;
    }

    private static @Nullable AmmoReference loadedAmmo(ItemStack stack) {
        if (!loaded(stack)) return null;
        ItemGunBaseNT gun = (ItemGunBaseNT) stack.getItem();
        IMagazine<?> magazine = gun.getConfig(stack, 0).getReceivers(stack)[0].getMagazine(stack);
        Object type = magazine.getType(stack, null);
        if (!(type instanceof BulletConfig bullet)) return null;

        if (bullet.ammoItem == null) return null;
        return new AmmoReference(BuiltInRegistries.ITEM.getKey(bullet.ammoItem.get()));
    }

    @Override
    public @Nullable Argument extractArgument(ItemStack stack) {
        return argumentFor(stack, DrawSet.WORLD, null);
    }

    private Argument argumentFor(
            ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner) {
        DrawSet set =
                switch (context) {
                    case GUI, NONE -> DrawSet.GUI;
                    case GROUND, FIXED, ON_SHELF -> DrawSet.ENTITY;
                    case THIRD_PERSON_RIGHT_HAND, THIRD_PERSON_LEFT_HAND ->
                            AkimboGhost.isGhost(stack) ? DrawSet.AKIMBO : DrawSet.EQUIPPED;
                    default -> DrawSet.WORLD;
                };
        FirstPerson firstPerson = null;
        if (context == ItemDisplayContext.FIRST_PERSON_RIGHT_HAND
                || context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND) {

            boolean live = HandPass.local(context, owner);
            boolean otherArm = context == ItemDisplayContext.FIRST_PERSON_LEFT_HAND;
            boolean drawn =
                    !AkimboGhost.isGhost(stack)
                            && (!live
                                    || (Minecraft.getInstance().player.getMainArm()
                                                    == HumanoidArm.RIGHT)
                                            != otherArm);
            firstPerson =
                    new FirstPerson(
                            drawn ? ItemRenderWeaponBase.get(stack) : null, stack, otherArm, live);
        }
        return argumentFor(stack, set, owner, firstPerson);
    }

    private Argument argumentFor(ItemStack stack, DrawSet set, @Nullable ItemOwner owner) {
        return argumentFor(stack, set, owner, null);
    }

    private Argument argumentFor(
            ItemStack stack,
            DrawSet set,
            @Nullable ItemOwner owner,
            @Nullable FirstPerson firstPerson) {
        UpgradeState upgrades = upgradeState(stack);
        AmmoReference loadedAmmo = loadedAmmo(stack);
        int poseVariant = poseVariantFor(upgrades);

        Shot shot = shot(stack, set, owner);
        boolean customTexture = false;
        int dark = -1;
        int light = -1;
        int grip = -1;
        if (stack.getItem() instanceof ItemGunNI4NI) {
            int[] colors = ItemGunNI4NI.getColors(stack);
            if (colors != null) {
                customTexture = true;
                dark = 0xFF000000 | colors[0];
                light = 0xFF000000 | colors[1];
                grip = 0xFF000000 | colors[2];
            }
        }
        return new Argument(
                set,
                upgrades,
                loaded(stack),
                loadedAmmo,
                poseVariant,
                shot.lastShot(),
                shot.shotRand(),
                customTexture,
                dark,
                light,
                grip,
                firstPerson);
    }

    public Shot shot(ItemStack stack, DrawSet set, @Nullable ItemOwner owner) {
        if ((set != DrawSet.EQUIPPED && set != DrawSet.AKIMBO)
                || !(stack.getItem() instanceof ItemGunBaseNT gun)
                || owner == null) return Shot.NONE;
        LivingEntity entity = owner.asLivingEntity();
        if (entity == null) return Shot.NONE;
        if (entity == Minecraft.getInstance().player) {
            return new Shot(
                    gun.lastShot[set == DrawSet.AKIMBO ? akimboShotConfig : equippedShotConfig],
                    (float) gun.shotRand);
        }
        return new Shot(ItemRenderWeaponBase.flashMap.getOrDefault(entity, -1L), 0F);
    }

    public Argument argument(
            ItemStack stack, ItemDisplayContext context, @Nullable ItemOwner owner) {
        return argumentFor(stack, context, owner);
    }

    public void visitDraws(Argument argument, DrawVisitor visitor) {
        List<BakedDraw> active =
                switch (argument.set()) {
                    case GUI -> guiDraws;
                    case ENTITY -> entityDraws;
                    case EQUIPPED -> equippedDraws;
                    case AKIMBO -> akimboDraws;
                    case WORLD -> draws;
                };
        for (int i = 0; i < active.size(); i++) {
            BakedDraw draw = active.get(i);
            if (!draw.matches(argument)) continue;
            Identifier texture =
                    argument.customTexture() && draw.customTexture() != null
                            ? draw.customTexture()
                            : draw.texture();
            visitor.accept(
                    i,
                    draw.model(),
                    draw.parts(),
                    draw.ops(),
                    texture,
                    draw.color().color(argument),
                    draw.fullBright(),
                    draw.glint() == DrawGlint.BALEFIRE);
        }
    }

    public void visitMuzzles(Argument argument, MuzzleVisitor visitor) {
        List<BakedMuzzle> muzzles =
                switch (argument.set()) {
                    case EQUIPPED -> equippedMuzzles;
                    case AKIMBO -> akimboMuzzles;
                    default -> List.of();
                };
        for (int i = 0; i < muzzles.size(); i++) {
            BakedMuzzle muzzle = muzzles.get(i);
            if (muzzle.matches(argument.upgrades())) visitor.accept(i, muzzle);
        }
    }

    public static void muzzlePose(PoseStack.Pose pose, BakedMuzzle muzzle, float shotRand) {
        pose.mulPose(muzzle.ops());
        if (muzzle.randomDegrees() != 0F)
            pose.rotate(Axis.XP.rotationDegrees(muzzle.randomDegrees() * shotRand));
    }

    @Override
    public ItemStackVisual createVisual(
            VisualizationContext ctx,
            @Nullable Argument argument,
            ItemStack stack,
            ItemDisplayContext context,
            @Nullable ItemOwner owner) {
        Argument arg = argument != null ? argument : argument(stack, context, owner);
        FirstPerson firstPerson = arg.firstPerson();
        if (firstPerson != null && !firstPerson.live() && firstPerson.renderer() != null) {
            return new GunRestVisual(ctx, firstPerson.renderer(), stack, firstPerson.otherArm());
        }
        return new GunItemVisual(ctx, this, arg, stack, context, owner);
    }

    private int poseVariantFor(UpgradeState upgrades) {
        for (int i = 1; i < poseVariants.size(); i++) {
            if (posePredicates.get(i - 1).matches(upgrades)) return i;
        }
        return 0;
    }

    public Matrix4fc modTable(ItemStack stack) {
        return modTableVariants.get(poseVariantFor(upgradeState(stack)));
    }

    private Matrix4fc poseFor(Argument argument, ItemDisplayContext context, Matrix4fc fallback) {
        Matrix4f pose = poseVariants.get(argument.poseVariant()).get(context);
        return pose != null ? pose : fallback;
    }

    @Override
    public void submit(
            @Nullable Argument argument,
            PoseStack ps,
            SubmitNodeCollector col,
            int lightCoords,
            int overlayCoords,
            boolean hasFoil,
            int outlineColor) {
        Argument arg =
                argument == null
                        ? new Argument(
                                DrawSet.WORLD,
                                NO_UPGRADES,
                                false,
                                null,
                                0,
                                -1L,
                                0F,
                                false,
                                -1,
                                -1,
                                -1,
                                null)
                        : argument;
        if (arg.firstPerson() != null) {
            FirstPerson firstPerson = arg.firstPerson();
            ItemRenderWeaponBase renderer = firstPerson.renderer();
            if (renderer == null) return;

            if (firstPerson.live()) {
                renderer.submitFirstPerson(
                        firstPerson.stack(), ps, col, lightCoords, firstPerson.otherArm());
            } else {
                renderer.submitRest(
                        firstPerson.stack(), ps, col, lightCoords, firstPerson.otherArm());
            }
            return;
        }
        visitDraws(
                arg,
                (index, model, parts, ops, texture, color, fullBright, balefire) -> {
                    int drawLight = fullBright ? LightCoordsUtil.FULL_BRIGHT : lightCoords;
                    col.submitCustomGeometry(
                            ps,
                            RenderTypes.entityCutout(texture),
                            (pose, buffer) -> {
                                PoseStack.Pose drawPose = pose;
                                if (!ops.equals(IDENTITY)) {
                                    PoseStack local = new PoseStack();
                                    local.last().set(pose);
                                    local.mulPose(ops);
                                    drawPose = local.last();
                                }
                                for (int part : parts)
                                    model.renderPart(drawPose, buffer, drawLight, color, part);
                            });
                    if (balefire) {
                        for (int layer = 0; layer < BALEFIRE_LAYERS; layer++) {
                            col.submitCustomGeometry(
                                    ps,
                                    WeaponRenderTypes.balefireGlint(
                                            ResourceManager.glint_bf_tex, layer),
                                    (pose, buffer) -> {
                                        PoseStack local = new PoseStack();
                                        local.last().set(pose);
                                        local.mulPose(ops);
                                        for (int part : parts)
                                            model.renderPart(
                                                    local.last(),
                                                    buffer,
                                                    drawLight,
                                                    WeaponRenderTypes.BALEFIRE_GLINT_TINT,
                                                    part);
                                    });
                        }
                    }
                });
        if (arg.lastShot() >= 0L)
            visitMuzzles(
                    arg,
                    (index, muzzle) -> {
                        PoseStack local = new PoseStack();
                        local.last().set(ps.last());
                        muzzlePose(local.last(), muzzle, arg.shotRand());
                        muzzle.kind()
                                .submit(
                                        col,
                                        local,
                                        arg.lastShot(),
                                        muzzle.duration(),
                                        muzzle.length(),
                                        muzzle.color());
                    });
    }

    @Override
    public void getExtents(Consumer<Vector3fc> output) {
        extents.forEach(output);
    }

    public enum DrawSet {
        WORLD,
        GUI,
        ENTITY,
        EQUIPPED,
        AKIMBO
    }

    public enum MagazineState {
        LOADED("loaded"),
        EMPTY("empty");

        static final Codec<MagazineState> CODEC =
                Codec.STRING.xmap(MagazineState::byName, s -> s.serializedName);
        private final String serializedName;

        MagazineState(String serializedName) {
            this.serializedName = serializedName;
        }

        private static MagazineState byName(String name) {
            for (MagazineState state : values())
                if (state.serializedName.equals(name)) return state;
            throw new IllegalArgumentException("Unknown gun magazine state: " + name);
        }
    }

    public enum DrawGlint {
        BALEFIRE("balefire");

        static final Codec<DrawGlint> CODEC =
                Codec.STRING.xmap(DrawGlint::byName, g -> g.serializedName);
        private final String serializedName;

        DrawGlint(String serializedName) {
            this.serializedName = serializedName;
        }

        private static DrawGlint byName(String name) {
            for (DrawGlint glint : values()) if (glint.serializedName.equals(name)) return glint;
            throw new IllegalArgumentException("Unknown gun draw glint: " + name);
        }
    }

    public enum DrawColor {
        WHITE("white"),
        NI4NI_DARK("ni4ni_dark"),
        NI4NI_LIGHT("ni4ni_light"),
        NI4NI_GRIP("ni4ni_grip"),
        NI4NI_COIN("ni4ni_coin");

        static final Codec<DrawColor> CODEC =
                Codec.STRING.xmap(DrawColor::byName, DrawColor::serializedName);
        private final String serializedName;

        DrawColor(String serializedName) {
            this.serializedName = serializedName;
        }

        private static DrawColor byName(String name) {
            for (DrawColor color : values()) if (color.serializedName.equals(name)) return color;
            throw new IllegalArgumentException("Unknown gun draw color: " + name);
        }

        private String serializedName() {
            return serializedName;
        }

        private int color(Argument argument) {
            return switch (this) {
                case WHITE -> -1;
                case NI4NI_DARK -> argument.dark();
                case NI4NI_LIGHT -> argument.light();
                case NI4NI_GRIP -> argument.grip();
                case NI4NI_COIN -> 0xFF00FF00;
            };
        }
    }

    public record ModReference(int config, int id) {
        public static final Codec<ModReference> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .optionalFieldOf("config", 0)
                                                        .forGetter(ModReference::config),
                                                Codec.INT.fieldOf("id").forGetter(ModReference::id))
                                        .apply(i, ModReference::new));
    }

    public record AmmoReference(Identifier item) {
        public static final Codec<AmmoReference> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                Identifier.CODEC
                                                        .fieldOf("item")
                                                        .forGetter(AmmoReference::item))
                                        .apply(i, AmmoReference::new));
    }

    public record UpgradeState(List<List<Integer>> configurations) {
        boolean has(ModReference reference) {
            return reference.config() >= 0
                    && reference.config() < configurations.size()
                    && configurations.get(reference.config()).contains(reference.id());
        }
    }

    public record Shot(long lastShot, float shotRand) {
        public static final Shot NONE = new Shot(-1L, 0F);
    }

    @FunctionalInterface
    public interface DrawVisitor {
        void accept(
                int index,
                HFRWavefrontObject model,
                int[] parts,
                Matrix4fc ops,
                Identifier texture,
                int color,
                boolean fullBright,
                boolean balefire);
    }

    @FunctionalInterface
    public interface MuzzleVisitor {
        void accept(int index, BakedMuzzle muzzle);
    }

    public record Argument(
            DrawSet set,
            UpgradeState upgrades,
            boolean loaded,
            @Nullable AmmoReference loadedAmmo,
            int poseVariant,
            long lastShot,
            float shotRand,
            boolean customTexture,
            int dark,
            int light,
            int grip,
            @Nullable FirstPerson firstPerson) {}

    public record FirstPerson(
            @Nullable ItemRenderWeaponBase renderer,
            ItemStack stack,
            boolean otherArm,
            boolean live) {}

    private record BakedDraw(
            HFRWavefrontObject model,
            Matrix4f ops,
            int[] parts,
            Identifier texture,
            @Nullable Identifier customTexture,
            DrawColor color,
            boolean fullBright,
            List<ModReference> requiresMods,
            List<ModReference> excludesMods,
            @Nullable AmmoReference requiresAmmo,
            List<AmmoReference> excludesAmmo,
            @Nullable MagazineState magazine,
            @Nullable DrawGlint glint) {
        boolean matches(Argument argument) {
            return requiresMods.stream().allMatch(argument.upgrades()::has)
                    && excludesMods.stream().noneMatch(argument.upgrades()::has)
                    && (requiresAmmo == null || requiresAmmo.equals(argument.loadedAmmo()))
                    && (argument.loadedAmmo() == null
                            || !excludesAmmo.contains(argument.loadedAmmo()))
                    && (magazine == null
                            || (magazine == MagazineState.LOADED) == argument.loaded());
        }
    }

    public record BakedMuzzle(
            Matrix4fc ops,
            MuzzleFlash kind,
            int duration,
            double length,
            int color,
            float randomDegrees,
            List<ModReference> requiresMods,
            List<ModReference> excludesMods) {
        boolean matches(UpgradeState upgrades) {
            return requiresMods.stream().allMatch(upgrades::has)
                    && excludesMods.stream().noneMatch(upgrades::has);
        }
    }

    public record Predicate(List<ModReference> requiresMods, List<ModReference> excludesMods) {
        static final Codec<Predicate> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("requires_mods", List.of())
                                                        .forGetter(Predicate::requiresMods),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("excludes_mods", List.of())
                                                        .forGetter(Predicate::excludesMods))
                                        .apply(i, Predicate::new));

        boolean matches(UpgradeState upgrades) {
            return requiresMods.stream().allMatch(upgrades::has)
                    && excludesMods.stream().noneMatch(upgrades::has);
        }
    }

    private record PosePredicate(List<ModReference> requiresMods, List<ModReference> excludesMods) {
        boolean matches(UpgradeState upgrades) {
            return requiresMods.stream().allMatch(upgrades::has)
                    && excludesMods.stream().noneMatch(upgrades::has);
        }
    }

    public record Draw(
            Optional<Matrix4fc> matrix,
            List<String> parts,
            Optional<Identifier> texture,
            Optional<Identifier> obj,
            DrawColor color,
            boolean fullBright,
            boolean untextured,
            List<ModReference> requiresMods,
            List<ModReference> excludesMods,
            Optional<AmmoReference> requiresAmmo,
            List<AmmoReference> excludesAmmo,
            Optional<MagazineState> magazine,
            Optional<DrawGlint> glint) {
        public static final Codec<Draw> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                ExtraCodecs.MATRIX4F
                                                        .optionalFieldOf("matrix")
                                                        .forGetter(Draw::matrix),
                                                Codec.STRING
                                                        .listOf()
                                                        .fieldOf("parts")
                                                        .forGetter(Draw::parts),
                                                Identifier.CODEC
                                                        .optionalFieldOf("texture")
                                                        .forGetter(Draw::texture),
                                                Identifier.CODEC
                                                        .optionalFieldOf("obj")
                                                        .forGetter(Draw::obj),
                                                DrawColor.CODEC
                                                        .optionalFieldOf("color", DrawColor.WHITE)
                                                        .forGetter(Draw::color),
                                                Codec.BOOL
                                                        .optionalFieldOf("fullbright", false)
                                                        .forGetter(Draw::fullBright),
                                                Codec.BOOL
                                                        .optionalFieldOf("untextured", false)
                                                        .forGetter(Draw::untextured),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("requires_mods", List.of())
                                                        .forGetter(Draw::requiresMods),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("excludes_mods", List.of())
                                                        .forGetter(Draw::excludesMods),
                                                AmmoReference.CODEC
                                                        .optionalFieldOf("requires_ammo")
                                                        .forGetter(Draw::requiresAmmo),
                                                AmmoReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("excludes_ammo", List.of())
                                                        .forGetter(Draw::excludesAmmo),
                                                MagazineState.CODEC
                                                        .optionalFieldOf("magazine")
                                                        .forGetter(Draw::magazine),
                                                DrawGlint.CODEC
                                                        .optionalFieldOf("glint")
                                                        .forGetter(Draw::glint))
                                        .apply(i, Draw::new));
    }

    public record PoseOverride(
            Optional<Matrix4fc> gui,
            Optional<Matrix4fc> thirdPerson,
            Optional<Matrix4fc> entity,
            List<ModReference> requiresMods,
            List<ModReference> excludesMods) {
        public static final Codec<PoseOverride> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                ExtraCodecs.MATRIX4F
                                                        .optionalFieldOf("gui")
                                                        .forGetter(PoseOverride::gui),
                                                ExtraCodecs.MATRIX4F
                                                        .optionalFieldOf("third_person")
                                                        .forGetter(PoseOverride::thirdPerson),
                                                ExtraCodecs.MATRIX4F
                                                        .optionalFieldOf("entity")
                                                        .forGetter(PoseOverride::entity),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("requires_mods", List.of())
                                                        .forGetter(PoseOverride::requiresMods),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("excludes_mods", List.of())
                                                        .forGetter(PoseOverride::excludesMods))
                                        .apply(i, PoseOverride::new));
    }

    public record Muzzle(
            Optional<Matrix4fc> matrix,
            String kind,
            int duration,
            double length,
            int color,
            float randomDegrees,
            List<ModReference> requiresMods,
            List<ModReference> excludesMods) {
        public static final Codec<Muzzle> CODEC =
                RecordCodecBuilder.create(
                        i ->
                                i.group(
                                                ExtraCodecs.MATRIX4F
                                                        .optionalFieldOf("matrix")
                                                        .forGetter(Muzzle::matrix),
                                                Codec.STRING
                                                        .optionalFieldOf("kind", "normal")
                                                        .forGetter(Muzzle::kind),
                                                Codec.INT
                                                        .fieldOf("duration")
                                                        .forGetter(Muzzle::duration),
                                                Codec.DOUBLE
                                                        .fieldOf("length")
                                                        .forGetter(Muzzle::length),
                                                Codec.INT
                                                        .optionalFieldOf("color", 0xFFFFFF)
                                                        .forGetter(Muzzle::color),
                                                Codec.FLOAT
                                                        .optionalFieldOf("random_degrees", 0F)
                                                        .forGetter(Muzzle::randomDegrees),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("requires_mods", List.of())
                                                        .forGetter(Muzzle::requiresMods),
                                                ModReference.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("excludes_mods", List.of())
                                                        .forGetter(Muzzle::excludesMods))
                                        .apply(i, Muzzle::new));
    }

    public record ShotConfigs(int equipped, int akimbo) {

        public static final MapCodec<ShotConfigs> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                Codec.INT
                                                        .optionalFieldOf("equipped_shot_config", 0)
                                                        .forGetter(ShotConfigs::equipped),
                                                Codec.INT
                                                        .optionalFieldOf("akimbo_shot_config", 0)
                                                        .forGetter(ShotConfigs::akimbo))
                                        .apply(i, ShotConfigs::new));
    }

    public record Unbaked(
            Identifier base,
            Optional<Identifier> akimboBase,
            Optional<Matrix4fc> modTable,
            Identifier obj,
            Identifier texture,
            Optional<Identifier> customTexture,
            ShotConfigs shotConfigs,
            Optional<List<String>> parts,
            Optional<List<Draw>> draws,
            Optional<List<Draw>> guiDraws,
            Optional<List<Draw>> entityDraws,
            Optional<List<Draw>> equippedDraws,
            Optional<List<Draw>> akimboDraws,
            Optional<List<PoseOverride>> poseOverrides,
            Optional<List<Muzzle>> equippedMuzzles,
            Optional<List<Muzzle>> akimboMuzzles)
            implements ItemModel.Unbaked {

        public static final MapCodec<Unbaked> MAP_CODEC =
                RecordCodecBuilder.mapCodec(
                        i ->
                                i.group(
                                                Identifier.CODEC
                                                        .fieldOf("base")
                                                        .forGetter(Unbaked::base),
                                                Identifier.CODEC
                                                        .optionalFieldOf("akimbo_base")
                                                        .forGetter(Unbaked::akimboBase),
                                                ExtraCodecs.MATRIX4F
                                                        .optionalFieldOf("mod_table")
                                                        .forGetter(Unbaked::modTable),
                                                Identifier.CODEC
                                                        .fieldOf("obj")
                                                        .forGetter(Unbaked::obj),
                                                Identifier.CODEC
                                                        .fieldOf("texture")
                                                        .forGetter(Unbaked::texture),
                                                Identifier.CODEC
                                                        .optionalFieldOf("custom_texture")
                                                        .forGetter(Unbaked::customTexture),
                                                ShotConfigs.MAP_CODEC.forGetter(
                                                        Unbaked::shotConfigs),
                                                Codec.STRING
                                                        .listOf()
                                                        .optionalFieldOf("parts")
                                                        .forGetter(Unbaked::parts),
                                                Draw.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("draws")
                                                        .forGetter(Unbaked::draws),
                                                Draw.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("gui_draws")
                                                        .forGetter(Unbaked::guiDraws),
                                                Draw.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("entity_draws")
                                                        .forGetter(Unbaked::entityDraws),
                                                Draw.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("equipped_draws")
                                                        .forGetter(Unbaked::equippedDraws),
                                                Draw.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("akimbo_draws")
                                                        .forGetter(Unbaked::akimboDraws),
                                                PoseOverride.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("pose_overrides")
                                                        .forGetter(Unbaked::poseOverrides),
                                                Muzzle.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("equipped_muzzles")
                                                        .forGetter(Unbaked::equippedMuzzles),
                                                Muzzle.CODEC
                                                        .listOf()
                                                        .optionalFieldOf("akimbo_muzzles")
                                                        .forGetter(Unbaked::akimboMuzzles))
                                        .apply(i, Unbaked::new));

        private static Map<ItemDisplayContext, Matrix4f> slotDeltas(PoseOverride override) {
            EnumMap<ItemDisplayContext, Matrix4f> out = new EnumMap<>(ItemDisplayContext.class);
            override.gui().ifPresent(m -> out.put(ItemDisplayContext.GUI, new Matrix4f(m)));
            override.thirdPerson()
                    .ifPresent(
                            m -> {
                                out.put(
                                        ItemDisplayContext.THIRD_PERSON_RIGHT_HAND,
                                        new Matrix4f(m));
                                out.put(ItemDisplayContext.THIRD_PERSON_LEFT_HAND, new Matrix4f(m));
                            });
            override.entity()
                    .ifPresent(
                            m -> {
                                out.put(ItemDisplayContext.GROUND, new Matrix4f(m));
                                out.put(ItemDisplayContext.FIXED, new Matrix4f(m));
                                out.put(ItemDisplayContext.ON_SHELF, new Matrix4f(m));
                                out.put(ItemDisplayContext.HEAD, new Matrix4f(m));
                            });
            return out;
        }

        private static Matrix4fc validatedModTable(Matrix4fc body) {
            float determinant = new Matrix4f(body).determinant3x3();
            if (determinant >= 0F) {
                throw new IllegalStateException(
                        "mod_table must carry a negative uniform scale, as every 1.7 "
                                + "setupModTable does; this body's 3x3 determinant is "
                                + determinant);
            }
            float scale = (float) Math.cbrt(-determinant);
            Matrix3f rotation = new Matrix3f(new Matrix4f(body)).scale(-1F / scale);
            if (!rotation.mul(new Matrix3f(rotation).transpose(), new Matrix3f())
                    .equals(new Matrix3f(), 1.0E-4F)) {
                throw new IllegalStateException(
                        "mod_table must be a uniform scale times a rotation, since the "
                                + "weapon table's picture-in-picture takes a scale and a quaternion; this body is not");
            }
            return body;
        }

        private static List<BakedMuzzle> bakeMuzzles(Optional<List<Muzzle>> list) {
            return list.orElse(List.of()).stream()
                    .map(
                            m ->
                                    new BakedMuzzle(
                                            m.matrix().map(Matrix4f::new).orElseGet(Matrix4f::new),
                                            MuzzleFlash.byName(m.kind()),
                                            m.duration(),
                                            m.length(),
                                            m.color(),
                                            m.randomDegrees(),
                                            List.copyOf(m.requiresMods()),
                                            List.copyOf(m.excludesMods())))
                    .toList();
        }

        @Override
        public void resolveDependencies(ResolvableModel.Resolver resolver) {
            resolver.markDependency(base);
            akimboBase.ifPresent(resolver::markDependency);
        }

        private List<BakedDraw> bakeDraws(
                List<Draw> list,
                HFRWavefrontObject defaultModel,
                Map<Identifier, HFRWavefrontObject> alternateModels) {
            return list.stream()
                    .map(
                            d -> {
                                Identifier drawTexture =
                                        d.untextured()
                                                ? RenderTextures.WHITE
                                                : d.texture().orElse(texture);
                                @Nullable Identifier drawCustomTexture =
                                        !d.untextured() && d.texture().isEmpty()
                                                ? customTexture.orElse(null)
                                                : null;
                                HFRWavefrontObject drawModel =
                                        d.obj()
                                                .map(
                                                        id ->
                                                                alternateModels.computeIfAbsent(
                                                                        id,
                                                                        model ->
                                                                                new HFRWavefrontObject(
                                                                                        Minecraft
                                                                                                .getInstance()
                                                                                                .getResourceManager(),
                                                                                        model)))
                                                .orElse(defaultModel);
                                return new BakedDraw(
                                        drawModel,
                                        d.matrix().map(Matrix4f::new).orElseGet(Matrix4f::new),
                                        drawModel.partIds(d.parts()),
                                        drawTexture,
                                        drawCustomTexture,
                                        d.color(),
                                        d.fullBright(),
                                        List.copyOf(d.requiresMods()),
                                        List.copyOf(d.excludesMods()),
                                        d.requiresAmmo().orElse(null),
                                        List.copyOf(d.excludesAmmo()),
                                        d.magazine().orElse(null),
                                        d.glint().orElse(null));
                            })
                    .toList();
        }

        @Override
        public ItemModel bake(ItemModel.BakingContext context, Matrix4fc transformation) {
            HFRWavefrontObject model =
                    new HFRWavefrontObject(Minecraft.getInstance().getResourceManager(), obj);
            Map<Identifier, HFRWavefrontObject> alternateModels = new HashMap<>();
            List<BakedDraw> baked =
                    draws.map(d -> bakeDraws(d, model, alternateModels))
                            .orElseGet(
                                    () ->
                                            List.of(
                                                    new BakedDraw(
                                                            model,
                                                            new Matrix4f(),
                                                            model.partIds(
                                                                    parts.orElseThrow(
                                                                            () ->
                                                                                    new IllegalStateException(
                                                                                            "hbm:gun needs either 'parts' or 'draws'"))),
                                                            texture,
                                                            null,
                                                            DrawColor.WHITE,
                                                            false,
                                                            List.of(),
                                                            List.of(),
                                                            null,
                                                            List.of(),
                                                            null,
                                                            null)));
            List<BakedDraw> gui =
                    guiDraws.map(d -> bakeDraws(d, model, alternateModels)).orElse(baked);
            List<BakedDraw> entity =
                    entityDraws.map(d -> bakeDraws(d, model, alternateModels)).orElse(baked);
            List<BakedDraw> equipped =
                    equippedDraws.map(d -> bakeDraws(d, model, alternateModels)).orElse(baked);
            List<BakedDraw> akimbo =
                    akimboDraws.map(d -> bakeDraws(d, model, alternateModels)).orElse(equipped);
            List<Map<ItemDisplayContext, Matrix4f>> variants = new ArrayList<>();
            List<Matrix4fc> modTables = new ArrayList<>();
            variants.add(Map.of());
            modTables.add(
                    modTable.<Matrix4fc>map(Matrix4f::new)
                            .map(Unbaked::validatedModTable)
                            .orElse(MOD_TABLE_BASE));
            List<PoseOverride> overrides = poseOverrides.orElse(List.of());
            for (PoseOverride override : overrides) {
                variants.add(slotDeltas(override));
                modTables.add(modTables.get(0));
            }
            GunItemRenderer renderer =
                    new GunItemRenderer(
                            baked,
                            gui,
                            entity,
                            equipped,
                            akimbo,
                            bakeMuzzles(equippedMuzzles),
                            bakeMuzzles(akimboMuzzles),
                            List.copyOf(variants),
                            List.copyOf(modTables),
                            shotConfigs.equipped(),
                            shotConfigs.akimbo());
            for (PoseOverride override : overrides)
                renderer.posePredicates.add(
                        new PosePredicate(
                                List.copyOf(override.requiresMods()),
                                List.copyOf(override.excludesMods())));

            ModelRenderProperties weapon = DynamicSpecialWrapper.properties(context, base);
            ModelRenderProperties secondGun =
                    akimboBase
                            .map(id -> DynamicSpecialWrapper.properties(context, id))
                            .orElse(weapon);
            return new DynamicSpecialWrapper<>(
                    renderer,
                    (stack, ctx, owner) -> renderer.argumentFor(stack, ctx, owner),
                    renderer::poseFor,
                    false,
                    stack -> AkimboGhost.isGhost(stack) ? secondGun : weapon,
                    Map.of());
        }

        @Override
        public MapCodec<Unbaked> type() {
            return MAP_CODEC;
        }
    }
}
