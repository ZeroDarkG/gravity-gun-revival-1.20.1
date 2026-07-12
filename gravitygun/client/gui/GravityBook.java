package com.zerokg2004.gravitygun.gravitygun.client.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import com.zerokg2004.gravitygun.gravitygun.client.RenderModeHandler;
import com.zerokg2004.gravitygun.gravitygun.client.RenderModeHandler.Mode;
import com.zerokg2004.gravitygun.gravitygun.item.GravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.item.SuperchargedGravityGunItem;
import com.zerokg2004.gravitygun.gravitygun.registry.ModItems;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.item.ItemStack;

public class GravityBook extends Screen {

    // Fondo del libro
    private static final ResourceLocation BACKGROUND =
            new ResourceLocation("gravitygun", "textures/gui/gravity_book_page.png");

    // Botón de cerrar
    private static final ResourceLocation CLOSE_BUTTON =
            new ResourceLocation("gravitygun", "textures/gui/close_button.png");
    private static final ResourceLocation CLOSE_BUTTON_HOVER =
            new ResourceLocation("gravitygun", "textures/gui/close_button_hover.png");

    // Textura vanilla de la mesa de crafteo
    private static final ResourceLocation CRAFTING_TABLE_TOP =
            new ResourceLocation("minecraft", "textures/block/crafting_table_top.png");

    // Imágenes de las mini interfaces
    private static final ResourceLocation RECIPE_TEXTURE =
            new ResourceLocation("gravitygun", "textures/gui/gg_recipe.png");
    private static final ResourceLocation SUPER_TUTORIAL_TEXTURE =
            new ResourceLocation("gravitygun", "textures/gui/gg_supercharged_tutorial.png");

    // Botón de info central
    private static final ResourceLocation INFO_BUTTON =
            new ResourceLocation("gravitygun", "textures/gui/info_button.png");
    private static final ResourceLocation INFO_BUTTON_HOVER =
            new ResourceLocation("gravitygun", "textures/gui/info_button_hover.png");

    // Páginas de “origen”
    private static final ResourceLocation ORIGIN_ES =
            new ResourceLocation("gravitygun", "textures/gui/origin_gg_es.png");
    private static final ResourceLocation ORIGIN_EN =
            new ResourceLocation("gravitygun", "textures/gui/origin_gg_en.png");

    private static final int BUTTON_SIZE = 25;
    private static final int BUTTON_MARGIN = 18;

    private final int textureWidth = 256;
    private final int textureHeight = 256;
    private final float scale = 0.75f;

    // coords internas del libro (sin escalar)
    private int closeX, closeY;
    private int modeX, modeY;
    private int recipeX, recipeY;
    private int superX, superY;
    private int infoX, infoY;
    private int guiLeft, guiTop;

    private boolean showRecipe = false;
    private boolean showSuperTutorial = false;
    private boolean showInfo = false;

    public GravityBook() {
        super(Component.literal("Gravity Book")); // nombre global en todos los idiomas
    }

    // Botón genérico sin texto (por si lo quieres usar en más sitios)
    public static class ImageButtonNoText extends Button {
        private final ResourceLocation texture;
        private final String tooltip;

        public ImageButtonNoText(int x, int y, int width, int height,
                                 ResourceLocation texture, String tooltip, OnPress onPress) {
            super(x, y, width, height, Component.empty(), onPress, DEFAULT_NARRATION);
            this.texture = texture;
            this.tooltip = tooltip;
        }

        @Override
        public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
            super.render(guiGraphics, mouseX, mouseY, partialTick);

            RenderSystem.setShaderTexture(0, texture);
            guiGraphics.blit(texture, getX(), getY(), 0, 0, width, height, width, height);

