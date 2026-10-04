// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.neoforge.inventory;

import appeng.api.parts.IPart;
import appeng.api.upgrades.IUpgradeableObject;
import appeng.blockentity.AEBaseInvBlockEntity;
import appeng.blockentity.crafting.CraftingBlockEntity;
import appeng.blockentity.misc.CellWorkbenchBlockEntity;
import appeng.blockentity.networking.CableBusBlockEntity;
import appeng.helpers.InterfaceLogicHost;
import appeng.helpers.externalstorage.GenericStackInv;
import appeng.helpers.patternprovider.PatternProviderLogicHost;
import appeng.parts.encoding.PatternEncodingTerminalPart;
import appeng.parts.reporting.AbstractTerminalPart;
import appeng.parts.reporting.CraftingTerminalPart;
import com.hbm.inventory.InventoryChildren;
import com.hbm.inventory.SlotSource;
import com.hbm.inventory.StorageLayouts.Host;
import com.hbm.inventory.StorageLayouts;
import java.util.function.Consumer;
import net.minecraft.core.Direction;
import net.minecraft.world.level.block.entity.BlockEntity;

public final class AE2Inventories {
    private enum Leaf {
        SOURCE,
        CHILDREN,
        EMPTY
    }

    private static final ClassValue<Host> OWNERS =
            new ClassValue<>() {
                @Override
                protected Host computeValue(Class<?> type) {
                    boolean part = IPart.class.isAssignableFrom(type);
                    for (Class<?> parent = type;
                            parent != null;
                            parent = part ? null : parent.getSuperclass()) {
                        var declaration = StorageLayouts.DECLARATIONS.get(parent.getName());
                        if (declaration != null
                                && declaration.loader() == StorageLayouts.Loader.NEOFORGE
                                && declaration.host() != Host.NONE) return declaration.host();
                    }
                    return Host.NONE;
                }
            };
    private static final ClassValue<Leaf> LEAVES =
            new ClassValue<>() {
                @Override
                protected Leaf computeValue(Class<?> type) {
                    if (SlotSource.class.isAssignableFrom(type)) return Leaf.SOURCE;
                    if (InventoryChildren.class.isAssignableFrom(type)) return Leaf.CHILDREN;
                    var declaration = StorageLayouts.DECLARATIONS.get(type.getName());
                    if (declaration != null && declaration.host() == Host.EMPTY) return Leaf.EMPTY;
                    throw new IllegalStateException("Untracked AE2 inventory: " + type.getName());
                }
            };

    private AE2Inventories() {}

    public static void sources(BlockEntity owner, Consumer<Object> sources) {
        collectHost(owner, sources);
    }

    private static void collectHost(Object host, Consumer<Object> sources) {
        if (host == null) return;
        switch (OWNERS.get(host.getClass())) {
            case NONE, EMPTY -> {}
            case CABLE -> {
                var bus = ((CableBusBlockEntity) host).getCableBus();
                collectHost(bus.getPart(null), sources);
                for (Direction side : Direction.values()) collectHost(bus.getPart(side), sources);
            }
            case INTERFACE -> {
                var logic = ((InterfaceLogicHost) host).getInterfaceLogic();
                collect(logic.getStorage(), sources);
                collect(logic.getUpgrades(), sources);
            }
            case PATTERN -> {
                var logic = ((PatternProviderLogicHost) host).getLogic();
                collect(logic.getPatternInv(), sources);
                collect(logic.getReturnInv(), sources);
                sources.accept(logic);
            }
            case WORKBENCH ->
                    collect(
                            ((CellWorkbenchBlockEntity) host)
                                    .getSubInventory(
                                            appeng.api.inventories.ISegmentedInventory.CELLS),
                            sources);
            case CPU -> {
                var block = (CraftingBlockEntity) host;
                if (block.getPreviousState() != null) sources.accept(block);
                else {
                    var cluster = block.getCluster();
                    if (cluster != null
                            && !cluster.isDestroyed()
                            && ((AE2CpuAnchor) (Object) cluster).craftingInventoryAnchor() == block)
                        sources.accept(cluster.craftingLogic);
                }
            }
            case CHILDREN -> {
                for (Object child : ((InventoryChildren) host).inventoryChildren())
                    collect(child, sources);
            }
            case STANDARD -> collect(((AEBaseInvBlockEntity) host).getInternalInventory(), sources);
            case STANDARD_UPGRADES -> {
                collect(((AEBaseInvBlockEntity) host).getInternalInventory(), sources);
                collect(((IUpgradeableObject) host).getUpgrades(), sources);
            }
            case UPGRADES -> collect(((IUpgradeableObject) host).getUpgrades(), sources);
            case TERMINAL -> collect(((AbstractTerminalPart) host).getViewCellStorage(), sources);
            case CRAFTING -> {
                var part = (CraftingTerminalPart) host;
                collect(part.getViewCellStorage(), sources);
                collect(part.getSubInventory(CraftingTerminalPart.INV_CRAFTING), sources);
            }
            case ENCODING -> {
                var part = (PatternEncodingTerminalPart) host;
                collect(part.getViewCellStorage(), sources);
                collect(part.getLogic().getBlankPatternInv(), sources);
                collect(part.getLogic().getEncodedPatternInv(), sources);
            }
        }
    }

    private static void collect(Object inventory, Consumer<Object> sources) {
        switch (LEAVES.get(inventory.getClass())) {
            case EMPTY -> {}
            case CHILDREN -> {
                for (Object child : ((InventoryChildren) inventory).inventoryChildren())
                    collect(child, sources);
            }
            case SOURCE -> {
                if (inventory instanceof GenericStackInv generic
                        && generic.getMode() != GenericStackInv.Mode.STORAGE) return;
                sources.accept(inventory);
            }
        }
    }
}
