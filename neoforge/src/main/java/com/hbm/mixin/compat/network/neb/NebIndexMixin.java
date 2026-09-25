// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.mixin.compat.network.neb;

import it.unimi.dsi.fastutil.ints.Int2ObjectArrayMap;
import it.unimi.dsi.fastutil.objects.Object2IntMap;
import java.util.ArrayList;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.resources.Identifier;
import org.objectweb.asm.Opcodes;
import org.spongepowered.asm.mixin.*;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Redirect;

@Pseudo
@Mixin(targets = "cn.ussshenzhou.notenoughbandwidth.indextype.NamespaceIndexManager", remap = false)
public abstract class NebIndexMixin {
    @Shadow private static volatile boolean initialized;
    @Shadow @Final private static ArrayList<String> NAMESPACES;
    @Shadow @Final private static ArrayList<ArrayList<String>> PATHS;
    @Shadow @Final private static Object2IntMap<String> NAMESPACE_MAP;
    @Shadow @Final private static Int2ObjectArrayMap<Object2IntMap<String>> PATH_MAPS;
    @Unique private static Identifier[][] hbm$identifiers;

    @Redirect(
            method = "init",
            at =
                    @At(
                            value = "FIELD",
                            target =
                                    "Lcn/ussshenzhou/notenoughbandwidth/indextype/NamespaceIndexManager;initialized:Z",
                            opcode = Opcodes.PUTSTATIC))
    private static void hbm$publish(boolean ready) {
        if (!ready) {
            initialized = false;
            hbm$identifiers = null;
            return;
        }
        Identifier[][] identifiers = new Identifier[NAMESPACES.size()][];
        for (int i = 0; i < identifiers.length; i++) {
            var paths = PATHS.get(i);
            Identifier[] row = identifiers[i] = new Identifier[paths.size()];
            for (int j = 0; j < row.length; j++)
                row[j] = Identifier.fromNamespaceAndPath(NAMESPACES.get(i), paths.get(j));
        }
        hbm$identifiers = identifiers;
        initialized = true;
    }

    @Unique
    private static void hbm$writePrefix(Identifier type, FriendlyByteBuf output) {
        int namespace = initialized ? NAMESPACE_MAP.getInt(type.getNamespace()) : -1;
        Object2IntMap<String> paths = namespace < 0 ? null : PATH_MAPS.get(namespace);
        int index = paths == null ? -1 : paths.getOrDefault(type.getPath(), -1);
        if (index < 0) {
            output.writeByte(0);
            output.writeIdentifier(type);
        } else {
            output.writeVarInt(namespace);
            output.writeVarInt(index);
        }
    }

    @Overwrite
    public static Identifier getIdentifier(int namespaceIndex, int pathIndex) {
        if (!initialized) return null;
        if (namespaceIndex == 0)
            throw new UnsupportedOperationException("namespaceIndex should not be 0");
        return hbm$identifiers[namespaceIndex][pathIndex];
    }
}
