package com.zerokg2004.gravitygun.gravitygun.registry;

import com.zerokg2004.gravitygun.gravitygun.Gravitygun;
import com.zerokg2004.gravitygun.gravitygun.recipe.*;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SimpleCraftingRecipeSerializer;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModRecipes {

    public static final DeferredRegister<RecipeSerializer<?>> SERIALIZERS =
            DeferredRegister.create(ForgeRegistries.RECIPE_SERIALIZERS, Gravitygun.MODID);

    public static final RegistryObject<RecipeSerializer<GravityBookRecipe>> GRAVITY_BOOK =
            SERIALIZERS.register("gravity_book",
                    () -> new SimpleCraftingRecipeSerializer<>(GravityBookRecipe::new));

    // --- VARIANTES NORMALES ---
    public static final RegistryObject<RecipeSerializer<GreenGravityGunRecipe>> GREEN_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("green_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(GreenGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<RedGravityGunRecipe>> RED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("red_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(RedGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<BlueGravityGunRecipe>> BLUE_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("blue_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(BlueGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<PurpleGravityGunRecipe>> PURPLE_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("purple_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(PurpleGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<OrangeGravityGunRecipe>> ORANGE_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("orange_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(OrangeGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<YellowGravityGunRecipe>> YELLOW_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("yellow_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(YellowGravityGunRecipe::new));

    // --- VARIANTES SUPERCHARGED ---
    public static final RegistryObject<RecipeSerializer<GreenSuperchargedGravityGunRecipe>> GREEN_SUPERCHARGED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("green_supercharged_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(GreenSuperchargedGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<RedSuperchargedGravityGunRecipe>> RED_SUPERCHARGED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("red_supercharged_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(RedSuperchargedGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<BlueSuperchargedGravityGunRecipe>> BLUE_SUPERCHARGED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("blue_supercharged_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(BlueSuperchargedGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<PurpleSuperchargedGravityGunRecipe>> PURPLE_SUPERCHARGED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("purple_supercharged_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(PurpleSuperchargedGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<OrangeSuperchargedGravityGunRecipe>> ORANGE_SUPERCHARGED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("orange_supercharged_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(OrangeSuperchargedGravityGunRecipe::new));

    public static final RegistryObject<RecipeSerializer<YellowSuperchargedGravityGunRecipe>> YELLOW_SUPERCHARGED_GRAVITY_GUN_RECIPE =
            SERIALIZERS.register("yellow_supercharged_gravity_gun_recipe",
                    () -> new SimpleCraftingRecipeSerializer<>(YellowSuperchargedGravityGunRecipe::new));
}