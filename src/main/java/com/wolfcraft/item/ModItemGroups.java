package com.wolfcraft.item;

import com.wolfcraft.WolfCraftMod;
import net.fabricmc.fabric.api.itemgroup.v1.FabricItemGroup;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;

public class ModItemGroups {
    public static final CreativeModeTab WOLFCRAFT_TAB = Registry.register(
            BuiltInRegistries.CREATIVE_MODE_TAB,
            Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, "wolfcraft_tab"),
            FabricItemGroup.builder()
                    .title(Component.translatable("itemGroup.wolfcraft.tab"))
                    .icon(() -> new ItemStack(ModItems.MP40))
                    .displayItems((params, output) -> {
                        output.accept(ModItems.KNIFE);
                        output.accept(ModItems.LUGER);
                        output.accept(ModItems.COLT);
                        output.accept(ModItems.MP40);
                        output.accept(ModItems.THOMPSON);
                        output.accept(ModItems.STEN);
                        output.accept(ModItems.MAUSER);
                        output.accept(ModItems.VENOM);
                        output.accept(ModItems.FLAMETHROWER);
                        output.accept(ModItems.TESLA_GUN);
                        output.accept(ModItems.PANZERFAUST);
                        output.accept(ModItems.GRENADE);
                        output.accept(ModItems.MEDKIT_SMALL);
                        output.accept(ModItems.MEDKIT_LARGE);
                        output.accept(ModItems.AMMO_9MM);
                        output.accept(ModItems.AMMO_45CAL);
                        output.accept(ModItems.AMMO_792MM);
                        output.accept(ModItems.AMMO_127MM);
                        output.accept(ModItems.FUEL_TANK);
                        output.accept(ModItems.PANZERFAUST_ROCKET);
                        output.accept(ModItems.TESLA_CELL);
                    })
                    .build()
    );

    public static void initialize() {
        WolfCraftMod.LOGGER.info("Registered WolfCraft Creative Tab!");
    }
}
