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

public class GreenGravityGunRecipe extends CustomRecipe {

    // El nombre del ítem del otro mod
    private static final String BRUSH_ID = "paintball:green_paintbrush";

    public GreenGravityGunRecipe(ResourceLocation id, CraftingBookCategory category) {
        super(id, category);
    }

    @Override
    public boolean matches(CraftingContainer inv, Level level) {
        int guns = 0;
        int brushes = 0;

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            if (stack.isEmpty()) continue;

            if (stack.is(ModItems.GRAVITY_GUN.get())) {
                guns++;
            } else {
                ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());
                if (name != null && name.toString().equals(BRUSH_ID)) {
                    brushes++;
                } else {
                    return false;
                }
            }
        }
        return guns == 1 && brushes == 1;
    }

    @Override
    public ItemStack assemble(CraftingContainer inv, RegistryAccess registryAccess) {
        return new ItemStack(ModItems.GRAVITY_GUN_GREEN.get());
    }

    @Override
    public NonNullList<ItemStack> getRemainingItems(CraftingContainer inv) {
        NonNullList<ItemStack> remaining = NonNullList.withSize(inv.getContainerSize(), ItemStack.EMPTY);

        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);
            ResourceLocation name = ForgeRegistries.ITEMS.getKey(stack.getItem());

            // 🔥 Aquí está el truco: si es el pincel, lo devolvemos
            if (name != null && name.toString().equals(BRUSH_ID)) {
                ItemStack copy = stack.copy();
                copy.setCount(1);
                remaining.set(i, copy);
            }
            // La Gravity Gun NO se añade a 'remaining', por eso se consume
        }
        return remaining;
    }

    @Override
    public boolean canCraftInDimensions(int width, int height) {
        return width * height >= 2;
    }

    @Override
    public RecipeSerializer<?> getSerializer() {
        return ModRecipes.GREEN_GRAVITY_GUN_RECIPE.get();
    }
}