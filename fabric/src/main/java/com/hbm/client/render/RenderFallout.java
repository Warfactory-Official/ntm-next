// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render;

import com.hbm.entity.effect.EntityFalloutRain;
import com.hbm.lib.Library;
import com.hbm.render.util.Vertices;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import dev.engine_room.flywheel.api.visualization.ConcurrentRenderStateExtraction;
import java.util.Random;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.culling.Frustum;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.client.renderer.rendertype.RenderType;
import net.minecraft.client.renderer.state.level.CameraRenderState;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import net.minecraft.util.LightCoordsUtil;
import net.minecraft.util.Mth;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.level.levelgen.Heightmap;
import net.minecraft.world.phys.Vec3;

public class RenderFallout extends EntityRenderer<EntityFalloutRain, RenderFallout.State>
        implements ConcurrentRenderStateExtraction {

    private static final Identifier TEXTURE = Library.id("textures/entity/fallout.png");
    private static final RenderType RENDER_TYPE =
            WorldRenderPipeline.oneSidedTranslucent(TEXTURE, false);
    private static final int TABLE_SIZE = 32;
    private static final int TABLE_HALF = 16;
    private static final int TABLE_MASK = TABLE_SIZE - 1;

    private static final double RENDER_EDGE = (4.0 + 20.0 + 4.0) / 3.0;

    private final float[] rainXCoords = new float[TABLE_SIZE * TABLE_SIZE];
    private final float[] rainZCoords = new float[TABLE_SIZE * TABLE_SIZE];

    public RenderFallout(EntityRendererProvider.Context context) {
        super(context);
        shadowRadius = 0.0F;
        for (int z = 0; z < TABLE_SIZE; z++) {
            for (int x = 0; x < TABLE_SIZE; x++) {
                float dx = x - TABLE_HALF;
                float dz = z - TABLE_HALF;
                float len = Mth.length(dx, dz);
                rainXCoords[z << 5 | x] = -dz / len;
                rainZCoords[z << 5 | x] = dx / len;
            }
        }
    }

    @Override
    protected boolean affectedByCulling(EntityFalloutRain entity) {
        return false;
    }

    @Override
    public boolean shouldRender(
            EntityFalloutRain entity, Frustum culler, double camX, double camY, double camZ) {
        double range = RENDER_EDGE * 64.0 * Entity.getViewScale();
        return entity.distanceToSqr(camX, camY, camZ) < range * range;
    }

    @Override
    public State createRenderState() {
        return new State();
    }

    @Override
    public void extractRenderState(EntityFalloutRain entity, State state, float partialTicks) {
        super.extractRenderState(entity, state, partialTicks);
        state.scale = entity.getScale();
    }

    @Override
    public void submit(
            State state,
            PoseStack poseStack,
            SubmitNodeCollector submitNodeCollector,
            CameraRenderState camera) {
        Vec3 cameraPos = camera.pos;
        double dx = cameraPos.x - state.x;
        double dy = cameraPos.y - state.y;
        double dz = cameraPos.z - state.z;
        if (dx * dx + dy * dy + dz * dz <= (double) state.scale * state.scale) {
            ClientLevel level = Minecraft.getInstance().level;
            if (level != null) {
                submitNodeCollector.submitCustomGeometry(
                        poseStack,
                        RENDER_TYPE,
                        new Geometry(
                                level,
                                state.x,
                                state.y,
                                state.z,
                                cameraPos,
                                state.ageInTicks,
                                rainXCoords,
                                rainZCoords));
            }
        }
        super.submit(state, poseStack, submitNodeCollector, camera);
    }

    public static class State extends EntityRenderState {
        int scale;
    }

    private record Geometry(
            ClientLevel level,
            double entityX,
            double entityY,
            double entityZ,
            Vec3 cameraPos,
            float ageInTicks,
            float[] rainXCoords,
            float[] rainZCoords)
            implements SubmitNodeCollector.CustomGeometryRenderer {

        private static void vertex(
                VertexConsumer buffer,
                PoseStack.Pose pose,
                float x,
                float y,
                float z,
                float u,
                float v,
                int color,
                int light) {
            Vertices.emit(buffer, pose, x, y, z, color, u, v, light, 0.0F, 1.0F, 0.0F);
        }

        @Override
        public void render(PoseStack.Pose pose, VertexConsumer buffer) {
            int cameraX = Mth.floor(cameraPos.x);
            int cameraY = Mth.floor(cameraPos.y);
            int cameraZ = Mth.floor(cameraPos.z);
            int layerCount =
                    Math.min(
                            10, Math.max(5, Minecraft.getInstance().options.weatherRadius().get()));
            float tick = ageInTicks;
            int wholeTick = Mth.floor(tick);
            float partialTick = tick - wholeTick;
            Random random = new Random();
            BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();

            for (int z = cameraZ - layerCount; z <= cameraZ + layerCount; z++) {
                for (int x = cameraX - layerCount; x <= cameraX + layerCount; x++) {
                    int tableIndex =
                            ((z - cameraZ + TABLE_HALF) & TABLE_MASK) * TABLE_SIZE
                                    + ((x - cameraX + TABLE_HALF) & TABLE_MASK);
                    float halfX = rainXCoords[tableIndex] * 0.5F;
                    float halfZ = rainZCoords[tableIndex] * 0.5F;

                    int rainHeight = level.getHeight(Heightmap.Types.MOTION_BLOCKING, x, z);
                    int minY = cameraY - layerCount;
                    int maxY = cameraY + layerCount;
                    if (minY < rainHeight) minY = rainHeight;
                    if (maxY < rainHeight) maxY = rainHeight;
                    if (minY == maxY) continue;

                    int lightY = Math.max(rainHeight, cameraY);
                    int sampled = LightCoordsUtil.getLightCoords(level, pos.set(x, lightY, z));
                    int light =
                            LightCoordsUtil.pack(
                                    (LightCoordsUtil.block(sampled) * 3 + 15) / 4,
                                    (LightCoordsUtil.sky(sampled) * 3 + 15) / 4);
                    random.setSeed(
                            (long) x * x * 3121L + (long) x * 45238971L
                                    ^ (long) z * z * 418711L + (long) z * 13761L);

                    float fallSpeed = 1.0F;
                    float swayLoop = ((wholeTick & 511) + partialTick) / 512.0F;
                    float fallVariation = 0.4F + random.nextFloat() * 0.2F;
                    float swayVariation = random.nextFloat();
                    double distX = x + 0.5D - cameraPos.x;
                    double distZ = z + 0.5D - cameraPos.z;
                    float intensityMod =
                            Mth.sqrt((float) (distX * distX + distZ * distZ)) / layerCount;
                    int color = ARGB.white(((1.0F - intensityMod * intensityMod) * 0.3F + 0.5F));

                    float x0 = (float) (x - entityX - halfX + 0.5D);
                    float x1 = (float) (x - entityX + halfX + 0.5D);
                    float y0 = (float) (minY - entityY);
                    float y1 = (float) (maxY - entityY);
                    float z0 = (float) (z - entityZ - halfZ + 0.5D);
                    float z1 = (float) (z - entityZ + halfZ + 0.5D);
                    float u0 = fallVariation;
                    float u1 = 1.0F + fallVariation;
                    float v0 = minY * fallSpeed / 4.0F + swayLoop * fallSpeed + swayVariation;
                    float v1 = maxY * fallSpeed / 4.0F + swayLoop * fallSpeed + swayVariation;

                    vertex(buffer, pose, x0, y0, z0, u0, v0, color, light);
                    vertex(buffer, pose, x1, y0, z1, u1, v0, color, light);
                    vertex(buffer, pose, x1, y1, z1, u1, v1, color, light);
                    vertex(buffer, pose, x0, y1, z0, u0, v1, color, light);
                }
            }
        }
    }
}
