package com.zerokg2004.gravitygun.gravitygun.recipe;

import com.zerokg2004.gravitygun.gravitygun.item.SuperchargedGravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import com.zerokg2004.gravitygun.gravitygun.registry.ModRecipes;
import net.minecraft.core.NonNullList;
import net.minecraft.core.RegistryAccess;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.inventory.CraftingContainer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CustomRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.level.Level;
import net.minecraftforge.registries.ForgeRegistries;

public class RedSuperchargedGravityGunRecipe extends CustomRecipe {

    // El pincel rojo del otro mod
    private static final String BRUSH_ID = "paintball:red_paintbrush";

    public RedSuperchargedGravityGunRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        int superGuns = 0;
        int brushes = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            // IMPORTANTE: Detectamos cualquier arma que sea de la clase Supercharged
            if (stack.getItem() instanceof SuperchargedGravityGunItem) {
                superGuns++;
            } else {
                ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (name != null && name.toString().equals(BRUSH_ID)) {
                    brushes++;
                } else {
                    return false;
                }
            }
        }
        return superGuns == 1 && brushes == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        // Retorna la versión Supercharged Roja
        return new ItemStack(ModItems.RED_SUPERCHARGED_GRAVITY_GUN.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());

            // Devolvemos el pincel rojo para que sea infinito
            if (name != null && name.toString().equals(BRUSH_ID)) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                remaining.set(i, copy);
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.RED_SUPERCHARGED_GRAVITY_GUN_RECIPE.get();
    }
}