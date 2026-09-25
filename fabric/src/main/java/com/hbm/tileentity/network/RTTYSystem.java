// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-FileCopyrightText: HbmMods (The Bobcat)
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.tileentity.network;

import com.hbm.interfaces.NotableComments;
import com.hbm.util.NoteBuilder.Instrument;
import com.hbm.util.NoteBuilder.Note;
import com.hbm.util.NoteBuilder.Octave;
import com.hbm.util.NoteBuilder;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map.Entry;
import java.util.concurrent.ConcurrentHashMap;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.level.Level;
import org.apache.commons.lang3.math.NumberUtils;

public class RTTYSystem {

    private static final Object[] MELODY;

    public static ConcurrentHashMap<ChannelKey, RTTYChannel> broadcast = new ConcurrentHashMap();

    public static ConcurrentHashMap<ChannelKey, Object> newMessages = new ConcurrentHashMap();

    static {
        int tempo = 4;
        MELODY = new Object[tempo * 160];
        Arrays.fill(MELODY, "");

        Instrument flute = Instrument.PIANO;
        Instrument accordion = Instrument.BASSGUITAR;

        MELODY[0] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        MELODY[tempo * 2] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 4] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 6] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        MELODY[tempo * 8] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.LOW)
                        .end();
        MELODY[tempo * 12] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
        MELODY[tempo * 14] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(accordion, Note.C, Octave.MID)
                        .end();
        MELODY[tempo * 16] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(accordion, Note.C, Octave.MID)
                        .end();
        MELODY[tempo * 18] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        MELODY[tempo * 20] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.LOW)
                        .end();
        MELODY[tempo * 24] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        MELODY[tempo * 26] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 28] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 30] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        MELODY[tempo * 32] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.LOW)
                        .end();
        MELODY[tempo * 36] = NoteBuilder.start().add(accordion, Note.D, Octave.LOW).end();
        MELODY[tempo * 38] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 40] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 42] = NoteBuilder.start().add(accordion, Note.C, Octave.LOW).end();
        MELODY[tempo * 44] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.LOW)
                        .end();
        MELODY[tempo * 48] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 50] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        MELODY[tempo * 52] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(flute, Note.D, Octave.MID)
                        .end();

        MELODY[tempo * 54] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW);
        MELODY[tempo * 56] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 58] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        MELODY[tempo * 60] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.LOW)
                        .add(flute, Note.D, Octave.MID)
                        .end();
        MELODY[tempo * 64] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(flute, Note.E, Octave.MID)
                        .end();
        MELODY[tempo * 66] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(accordion, Note.C, Octave.MID)
                        .end();
        MELODY[tempo * 67] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        MELODY[tempo * 68] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(accordion, Note.C, Octave.MID)
                        .add(flute, Note.E, Octave.MID)
                        .end();
        MELODY[tempo * 69] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        MELODY[tempo * 70] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.E, Octave.MID)
                        .end();
        MELODY[tempo * 71] = NoteBuilder.start().add(flute, Note.B, Octave.MID).end();
        MELODY[tempo * 72] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.LOW)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 76] =
                NoteBuilder.start()
                        .add(accordion, Note.G, Octave.LOW)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 78] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 80] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.F, Octave.LOW)
                        .end();
        MELODY[tempo * 81] = NoteBuilder.start().add(flute, Note.G, Octave.MID).end();
        MELODY[tempo * 82] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 84] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 88] =
                NoteBuilder.start()
                        .add(accordion, Note.G, Octave.LOW)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 90] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 92] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.F, Octave.LOW)
                        .end();
        MELODY[tempo * 93] =
                NoteBuilder.start()
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.G, Octave.MID)
                        .end();
        MELODY[tempo * 94] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(flute, Note.E, Octave.LOW)
                        .end();
        MELODY[tempo * 96] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .end();
        MELODY[tempo * 100] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 101] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        MELODY[tempo * 102] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.D, Octave.MID)
                        .end();
        MELODY[tempo * 104] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .end();
        MELODY[tempo * 106] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 107] = NoteBuilder.start().add(flute, Note.F, Octave.LOW).end();
        MELODY[tempo * 108] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.D, Octave.MID)
                        .end();
        MELODY[tempo * 112] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(flute, Note.E, Octave.MID)
                        .end();
        MELODY[tempo * 114] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(accordion, Note.C, Octave.MID)
                        .end();
        MELODY[tempo * 115] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        MELODY[tempo * 116] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.LOW)
                        .add(accordion, Note.C, Octave.MID)
                        .add(flute, Note.E, Octave.MID)
                        .end();
        MELODY[tempo * 117] = NoteBuilder.start().add(flute, Note.F, Octave.MID).end();
        MELODY[tempo * 118] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.E, Octave.MID)
                        .end();
        MELODY[tempo * 119] = NoteBuilder.start().add(flute, Note.C, Octave.MID).end();
        MELODY[tempo * 120] =
                NoteBuilder.start()
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.G, Octave.LOW)
                        .add(accordion, Note.B, Octave.MID)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 124] =
                NoteBuilder.start()
                        .add(accordion, Note.G, Octave.LOW)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 126] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.MID)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 128] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.MID)
                        .add(flute, Note.F, Octave.LOW)
                        .end();
        MELODY[tempo * 129] = NoteBuilder.start().add(flute, Note.G, Octave.MID).end();
        MELODY[tempo * 130] =
                NoteBuilder.start()
                        .add(accordion, Note.F, Octave.LOW)
                        .add(flute, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 132] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(accordion, Note.E, Octave.LOW)
                        .add(accordion, Note.A, Octave.MID)
                        .add(accordion, Note.G, Octave.LOW)
                        .end();
        MELODY[tempo * 134] = NoteBuilder.start().add(flute, Note.A, Octave.MID).end();
        MELODY[tempo * 136] =
                NoteBuilder.start()
                        .add(accordion, Note.C, Octave.LOW)
                        .add(flute, Note.D, Octave.LOW)
                        .end();
        MELODY[tempo * 138] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.MID)
                        .end();
        MELODY[tempo * 140] =
                NoteBuilder.start()
                        .add(accordion, Note.D, Octave.LOW)
                        .add(accordion, Note.F, Octave.LOW)
                        .add(accordion, Note.A, Octave.MID)
                        .end();
    }

    public static void broadcast(Level world, String channelName, Object signal) {
        ChannelKey identifier = new ChannelKey(world.dimension(), channelName);

        if (NumberUtils.isCreatable("" + signal) && newMessages.containsKey(identifier)) {
            Object existing = newMessages.get(identifier);
            if (NumberUtils.isCreatable("" + existing)) {
                try {
                    long first = Long.parseLong("" + signal);
                    long second = Long.parseLong("" + existing);
                    newMessages.put(identifier, "" + (first + second));
                    return;
                } catch (Exception ex) {
                }
            }
        }

        newMessages.put(identifier, signal);
    }

    public static RTTYChannel listen(Level world, String channelName) {
        RTTYChannel channel = broadcast.get(new ChannelKey(world.dimension(), channelName));
        return channel;
    }

    public static void updateBroadcastQueue(MinecraftServer server) {

        for (Entry<ChannelKey, Object> worldEntry : newMessages.entrySet()) {
            ChannelKey identifier = worldEntry.getKey();
            Object lastSignal = worldEntry.getValue();

            ServerLevel level = server.getLevel(identifier.level());
            if (level == null) continue;

            RTTYChannel channel = new RTTYChannel();
            channel.timeStamp = level.getGameTime();
            channel.signal = lastSignal;

            broadcast.put(identifier, channel);
        }

        HashMap<ChannelKey, RTTYChannel> toAdd = new HashMap();
        for (ServerLevel world : server.getAllLevels()) {
            RTTYChannel chan = new RTTYChannel();
            chan.timeStamp = world.getGameTime();
            chan.signal = getTestSender(chan.timeStamp);
            toAdd.put(new ChannelKey(world.dimension(), "2012-08-06"), chan);
        }

        broadcast.putAll(toAdd);
        newMessages.clear();
    }

    public static void onServerStopping() {
        broadcast.clear();
        newMessages.clear();
    }

    public static Object getTestSender(long timeStamp) {
        return MELODY[(int) (timeStamp % MELODY.length)];
    }

    public enum RTTYSpecialSignal {
        BEGIN_TTY,
        STOP_TTY,
        PRINT_BUFFER
    }

    public record ChannelKey(ResourceKey<Level> level, String channel) {}

    @NotableComments
    public static class RTTYChannel {
        public long timeStamp = -1;
        public Object signal;
    }
}
