/*
 * SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
 * SPDX-License-Identifier: Apache-2.0
 *
 * Copyright (C) 2026 movblock. All rights reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * You may not use this file under the Apache License except in
 * compliance with that License. You may obtain a copy at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the Apache License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the Apache License for the specific language governing permissions
 * and limitations under the License.
 */

package com.hbm.client.render.flywheel;

import com.hbm.blocks.generic.BobbleBlock.Bobble;
import com.hbm.blocks.generic.BobbleBlock.BobbleType;
import com.hbm.client.render.BobbleFigures;
import com.hbm.client.render.BobbleRenderer;
import com.hbm.main.ResourceManager;
import com.hbm.render.loader.HFRWavefrontObject;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.lib.material.CutoutShaders;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Supplier;
import net.minecraft.resources.Identifier;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import org.joml.Matrix4f;
import org.joml.Matrix4fc;
import org.joml.Quaternionfc;
import org.jspecify.annotations.Nullable;

final class BobbleDraws {
    private static final Material SOCKET = material(ResourceManager.bobble_socket_tex, true);
    private static final Material SHINE =
            fixed(
                    ResourceManager.white_tex,
                    Transparency.ORDER_INDEPENDENT,
                    CardinalLightingMode.OFF,
                    true);
    private static final MeshPart SOCKET_MESH =
            mesh(ResourceManager.bobble, BobbleRenderer.SOCKET_PART, SOCKET);
    private static final MeshPart DRILLGON_MESH =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.DRILLGON,
                    material(ResourceManager.bobble_drillgon_tex, true));
    private static final SkinMeshes[] GUY_MESHES = buildGuyMeshes();
    private static final Map<Bobble, SkinMeshes> FIGURE_MESHES = new ConcurrentHashMap<>();
    private static final Map<Bobble, MeshPart> FIGURE_BODIES = new ConcurrentHashMap<>();
    private static final SkinMeshes MELLOW_MESHES =
            skinMeshes(
                    fixed(
                            ResourceManager.bobble_mellow_glow_tex,
                            Transparency.ORDER_INDEPENDENT,
                            CardinalLightingMode.CHUNK,
                            false),
                    BobbleRenderer.LAYERED);
    private static final SkinMeshes ABEL_MESHES =
            skinMeshes(
                    fixed(
                            ResourceManager.bobble_abel_glow_tex,
                            Transparency.ORDER_INDEPENDENT,
                            CardinalLightingMode.CHUNK,
                            false),
                    BobbleRenderer.LAYERED);
    private static final MeshPart PEEP_TAIL_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.PEEP_TAIL,
                    material(ResourceManager.bobble_peep_tex, true));
    private static final MeshPart PEEP_HAT_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.PEEP_HAT,
                    blended(ResourceManager.bobble_peep_tex));
    private static final MeshPart HORN_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.HORN,
                    blended(ResourceManager.bobble_vt_tex));
    private static final MeshPart PELLET_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.PELLET,
                    SimpleMaterial.builderOf(material(ResourceManager.bobble_pu238_tex, true))
                            .cardinalLightingMode(CardinalLightingMode.OFF)
                            .build());
    private static final MeshPart PELLET_SHINE_PART =
            mesh(ResourceManager.bobble, BobbleRenderer.PELLET_SHINE, SHINE);
    private static final MeshPart FUMO_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.FUMO,
                    material(ResourceManager.bobble_uffr_tex, true));
    private static final MeshPart FUMO_HEAD_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.FUMO_HEAD,
                    material(ResourceManager.bobble_uffr_tex, false));
    private static final MeshPart HEV_HEAD_PART =
            mesh(
                    ResourceManager.armor_hev,
                    BobbleRenderer.HEV_HEAD,
                    material(ResourceManager.hev_helmet_tex, true));
    private static final MeshPart[] HEV_HAT_PARTS =
            buildParts(ResourceManager.armor_hat, blended(ResourceManager.hat_tex));
    private static final MeshPart[] NI4NI_PARTS =
            meshParts(
                    ResourceManager.n_i_4_n_i,
                    BobbleRenderer.NI4NI,
                    material(ResourceManager.n_i_4_n_i_tex, true));
    private static final MeshPart[] SHIMMER_AXE_PARTS =
            buildParts(
                    ResourceManager.shimmer_axe, material(ResourceManager.shimmer_axe_tex, true));
    private static final MeshPart MINI_NUKE_PART =
            mesh(
                    ResourceManager.fatman,
                    BobbleRenderer.MINI_NUKE,
                    material(ResourceManager.fatman_mininuke_tex, true));
    private static final MeshPart[] DOUBLE_BARREL_PARTS =
            meshParts(
                    ResourceManager.double_barrel,
                    BobbleRenderer.DOUBLE_BARREL,
                    material(ResourceManager.double_barrel_sacred_dragon_tex, true));
    private static final MeshPart FLUORO_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.FLUORO,
                    fixed(
                            ResourceManager.fluorescent_lamp_tex,
                            Transparency.ORDER_INDEPENDENT_ADDITIVE,
                            CardinalLightingMode.CHUNK,
                            true));
    private static final MeshPart GLOW_PART =
            mesh(
                    ResourceManager.bobble,
                    BobbleRenderer.GLOW,
                    fixed(
                            ResourceManager.bobble_glow_tex,
                            Transparency.ORDER_INDEPENDENT_ADDITIVE,
                            CardinalLightingMode.CHUNK,
                            true));

    final List<Draw> draws = new ArrayList<>();
    final Matrix4f propLocal = new Matrix4f();
    @Nullable Matrix4f propPivot;
    final Matrix4f labelLocal;

    BobbleDraws(Bobble type, PoseStack root) {
        part(root, SOCKET_MESH, 0xFFFFFFFF, 0);
        Supplier<BobbleRenderer.Body> body =
                type instanceof BobbleType ? null : BobbleFigures.get(type).body();
        if (body != null) {
            part(
                    root,
                    FIGURE_BODIES.computeIfAbsent(
                            type,
                            key ->
                                    mesh(
                                            body.get().mesh(),
                                            body.get().group(),
                                            material(BobbleRenderer.skinTexture(key), true))),
                    0xFFFFFFFF,
                    0);
        } else if (!(type instanceof BobbleType ntm)) {
            renderGuy(root, type, FIGURE_MESHES.computeIfAbsent(type, BobbleDraws::guyMeshes), 0);
        } else {
            switch (ntm) {
                case PU238 -> renderPellet(root);
                case UFFR -> renderFumo(root);
                case DRILLGON -> part(root, DRILLGON_MESH, 0xFFFFFFFF, 0);
                default -> renderGuy(root, type, GUY_MESHES[ntm.ordinal()], 0);
            }
        }
        root.pushPose();
        renderPost(type, root);
        root.popPose();
        labelLocal = BobbleRenderer.labelBase(new Matrix4f(root.last().pose()));
    }

    static Matrix4f bob(
            Matrix4f out, Matrix4fc anchor, Matrix4fc pivot, long time, Matrix4fc pose) {
        return out.set(anchor)
                .mul(pivot)
                .rotateX(BobbleRenderer.wobbleX(time) * Mth.DEG_TO_RAD)
                .rotateZ(BobbleRenderer.wobbleZ(time) * Mth.DEG_TO_RAD)
                .mul(pose);
    }

    private static SkinMeshes[] buildGuyMeshes() {
        SkinMeshes[] meshes = new SkinMeshes[BobbleType.values().length];
        for (BobbleType type : BobbleType.values()) meshes[type.ordinal()] = guyMeshes(type);
        return meshes;
    }

    private static SkinMeshes guyMeshes(Bobble type) {
        return skinMeshes(
                blended(BobbleRenderer.skinTexture(type)),
                type.skinLayers() ? BobbleRenderer.LAYERED : BobbleRenderer.FLAT);
    }

    private static SkinMeshes skinMeshes(Material material, BobbleRenderer.Skin skin) {
        return new SkinMeshes(
                mesh(ResourceManager.bobble, skin.leftLeg(), material),
                mesh(ResourceManager.bobble, skin.rightLeg(), material),
                mesh(ResourceManager.bobble, skin.leftArm(), material),
                mesh(ResourceManager.bobble, skin.rightArm(), material),
                mesh(ResourceManager.bobble, skin.body(), material),
                mesh(ResourceManager.bobble, skin.head(), material));
    }

    private static MeshPart mesh(HFRWavefrontObject model, int group, Material material) {
        return MeshPart.obj(model.groups[group], model.smoothing(), material);
    }

    private static MeshPart[] meshParts(HFRWavefrontObject model, int[] groups, Material material) {
        MeshPart[] parts = new MeshPart[groups.length];
        for (int i = 0; i < groups.length; i++) parts[i] = mesh(model, groups[i], material);
        return parts;
    }

    private static MeshPart[] buildParts(HFRWavefrontObject model, Material material) {
        int[] groups = new int[model.groups.length];
        for (int i = 0; i < groups.length; i++) groups[i] = i;
        return meshParts(model, groups, material);
    }

    private static Material material(Identifier texture, boolean cull) {
        return SimpleMaterial.builderOf(Materials.CUTOUT)
                .texture(texture)
                .mipmap(false)
                .cutout(CutoutShaders.ONE_TENTH)
                .light(LightShaders.SMOOTH)
                .ambientOcclusion(false)
                .cardinalLightingMode(CardinalLightingMode.CHUNK)
                .backfaceCulling(cull)
                .build();
    }

    private static Material blended(Identifier texture) {
        return SimpleMaterial.builderOf(material(texture, false))
                .cutout(CutoutShaders.TINY)
                .transparency(Transparency.ORDER_INDEPENDENT)
                .writeMask(WriteMask.COLOR)
                .build();
    }

    private static Material fixed(
            Identifier texture,
            Transparency transparency,
            CardinalLightingMode cardinal,
            boolean cull) {
        return SimpleMaterial.builder()
                .texture(texture)
                .mipmap(false)
                .cutout(CutoutShaders.TINY)
                .useLight(false)
                .useOverlay(false)
                .cardinalLightingMode(cardinal)
                .transparency(transparency)
                .writeMask(WriteMask.COLOR)
                .backfaceCulling(cull)
                .build();
    }

    private void renderGuy(PoseStack pose, Bobble type, SkinMeshes skin, int light) {
        BobbleRenderer.Pose angles = BobbleRenderer.pose(type);
        pose.pushPose();
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees((float) angles.body()));
        if (type == BobbleType.PEEP) part(pose, PEEP_TAIL_PART, -1, light);
        limb(pose, skin.leftLeg, 0D, 1D, -.125D, angles.leftLeg(), light);
        limb(pose, skin.rightLeg, 0D, 1D, .125D, angles.rightLeg(), light);
        limb(pose, skin.leftArm, 0D, 1.625D, -.25D, angles.leftArm(), light);
        limb(pose, skin.rightArm, 0D, 1.625D, .25D, angles.rightArm(), light);
        part(pose, skin.body, -1, light);
        pose.pushPose();
        pose.translate(0D, 1.75D, 0D);
        Matrix4f pivot = new Matrix4f(pose.last().pose());
        PoseStack head = new PoseStack();
        head.mulPose((Quaternionfc) Axis.XP.rotationDegrees((float) angles.head()[0]));
        head.mulPose((Quaternionfc) Axis.YP.rotationDegrees((float) angles.head()[1]));
        head.mulPose((Quaternionfc) Axis.ZP.rotationDegrees((float) angles.head()[2]));
        head.translate(0D, -1.75D, 0D);
        bobbing(pivot, head, skin.head, -1, light);
        if (type == BobbleType.VT) bobbing(pivot, head, HORN_PART, -1, light);
        if (type == BobbleType.PEEP) bobbing(pivot, head, PEEP_HAT_PART, -1, light);
        if (type == BobbleType.VAER) holdProp(type, head, pivot);
        if (type == BobbleType.NOS) {
            head.translate(0D, 1.75D, 0D);
            head.mulPose((Quaternionfc) Axis.XP.rotationDegrees(180F));
            head.scale(.095F, .095F, .095F);
            for (MeshPart part : HEV_HAT_PARTS) bobbing(pivot, head, part, -1, light);
        }
        pose.popPose();
        pose.popPose();
    }

    private void limb(
            PoseStack pose,
            MeshPart part,
            double x,
            double y,
            double z,
            double[] rotation,
            int light) {
        pose.pushPose();
        pose.translate(x, y, z);
        pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees((float) rotation[0]));
        pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees((float) rotation[1]));
        pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees((float) rotation[2]));
        pose.translate(-x, -y, -z);
        part(pose, part, -1, light);
        pose.popPose();
    }

    private void renderPellet(PoseStack pose) {
        part(pose, PELLET_PART, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
        draws.add(
                new Draw(
                        PELLET_SHINE_PART,
                        new Matrix4f(pose.last().pose()),
                        null,
                        0,
                        LightCoordsUtil.FULL_BRIGHT,
                        true));
    }

    private void renderFumo(PoseStack pose) {
        part(pose, FUMO_PART, 0xFFFFFFFF, 0);
        pose.pushPose();
        pose.translate(0D, .75D, 0D);
        PoseStack head = new PoseStack();
        head.translate(0D, -.75D, 0D);
        bobbing(new Matrix4f(pose.last().pose()), head, FUMO_HEAD_PART, 0xFFFFFFFF, 0);
        pose.popPose();
    }

    private void renderPost(Bobble type, PoseStack pose) {
        if (!(type instanceof BobbleType ntm)) return;
        switch (ntm) {
            case BLUEHAT -> {
                pose.translate(0D, .875D, -.5D);
                pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(-90F));
                pose.mulPose((Quaternionfc) Axis.ZP.rotationDegrees(-160F));
                pose.scale(.0625F, .0625F, .0625F);
                part(pose, HEV_HEAD_PART, 0xFFFFFFFF, 0);
            }
            case FRIZZLE -> {
                pose.pushPose();
                pose.translate(.8D, 1.6D, .4D);
                pose.scale(.125F, .125F, .125F);
                pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(90F));
                pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(10F));
                for (MeshPart part : NI4NI_PARTS) part(pose, part, -1, 0);
                pose.popPose();
                holdProp(type, pose, null);
            }
            case ADAM29 -> holdProp(type, pose, null);
            case PHEO -> {
                pose.translate(.5D, 1.15D, .45D);
                pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(-60F));
                pose.scale(2F, 2F, 2F);
                for (MeshPart part : SHIMMER_AXE_PARTS) part(pose, part, -1, 0);
            }
            case BOB -> {
                pose.pushPose();
                pose.translate(0D, .6875D, .625D);
                pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(-90F));
                pose.scale(.125F, .125F, .125F);
                pose.translate(-6D, 0D, 0D);
                for (int i = -1; i <= 1; i++) {
                    pose.translate(3D, 0D, 0D);
                    part(pose, MINI_NUKE_PART, -1, 0);
                }
                pose.popPose();
                pose.pushPose();
                pose.translate(.25D, .3125D, -.5D);
                pose.mulPose((Quaternionfc) Axis.XP.rotationDegrees(-90F));
                pose.mulPose((Quaternionfc) Axis.YP.rotationDegrees(90F));
                pose.scale(.1F, .1F, .1F);
                for (MeshPart part : DOUBLE_BARREL_PARTS) part(pose, part, -1, 0);
                pose.popPose();
            }
            case MELLOW -> {
                renderGuy(pose, type, MELLOW_MESHES, LightCoordsUtil.FULL_BRIGHT);
                part(pose, FLUORO_PART, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
                part(pose, GLOW_PART, 0xFFFFFFFF, LightCoordsUtil.FULL_BRIGHT);
            }
            case ABEL -> renderGuy(pose, type, ABEL_MESHES, LightCoordsUtil.FULL_BRIGHT);
            case NONE,
                    STRENGTH,
                    PERCEPTION,
                    ENDURANCE,
                    CHARISMA,
                    INTELLIGENCE,
                    AGILITY,
                    LUCK,
                    DOC,
                    VT,
                    UFFR,
                    VAER,
                    NOS,
                    DRILLGON,
                    CIRNO,
                    MICROWAVE,
                    PEEP,
                    PU238 -> {}
        }
    }

    private void holdProp(Bobble type, PoseStack pose, @Nullable Matrix4f pivot) {
        BobbleRenderer.propPose(type, pose);
        propLocal.set(pose.last().pose());
        propPivot = pivot;
    }

    private void part(PoseStack pose, MeshPart part, int color, int light) {
        draws.add(new Draw(part, new Matrix4f(pose.last().pose()), null, color, light, false));
    }

    private void bobbing(Matrix4f pivot, PoseStack head, MeshPart part, int color, int light) {
        draws.add(new Draw(part, new Matrix4f(head.last().pose()), pivot, color, light, false));
    }

    record Draw(
            MeshPart part,
            Matrix4f pose,
            @Nullable Matrix4f pivot,
            int color,
            int light,
            boolean shine) {}

    private record SkinMeshes(
            MeshPart leftLeg,
            MeshPart rightLeg,
            MeshPart leftArm,
            MeshPart rightArm,
            MeshPart body,
            MeshPart head) {}
}
