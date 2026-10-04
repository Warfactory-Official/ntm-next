// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.inventory;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;

public final class StorageLayouts {
    private static final String BE = "net.minecraft.world.level.block.entity.";
    public static final String BASE = BE + "BaseContainerBlockEntity";
    public static final String CHEST = BE + "ChestBlockEntity";
    public static final String BARREL = BE + "BarrelBlockEntity";
    public static final String HOPPER = BE + "HopperBlockEntity";
    public static final String DISPENSER = BE + "DispenserBlockEntity";
    public static final String SHULKER = BE + "ShulkerBoxBlockEntity";
    public static final String BREWING = BE + "BrewingStandBlockEntity";
    public static final String CRAFTER = BE + "CrafterBlockEntity";
    public static final String FURNACE = BE + "AbstractFurnaceBlockEntity";
    public static final String CAMPFIRE = BE + "CampfireBlockEntity";
    public static final String SHELF = BE + "ShelfBlockEntity";
    public static final String BOOKSHELF = BE + "ChiseledBookShelfBlockEntity";
    public static final String LECTERN = BE + "LecternBlockEntity";
    public static final String LECTERN_MENU = LECTERN + "$1";
    public static final String BRUSHABLE = BE + "BrushableBlockEntity";
    public static final String JUKEBOX = BE + "JukeboxBlockEntity";
    public static final String POT = BE + "DecoratedPotBlockEntity";
    public static final String VAULT = BE + "vault.VaultBlockEntity";
    public static final String CRAFTING =
            "net.minecraft.world.inventory.TransientCraftingContainer";
    public static final String TOM_MATRIX = "com.tom.storagemod.util.CraftingMatrix";
    public static final String COMPACT =
            "com.witchica.compactstorage.block.entity.base.BaseCompactStorageBlockEntity";
    public static final String DRUM =
            "com.witchica.compactstorage.block.entity.base.BaseItemDrumBlockEntity";
    public static final String REBORN_INVENTORY = "reborncore.api.items.InventoryBase";
    public static final String REBORN_BULK =
            "techreborn.blockentity.storage.item.StorageUnitBaseBlockEntity";
    public static final String REBORN_TRANSFER = REBORN_BULK + "$1";
    public static final String STACK_HANDLER =
            "net.neoforged.neoforge.transfer.StacksResourceHandler";
    public static final String ITEM_HANDLER =
            "net.neoforged.neoforge.transfer.item.ItemStacksResourceHandler";
    public static final String UPGRADES = "net.p3pp3rf1y.sophisticatedcore.upgrades.UpgradeHandler";
    public static final String SOPH_STORAGE =
            "net.p3pp3rf1y.sophisticatedstorage.block.StorageBlockEntity";
    public static final String SOPH_BACKPACK =
            "net.p3pp3rf1y.sophisticatedbackpacks.backpack.BackpackBlockEntity";
    public static final String SOPH_INVENTORY_DATA =
            "net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents$InventoryData";
    public static final String SOPH_UPGRADE_DATA =
            "net.p3pp3rf1y.sophisticatedcore.inventory.ContainerContents$UpgradeData";
    public static final String DRAWER =
            "com.jaquadro.minecraft.storagedrawers.block.tile.tiledata.StandardDrawerGroup$DrawerData";
    public static final String AE_INVENTORY = "appeng.util.inv.AppEngInternalInventory";
    public static final String AE_CELL_INVENTORY =
            "appeng.blockentity.inventory.AppEngCellInventory";
    public static final String AE_COMBINED = "appeng.util.inv.CombinedInternalInventory";
    public static final String AE_ITEM_GEN = "appeng.debug.ItemGenBlockEntity";
    public static final String AE_GENERIC = "appeng.helpers.externalstorage.GenericStackInv";
    public static final String AE_RETURN =
            "appeng.helpers.patternprovider.PatternProviderReturnInventory";
    public static final String AE_PATTERN = "appeng.helpers.patternprovider.PatternProviderLogic";
    public static final String AE_CABLE_BUS = "appeng.parts.CableBusContainer";
    public static final String AE_CPU_LOGIC = "appeng.crafting.execution.CraftingCpuLogic";
    public static final String AE_CPU_BLOCK = "appeng.blockentity.crafting.CraftingBlockEntity";

    public enum Kind {
        METHODS,
        LIST,
        STACK,
        DECLARED,
        ARRAY,
        CHILDREN,
        KEYED,
        INTERIOR
    }

    public enum Owner {
        SELF,
        EXTERNAL,
        MENU
    }

