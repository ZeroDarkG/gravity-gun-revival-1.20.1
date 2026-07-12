package com.zerokg2004.gravitygun.gravitygun.recipe;

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

public class RedGravityGunRecipe extends CustomRecipe {
    private static final String BRUSH_ID = "paintball:red_paintbrush";

    public RedGravityGunRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        int guns = 0, brushes = 0;
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;
            if (stack.is(ModItems.GRAVITY_GUN.get())) {
                guns++;
            } else {
                ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (name != null && name.toString().equals(BRUSH_ID)) brushes++;
                else return false;
            }
        }
        return guns == 1 && brushes == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        return new ItemStack(ModItems.GRAVITY_GUN_RED.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());
            if (name != null && name.toString().equals(BRUSH_ID)) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                remaining.set(i, copy);
            }
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) { return width * height >= 2; }

    @Override
    public RecipeSerializer<?> getSerializer() { return ModRecipes.RED_GRAVITY_GUN_RECIPE.get(); }
}