            if (isHoveredOrFocused()) {
                guiGraphics.fill(getX(), getY(), getX() + width, getY() + height, 0x80FFFFFF);
                guiGraphics.renderTooltip(Minecraft.getInstance().font,
                        Component.literal(tooltip), mouseX, mouseY);
            }
        }
    }

    // === Helper para saber si el idioma actual es español ===
    private boolean isSpanishLang() {
        Minecraft mc = Minecraft.getInstance();
        String langCode = mc.getLanguageManager().getSelected(); // ej: "es_es", "en_us"
        if (langCode == null) return false;
        langCode = langCode.toLowerCase();
        return langCode.startsWith("es");
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.pose().pushPose();

        int scaledWidth = (int) (textureWidth * scale);
        int scaledHeight = (int) (textureHeight * scale);
        guiLeft = (this.width - scaledWidth) / 2;
        guiTop = (this.height - scaledHeight) / 2;

        guiGraphics.pose().translate(guiLeft, guiTop, 0);
        guiGraphics.pose().scale(scale, scale, 1);

        // Fondo del libro
        guiGraphics.blit(BACKGROUND, 0, 0, 0, 0, textureWidth, textureHeight);

        // --- Posiciones de botones (en coords internas del libro) ---
        // Arriba derecha: cerrar
        closeX = textureWidth - BUTTON_SIZE - BUTTON_MARGIN;
        closeY = BUTTON_MARGIN;

        // Arriba izquierda: cambiar modo
        modeX = BUTTON_MARGIN;
        modeY = BUTTON_MARGIN;

        // Abajo izquierda: receta
        recipeX = BUTTON_MARGIN;
        recipeY = textureHeight - BUTTON_SIZE - BUTTON_MARGIN;

        // Abajo derecha: tutorial supercharged
        superX = textureWidth - BUTTON_SIZE - BUTTON_MARGIN;
        superY = textureHeight - BUTTON_SIZE - BUTTON_MARGIN;

        // Centro del libro: botón de info
        infoX = textureWidth / 2 - BUTTON_SIZE / 2;
        infoY = textureHeight / 2 - BUTTON_SIZE / 2;

        // Ratón en coords internas del libro
        int actualMouseX = (int) ((mouseX - guiLeft) / scale);
        int actualMouseY = (int) ((mouseY - guiTop) / scale);

        boolean hoveringClose = isInside(actualMouseX, actualMouseY, closeX, closeY);
        boolean hoveringMode = isInside(actualMouseX, actualMouseY, modeX, modeY);
        boolean hoveringRecipe = isInside(actualMouseX, actualMouseY, recipeX, recipeY);
        boolean hoveringSuper = isInside(actualMouseX, actualMouseY, superX, superY);
        boolean hoveringInfo = isInside(actualMouseX, actualMouseY, infoX, infoY);

        Mode currentMode = RenderModeHandler.getMode();
        boolean spanish = isSpanishLang();

        // --- Botón cerrar ---
        ResourceLocation closeTex = hoveringClose ? CLOSE_BUTTON_HOVER : CLOSE_BUTTON;
        guiGraphics.blit(closeTex, closeX, closeY, 0, 0,
                BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE, BUTTON_SIZE);

        // --- Botón de modo (arriba izquierda) ---
        ItemStack gunForButton = getCurrentGunForModeButton();
        guiGraphics.pose().pushPose();
        float iconScale = 1.5f;
        float centerX = modeX + BUTTON_SIZE / 2f;
        float centerY = modeY + BUTTON_SIZE / 2f;
        guiGraphics.pose().translate(centerX, centerY, 0);
        guiGraphics.pose().scale(iconScale, iconScale, 1);
        guiGraphics.renderItem(gunForButton, -8, -8);
        guiGraphics.pose().popPose();

        if (hoveringMode) {
            guiGraphics.fill(modeX, modeY, modeX + BUTTON_SIZE, modeY + BUTTON_SIZE, 0x40FFFFFF);
        }

        // --- Botón receta (abajo izquierda) con crafting table top, ESCALADA ---
        guiGraphics.pose().pushPose();
        float craftScale = (float) BUTTON_SIZE / 16.0f; // escalar 16x16 a ~25x25
        float craftCenterX = recipeX + BUTTON_SIZE / 2f;
        float craftCenterY = recipeY + BUTTON_SIZE / 2f;
        guiGraphics.pose().translate(craftCenterX, craftCenterY, 0);
        guiGraphics.pose().scale(craftScale, craftScale, 1);
        guiGraphics.blit(CRAFTING_TABLE_TOP, -8, -8,
                0, 0, 16, 16, 16, 16);
        guiGraphics.pose().popPose();

        if (hoveringRecipe) {
            guiGraphics.fill(recipeX, recipeY, recipeX + BUTTON_SIZE, recipeY + BUTTON_SIZE, 0x40FFFFFF);
        }

        // --- Botón tutorial supercharged (abajo derecha) ---
        ItemStack superGun = new ItemStack(ModItems.GRAVITY_GUN_SUPERCHARGED.get());
        guiGraphics.pose().pushPose();
        float iconScaleSuper = 1.5f;
        float centerSuperX = superX + BUTTON_SIZE / 2f;
        float centerSuperY = superY + BUTTON_SIZE / 2f;
        guiGraphics.pose().translate(centerSuperX, centerSuperY, 0);
        guiGraphics.pose().scale(iconScaleSuper, iconScaleSuper, 1);
        guiGraphics.renderItem(superGun, -8, -8);
        guiGraphics.pose().popPose();

        if (hoveringSuper) {
            guiGraphics.fill(superX, superY, superX + BUTTON_SIZE, superY + BUTTON_SIZE, 0x40FFFFFF);
        }

        // --- Botón de info central ---
        guiGraphics.pose().pushPose();
        float infoScale = (float) BUTTON_SIZE / 16.0f;
        float infoCenterX = infoX + BUTTON_SIZE / 2f;
        float infoCenterY = infoY + BUTTON_SIZE / 2f;
        guiGraphics.pose().translate(infoCenterX, infoCenterY, 0);
        guiGraphics.pose().scale(infoScale, infoScale, 1);

        ResourceLocation infoTex = hoveringInfo ? INFO_BUTTON_HOVER : INFO_BUTTON;
        guiGraphics.blit(infoTex, -8, -8, 0, 0, 16, 16, 16, 16);

        guiGraphics.pose().popPose();

        if (hoveringInfo) {
            guiGraphics.fill(infoX, infoY, infoX + BUTTON_SIZE, infoY + BUTTON_SIZE, 0x40FFFFFF);
        }

        // --- Mini interfaces dentro del libro, GRANDES y CENTRADAS ---
        if (showRecipe || showSuperTutorial || showInfo) {

            ResourceLocation tex;
            int baseW;
            int baseH;
            float overlayScale;

            if (showInfo) {
                tex = spanish ? ORIGIN_ES : ORIGIN_EN;
                baseW = 300;
                baseH = 450;
                overlayScale = 0.5f;   // tamaño de la página de info
            } else {
                tex = showRecipe ? RECIPE_TEXTURE : SUPER_TUTORIAL_TEXTURE;
                baseW = 47;
                baseH = 30;
                overlayScale = 4.0f;
            }

            int centerBookX = textureWidth / 2;
            int centerBookY = textureHeight / 2;

            guiGraphics.pose().pushPose();
            guiGraphics.pose().translate(centerBookX, centerBookY, 0);
            guiGraphics.pose().scale(overlayScale, overlayScale, 1);
            guiGraphics.blit(tex,
                    -baseW / 2, -baseH / 2,
                    0, 0, baseW, baseH, baseW, baseH);
            guiGraphics.pose().popPose();
        }

        guiGraphics.pose().popPose();

        // --- Tooltips en coords normales ---
        if (hoveringClose) {
            String text = spanish ? "Cerrar libro" : "Close book";
            guiGraphics.renderTooltip(this.font, Component.literal(text), mouseX, mouseY);
        }

        if (hoveringMode) {
            String texto = spanish
                    ? (currentMode == Mode.PLANE ? "Cambiar a 3D" : "Cambiar a 2D")
                    : (currentMode == Mode.PLANE ? "Switch to 3D" : "Switch to 2D");
            guiGraphics.renderTooltip(this.font, Component.literal(texto), mouseX, mouseY);
        }

        if (hoveringRecipe) {
            String text = spanish ? "Receta de la Gravity Gun" : "Gravity Gun recipe";
            guiGraphics.renderTooltip(this.font, Component.literal(text),
                    mouseX, mouseY);
        }

        if (hoveringSuper) {
            String text = spanish ? "Cómo conseguir la Supercharged" : "How to obtain the Supercharged gun";
            guiGraphics.renderTooltip(this.font, Component.literal(text),
                    mouseX, mouseY);
        }

        if (hoveringInfo) {
            String text = spanish ? "Información" : "Info";
            guiGraphics.renderTooltip(this.font, Component.literal(text),
                    mouseX, mouseY);
        }

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private boolean isInside(int mouseX, int mouseY, int x, int y) {
        return mouseX >= x && mouseX < x + BUTTON_SIZE &&
                mouseY >= y && mouseY < y + BUTTON_SIZE;
    }

    private ItemStack getCurrentGunForModeButton() {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null) {
            return new ItemStack(ModItems.GRAVITY_GUN.get());
        }

        ItemStack held = mc.player.getMainHandItem();

        if (held.getItem() instanceof GravityGunItem ||
                held.getItem() instanceof SuperchargedGravityGunItem) {
            return held;
        }

        return new ItemStack(ModItems.GRAVITY_GUN.get());
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        int actualMouseX = (int) ((mouseX - guiLeft) / scale);
        int actualMouseY = (int) ((mouseY - guiTop) / scale);

        boolean spanish = isSpanishLang();

        // Cerrar libro (Mantiene sonido de cerrar libro vanilla)
        if (isInside(actualMouseX, actualMouseY, closeX, closeY)) {
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BOOK_PUT, 1.0F, 1.0F);
            }
            this.onClose();
            return true;
        }

        // --- ✅ CAMBIO DE MODO (Aquí es donde entra tu "Hazaña" con el nuevo sonido) ---
        if (isInside(actualMouseX, actualMouseY, modeX, modeY)) {
            Minecraft mc = Minecraft.getInstance();
            Mode current = RenderModeHandler.getMode();

            if (current == Mode.PLANE) {
                boolean fancy = mc.options.graphicsMode().get().ordinal() > 0;
                RenderModeHandler.setMode(fancy ? Mode.DETAILED : Mode.SIMPLE);
            } else {
                RenderModeHandler.setMode(Mode.PLANE);
            }

            if (mc.player != null) {
                String modeNameEs = switch (RenderModeHandler.getMode()) {
                    case PLANE -> "2D";
                    case SIMPLE -> "3D Simple";
                    case DETAILED -> "3D Detallado";
                    case AUTO -> "Automático";
                };
                String modeNameEn = switch (RenderModeHandler.getMode()) {
                    case PLANE -> "2D";
                    case SIMPLE -> "Simple 3D";
                    case DETAILED -> "Detailed 3D";
                    case AUTO -> "Automatic";
                };

                String msg = spanish
                        ? "Modo de render cambiado a: " + modeNameEs
                        : "Render mode changed to: " + modeNameEn;

                mc.player.displayClientMessage(Component.literal(msg), true);

                // REEMPLAZADO: Quitamos el Pling y ponemos tu sonido personalizado
                mc.player.playSound(com.zerokg2004.gravitygun.gravitygun.registry.SoundEventsRegistry.BUTTON_SOUND.get(), 1.0F, 1.0F);
            }

            return true;
        }

        // Botón receta (Mantiene sonido de pasar página)
        if (isInside(actualMouseX, actualMouseY, recipeX, recipeY)) {
            showRecipe = !showRecipe;
            if (showRecipe) {
                showSuperTutorial = false;
                showInfo = false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.3F);
            }
            return true;
        }

        // Botón tutorial supercharged (Mantiene sonido de pasar página)
        if (isInside(actualMouseX, actualMouseY, superX, superY)) {
            showSuperTutorial = !showSuperTutorial;
            if (showSuperTutorial) {
                showRecipe = false;
                showInfo = false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.7F, 1.5F);
            }
            return true;
        }

        // Botón de info central (Mantiene sonido de pasar página)
        if (isInside(actualMouseX, actualMouseY, infoX, infoY)) {
            showInfo = !showInfo;
            if (showInfo) {
                showRecipe = false;
                showSuperTutorial = false;
            }
            Minecraft mc = Minecraft.getInstance();
            if (mc.player != null) {
                mc.player.playSound(SoundEvents.BOOK_PAGE_TURN, 0.9F, 1.0F);
            }
            return true;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }
}