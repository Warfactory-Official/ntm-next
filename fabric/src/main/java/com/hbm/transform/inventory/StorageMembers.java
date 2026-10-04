// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.transform.inventory;

import com.hbm.inventory.StorageLayouts;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.commons.GeneratorAdapter;
import org.objectweb.asm.commons.Method;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FieldNode;
import org.objectweb.asm.tree.MethodNode;
import org.spongepowered.asm.mixin.transformer.ClassInfo;

final class StorageMembers implements Opcodes {
    private static final Type OBJECT = Type.getType(Object.class);
    private static final Type STACK = Type.getObjectType("net/minecraft/world/item/ItemStack");
    private static final Type LIST = Type.getObjectType("net/minecraft/core/NonNullList");
    private static final Type CONTAINER = Type.getObjectType("net/minecraft/world/Container");
    private static final Type CHANGES = Type.getObjectType("com/hbm/inventory/InventoryChanges");
    private static final Type BINDING =
            Type.getObjectType("com/hbm/inventory/InventorySlotBinding");

    private StorageMembers() {}

    static void transform(ClassNode node, StorageLayouts.Layout layout) {
        var source = layout.source();
        if (source != null) {
            if (source.kind() == StorageLayouts.Kind.CHILDREN) {
                children(node, source);
                return;
            }
            if (source.kind() == StorageLayouts.Kind.STACK) scalar(node, source);
            else if (source.kind() == StorageLayouts.Kind.KEYED) keyed(node);
            else if (source.kind() == StorageLayouts.Kind.ARRAY) array(node, source);
            else if (source.kind() == StorageLayouts.Kind.DECLARED)
                implement(node, "com/hbm/inventory/SlotSource");
            else list(node, source);
            owner(node, source.owner());
            publish(node, source.publish());
        }
        if (layout.root() != null) root(node, layout.root());
    }

    private static void keyed(ClassNode node) {
        implement(node, "com/hbm/inventory/KeyedSlotSource");
        Type map = Type.getObjectType("it/unimi/dsi/fastutil/objects/Object2IntOpenHashMap");
        Type free = Type.getObjectType("it/unimi/dsi/fastutil/ints/IntArrayList");
        Type array = Type.getType(Object[].class);
        lazyField(
                node,
                "hbm$keySlots",
                "inventoryKeySlots",
                map,
                method -> {
                    method.newInstance(map);
                    method.dup();
                    method.invokeConstructor(map, new Method("<init>", "()V"));
                    method.dup();
                    method.push(-1);
                    method.invokeVirtual(map, new Method("defaultReturnValue", "(I)V"));
                });
        lazyField(
                node,
                "hbm$slotKeys",
                "inventorySlotKeys",
                array,
                method -> {
                    method.push(0);
                    method.newArray(OBJECT);
                });
        lazyField(
                node,
                "hbm$freeSlots",
                "inventoryFreeSlots",
                free,
                method -> {
                    method.newInstance(free);
                    method.dup();
                    method.invokeConstructor(free, new Method("<init>", "()V"));
                });
        var set = method(node, "inventorySlotKeys", "([Ljava/lang/Object;)V");
        set.loadThis();
        set.loadArg(0);
        set.putField(Type.getObjectType(node.name), "hbm$slotKeys", array);
        end(set);
    }

    private static void lazyField(
            ClassNode node,
            String field,
            String getter,
            Type type,
            java.util.function.Consumer<GeneratorAdapter> initialize) {
        if (node.fields.stream().anyMatch(value -> value.name.equals(field)))
            throw new IllegalStateException(
                    "Duplicate sparse inventory field: " + node.name + "." + field);
        node.fields.add(
                new FieldNode(
                        ACC_PRIVATE | ACC_SYNTHETIC, field, type.getDescriptor(), null, null));
        Type self = Type.getObjectType(node.name);
        var get = method(node, getter, "()" + type.getDescriptor());
        var initialized = get.newLabel();
        get.loadThis();
        get.getField(self, field, type);
        get.ifNonNull(initialized);
        get.loadThis();
        initialize.accept(get);
        get.putField(self, field, type);
        get.mark(initialized);
        get.loadThis();
        get.getField(self, field, type);
        end(get);
    }

