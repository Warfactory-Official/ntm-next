// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.projectile.EntityRubble;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.math.Axis;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.block.BlockStateModelSet;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

public class RenderRubble extends EntityRenderer<EntityRubble, RenderRubble.State>
        implements ConcurrentRenderStateExtraction {

    private static final Axis TUMBLE = Axis.of(new Vector3f(1F, 1F, 1F));

    private final ModelPart model = ModelRubble.createBodyLayer().bakeRoot();
    private final Map<BlockState, RenderType> skins = new ConcurrentHashMap<>();

    public RenderRubble(EntityRendererProvider.Context context) {
        super(context);

        this.shadowRadius = 0F;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityRubble entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.skin = skins.computeIfAbsent(entity.getBlockState(), RenderRubble::skinFor);
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector collector,
            CameraRenderState camera) {
        poseStack.pushPose();
        poseStack.mulPose(Axis.XP.rotationDegrees(180F));
        poseStack.mulPose(TUMBLE.rotationDegrees(state.ageInTicks % 360F * 10F));
        collector.submitModelPart(
                model, poseStack, state.skin, state.lightCoords, OverlayTexture.NO_OVERLAY, null);
        poseStack.popPose();
        super.submit(state, poseStack, collector, camera);
    }

    private static RenderType skinFor(BlockState state) {
        Identifier name = downSprite(state).contents().name();
        return RenderTypes.entityCutoutCull(
                Identifier.fromNamespaceAndPath(
                        name.getNamespace(), "textures/" + name.getPath() + ".png"));
    }

    private static TextureAtlasSprite downSprite(BlockState state) {
        BlockStateModelSet models =
                Minecraft.getInstance().getModelManager().getBlockStateModelSet();
        BlockStateModel model = models.get(state);
        List<BlockStateModelPart> parts = new ArrayList<>(1);
        model.collectParts(RandomSource.create(state.getSeed(BlockPos.ZERO)), parts);
        for (BlockStateModelPart part : parts) {
            List<BakedQuad> quads = part.getQuads(Direction.DOWN);
            if (!quads.isEmpty()) return quads.getFirst().materialInfo().sprite();
        }
        return model.particleMaterial().sprite();
    }

    public static final class State extends EntityRenderState {
        RenderType skin;
    }
}