    public enum Publish {
        NONE,
        CONTAINER,
        BOOKSHELF,
        LECTERN,
        JUKEBOX
    }

    public enum Event {
        LIST,
        ROOT,
        OWNER
    }

    public record Source(
            Kind kind,
            String field,
            String fieldOwner,
            Owner owner,
            Publish publish,
            boolean containerCommit) {}

    public record Write(String owner, String field, boolean nullableOld, boolean array) {
        public Write(String owner, String field) {
            this(owner, field, false, false);
        }

        public Write(String owner, String field, boolean nullableOld) {
            this(owner, field, nullableOld, false);
        }

        public static Write array(String owner, String field) {
            return new Write(owner, field, false, true);
        }
    }

    public record Hook(String method, Event event, String ownerGetter) {
        public Hook(String method, Event event) {
            this(method, event, null);
        }
    }

    public record Root(String getter, String descriptor, boolean refresh) {}

    public record Layout(Source source, List<Write> writes, List<Hook> hooks, Root root) {}

    public enum Admission {
        NONE,
        EXACT,
        FAMILY
    }

    public enum Loader {
        COMMON,
        NEOFORGE
    }

    public enum Host {
        NONE,
        EMPTY,
        CABLE,
        INTERFACE,
        PATTERN,
        WORKBENCH,
        CPU,
        CHILDREN,
        STANDARD,
        STANDARD_UPGRADES,
        TERMINAL,
        CRAFTING,
        ENCODING,
        UPGRADES
    }

    public enum Format {
        INV_MAP,
        ITEM_LIST,
        KEY_LIST,
        KEY_MAP
    }

    public record Field(String name, Format format, int capacity, int firstSlot) {}

    public record Packed(List<String> typeIds, List<Field> fields, boolean coreOnly) {}

    public record Metadata(
            Admission admission,
            Loader loader,
            Host host,
            @Nullable Packed packed,
            @Nullable Layout weaving) {}

    public static final Map<String, Metadata> DECLARATIONS = declarations();
    public static final Map<String, Layout> TARGETS = weaving();
    public static final Map<String, Metadata> BLOCK_TYPES = blockTypes();

    private StorageLayouts() {}