    private static void children(ClassNode node, StorageLayouts.Source source) {
        implement(node, "com/hbm/inventory/InventoryChildren");
        FieldNode field =
                node.fields.stream()
                        .filter(
                                value ->
                                        value.name.equals(source.field())
                                                && (value.desc.startsWith("[L")
                                                        || value.desc.startsWith("L")))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Missing inventory children: " + node.name));
        var get = method(node, "inventoryChildren", "()[Ljava/lang/Object;");
        if (!field.desc.startsWith("[L")) {
            get.push(1);
            get.newArray(OBJECT);
            get.dup();
            get.push(0);
        }
        get.loadThis();
        get.getField(Type.getObjectType(node.name), field.name, Type.getType(field.desc));
        if (!field.desc.startsWith("[L")) get.arrayStore(OBJECT);
        end(get);
    }

    private static void array(ClassNode node, StorageLayouts.Source source) {
        implement(node, "com/hbm/inventory/ArraySlotSource");
        FieldNode field =
                node.fields.stream()
                        .filter(
                                value ->
                                        value.name.equals(source.field())
                                                && value.desc.startsWith("[L"))
                        .findFirst()
                        .orElseThrow(
                                () ->
                                        new IllegalStateException(
                                                "Missing inventory array: " + node.name));
        var get = method(node, "inventorySlotArray", "()Ljava/lang/Object;");
        get.loadThis();
        get.getField(Type.getObjectType(node.name), field.name, Type.getType(field.desc));
        end(get);
    }

    private static void list(ClassNode node, StorageLayouts.Source source) {
        implement(
                node,
                source.kind() == StorageLayouts.Kind.INTERIOR
                        ? "com/hbm/inventory/InteriorSlotSource"
                        : "com/hbm/inventory/ListSlotSource");
        Type self = Type.getObjectType(node.name);
        Type fieldOwner =
                source.fieldOwner() == null
                        ? self
                        : Type.getObjectType(source.fieldOwner().replace('.', '/'));
        if (source.kind() != StorageLayouts.Kind.METHODS) {
            if (fieldOwner.equals(self)) field(node, source.field(), LIST).access &= ~ACC_FINAL;
            else if (!node.superName.equals(fieldOwner.getInternalName()))
                throw new IllegalStateException("Storage field parent changed: " + node.name);
        }
        if (source.kind() == StorageLayouts.Kind.METHODS) {
            requireMethod(node, "getItems", "()" + LIST.getDescriptor());
            requireMethod(node, "setItems", "(" + LIST.getDescriptor() + ")V");
        }
        var get = method(node, "inventorySlotList", "()" + LIST.getDescriptor());
        get.loadThis();
        if (source.kind() == StorageLayouts.Kind.METHODS)
            get.invokeVirtual(self, new Method("getItems", "()" + LIST.getDescriptor()));
        else get.getField(fieldOwner, source.field(), LIST);
        end(get);
        var set = method(node, "inventorySlotList", "(" + LIST.getDescriptor() + ")V");
        set.loadThis();
        set.loadArg(0);
        if (source.kind() == StorageLayouts.Kind.METHODS)
            set.invokeVirtual(self, new Method("setItems", "(" + LIST.getDescriptor() + ")V"));
        else set.putField(fieldOwner, source.field(), LIST);
        end(set);
        if (source.containerCommit()) {
            var replace =
                    method(
                            node,
                            "replaceStoredItem",
                            "(ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z");
            replace.loadThis();
            replace.invokeVirtual(
                    self, new Method("inventorySlotList", "()" + LIST.getDescriptor()));
            replace.loadArg(0);
            replace.invokeVirtual(LIST, new Method("get", "(I)Ljava/lang/Object;"));
            replace.loadArg(1);
            var accepted = replace.newLabel();
            replace.ifCmp(OBJECT, GeneratorAdapter.EQ, accepted);
            replace.push(false);
            replace.returnValue();
            replace.mark(accepted);
            replace.loadThis();
            replace.loadArg(0);
            replace.loadArg(2);
            replace.invokeInterface(
                    CONTAINER, new Method("setItem", "(ILnet/minecraft/world/item/ItemStack;)V"));
            replace.push(true);
            end(replace);
        }
    }

