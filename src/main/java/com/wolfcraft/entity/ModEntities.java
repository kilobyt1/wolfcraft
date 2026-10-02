package com.wolfcraft.entity;

import com.wolfcraft.WolfCraftMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MobCategory;

public class ModEntities {
    public static final EntityType<TeslaArcEntity> TESLA_ARC = register(
            "tesla_arc",
            EntityType.Builder.<TeslaArcEntity>of(TeslaArcEntity::new, MobCategory.MISC)
                    .sized(0.1f, 0.1f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
    );

    public static final EntityType<PanzerfaustRocketEntity> PANZERFAUST_ROCKET = register(
            "panzerfaust_rocket",
            EntityType.Builder.<PanzerfaustRocketEntity>of(PanzerfaustRocketEntity::new, MobCategory.MISC)
                    .sized(0.3f, 0.3f)
                    .clientTrackingRange(128)
                    .updateInterval(1)
    );

    public static final EntityType<GrenadeEntity> GRENADE = register(
            "grenade",
            EntityType.Builder.<GrenadeEntity>of(GrenadeEntity::new, MobCategory.MISC)
                    .sized(0.25f, 0.25f)
                    .clientTrackingRange(64)
                    .updateInterval(1)
    );

    private static <T extends net.minecraft.world.entity.Entity> EntityType<T> register(String name, EntityType.Builder<T> builder) {
        Identifier id = Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, name);
        ResourceKey<EntityType<?>> key = ResourceKey.create(Registries.ENTITY_TYPE, id);
        return Registry.register(BuiltInRegistries.ENTITY_TYPE, id, builder.build(key));
    }

    public static void initialize() {
        WolfCraftMod.LOGGER.info("Registered WolfCraft Entities successfully!");
    }
}