    private static Map<String, Metadata> declarations() {
        var targets = new LinkedHashMap<String, Metadata>();
        declare(
                targets,
                BASE,
                weave(
                        source(
                                new Source(
                                        Kind.METHODS,
                                        null,
                                        null,
                                        Owner.SELF,
                                        Publish.NONE,
                                        false))));
        for (String target :
                List.of(CHEST, BARREL, HOPPER, DISPENSER, SHULKER, BREWING, CRAFTER, FURNACE))
            declare(
                    targets,
                    target,
                    target.equals(FURNACE)
                            ? weave(hooks("setItems"))
                            : nativeOwner(hooks("setItems")));
        for (String name :
                List.of(
                        "TrappedChestBlockEntity",
                        "DropperBlockEntity",
                        "FurnaceBlockEntity",
                        "BlastFurnaceBlockEntity",
                        "SmokerBlockEntity")) declare(targets, BE + name, nativeOwner(null));
        for (String target : List.of(CAMPFIRE, SHELF))
            declare(targets, target, nativeOwner(source(list("items", Owner.SELF, Publish.NONE))));
        declare(
                targets,
                BOOKSHELF,
                nativeOwner(source(list("items", Owner.SELF, Publish.BOOKSHELF))));
        declare(targets, CRAFTING, weave(source(list("items", Owner.MENU, Publish.CONTAINER))));
        declare(
                targets,
                TOM_MATRIX,
                weave(
                        source(
                                new Source(
                                        Kind.LIST,
                                        "items",
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.CONTAINER,
                                        true))));
        for (String target : List.of(BRUSHABLE, POT, JUKEBOX, LECTERN)) {
            String field = target.equals(LECTERN) ? "book" : "item";
            Publish publish =
                    target.equals(LECTERN)
                            ? Publish.LECTERN
                            : target.equals(JUKEBOX) ? Publish.JUKEBOX : Publish.NONE;
            declare(
                    targets,
                    target,
                    nativeOwner(
                            new Layout(
                                    new Source(Kind.STACK, field, null, Owner.SELF, publish, false),
                                    List.of(new Write(target, field)),
                                    List.of(),
                                    null)));
        }
        declare(targets, LECTERN_MENU, weave(writes(LECTERN, "book")));
        var bulkWrites = List.of(new Write(REBORN_BULK, "storeItemStack", true));
        declare(
                targets,
                REBORN_BULK,
                weave(
                        new Layout(
                                null,
                                bulkWrites,
                                List.of(new Hook("loadAdditional", Event.ROOT)),
                                null)));
        declare(targets, REBORN_TRANSFER, weave(new Layout(null, bulkWrites, List.of(), null)));
        declare(
                targets,
                DRAWER,
                weave(
                        new Layout(
                                null,
                                List.of(
                                        new Write(DRAWER, "protoStack"),
                                        new Write(DRAWER, "count"),
                                        new Write(DRAWER, "missing")),
                                List.of(),
                                null)));
        declare(targets, COMPACT, family(hooks("loadAdditional", "resizeInventory", "setItems")));
        declare(targets, STACK_HANDLER, weave(hooks("setStacks", "deserialize")));
        declare(targets, UPGRADES, weave(hooks("loadUpgradesFromData", "deserialize")));
        var interior =
                new Source(Kind.INTERIOR, "stacks", null, Owner.EXTERNAL, Publish.NONE, false);
        declare(
                targets,
                SOPH_INVENTORY_DATA,
                weave(
                        new Layout(
                                interior,
                                List.of(),
                                List.of(
                                        new Hook("reloadFrom", Event.LIST),
                                        new Hook("resize", Event.LIST)),
                                null)));
        declare(
                targets,
                SOPH_UPGRADE_DATA,
                weave(
                        new Layout(
                                interior,
                                List.of(),
                                List.of(
                                        new Hook("reloadFrom", Event.LIST),
                                        new Hook("setStacks", Event.LIST)),
                                null)));
        declare(
                targets,
                SOPH_STORAGE,
                family(
                        new Layout(
                                null,
                                List.of(),
                                List.of(new Hook("loadStorageWrapper", Event.ROOT)),
                                null)));
        declare(
                targets,
                SOPH_BACKPACK,
                family(
                        new Layout(
                                null,
                                List.of(),
                                List.of(new Hook("setBackpack", Event.ROOT)),
                                null)));
        declare(
                targets,
                REBORN_INVENTORY,
                weave(
                        new Layout(
                                list("stacks", Owner.EXTERNAL, Publish.NONE),
                                List.of(),
                                List.of(new Hook("readData", Event.LIST)),
                                null)));
        declare(
                targets,
                ITEM_HANDLER,
                weave(
                        source(
                                new Source(
                                        Kind.LIST,
                                        "stacks",
                                        STACK_HANDLER,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false))));
        declare(
                targets,
                VAULT,
                nativeOwner(
                        root(
                                "getServerData",
                                "()Lnet/minecraft/world/level/block/entity/vault/VaultServerData;",
                                false)));
        declare(
                targets,
                DRUM,
                family(
                        root(
                                "getDrumInventory",
                                "()Lcom/witchica/compactstorage/inventory/DrumInventory;",
                                true)));
        for (String name :
                List.of(
                        "net.minecraft.world.level.block.piston.PistonMovingBlockEntity",
                        "com.tom.storagemod.block.entity.FilingCabinetBlockEntity",
                        "com.tom.storagemod.block.entity.CraftingTerminalBlockEntity",
                        "reborncore.common.blockentity.MachineBaseBlockEntity",
                        "com.jaquadro.minecraft.storagedrawers.block.tile.BlockEntityDrawersStandard",
                        "com.hbm.blockentity.BlockEntityMachineBase",
                        "com.hbm.blockentity.machine.BlockEntityFoundryCastingBase"))
            declare(targets, name, family(null));

        declare(targets, AE_INVENTORY, weave(source(list("stacks", Owner.EXTERNAL, Publish.NONE))));
        declare(
                targets,
                AE_CELL_INVENTORY,
                weave(
                        source(
                                new Source(
                                        Kind.DECLARED,
                                        null,
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false))));
        declare(
                targets,
                AE_COMBINED,
                weave(
                        source(
                                new Source(
                                        Kind.CHILDREN,
                                        "inventories",
                                        null,
                                        Owner.SELF,
                                        Publish.NONE,
                                        false))));
        declare(
                targets,
                AE_ITEM_GEN,
                ae(
                        Host.CHILDREN,
                        source(
                                new Source(
                                        Kind.CHILDREN,
                                        "inv",
                                        null,
                                        Owner.SELF,
                                        Publish.NONE,
                                        false)),
                        null));
        declare(
                targets,
                AE_GENERIC,
                weave(
                        new Layout(
                                new Source(
                                        Kind.ARRAY,
                                        "stacks",
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false),
                                List.of(Write.array(AE_GENERIC, "stacks")),
                                List.of(),
                                null)));
        declare(
                targets,
                AE_RETURN,
                weave(
                        new Layout(
                                null,
                                List.of(Write.array(AE_GENERIC, "stacks")),
                                List.of(),
                                null)));
        declare(
                targets,
                AE_PATTERN,
                weave(
                        source(
                                new Source(
                                        Kind.DECLARED,
                                        null,
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false))));
        declare(
                targets,
                AE_CABLE_BUS,
                weave(
                        new Layout(
                                null,
                                List.of(),
                                List.of(
                                        new Hook(
                                                "updateAfterPartChange",
                                                Event.OWNER,
                                                "getBlockEntity"),
                                        new Hook("readFromNBT", Event.OWNER, "getBlockEntity"),
                                        new Hook("setHost", Event.OWNER, "getBlockEntity")),
                                null)));
        declare(
                targets,
                AE_CPU_LOGIC,
                weave(
                        source(
                                new Source(
                                        Kind.KEYED,
                                        null,
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false))));
        var held = new Field("inventory", Format.KEY_MAP, Integer.MAX_VALUE, 0);
        declare(
                targets,
                AE_CPU_BLOCK,
                ae(
                        Host.CPU,
                        new Layout(
                                new Source(
                                        Kind.KEYED,
                                        null,
                                        null,
                                        Owner.EXTERNAL,
                                        Publish.NONE,
                                        false),
                                List.of(),
                                List.of(new Hook("updateStatus", Event.ROOT)),
                                null),
                        new Packed(
                                List.of("ae2:crafting_unit", "ae2:crafting_storage"),
                                List.of(held),
                                true)));
        declare(
                targets,
                "appeng.blockentity.crafting.CraftingMonitorBlockEntity",
                ae(Host.CPU, "crafting_monitor", held));
        declare(
                targets,
                "appeng.blockentity.storage.DriveBlockEntity",
                ae(Host.STANDARD, "drive", inv(10)));
        declare(
                targets,
                "appeng.blockentity.storage.MEChestBlockEntity",
                ae(Host.STANDARD, "chest", inv(2)));
        declare(
                targets,
                "appeng.blockentity.storage.SkyStoneChestBlockEntity",
                ae(Host.STANDARD, "sky_chest", inv(36)));
        declare(
                targets,
                "appeng.blockentity.storage.IOPortBlockEntity",
                ae(Host.STANDARD_UPGRADES, "io_port", inv(12), upgrades(3)));
        declare(
                targets,
                "appeng.blockentity.spatial.SpatialIOPortBlockEntity",
                ae(Host.STANDARD, "spatial_io_port", inv(2)));
        declare(
                targets,
                "appeng.blockentity.misc.InterfaceBlockEntity",
                ae(Host.INTERFACE, "interface", keys("storage", 9), upgrades(1)));
        declare(
                targets,
                "appeng.blockentity.crafting.PatternProviderBlockEntity",
                ae(
                        Host.PATTERN,
                        "pattern_provider",
                        new Field("patterns", Format.ITEM_LIST, 9, 0),
                        new Field("sendList", Format.KEY_MAP, Integer.MAX_VALUE, 0),
                        keys("returnInv", 9)));
        declare(
                targets,
                "appeng.blockentity.crafting.MolecularAssemblerBlockEntity",
                ae(Host.STANDARD_UPGRADES, "molecular_assembler", inv(11), upgrades(5)));
        declare(
                targets,
                "appeng.blockentity.misc.InscriberBlockEntity",
                ae(Host.STANDARD_UPGRADES, "inscriber", inv(4), upgrades(4)));
        declare(
                targets,
                "appeng.blockentity.misc.CondenserBlockEntity",
                ae(Host.STANDARD, "condenser", new Field("inv", Format.INV_MAP, 3, 1)));
        declare(
                targets,
                "appeng.blockentity.misc.ChargerBlockEntity",
                ae(Host.STANDARD, "charger", inv(1)));
        declare(
                targets,
                "appeng.blockentity.misc.VibrationChamberBlockEntity",
                ae(Host.STANDARD_UPGRADES, "vibration_chamber", inv(1), upgrades(3)));
        declare(
                targets,
                "appeng.blockentity.misc.CellWorkbenchBlockEntity",
                ae(Host.WORKBENCH, "cell_workbench", new Field("cell", Format.ITEM_LIST, 1, 0)));
        declare(
                targets,
                "appeng.blockentity.qnb.QuantumBridgeBlockEntity",
                ae(Host.STANDARD, "quantum_ring", inv(1)));
        declare(
                targets,
                "appeng.blockentity.networking.WirelessAccessPointBlockEntity",
                ae(Host.STANDARD, "wireless_access_point", inv(1)));
        declare(targets, "appeng.blockentity.networking.CableBusBlockEntity", ae(Host.CABLE));
        declare(targets, "appeng.parts.misc.InterfacePart", part(Host.INTERFACE));
        declare(targets, "appeng.parts.crafting.PatternProviderPart", part(Host.PATTERN));
        declare(targets, "appeng.parts.reporting.TerminalPart", part(Host.TERMINAL));
        declare(targets, "appeng.parts.reporting.CraftingTerminalPart", part(Host.CRAFTING));
        declare(targets, "appeng.parts.encoding.PatternEncodingTerminalPart", part(Host.ENCODING));
        for (String name :
                List.of(
                        "appeng.parts.storagebus.StorageBusPart",
                        "appeng.parts.automation.ImportBusPart",
                        "appeng.parts.automation.ExportBusPart",
                        "appeng.parts.automation.StorageLevelEmitterPart",
                        "appeng.parts.automation.FormationPlanePart"))
            declare(targets, name, part(Host.UPGRADES));
        for (String name :
                List.of(
                        "appeng.api.inventories.EmptyInternalInventory",
                        "appeng.api.upgrades.EmptyUpgradeInventory",
                        "appeng.blockentity.misc.CondenserBlockEntity$CondenseItemHandler"))
            declare(targets, name, part(Host.EMPTY));
        return Map.copyOf(targets);
    }

