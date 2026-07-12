package com.zerokg2004.gravitygun.gravitygun.registry;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.item.SuperchargedGravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.item.GravityBookItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.Rarity;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;
import net.minecraft.world.item.BlockItem;

public class ModItems {

    public static final DeferredRegister<Item> ITEMS =
            DeferredRegister.create(ForgeRegistries.ITEMS, Gravitygun.MODID);

    public static final RegistryObject<Item> GRAVITY_GUN = ITEMS.register("gravity_gun",
            () -> new GravityGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.RARE)));

    public static final RegistryObject<Item> GRAVITY_GUN_SUPERCHARGED = ITEMS.register("gravity_gun_supercharged",
            () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1).rarity(Rarity.EPIC)));

    public static final RegistryObject<Item> GRAVITY_GUN_RED =
            ITEMS.register("red_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_BLUE =
            ITEMS.register("blue_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_GREEN =
            ITEMS.register("green_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_ORANGE =
            ITEMS.register("orange_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_YELLOW =
            ITEMS.register("yellow_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_PURPLE =
            ITEMS.register("purple_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_WHITE =
            ITEMS.register("white_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_GUN_CYAN =
            ITEMS.register("cyan_gravity_gun", () -> new GravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> RED_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("red_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> BLUE_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("blue_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GREEN_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("green_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> ORANGE_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("orange_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> YELLOW_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("yellow_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> PURPLE_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("purple_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> WHITE_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("white_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> CYAN_SUPERCHARGED_GRAVITY_GUN =
            ITEMS.register("cyan_supercharged_gravity_gun", () -> new SuperchargedGravityGunItem(new Item.Properties().stacksTo(1)));

    public static final RegistryObject<Item> GRAVITY_BOOK = ITEMS.register("gravity_book",
            () -> new GravityBookItem(new Item.Properties().stacksTo(1).rarity(Rarity.UNCOMMON)));

    public static void register(IEventBus eventBus) {
        ITEMS.register(eventBus);
    }
}
