// SPDX-FileCopyrightText: 2026 movblock <admin@movblock.mov>
// SPDX-License-Identifier: LGPL-3.0-only

package com.hbm.lib;

import com.hbm.util.DamageClass;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.damagesource.DamageType;

public final class ModDamageTypes {

    public static final ResourceKey<DamageType> PHYSICAL = key("physical");
    public static final ResourceKey<DamageType> FIRE = key("fire");
    public static final ResourceKey<DamageType> EXPLOSIVE = key("explosive");
    public static final ResourceKey<DamageType> ELECTRIC = key("electric");
    public static final ResourceKey<DamageType> PLASMA = key("plasma");
    public static final ResourceKey<DamageType> LASER = key("laser");
    public static final ResourceKey<DamageType> MICROWAVE = key("microwave");
    public static final ResourceKey<DamageType> SUBATOMIC = key("subatomic");
    public static final ResourceKey<DamageType> OTHER = key("other");

    public static final ResourceKey<DamageType> NUCLEAR_BLAST = key("nuclear_blast");
    public static final ResourceKey<DamageType> MUD_POISONING = key("mud_poisoning");
    public static final ResourceKey<DamageType> ACID = key("acid");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF = key("euthanized_self");
    public static final ResourceKey<DamageType> EUTHANIZED_SELF_2 = key("euthanized_self_2");
    public static final ResourceKey<DamageType> TAU_BLAST = key("tau_blast");
    public static final ResourceKey<DamageType> RADIATION = key("radiation");
    public static final ResourceKey<DamageType> DIGAMMA = key("digamma");
    public static final ResourceKey<DamageType> SUICIDE = key("suicide");
    public static final ResourceKey<DamageType> RUBBLE = key("rubble");
    public static final ResourceKey<DamageType> SHRAPNEL = key("shrapnel");
    public static final ResourceKey<DamageType> BLACKHOLE = key("blackhole");

    public static final ResourceKey<DamageType> BLENDER = key("blender");
    public static final ResourceKey<DamageType> METEORITE = key("meteorite");
    public static final ResourceKey<DamageType> BOXCAR = key("boxcar");
    public static final ResourceKey<DamageType> BOAT = key("boat");
    public static final ResourceKey<DamageType> BUILDING = key("building");
    public static final ResourceKey<DamageType> TAINT = key("taint");
    public static final ResourceKey<DamageType> AMS = key("ams");
    public static final ResourceKey<DamageType> AMS_CORE = key("ams_core");
    public static final ResourceKey<DamageType> BROADCAST = key("broadcast");
    public static final ResourceKey<DamageType> BANG = key("bang");

    public static final ResourceKey<DamageType> PINK_CLOUD = key("pink_cloud");
    public static final ResourceKey<DamageType> CLOUD = key("cloud");
    public static final ResourceKey<DamageType> LEAD = key("lead");
    public static final ResourceKey<DamageType> ENERVATION = key("enervation");

    public static final ResourceKey<DamageType> ELECTRICITY = key("electricity");
    public static final ResourceKey<DamageType> EXHAUST = key("exhaust");
    public static final ResourceKey<DamageType> SPIKES = key("spikes");
    public static final ResourceKey<DamageType> LUNAR = key("lunar");
    public static final ResourceKey<DamageType> MONOXIDE = key("monoxide");
    public static final ResourceKey<DamageType> ASBESTOS = key("asbestos");
    public static final ResourceKey<DamageType> BLACKLUNG = key("blacklung");
    public static final ResourceKey<DamageType> MKU = key("mku");
    public static final ResourceKey<DamageType> VACUUM = key("vacuum");
    public static final ResourceKey<DamageType> OVERDOSE = key("overdose");

    public static final ResourceKey<DamageType> BOIL = key("boil");

    public static final ResourceKey<DamageType> FLAMETHROWER = key("flamethrower");

    public static final ResourceKey<DamageType> ICE = key("ice");

    public static final ResourceKey<DamageType> ACID_PLAYER = key("acid_player");

    public static final ResourceKey<DamageType> REVOLVER_BULLET = key("revolver_bullet");

    public static final ResourceKey<DamageType> EMPLACER = key("emplacer");

    public static final ResourceKey<DamageType> TAU = key("tau");

    public static final ResourceKey<DamageType> ELECTRIFIED = key("electrified");

    public static final ResourceKey<DamageType> EUTHANIZED = key("euthanized");

    public static final ResourceKey<DamageType> COMBINE = key("combine");

    public static final ResourceKey<DamageType> BOLT = key("bolt");

    public static final TagKey<DamageType> IS_BOLT =
            TagKey.create(Registries.DAMAGE_TYPE, Library.id("is_bolt"));

    public static final ResourceKey<DamageType> SHOCKWAVE = key("shockwave");

    private ModDamageTypes() {}

    private static ResourceKey<DamageType> key(String id) {
        return ResourceKey.create(Registries.DAMAGE_TYPE, Library.id(id));
    }

    public static ResourceKey<DamageType> forClass(DamageClass clazz) {
        return switch (clazz) {
            case PHYSICAL -> PHYSICAL;
            case FIRE -> FIRE;
            case EXPLOSIVE -> EXPLOSIVE;
            case ELECTRIC -> ELECTRIC;
            case PLASMA -> PLASMA;
            case LASER -> LASER;
            case MICROWAVE -> MICROWAVE;
            case SUBATOMIC -> SUBATOMIC;
            case OTHER -> OTHER;
        };
    }
}
