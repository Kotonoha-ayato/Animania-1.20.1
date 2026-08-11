package com.animania.extra.client.screen;

import com.animania.extra.ExtraHamsterWheelMenu;
import com.mojang.blaze3d.systems.RenderSystem;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;

/** Compact native screen for the wheel's single food slot. */
public final class ExtraHamsterWheelScreen extends AbstractContainerScreen<ExtraHamsterWheelMenu> {
    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath("minecraft", "textures/gui/container/dispenser.png");

    public ExtraHamsterWheelScreen(ExtraHamsterWheelMenu menu, Inventory inventory, Component title) {
        super(menu, inventory, title);
        imageHeight = 133;
        inventoryLabelY = 39;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        super.render(graphics, mouseX, mouseY, partialTick);
        renderTooltip(graphics, mouseX, mouseY);
    }

    @Override
    protected void renderBg(GuiGraphics graphics, float partialTick, int mouseX, int mouseY) {
        RenderSystem.setShaderColor(1.0F, 1.0F, 1.0F, 1.0F);
        graphics.blit(TEXTURE, leftPos, topPos, 0, 0, imageWidth, imageHeight);
        // The dispenser texture contains a 3x3 grid; cover it and expose only
        // the real one-slot inventory so eight phantom slots cannot appear.
        graphics.fill(leftPos + 60, topPos + 16, leftPos + 116, topPos + 48, 0xFFC6C6C6);
        graphics.fill(leftPos + 79, topPos + 19, leftPos + 97, topPos + 37, 0xFF373737);
        graphics.fill(leftPos + 80, topPos + 20, leftPos + 96, topPos + 36, 0xFF8B8B8B);
    }
}
