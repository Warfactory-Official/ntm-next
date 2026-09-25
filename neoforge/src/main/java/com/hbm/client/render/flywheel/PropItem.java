// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.client.render.flywheel;

import dev.engine_room.flywheel.api.material.Material;
import dev.engine_room.flywheel.api.model.Model;
import dev.engine_room.flywheel.lib.material.Materials;
import dev.engine_room.flywheel.lib.model.ModelUtil;
import dev.engine_room.flywheel.lib.model.SimpleModel;
import dev.engine_room.flywheel.lib.model.baked.BakedModelBufferer;
import dev.engine_room.flywheel.lib.util.RendererReloadCache;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import net.minecraft.core.component.DataComponents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

final class PropItem {
    private static final RendererReloadCache<Key, Model> MODELS =
            new RendererReloadCache<>(PropItem::build);

    private PropItem() {}

    static Model model(ItemStack stack, ItemDisplayContext context) {
        var meshes =
                Objects.requireNonNull(
                        BakedModelBufferer.INSTANCE.bufferItem(stack, context, null, 0),
                        () -> stack + " has no baked model at " + context);
        return MODELS.get(
                new Key(
                        context,
                        meshes.identity(),
                        stack.getItem(),
                        stack.get(DataComponents.ITEM_MODEL),
                        meshes));
    }

    private static Model build(Key key) {
        List<Model.ConfiguredMesh> configured = new ArrayList<>();
        key.meshes
                .meshes()
                .forEach(
                        (bucket, mesh) -> {
                            Material material =
                                    ModelUtil.getItemMaterial(bucket.layer(), bucket.blocksAtlas());
                            if (material == null) return;
                            configured.add(new Model.ConfiguredMesh(material, mesh));
                            if (key.meshes.foil())
                                configured.add(new Model.ConfiguredMesh(Materials.GLINT, mesh));
                        });
        return new SimpleModel(configured);
    }

    private static final class Key {
        private final ItemDisplayContext context;
        private final Object identity;
        private final Item item;
        private final @Nullable Identifier model;
        private final BakedModelBufferer.ItemMeshes meshes;

        private Key(
                ItemDisplayContext context,
                Object identity,
                Item item,
                @Nullable Identifier model,
                BakedModelBufferer.ItemMeshes meshes) {
            this.context = context;
            this.identity = identity;
            this.item = item;
            this.model = model;
            this.meshes = meshes;
        }

        @Override
        public boolean equals(Object other) {
            return other instanceof Key key
                    && context == key.context
                    && identity.equals(key.identity)
                    && item == key.item
                    && Objects.equals(model, key.model);
        }

        @Override
        public int hashCode() {
            return Objects.hash(context, identity, item, model);
        }
    }
}
