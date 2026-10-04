// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.particle;

import com.hbm.client.render.RenderTextures;
import com.hbm.client.render.flywheel.PackedQuadMesh;
import com.hbm.lib.Library;
import com.hbm.particle.ParticleSpentCasing;
import dev.engine_room.flywheel.api.instance.Instancer;
import dev.engine_room.flywheel.api.material.CardinalLightingMode;
import dev.engine_room.flywheel.api.material.Transparency;
import dev.engine_room.flywheel.api.material.WriteMask;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.api.visual.DynamicVisual;
import dev.engine_room.flywheel.api.visual.EffectVisual;
import dev.engine_room.flywheel.api.visualization.VisualizationContext;
import dev.engine_room.flywheel.lib.material.LightShaders;
import dev.engine_room.flywheel.lib.material.SimpleCutoutShader;
import dev.engine_room.flywheel.lib.material.SimpleMaterial;
import dev.engine_room.flywheel.lib.model.SingleMeshModel;
import dev.engine_room.flywheel.lib.visual.SimpleDynamicVisual;
import java.util.Arrays;
import net.minecraft.core.Vec3i;

public final class CasingSmokeVisual
        implements EffectVisual<ParticleSpentCasing>, SimpleDynamicVisual {
    private static final Model MODEL =
            new SingleMeshModel(
                    PackedQuadMesh.builder(2)
                            .normal(0F, 1F, 0F)
                            .vertex(0D, 0D, 0D, 0xFFFFFFFF)
                            .vertex(1D, 0D, 0D, 0x00FFFFFF)
                            .vertex(1D, 1D, 0D, 0x00FFFFFF)
                            .vertex(0D, 1D, 0D, 0xFFFFFFFF)
                            .vertex(0D, 0D, 0D, 0xFFFFFFFF)
                            .vertex(-1D, 0D, 0D, 0x00FFFFFF)
                            .vertex(-1D, 1D, 0D, 0x00FFFFFF)
                            .vertex(0D, 1D, 0D, 0xFFFFFFFF)
                            .build(),
                    SimpleMaterial.builder()
                            .texture(RenderTextures.WHITE)
                            .mipmap(false)
                            .useOverlay(false)
                            .transparency(Transparency.ORDER_INDEPENDENT)
                            .writeMask(WriteMask.COLOR)
                            .cutout(new SimpleCutoutShader(Library.id("cutout/zero.glsl")))
                            .light(LightShaders.FLAT)
                            .ambientOcclusion(false)
                            .cardinalLightingMode(CardinalLightingMode.ENTITY)
                            .backfaceCulling(false)
                            .build());

    private final ParticleSpentCasing particle;
    private final Instancer<ParticleInstance> instancer;
    private final Vec3i origin;
    private ParticleInstance[] instances = new ParticleInstance[0];
    private int active;

    public CasingSmokeVisual(
            VisualizationContext context, ParticleSpentCasing particle, float partialTick) {
        this.particle = particle;
        this.origin = context.renderOrigin();
        this.instancer =
                context.instancerProvider().instancer(ParticleInstance.CASING_SMOKE, MODEL);
        writeFrame();
    }

    @Override
    public void beginFrame(DynamicVisual.Context context) {
        writeFrame();
    }

    private void writeFrame() {
        ParticleSpentCasing.SmokeFrame frame = particle.smokeFrame();
        int count =
                particle.isAlive() && frame != null ? Math.max(0, frame.nodes().length / 4 - 1) : 0;
        if (count > instances.length) {
            int old = instances.length;
            instances = Arrays.copyOf(instances, Math.max(count, old * 2 + 1));
            for (int i = old; i < instances.length; i++) {
                instances[i] = instancer.createInstance();
                instances[i].setVisible(false);
            }
        }
        if (frame != null) {
            float[] nodes = frame.nodes();
            float baseX = (float) (frame.x() - origin.getX());
            float baseY = (float) (frame.y() - origin.getY()) - frame.heightQuarter();
            float baseZ = (float) (frame.z() - origin.getZ());
            for (int i = 0; i < count; i++) {
                int b = i * 4;
                ParticleInstance instance = instances[i];
                instance.setVisible(true);
                instance.position(baseX + nodes[b], baseY + nodes[b + 1], baseZ + nodes[b + 2]);
                instance.previousPosition(
                        baseX + nodes[b + 4], baseY + nodes[b + 5], baseZ + nodes[b + 6]);
                instance.uvRegion(frame.widthX(), frame.widthZ(), nodes[b + 3], nodes[b + 7]);
                instance.light(frame.light()).setChanged();
            }
        }
        for (int i = count; i < active; i++) instances[i].setVisible(false);
        active = count;
    }

    @Override
    public void update(float partialTick) {}

    @Override
    public void delete() {
        for (ParticleInstance instance : instances) instance.delete();
    }
}
