// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.blockentity.machine.storage;

import com.hbm.hazard.transformer.HazardTransformerRadiationContainer;
import com.hbm.inventory.ItemContents;
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
import java.util.List;
import javax.crypto.AEADBadTagException;
import javax.crypto.Cipher;
import javax.crypto.SecretKey;
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

public final class LockedContents {

    private final byte[] sealed;
    private final boolean heavy;
    private final float radiation;
    public final long minimumDeadline;
    public final boolean hasReferences;
    private @Nullable SecretKey openedKey;
    private @Nullable ItemContainerContents opened;

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
                                                    .forGetter(LockedContents::radiation),
                                            Codec.LONG
                                                    .fieldOf("minimum_deadline")
                                                    .forGetter(LockedContents::minimumDeadline),
                                            Codec.BOOL
                                                    .fieldOf("has_references")
                                                    .forGetter(LockedContents::hasReferences))
                                    .apply(i, LockedContents::new));
    public static final StreamCodec<ByteBuf, LockedContents> STREAM_CODEC =
            StreamCodec.composite(
                    ByteBufCodecs.BYTE_ARRAY,
                    LockedContents::sealed,
                    ByteBufCodecs.BOOL,
                    LockedContents::heavy,
                    ByteBufCodecs.FLOAT,
                    LockedContents::radiation,
                    ByteBufCodecs.LONG,
                    LockedContents::minimumDeadline,
                    ByteBufCodecs.BOOL,
                    LockedContents::hasReferences,
                    LockedContents::new);

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int NONCE_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final SecureRandom RANDOM = new SecureRandom();

    public LockedContents(byte[] sealed, boolean heavy, float radiation) {
        this(sealed, heavy, radiation, Long.MAX_VALUE, false);
    }

    public LockedContents(
            byte[] sealed,
            boolean heavy,
            float radiation,
            long minimumDeadline,
            boolean hasReferences) {
        this.sealed = sealed.clone();
        this.heavy = heavy;
        this.radiation = radiation;
        this.minimumDeadline = minimumDeadline;
        this.hasReferences = hasReferences;
    }

    public byte[] sealed() {
        return sealed.clone();
    }

    public boolean heavy() {
        return heavy;
    }

    public float radiation() {
        return radiation;
    }

    public long minimumDeadline() {
        return minimumDeadline;
    }

    public boolean hasReferences() {
        return hasReferences;
    }

    public static LockedContents seal(ServerLevel level, ItemContainerContents contents) {
        assert level.getServer().isSameThread();
        boolean heavy = contents.nonEmptyItems().iterator().hasNext();
        float radiation = HazardTransformerRadiationContainer.sum(contents);
        long minimumDeadline = ItemContents.minimumDeadline(contents);
        boolean hasReferences = ItemContents.hasReferences(contents);
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
            SecretKey key = LockedContentsKey.get(level);
            cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(TAG_BITS, nonce));
            cipher.updateAAD(metadata(heavy, radiation, minimumDeadline, hasReferences));
            byte[] body = cipher.doFinal(bytes.toByteArray());
            byte[] sealed = Arrays.copyOf(nonce, NONCE_BYTES + body.length);
            System.arraycopy(body, 0, sealed, NONCE_BYTES, body.length);
            LockedContents result =
                    new LockedContents(sealed, heavy, radiation, minimumDeadline, hasReferences);
            result.openedKey = key;
            result.opened = new ItemContainerContents(List.copyOf(contents.items));
            return result;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }

    public @Nullable ItemContainerContents open(ServerLevel level) {
        assert level.getServer().isSameThread();
        if (sealed.length < NONCE_BYTES) return null;
        SecretKey key = LockedContentsKey.get(level);
        if (openedKey == key) return opened;
        byte[] plain;
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(
                    Cipher.DECRYPT_MODE,
                    key,
                    new GCMParameterSpec(TAG_BITS, sealed, 0, NONCE_BYTES));
            cipher.updateAAD(metadata(heavy, radiation, minimumDeadline, hasReferences));
            plain = cipher.doFinal(sealed, NONCE_BYTES, sealed.length - NONCE_BYTES);
        } catch (AEADBadTagException e) {
            openedKey = key;
            opened = null;
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
        ItemContainerContents result =
                ItemContainerContents.CODEC
                        .parse(
                                level.registryAccess().createSerializationContext(NbtOps.INSTANCE),
                                items)
                        .getOrThrow();
        openedKey = key;
        opened = new ItemContainerContents(List.copyOf(result.items));
        return opened;
    }

    private static byte[] metadata(
            boolean heavy, float radiation, long minimumDeadline, boolean hasReferences) {
        return ByteBuffer.allocate(1 + Float.BYTES + Long.BYTES + 1)
                .put((byte) (heavy ? 1 : 0))
                .putInt(Float.floatToIntBits(radiation))
                .putLong(minimumDeadline)
                .put((byte) (hasReferences ? 1 : 0))
                .array();
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
                && Float.floatToIntBits(radiation) == Float.floatToIntBits(other.radiation)
                && minimumDeadline == other.minimumDeadline
                && hasReferences == other.hasReferences
                && Arrays.equals(sealed, other.sealed);
    }

    @Override
    public int hashCode() {
        int hash =
                31 * (31 * Arrays.hashCode(sealed) + Boolean.hashCode(heavy))
                        + Float.hashCode(radiation);
        return 31 * (31 * hash + Long.hashCode(minimumDeadline)) + Boolean.hashCode(hasReferences);
    }

    @Override
    public String toString() {
        return "LockedContents["
                + sealed.length
                + " bytes, heavy="
                + heavy
                + ", radiation="
                + radiation
                + ", minimumDeadline="
                + minimumDeadline
                + ", hasReferences="
                + hasReferences
                + "]";
    }
}
