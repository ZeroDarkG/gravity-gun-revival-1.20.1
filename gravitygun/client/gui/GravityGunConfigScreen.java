package com.zerokg2004.gravitygun.gravitygun.client.gui;

import com.zerokg2004.gravitygun.gravitygun.registry.Config;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

@OnlyIn(Dist.CLIENT)
public class GravityGunConfigScreen extends Screen {

    private static final int MENU_COLOR_TOP = 0xEE585858;
    private static final int MENU_COLOR_BOTTOM = 0xEE1F1F1F;
    private static final ResourceLocation GRAVITY_GUN_2D =
            new ResourceLocation("gravitygun", "textures/item/gravity_gun_2d.png");
    private static final ResourceLocation SUPERCHARGED_GUN_2D =
            new ResourceLocation("gravitygun", "textures/item/gravity_gun_supercharged_2d.png");
    private static final int SUPERCHARGED_BLUE = 0x55FFFF;

    private Button pushValueButton;
    private Button superPushValueButton;
    private final Screen lastScreen;
    private int pushValueX;
    private int pushValueY;
    private int superPushValueX;
    private int superPushValueY;
    private int valueWidth;
    private static final int VALUE_HEIGHT = 20;

    public GravityGunConfigScreen(Screen lastScreen) {
        super(Component.literal("Gravity Gun Config"));
        this.lastScreen = lastScreen;
    }

