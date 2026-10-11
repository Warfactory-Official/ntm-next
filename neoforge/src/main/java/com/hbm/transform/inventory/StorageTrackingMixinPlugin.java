// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.transform.inventory;

import com.hbm.inventory.StorageLayouts;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.tree.AnnotationNode;
import org.objectweb.asm.tree.ClassNode;
import org.spongepowered.asm.mixin.extensibility.IMixinConfigPlugin;
import org.spongepowered.asm.mixin.extensibility.IMixinInfo;

public final class StorageTrackingMixinPlugin implements IMixinConfigPlugin {
    private static final String MARKER = "com.hbm.mixin.MixinStorageTargets";
    private static final Map<String, String> WEAVING_MIXINS =
            Map.ofEntries(
                    Map.entry(
                            StorageLayouts.AE_CPU_BLOCK,
                            "com.hbm.neoforge.mixin.storage.MixinCraftingBlockEntity"),
                    Map.entry(
                            StorageLayouts.AE_CELL_INVENTORY,
                            "com.hbm.neoforge.mixin.storage.MixinAppEngCellInventory"),
                    Map.entry(
                            StorageLayouts.AE_CPU_LOGIC,
                            "com.hbm.neoforge.mixin.storage.MixinCraftingCpuLogic"),
                    Map.entry(
                            StorageLayouts.AE_ITEM_GEN,
                            "com.hbm.neoforge.mixin.storage.MixinItemGenBlockEntity"),
                    Map.entry(
                            StorageLayouts.AE_GENERIC,
                            "com.hbm.neoforge.mixin.storage.MixinGenericStackInv"),
                    Map.entry(
                            StorageLayouts.AE_PATTERN,
                            "com.hbm.neoforge.mixin.storage.MixinPatternProviderLogic"),
                    Map.entry(
                            StorageLayouts.AE_INVENTORY,
                            "com.hbm.neoforge.mixin.storage.MixinAppEngInternalInventory"),
                    Map.entry(StorageLayouts.DRAWER, "com.hbm.mixin.storage.MixinDrawerData"),
                    Map.entry(
                            StorageLayouts.FURNACE,
                            "com.hbm.mixin.MixinAbstractFurnaceBlockEntity"),
                    Map.entry(
                            StorageLayouts.ITEM_HANDLER,
                            "com.hbm.neoforge.mixin.MixinItemStacksResourceHandler"),
                    Map.entry(
                            StorageLayouts.SOPH_BACKPACK,
                            "com.hbm.neoforge.mixin.storage.MixinBackpackBlockEntity"),
                    Map.entry(
                            StorageLayouts.UPGRADES,
                            "com.hbm.neoforge.mixin.storage.MixinUpgradeHandler"),
                    Map.entry(
                            StorageLayouts.SOPH_STORAGE,
                            "com.hbm.neoforge.mixin.storage.MixinStorageBlockEntity"),
                    Map.entry(
                            StorageLayouts.REBORN_INVENTORY,
                            "com.hbm.fabric.mixin.storage.MixinInventoryBase"),
                    Map.entry(
                            StorageLayouts.REBORN_BULK,
                            "com.hbm.fabric.mixin.storage.MixinStorageUnitBaseBlockEntity"));

    @Override
    public void postApply(String target, ClassNode node, String mixin, IMixinInfo info) {
        if (!mixin.equals(WEAVING_MIXINS.getOrDefault(target, MARKER))) return;
        var layout = StorageLayouts.TARGETS.get(target);
        if (layout == null) throw new IllegalStateException("Undeclared storage target: " + target);
        StorageWrites.transform(node, layout.writes());
        StorageMembers.transform(node, layout);
        StorageHooks.transform(node, layout.hooks());
    }

    @Override
    public boolean shouldApplyMixin(String target, String mixin) {
        return true;
    }

    @Override
    public void onLoad(String mixinPackage) {
        try (var bytes =
                getClass()
                        .getClassLoader()
                        .getResourceAsStream(MARKER.replace('.', '/') + ".class")) {
            var node = new ClassNode();
            new ClassReader(bytes)
                    .accept(
                            node,
                            ClassReader.SKIP_CODE
                                    | ClassReader.SKIP_DEBUG
                                    | ClassReader.SKIP_FRAMES);
            var declared = new HashSet<String>();
            targets(node.visibleAnnotations, declared);
            targets(node.invisibleAnnotations, declared);
            declared.addAll(WEAVING_MIXINS.keySet());
            if (!declared.equals(StorageLayouts.TARGETS.keySet()))
                throw new IllegalStateException("Storage target roster differs from its layouts");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }

    private static void targets(List<AnnotationNode> annotations, Set<String> targets) {
        if (annotations == null) return;
        for (var annotation : annotations) {
            if (!annotation.desc.equals("Lorg/spongepowered/asm/mixin/Mixin;")) continue;
            for (int i = 0; i < annotation.values.size(); i += 2) {
                if (!annotation.values.get(i).equals("targets")) continue;
                for (Object name : (List<?>) annotation.values.get(i + 1))
                    targets.add((String) name);
            }
        }
    }

    @Override
    public String getRefMapperConfig() {
        return null;
    }

    @Override
    public void acceptTargets(Set<String> mine, Set<String> other) {}

    @Override
    public List<String> getMixins() {
        return null;
    }

    @Override
    public void preApply(String target, ClassNode node, String mixin, IMixinInfo info) {}
}
