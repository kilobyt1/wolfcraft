package com.wolfcraft.item;

import com.wolfcraft.WolfCraftMod;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;

public class ModItems {
    public static final Item KNIFE = register("knife", new KnifeItem(props("knife").stacksTo(1).durability(600)));
    public static final Item MP40 = register("mp40", new MP40Item(props("mp40").stacksTo(1).durability(1200)));
    public static final Item TESLA_GUN = register("tesla_gun", new TeslaGunItem(props("tesla_gun").stacksTo(1).durability(800)));
    public static final Item STEN = register("sten", new StenItem(props("sten").stacksTo(1).durability(1000)));
    public static final Item VENOM = register("venom", new VenomItem(props("venom").stacksTo(1).durability(2500)));
    public static final Item PANZERFAUST = register("panzerfaust", new PanzerfaustItem(props("panzerfaust").stacksTo(1).durability(200)));
    public static final Item MAUSER = register("mauser", new MauserItem(props("mauser").stacksTo(1).durability(800)));
    public static final Item THOMPSON = register("thompson", new ThompsonItem(props("thompson").stacksTo(1).durability(1200)));
    public static final Item COLT = register("colt", new ColtItem(props("colt").stacksTo(1).durability(600)));
    public static final Item LUGER = register("luger", new LugerItem(props("luger").stacksTo(1).durability(600)));
    public static final Item FLAMETHROWER = register("flamethrower", new FlamethrowerItem(props("flamethrower").stacksTo(1).durability(1500)));

    public static final Item GRENADE = register("grenade", new GrenadeItem(props("grenade").stacksTo(16)));
    public static final Item MEDKIT_SMALL = register("medkit_small", new MedkitItem(props("medkit_small").stacksTo(16), false));
    public static final Item MEDKIT_LARGE = register("medkit_large", new MedkitItem(props("medkit_large").stacksTo(8), true));

    public static final Item AMMO_9MM = register("ammo_9mm", new Item(props("ammo_9mm").stacksTo(64)));
    public static final Item AMMO_792MM = register("ammo_792mm", new Item(props("ammo_792mm").stacksTo(64)));
    public static final Item AMMO_127MM = register("ammo_127mm", new Item(props("ammo_127mm").stacksTo(16)));
    public static final Item AMMO_45CAL = register("ammo_45cal", new Item(props("ammo_45cal").stacksTo(64)));
    public static final Item FUEL_TANK = register("fuel_tank", new Item(props("fuel_tank").stacksTo(16)));
    public static final Item PANZERFAUST_ROCKET = register("panzerfaust_rocket", new Item(props("panzerfaust_rocket").stacksTo(8)));
    public static final Item TESLA_CELL = register("tesla_cell", new Item(props("tesla_cell").stacksTo(16)));

    private static Item.Properties props(String name) {
        Item.Properties p = new Item.Properties();
        Identifier id = Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, name);
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, id);
        p.setId(key);
        return p;
    }

    private static Item register(String name, Item item) {
        Identifier id = Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, name);
        return Registry.register(BuiltInRegistries.ITEM, id, item);
    }

    public static void initialize() {
        WolfCraftMod.LOGGER.info("Registered WolfCraft Items successfully!");
    }
}