    @Override
    protected void init() {
        int centerX = this.width / 2;
        int startY = this.height / 2 - 58;
        int toggleWidth = 100;
        this.valueWidth = 72;
        int adjustWidth = 24;

        this.addRenderableWidget(Button.builder(
                        Component.literal(Config.crosshairEnabled ? "§aON" : "§cOFF"),
                        button -> {
                            Config.setCrosshairEnabled(!Config.crosshairEnabled);
                            button.setMessage(Component.literal(Config.crosshairEnabled ? "§aON" : "§cOFF"));
                        })
                .pos(centerX + 10, startY)
                .size(toggleWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(
                        Component.literal(Config.simpleModelFastGraphics ? "§aON" : "§cOFF"),
                        button -> {
                            Config.setSimpleModelFastGraphics(!Config.simpleModelFastGraphics);
                            button.setMessage(Component.literal(Config.simpleModelFastGraphics ? "§aON" : "§cOFF"));
                        })
                .pos(centerX + 10, startY + 25)
                .size(toggleWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                    Config.setPushFactor(Math.max(5, Config.pushFactor - 1));
                    refreshValueButtons();
                })
                .pos(centerX + 10, startY + 54)
                .size(adjustWidth, 20)
                .build());

        this.pushValueButton = Button.builder(Component.literal(""), button -> {})
                .pos(centerX + 38, startY + 54)
                .size(this.valueWidth, VALUE_HEIGHT)
                .build();
        this.pushValueButton.active = false;
        this.pushValueButton.visible = false;
        this.addRenderableWidget(this.pushValueButton);
        this.pushValueX = centerX + 38;
        this.pushValueY = startY + 54;

        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    Config.setPushFactor(Math.min(100, Config.pushFactor + 1));
                    refreshValueButtons();
                })
                .pos(centerX + 114, startY + 54)
                .size(adjustWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("-"), button -> {
                    Config.setSuperchargedPushFactor(Math.max(100, Config.superchargedPushFactor - 5));
                    refreshValueButtons();
                })
                .pos(centerX + 10, startY + 79)
                .size(adjustWidth, 20)
                .build());

        this.superPushValueButton = Button.builder(Component.literal(""), button -> {})
                .pos(centerX + 38, startY + 79)
                .size(this.valueWidth, VALUE_HEIGHT)
                .build();
        this.superPushValueButton.active = false;
        this.superPushValueButton.visible = false;
        this.addRenderableWidget(this.superPushValueButton);
        this.superPushValueX = centerX + 38;
        this.superPushValueY = startY + 79;

        this.addRenderableWidget(Button.builder(Component.literal("+"), button -> {
                    Config.setSuperchargedPushFactor(Math.min(300, Config.superchargedPushFactor + 5));
                    refreshValueButtons();
                })
                .pos(centerX + 114, startY + 79)
                .size(adjustWidth, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("RESET"), button -> {
                    Config.setCrosshairEnabled(true);
                    Config.setSimpleModelFastGraphics(false);
                    Config.setPushFactor(10);
                    Config.setSuperchargedPushFactor(130);
                    this.rebuildWidgets();
                })
                .pos(centerX - 80, startY + 118)
                .size(76, 20)
                .build());

        this.addRenderableWidget(Button.builder(Component.literal("DONE"), button -> {
                    if (this.minecraft != null) {
                        this.minecraft.setScreen(this.lastScreen);
                    }
                })
                .pos(centerX + 4, startY + 118)
                .size(76, 20)
                .build());

        refreshValueButtons();
    }

    private void refreshValueButtons() {
        if (this.pushValueButton != null) {
            this.pushValueButton.setMessage(Component.literal(Integer.toString(Config.pushFactor)));
        }
        if (this.superPushValueButton != null) {
            this.superPushValueButton.setMessage(Component.literal(Config.superchargedPushFactor + "%"));
        }
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        this.renderBackground(guiGraphics);
        guiGraphics.fillGradient(0, 0, this.width, this.height, MENU_COLOR_TOP, MENU_COLOR_BOTTOM);

        drawColoredTitle(guiGraphics);

        int centerX = this.width / 2;
        int startY = this.height / 2 - 58;
        boolean spanish = isSpanish();

        guiGraphics.drawString(this.font, spanish ? "Mira del Mod:" : "Mod Crosshair:", centerX - 120, startY + 6, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, spanish ? "Modelo Simple:" : "Simple Model:", centerX - 120, startY + 28, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, spanish ? "(Graficos Rapidos)" : "(Fast Graphics)", centerX - 120, startY + 39, 0xD0D0D0, true);
        guiGraphics.drawString(this.font, spanish ? "Fuerza de Empuje:" : "Push Force:", centerX - 120, startY + 60, 0xFFFFFF, true);
        guiGraphics.drawString(this.font, spanish ? "Empuje Supercharged:" : "Supercharged Push:", centerX - 120, startY + 85, SUPERCHARGED_BLUE, true);

        guiGraphics.blit(GRAVITY_GUN_2D, centerX - 116, 20, 0, 0, 16, 16, 16, 16);
        guiGraphics.blit(SUPERCHARGED_GUN_2D, centerX + 100, 20, 0, 0, 16, 16, 16, 16);

        drawValueBox(guiGraphics, this.pushValueX, this.pushValueY, Integer.toString(Config.pushFactor));
        drawValueBox(guiGraphics, this.superPushValueX, this.superPushValueY, Config.superchargedPushFactor + "%");

        guiGraphics.drawString(this.font, "v1.0.0", this.width - 42, this.height - 15, 0xE0E0E0, true);

        super.render(guiGraphics, mouseX, mouseY, partialTick);
    }

    private void drawValueBox(GuiGraphics guiGraphics, int x, int y, String text) {
        guiGraphics.fill(x, y, x + this.valueWidth, y + VALUE_HEIGHT, 0xFF000000);
        guiGraphics.fill(x, y, x + this.valueWidth, y + 1, 0xFFFFFFFF);
        guiGraphics.fill(x, y + VALUE_HEIGHT - 1, x + this.valueWidth, y + VALUE_HEIGHT, 0xFFFFFFFF);
        guiGraphics.fill(x, y, x + 1, y + VALUE_HEIGHT, 0xFFFFFFFF);
        guiGraphics.fill(x + this.valueWidth - 1, y, x + this.valueWidth, y + VALUE_HEIGHT, 0xFFFFFFFF);
        int textX = x + (this.valueWidth - this.font.width(text)) / 2;
        guiGraphics.drawString(this.font, text, textX, y + 6, 0xFFFFFF, true);
    }

    private void drawColoredTitle(GuiGraphics guiGraphics) {
        String left = "Gravity Gun";
        String right = " Config";
        int baseX = this.width / 2;

        guiGraphics.pose().pushPose();
        guiGraphics.pose().scale(2.0F, 2.0F, 2.0F);

        int scaledCenterX = baseX / 2;
        int totalWidth = this.font.width(left) + this.font.width(right);
        int startX = scaledCenterX - totalWidth / 2;

        guiGraphics.drawString(this.font, left, startX, 10, 0xFF8A00, true);
        guiGraphics.drawString(this.font, right, startX + this.font.width(left), 10, SUPERCHARGED_BLUE, true);

        guiGraphics.pose().popPose();
    }

    private boolean isSpanish() {
        Minecraft minecraft = this.minecraft != null ? this.minecraft : Minecraft.getInstance();
        if (minecraft == null || minecraft.getLanguageManager() == null || minecraft.getLanguageManager().getSelected() == null) {
            return false;
        }
        String code = minecraft.getLanguageManager().getSelected();
        return code != null && code.toLowerCase().startsWith("es");
    }
}
