// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.machine.storage;

import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.mojang.serialization.Codec;
import com.mojang.serialization.Dynamic;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import io.netty.buffer.ByteBuf;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Arrays;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import net.minecraft.SharedConstants;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtAccounter;
import net.minecraft.nbt.NbtIo;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.datafix.DataFixers;
import net.minecraft.util.datafix.fixes.References;
import net.minecraft.world.item.component.ItemContainerContents;
import org.jspecify.annotations.Nullable;

public record LockedContents(byte[] sealed, boolean heavy, float radiation) {

    public static final Codec<LockedContents> CODEC =
            RecordCodecBuilder.create(
                    i ->
                            i.group(
                                            Codec.BYTE_BUFFER
                                                    .xmap(LockedContents::bytes, ByteBuffer::wrap)
                                                    .fieldOf("sealed")
                                                    .forGetter(LockedContents::sealed),
                                            Codec.BOOL
                                                    .fieldOf("heavy")
                                                    .forGetter(LockedContents::heavy),
                                            Codec.FLOAT
                                                    .fieldOf("radiation")
                                                    .forGetter(LockedContents::radiation))
                                    .apply(i, LockedContents::new));
    public static final StreamCodec<ByteBuf, LockedContents> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE_ARRAY,
                    LockedContents::sealed,
                    ByteBufCodecs.BOOL,
                    LockedContents::heavy,
                    ByteBufCodecs.FLOAT,
                    LockedContents::radiation,
                    LockedContents::new);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    public static LockedContents seal(ServerLevel level, ItemContainerContents contents) {
        CompoundTag plain = new CompoundTag();
        plain.putInt("DataVersion", SharedConstants.getCurrentVersion().dataVersion().version());
        plain.put(
                "items",
                ItemContainerContents.CODEC
                        .encodeStart(
                                level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                                contents)
                        .getOrThrow());
        byte[] nonce = new byte[NONCE_BYTES];
        RANDOM.nextBytes(nonce);
        try {
            ByteArrayOutputStream bytes = new ByteArrayOutputStream();
            NbtIo.writeCompressed(plain, bytes);
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.ENCRYPT_MODE,
                    LockedContentsKey.get(level),
                    new GCMParameterSpec(TAG_BITS, nonce));
            byte[] body = cipher.doFinal(bytes.toByteArray());
            byte[] sealed = Arrays.copyOf(nonce, NONCE_BYTES + body.length);
            System.arraycopy(body, 0, sealed, NONCE_BYTES, body.length);
            return new LockedContents(
                    sealed,
                    contents.nonEmptyItems().iterator().hasNext(),
                    HazardTransformerRadiationContainer.sum(contents));
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public @Nullable ItemContainerContents open(ServerLevel level) {
        if (sealed.length < NONCE_BYTES) return null;
        byte[] plain;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    LockedContentsKey.get(level),
                    new GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES));
            plain = cipher.doFinal(sealed, NONCE_BYTES, sealed.length - NONCE_BYTES);
        } catch (AEADBadTagException e) {
            return null;
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
        CompoundTag tag;
        try {
            tag =
                    NbtIo.readCompressed(
                            new ByteArrayInputStream(plain), NbtAccounter.unlimitedHeap());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        int from = tag.getIntOr("DataVersion", 0);
        int to = SharedConstants.getCurrentVersion().dataVersion().version();
        ListTag items = tag.getListOrEmpty("items");
        if (from < to) {
            for (Tag slot : items) {
                if (!(slot instanceof CompoundTag compound)) continue;
                compound.getCompound("item")
                        .ifPresent(
                                item ->
                                        compound.put(
                                                "item",
                                                DataFixers.getDataFixer()
                                                        .update(
                                                                References.ITEM_STACK,
                                                                new Dynamic<>(
                                                                        NbtOps.INSTANCE, item),
                                                                from,
                                                                to)
                                                        .getValue()));
            }
        }
        return ItemContainerContents.CODEC
                .parse(level.registryAccess().createSerializationContext(NbtOps.INSTANCE), items)
                .getOrThrow();
    }

    private static byte[] bytes(ByteBuffer buffer) {
        byte[] bytes = new byte[buffer.remaining()];
        buffer.duplicate().get(bytes);
        return bytes;
    }

    @Override
    public boolean equals(Object o) {
        return o instanceof LockedContents other
                && heavy == other.heavy
                && radiation == other.radiation
                && Arrays.equals(sealed, other.sealed);
    }

    @Override
    public int hashCode() {
        return 31 * (31 * Arrays.hashCode(sealed) + Boolean.hashCode(heavy))
                + Float.hashCode(radiation);
    }

    @Override
    public String toString() {
        return "LockedContents["
                + sealed.length
                + " bytes, heavy="
                + heavy
                + ", radiation="
                + radiation
                + "]";
    }
}
