// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.transform.inventory;

import com.hbm.inventory.StorageLayouts;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.*;

final class StorageWrites implements Opcodes {
    private StorageWrites() {}

    static void transform(ClassNode node, List<StorageLayouts.Write> writes) {
        for (var write : writes) {
            if (write.array()) {
                arrays(node, write);
                continue;
            }
            String owner = write.owner().replace('.', '/');
            String descriptor = null;
            if (owner.equals(node.name))
                descriptor =
                        node.fields.stream()
                                .filter(field -> field.name.equals(write.field()))
                                .map(field -> field.desc)
                                .findFirst()
                                .orElseThrow(() -> missing(node, write));
            int matched = 0;
            for (MethodNode method : node.methods) {
                if (method.name.equals("<init>") || method.name.equals("replaceStoredItem"))
                    continue;
                int targetLocal = method.maxLocals, valueLocal = targetLocal + 1;
                boolean used = false;
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (!(instruction instanceof FieldInsnNode field)
                            || field.getOpcode() != PUTFIELD
                            || !field.owner.equals(owner)
                            || !field.name.equals(write.field())) continue;
                    if (descriptor != null && !field.desc.equals(descriptor))
                        throw missing(node, write);
                    boolean stack = field.desc.equals("Lnet/minecraft/world/item/ItemStack;");
                    if (!stack && !field.desc.equals("I") && !field.desc.equals("Z"))
                        throw missing(node, write);
                    Type type = Type.getType(field.desc);
                    var before = new InsnList();
                    before.add(new VarInsnNode(type.getOpcode(ISTORE), valueLocal));
                    before.add(new VarInsnNode(ASTORE, targetLocal));
                    if (stack) {
                        before.add(new VarInsnNode(ALOAD, targetLocal));
                        before.add(
                                new FieldInsnNode(GETFIELD, field.owner, field.name, field.desc));
                        var present = new LabelNode();
                        var done = new LabelNode();
                        if (write.nullableOld()) {
                            before.add(new InsnNode(DUP));
                            before.add(new JumpInsnNode(IFNONNULL, present));
                            before.add(new InsnNode(POP));
                            before.add(new JumpInsnNode(GOTO, done));
                            before.add(present);
                        }
                        before.add(new VarInsnNode(ALOAD, targetLocal));
                        before.add(new InsnNode(ICONST_0));
                        before.add(
                                new MethodInsnNode(
                                        INVOKESTATIC,
                                        "com/hbm/inventory/InventorySlotBinding",
                                        "unbind",
                                        "(Lnet/minecraft/world/item/ItemStack;Ljava/lang/Object;I)V",
                                        false));
                        if (write.nullableOld()) before.add(done);
                    }
                    before.add(new VarInsnNode(ALOAD, targetLocal));
                    before.add(new VarInsnNode(type.getOpcode(ILOAD), valueLocal));
                    method.instructions.insertBefore(field, before);
                    var after = new InsnList();
                    after.add(new VarInsnNode(ALOAD, targetLocal));
                    after.add(new InsnNode(ICONST_0));
                    if (stack) after.add(new VarInsnNode(ALOAD, valueLocal));
                    after.add(
                            new MethodInsnNode(
                                    INVOKESTATIC,
                                    "com/hbm/inventory/InventoryChanges",
                                    stack ? "publishedStack" : "slotChanged",
                                    stack
                                            ? "(Ljava/lang/Object;ILnet/minecraft/world/item/ItemStack;)V"
                                            : "(Ljava/lang/Object;I)V",
                                    false));
                    method.instructions.insert(field, after);
                    matched++;
                    used = true;
                }
                if (used) method.maxLocals = Math.max(method.maxLocals, valueLocal + 1);
            }
            if (matched == 0) throw missing(node, write);
        }
    }

    private static void arrays(ClassNode node, StorageLayouts.Write write) {
        boolean fieldRead =
                node.methods.stream()
                        .flatMap(method -> java.util.Arrays.stream(method.instructions.toArray()))
                        .anyMatch(
                                instruction ->
                                        instruction instanceof FieldInsnNode field
                                                && field.getOpcode() == GETFIELD
                                                && field.name.equals(write.field())
                                                && field.desc.startsWith("[L"));
        if (!fieldRead) throw missing(node, write);
        int matched = 0;
        for (MethodNode method : node.methods) {
            if (method.name.equals("<init>") || method.name.equals("replaceStoredItem")) continue;
            int array = method.maxLocals,
                    index = array + 1,
                    value = index + 1,
                    previous = value + 1;
            boolean used = false;
            for (AbstractInsnNode instruction : method.instructions.toArray()) {
                if (instruction.getOpcode() != AASTORE) continue;
                if ((method.access & ACC_STATIC) != 0)
                    throw new IllegalStateException(
                            "Static inventory array write: " + node.name + "." + method.name);
                var before = new InsnList();
                before.add(new VarInsnNode(ASTORE, value));
                before.add(new VarInsnNode(ISTORE, index));
                before.add(new VarInsnNode(ASTORE, array));
                before.add(new VarInsnNode(ALOAD, array));
                before.add(new VarInsnNode(ILOAD, index));
                before.add(new InsnNode(AALOAD));
                before.add(new VarInsnNode(ASTORE, previous));
                before.add(new VarInsnNode(ALOAD, array));
                before.add(new VarInsnNode(ILOAD, index));
                before.add(new VarInsnNode(ALOAD, value));
                method.instructions.insertBefore(instruction, before);
                var after = new InsnList();
                var unchanged = new LabelNode();
                after.add(new VarInsnNode(ALOAD, previous));
                after.add(new VarInsnNode(ALOAD, value));
                after.add(
                        new MethodInsnNode(
                                INVOKESTATIC,
                                "java/util/Objects",
                                "equals",
                                "(Ljava/lang/Object;Ljava/lang/Object;)Z",
                                false));
                after.add(new JumpInsnNode(IFNE, unchanged));
                after.add(new VarInsnNode(ALOAD, 0));
                after.add(new VarInsnNode(ALOAD, array));
                after.add(new VarInsnNode(ILOAD, index));
                after.add(
                        new MethodInsnNode(
                                INVOKESTATIC,
                                "com/hbm/inventory/InventoryChanges",
                                "arraySlotChanged",
                                "(Ljava/lang/Object;Ljava/lang/Object;I)V",
                                false));
                after.add(unchanged);
                method.instructions.insert(instruction, after);
                matched++;
                used = true;
            }
            if (used) method.maxLocals = Math.max(method.maxLocals, previous + 1);
        }
        if (matched == 0)
            throw new IllegalStateException("Missing inventory array writes: " + node.name);
    }

    private static IllegalStateException missing(ClassNode node, StorageLayouts.Write write) {
        return new IllegalStateException(
                "Missing storage writes: "
                        + node.name
                        + " -> "
                        + write.owner()
                        + "."
                        + write.field());
    }
}
