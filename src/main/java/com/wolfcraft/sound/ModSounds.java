package com.wolfcraft.sound;

import com.wolfcraft.WolfCraftMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvent;

public class ModSounds {
    public static final SoundEvent KNIFE_SLASH = register("knife_slash");
    public static final SoundEvent KNIFE_HIT = register("knife_hit");
    public static final SoundEvent MP40_FIRE = register("mp40_fire");
    public static final SoundEvent MP40_RELOAD = register("mp40_reload");
    public static final SoundEvent TESLA_ZAP = register("tesla_zap");
    public static final SoundEvent TESLA_IDLE = register("tesla_idle");
    public static final SoundEvent TESLA_LOOP = register("tesla_loop");
    public static final SoundEvent SOLDIER_AMBIENT = register("soldier_ambient");
    public static final SoundEvent SOLDIER_ATTACK = register("soldier_attack");
    public static final SoundEvent SOLDIER_HURT = register("soldier_hurt");
    public static final SoundEvent SOLDIER_DEATH = register("soldier_death");

    // Sten
    public static final SoundEvent STEN_FIRE = register("sten_fire");
    public static final SoundEvent STEN_OVERHEAT = register("sten_overheat");
    public static final SoundEvent STEN_RELOAD = register("sten_reload");

    // Venom
    public static final SoundEvent VENOM_FIRE = register("venom_fire");
    public static final SoundEvent VENOM_SPINUP = register("venom_spinup");
    public static final SoundEvent VENOM_SPINDOWN = register("venom_spindown");
    public static final SoundEvent VENOM_OVERHEAT = register("venom_overheat");

    // Panzerfaust
    public static final SoundEvent PANZERFAUST_FIRE = register("panzerfaust_fire");
    public static final SoundEvent PANZERFAUST_FLY = register("panzerfaust_fly");
    public static final SoundEvent PANZERFAUST_EXPLODE = register("panzerfaust_explode");

    // Mauser
    public static final SoundEvent MAUSER_FIRE = register("mauser_fire");
    public static final SoundEvent MAUSER_EJECT = register("mauser_eject");
    public static final SoundEvent MAUSER_RELOAD = register("mauser_reload");

    // Thompson
    public static final SoundEvent THOMPSON_FIRE = register("thompson_fire");
    public static final SoundEvent THOMPSON_RELOAD = register("thompson_reload");

    // Colt 1911
    public static final SoundEvent COLT_FIRE = register("colt_fire");
    public static final SoundEvent COLT_RELOAD = register("colt_reload");

    // Luger P08
    public static final SoundEvent LUGER_FIRE = register("luger_fire");
    public static final SoundEvent LUGER_RELOAD = register("luger_reload");

    // Flamethrower
    public static final SoundEvent FLAMETHROWER_START = register("flamethrower_start");
    public static final SoundEvent FLAMETHROWER_FIRE = register("flamethrower_fire");
    public static final SoundEvent FLAMETHROWER_END = register("flamethrower_end");

    // Grenade
    public static final SoundEvent GRENADE_THROW = register("grenade_throw");
    public static final SoundEvent GRENADE_BOUNCE = register("grenade_bounce");
    public static final SoundEvent GRENADE_EXPLODE = register("grenade_explode");

    // Medkit
    public static final SoundEvent MEDKIT_USE = register("medkit_use");

    private static SoundEvent register(String name) {
        Identifier id = Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.SOUND_EVENT, id, SoundEvent.createVariableRangeEvent(id));
    }

    public static void initialize() {
        WolfCraftMod.LOGGER.info("Registered RTCW Sounds!");
    }
}