    private static Map<String, Layout> weaving() {
        var targets = new LinkedHashMap<String, Layout>();
        DECLARATIONS.forEach(
                (name, metadata) -> {
                    if (metadata.weaving() != null) targets.put(name, metadata.weaving());
                });
        return Map.copyOf(targets);
    }

    private static Map<String, Metadata> blockTypes() {
        var types = new LinkedHashMap<String, Metadata>();
        for (Metadata metadata : DECLARATIONS.values()) {
            if (metadata.packed() == null) continue;
            for (String id : metadata.packed().typeIds())
                if (types.putIfAbsent(id, metadata) != null)
                    throw new IllegalStateException("Duplicate storage block type: " + id);
        }
        return Map.copyOf(types);
    }

    private static void declare(Map<String, Metadata> targets, String name, Metadata metadata) {
        if (targets.putIfAbsent(name, metadata) != null)
            throw new IllegalStateException("Duplicate storage declaration: " + name);
    }

    private static Metadata weave(Layout layout) {
        return new Metadata(Admission.NONE, Loader.COMMON, Host.NONE, null, layout);
    }

    private static Metadata nativeOwner(@Nullable Layout layout) {
        return new Metadata(Admission.EXACT, Loader.COMMON, Host.NONE, null, layout);
    }

    private static Metadata family(@Nullable Layout layout) {
        return new Metadata(Admission.FAMILY, Loader.COMMON, Host.NONE, null, layout);
    }