    private static void scalar(ClassNode node, StorageLayouts.Source source) {
        Type self = Type.getObjectType(node.name);
        field(node, source.field(), STACK);
        implement(node, "com/hbm/inventory/SlotSource");
        implement(node, "com/hbm/inventory/TrackedStorage");
        var count = method(node, "storedSlotCount", "()I");
        count.push(1);
        end(count);
        var direct = method(node, "directStoredMutation", "()Z");
        direct.push(true);
        end(direct);
        var read = method(node, "storedItem", "(I)Lnet/minecraft/world/item/ItemStack;");
        read.loadThis();
        read.getField(self, source.field(), STACK);
        end(read);
        var replace =
                method(
                        node,
                        "replaceStoredItem",
                        "(ILnet/minecraft/world/item/ItemStack;Lnet/minecraft/world/item/ItemStack;)Z");
        replace.loadThis();
        replace.getField(self, source.field(), STACK);
        replace.loadArg(1);
        var accepted = replace.newLabel();
        replace.ifCmp(OBJECT, GeneratorAdapter.EQ, accepted);
        replace.push(false);
        replace.returnValue();
        replace.mark(accepted);
        replace.loadArg(1);
        replace.loadThis();
        replace.push(0);
        replace.invokeStatic(
                BINDING,
                new Method("unbind", "(Lnet/minecraft/world/item/ItemStack;Ljava/lang/Object;I)V"));
        replace.loadThis();
        replace.loadArg(2);
        replace.putField(self, source.field(), STACK);
        replace.loadThis();
        replace.push(0);
        replace.loadArg(2);
        replace.invokeStatic(
                CHANGES,
                new Method(
                        "publishedStack",
                        "(Ljava/lang/Object;ILnet/minecraft/world/item/ItemStack;)V"));
        replace.push(true);
        end(replace);
        var sources = method(node, "inventorySources", "(Ljava/util/function/Consumer;)V");
        sources.loadArg(0);
        sources.loadThis();
        accept(sources);
        end(sources);
    }

    private static void owner(ClassNode node, StorageLayouts.Owner owner) {
        if (owner == StorageLayouts.Owner.SELF) return;
        Type self = Type.getObjectType(node.name);
        var get = method(node, "inventoryOwner", "()Ljava/lang/Object;");
        var set = method(node, "inventoryOwner", "(Ljava/lang/Object;)V");
        if (owner == StorageLayouts.Owner.EXTERNAL) {
            String name = "hbm$inventoryOwner";
            if (node.fields.stream().anyMatch(field -> field.name.equals(name)))
                throw new IllegalStateException("Duplicate storage owner: " + node.name);
            node.fields.add(
                    new FieldNode(
                            ACC_PRIVATE | ACC_SYNTHETIC, name, OBJECT.getDescriptor(), null, null));
            get.loadThis();
            get.getField(self, name, OBJECT);
            set.loadThis();
            set.loadArg(0);
            set.putField(self, name, OBJECT);
        } else {
            Type menu = Type.getObjectType("net/minecraft/world/inventory/AbstractContainerMenu");
            field(node, "menu", menu);
            get.loadThis();
            get.getField(self, "menu", menu);
            get.invokeVirtual(menu, new Method("hbm$fuseOwner", "()Ljava/lang/Object;"));
            set.loadThis();
            set.invokeVirtual(self, new Method("inventoryOwner", "()Ljava/lang/Object;"));
            set.loadArg(0);
            set.invokeStatic(
                    CHANGES, new Method("fixedOwner", "(Ljava/lang/Object;Ljava/lang/Object;)V"));
        }
        end(get);
        end(set);
    }

