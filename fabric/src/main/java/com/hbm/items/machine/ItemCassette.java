// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.items.machine;

import com.hbm.items.ModItems;
import java.util.function.Consumer;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import org.jspecify.annotations.Nullable;

public class ItemCassette extends Item {

    public ItemCassette(Properties properties) {
        super(properties);
    }

    public static TrackType typeOf(ItemStack stack) {
        TrackType track = ModItems.SIREN_TRACK.typeOf(stack);
        return track == null ? TrackType.NULL : track;
    }

    @Override
    public void appendHoverText(
            ItemStack stack,
            Item.TooltipContext context,
            TooltipDisplay display,
            Consumer<Component> adder,
            TooltipFlag flag) {
        TrackType track = typeOf(stack);
        adder.accept(Component.translatable("desc.item.cassette.sirenSoundCassette"));
        adder.accept(Component.translatable("desc.item.cassette.name", track.getTrackTitle()));
        adder.accept(Component.translatable("desc.item.cassette.type", track.getType().name()));
        adder.accept(Component.translatable("desc.item.cassette.volume", track.getVolume()));
    }

    public enum SoundType {
        LOOP,
        PASS,
        SOUND
    }

    public enum TrackType {
        NULL(" ", null, SoundType.SOUND, 0, 0),
        HATCH("Hatch Siren", "alarm.hatch", SoundType.LOOP, 3358839, 250),
        ATUOPILOT("Autopilot Disconnected", "alarm.autopilot", SoundType.LOOP, 11908533, 50),
        AMS_SIREN("AMS Siren", "alarm.ams_siren", SoundType.LOOP, 15055698, 50),
        BLAST_DOOR("Blast Door Alarm", "alarm.blast_door_alarm", SoundType.LOOP, 11665408, 50),
        APC_LOOP("APC Siren", "alarm.apc_loop", SoundType.LOOP, 3565216, 50),
        KLAXON("Klaxon", "alarm.klaxon", SoundType.LOOP, 8421504, 50),
        KLAXON_A("Vault Door Alarm", "alarm.fo_klaxon_a", SoundType.LOOP, 0x8c810b, 50),
        KLAXON_B("Security Alert", "alarm.fo_klaxon_b", SoundType.LOOP, 0x76818e, 50),
        SIREN("Standard Siren", "alarm.regular_siren", SoundType.LOOP, 6684672, 100),
        CLASSIC("Classic Siren", "alarm.classic", SoundType.LOOP, 0xc0cfe8, 100),
        BANK_ALARM("Bank Alarm", "alarm.bank_alarm", SoundType.LOOP, 3572962, 100),
        BEEP_SIREN("Beep Siren", "alarm.beep_siren", SoundType.LOOP, 13882323, 100),
        CONTAINER_ALARM("Container Alarm", "alarm.container_alarm", SoundType.LOOP, 14727839, 100),
        SWEEP_SIREN("Sweep Siren", "alarm.sweep_siren", SoundType.LOOP, 15592026, 500),
        STRIDER_SIREN("Missile Silo Siren", "alarm.strider_siren", SoundType.LOOP, 11250586, 500),
        AIR_RAID("Air Raid Siren", "alarm.air_raid", SoundType.LOOP, 0xDF3795, 500),
        NOSTROMO_SIREN(
                "Nostromo Self Destruct", "alarm.nostromo_siren", SoundType.LOOP, 0x5dd800, 100),
        EAS_ALARM("EAS Alarm Screech", "alarm.eas_alarm", SoundType.LOOP, 0xb3a8c1, 50),
        APC_PASS("APC Pass", "alarm.apc_pass", SoundType.PASS, 3422163, 50),
        RAZORTRAIN("Razortrain Horn", "alarm.razortrain_horn", SoundType.SOUND, 7819501, 250);

        public static final TrackType[] VALUES = values();

        private final String title;
        private final @Nullable String sound;
        private final SoundType type;
        private final int color;
        private final int volume;

        TrackType(String name, @Nullable String sound, SoundType type, int color, int volume) {
            this.title = name;
            this.sound = sound;
            this.type = type;
            this.color = color;
            this.volume = volume;
        }

        public static TrackType byId(int i) {
            if (i >= 0 && i < VALUES.length) return VALUES[i];
            return NULL;
        }

        public String getTrackTitle() {
            return title;
        }

        public @Nullable String getSoundName() {
            return sound;
        }

        public SoundType getType() {
            return type;
        }

        public int getColor() {
            return color;
        }

        public int getVolume() {
            return volume;
        }
    }
}
