// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.transform.inventory;

import com.hbm.inventory.StorageLayouts;
import java.util.List;
import org.objectweb.asm.Opcodes;
import org.objectweb.asm.tree.*;

final class StorageHooks implements Opcodes {
    private StorageHooks() {}

    static void transform(ClassNode node, List<StorageLayouts.Hook> hooks) {
        for (var hook : hooks) {
            int matched = 0;
            for (MethodNode method : node.methods) {
                if (!method.name.equals(hook.method())) continue;
                if ((method.access & (ACC_STATIC | ACC_ABSTRACT)) != 0)
                    throw new IllegalStateException(
                            "Invalid storage hook: " + node.name + "." + method.name + method.desc);
                for (AbstractInsnNode instruction : method.instructions.toArray()) {
                    if (instruction.getOpcode() < IRETURN || instruction.getOpcode() > RETURN)
                        continue;
                    var publish = new InsnList();
                    publish.add(new VarInsnNode(ALOAD, 0));
                    if (hook.event() == StorageLayouts.Event.OWNER) {
                        if (node.methods.stream()
                                .noneMatch(
                                        value ->
                                                value.name.equals(hook.ownerGetter())
                                                        && value.desc.equals(
                                                                "()Lnet/minecraft/world/level/block/entity/BlockEntity;")))
                            throw new IllegalStateException(
                                    "Missing inventory owner getter: "
                                            + node.name
                                            + "."
                                            + hook.ownerGetter());
                        publish.add(
                                new MethodInsnNode(
                                        INVOKEVIRTUAL,
                                        node.name,
                                        hook.ownerGetter(),
                                        "()Lnet/minecraft/world/level/block/entity/BlockEntity;",
                                        false));
                    }
                    boolean root = hook.event() != StorageLayouts.Event.LIST;
                    publish.add(
                            new MethodInsnNode(
                                    INVOKESTATIC,
                                    "com/hbm/inventory/InventoryChanges",
                                    root ? "storageLayoutChanged" : "listReplaced",
                                    root
                                            ? "(Lnet/minecraft/world/level/block/entity/BlockEntity;)V"
                                            : "(Ljava/lang/Object;)V",
                                    false));
                    method.instructions.insertBefore(instruction, publish);
                    matched++;
                }
            }
            if (matched == 0)
                throw new IllegalStateException(
                        "Missing storage hook: " + node.name + "." + hook.method());
        }
    }
}