    private static void publish(ClassNode node, StorageLayouts.Publish publish) {
        if (publish == StorageLayouts.Publish.NONE) return;
        Type self = Type.getObjectType(node.name);
        var method = method(node, "publishStoredChanges", "(I)V");
        if (publish == StorageLayouts.Publish.CONTAINER) {
            method.loadThis();
            method.invokeInterface(CONTAINER, new Method("setChanged", "()V"));
        } else if (publish == StorageLayouts.Publish.BOOKSHELF) {
            requireMethod(node, "updateState", "(I)V");
            method.loadThis();
            method.loadArg(0);
            method.invokeVirtual(self, new Method("updateState", "(I)V"));
        } else {
            String field = publish == StorageLayouts.Publish.LECTERN ? "book" : "item";
            method.loadThis();
            method.getField(self, field, STACK);
            method.invokeVirtual(STACK, new Method("isEmpty", "()Z"));
            var done = method.newLabel();
            method.ifZCmp(GeneratorAdapter.EQ, done);
            method.loadThis();
            if (publish == StorageLayouts.Publish.LECTERN) {
                requireMethod(node, "onBookItemRemove", "()V");
                method.invokeVirtual(self, new Method("onBookItemRemove", "()V"));
            } else {
                requireMethod(node, "setTheItem", "(Lnet/minecraft/world/item/ItemStack;)V");
                method.getStatic(STACK, "EMPTY", STACK);
                method.invokeVirtual(
                        self, new Method("setTheItem", "(Lnet/minecraft/world/item/ItemStack;)V"));
            }
            method.mark(done);
        }
        end(method);
    }

    private static void root(ClassNode node, StorageLayouts.Root root) {
        implement(node, "com/hbm/inventory/TrackedStorage");
        requireMethod(node, root.getter(), root.descriptor());
        Type self = Type.getObjectType(node.name);
        var sources = method(node, "inventorySources", "(Ljava/util/function/Consumer;)V");
        sources.loadArg(0);
        sources.loadThis();
        sources.invokeVirtual(self, new Method(root.getter(), root.descriptor()));
        accept(sources);
        end(sources);
        if (root.refresh()) {
            var refresh = method(node, "refreshInventorySources", "()V");
            refresh.loadThis();
            refresh.invokeStatic(
                    CHANGES,
                    new Method(
                            "sources",
                            "(Lnet/minecraft/world/level/block/entity/BlockEntity;)Ljava/lang/Object;"));
            refresh.loadThis();
            refresh.invokeVirtual(self, new Method(root.getter(), root.descriptor()));
            var done = refresh.newLabel();
            refresh.ifCmp(OBJECT, GeneratorAdapter.EQ, done);
            refresh.loadThis();
            refresh.invokeStatic(
                    CHANGES,
                    new Method(
                            "storageLayoutChanged",
                            "(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"));
            refresh.mark(done);
            end(refresh);
        }
    }

    private static void accept(GeneratorAdapter method) {
        method.invokeInterface(
                Type.getType(java.util.function.Consumer.class),
                new Method("accept", "(Ljava/lang/Object;)V"));
    }

    private static void implement(ClassNode node, String name) {
        if (node.interfaces.contains(name)) return;
        node.interfaces.add(name);
        ClassInfo.forName(node.name).getSignature().addInterface(name);
    }

    private static GeneratorAdapter method(ClassNode node, String name, String descriptor) {
        if (node.methods.stream()
                .anyMatch(method -> method.name.equals(name) && method.desc.equals(descriptor)))
            throw new IllegalStateException(
                    "Duplicate storage member: " + node.name + "." + name + descriptor);
        var method = new MethodNode(ACC_PUBLIC | ACC_SYNTHETIC, name, descriptor, null, null);
        node.methods.add(method);
        return new GeneratorAdapter(method, method.access, name, descriptor);
    }

    private static void end(GeneratorAdapter method) {
        method.returnValue();
        method.endMethod();
    }

    private static FieldNode field(ClassNode node, String name, Type type) {
        return node.fields.stream()
                .filter(
                        candidate ->
                                candidate.name.equals(name)
                                        && candidate.desc.equals(type.getDescriptor()))
                .findFirst()
                .orElseThrow(
                        () ->
                                new IllegalStateException(
                                        "Missing storage field: " + node.name + "." + name + type));
    }

    private static void requireMethod(ClassNode node, String name, String descriptor) {
        if (node.methods.stream()
                .noneMatch(method -> method.name.equals(name) && method.desc.equals(descriptor)))
            throw new IllegalStateException(
                    "Missing storage method: " + node.name + "." + name + descriptor);
    }
}
