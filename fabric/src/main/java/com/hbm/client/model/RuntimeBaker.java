// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.model;

import java.util.LinkedHashMap;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.resources.model.ModelBaker;
import net.minecraft.client.resources.model.ModelDebugName;
import net.minecraft.client.resources.model.ResolvedModel;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.client.resources.model.sprite.Material;
import net.minecraft.client.resources.model.sprite.MaterialBaker;
import net.minecraft.resources.Identifier;
import org.joml.Vector3fc;

final class RuntimeBaker extends MaterialBaker implements ModelBaker, ModelBaker.Interner {
    private final Identifier[] modelIds;
    private final ResolvedModel[] models;
    private final Material[] materialKeys;
    private final Material.Baked[] materials;
    private final BlockStateModelPart missing;

    private RuntimeBaker(ModelBaker source, ResolvedModel[] inputs) {
        super(source.missingBlockModelPart().particleMaterial().sprite());
        missing = source.missingBlockModelPart();
        var modelMap = new LinkedHashMap<Identifier, ResolvedModel>();
        var materialMap = new LinkedHashMap<Material, Material.Baked>();
        for (ResolvedModel input : inputs) {
            var slots = input.getTopTextureSlots();
            for (ResolvedModel model = input; model != null; model = model.parent()) {
                modelMap.putIfAbsent(Identifier.parse(model.debugName()), model);
                for (String slot : model.wrapped().textureSlots().values().keySet()) {
                    Material material = slots.getMaterial(slot);
                    if (material != null)
                        materialMap.computeIfAbsent(
                                material, key -> source.materials().get(key, input));
                }
            }
        }
        modelIds = modelMap.keySet().toArray(Identifier[]::new);
        models = modelMap.values().toArray(ResolvedModel[]::new);
        materialKeys = materialMap.keySet().toArray(Material[]::new);
        materials = materialMap.values().toArray(Material.Baked[]::new);
    }

    static ModelBaker capture(ModelBaker source, ResolvedModel... models) {
        return new RuntimeBaker(source, models);
    }

    @Override
    public Material.Baked get(Material material, ModelDebugName name) {
        for (int i = 0; i < materialKeys.length; i++) {
            if (material.equals(materialKeys[i])) return materials[i];
        }
        throw new IllegalStateException(
                name.debugName() + ": uncaptured runtime material " + material);
    }

    @Override
    protected Material.Baked bake(Material material) {
        throw new UnsupportedOperationException();
    }

    @Override
    public ResolvedModel getModel(Identifier id) {
        for (int i = 0; i < modelIds.length; i++) {
            if (id.equals(modelIds[i])) return models[i];
        }
        throw new IllegalStateException("Uncaptured runtime model " + id);
    }

    @Override
    public BlockStateModelPart missingBlockModelPart() {
        return missing;
    }

    @Override
    public MaterialBaker materials() {
        return this;
    }

    @Override
    public ModelBaker.Interner interner() {
        return this;
    }

    @Override
    public <T> T compute(ModelBaker.SharedOperationKey<T> key) {
        return key.compute(this);
    }

    @Override
    public Vector3fc vector(Vector3fc value) {
        return value;
    }

    @Override
    public BakedQuad.MaterialInfo materialInfo(BakedQuad.MaterialInfo value) {
        return value;
    }
}