    private static Metadata part(Host host) {
        return new Metadata(Admission.NONE, Loader.NEOFORGE, host, null, null);
    }

    private static Metadata ae(Host host, @Nullable Layout layout, @Nullable Packed packed) {
        return new Metadata(Admission.EXACT, Loader.NEOFORGE, host, packed, layout);
    }

    private static Metadata ae(Host host) {
        return ae(host, null, (Packed) null);
    }

    private static Metadata ae(Host host, String type, Field... fields) {
        return ae(
                host, null, new Packed(List.of("ae2:" + type), List.of(fields), host == Host.CPU));
    }

    private static Field inv(int capacity) {
        return new Field("inv", Format.INV_MAP, capacity, 0);
    }

    private static Field upgrades(int capacity) {
        return new Field("upgrades", Format.ITEM_LIST, capacity, 0);
    }

    private static Field keys(String name, int capacity) {
        return new Field(name, Format.KEY_LIST, capacity, 0);
    }

    private static Source list(String field, Owner owner, Publish publish) {
        return new Source(Kind.LIST, field, null, owner, publish, false);
    }

    private static Layout source(Source source) {
        return new Layout(source, List.of(), List.of(), null);
    }

    private static Layout hooks(String... methods) {
        return new Layout(
                null,
                List.of(),
                java.util.Arrays.stream(methods)
                        .map(method -> new Hook(method, Event.LIST))
                        .toList(),
                null);
    }

    private static Layout writes(String owner, String field) {
        return new Layout(null, List.of(new Write(owner, field)), List.of(), null);
    }

    private static Layout root(String getter, String descriptor, boolean refresh) {
        return new Layout(null, List.of(), List.of(), new Root(getter, descriptor, refresh));
    }
}
