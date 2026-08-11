package com.animania.client.config;

import com.animania.common.config.AnimaniaConfig;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.List;

/** Native Forge config screen replacing the 1.12 GuiConfig/GuiFactory pair. */
@OnlyIn(Dist.CLIENT)
public final class AnimaniaConfigScreen extends Screen {
    private final Screen parent;
    private final List<String> lines = List.of(
            "hungerInterval", "thirstInterval", "gestationTicks", "childGrowthTick",
            "feedTimer", "waterTimer", "playTimer", "laidTimer", "fancyEggs", "fancyEggsRotate",
            "animalsSleep", "allowMobRiding", "allowTroughAutomation", "enableNaturalSpawns");

    public AnimaniaConfigScreen(Screen parent) {
        super(Component.translatable("screen.animania.config.title"));
        this.parent = parent;
    }

    @Override
    protected void init() {
        addRenderableWidget(Button.builder(Component.translatable("gui.done"), button -> onClose())
                .bounds(width / 2 - 50, height - 34, 100, 20).build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        graphics.drawCenteredString(font, title, width / 2, 16, 0xFFFFFFFF);
        int y = 38;
        for (String key : lines) {
            graphics.drawString(font, Component.literal(key + " = " + value(key)), width / 2 - 130, y, 0xFFE0E0E0);
            y += 18;
        }
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    private static Object value(String key) {
        return switch (key) {
            case "hungerInterval" -> AnimaniaConfig.HUNGER_INTERVAL.get();
            case "thirstInterval" -> AnimaniaConfig.THIRST_INTERVAL.get();
            case "gestationTicks" -> AnimaniaConfig.GESTATION_TICKS.get();
            case "childGrowthTick" -> AnimaniaConfig.CHILD_GROWTH_TICK.get();
            case "feedTimer" -> AnimaniaConfig.FEED_TIMER.get();
            case "waterTimer" -> AnimaniaConfig.WATER_TIMER.get();
            case "playTimer" -> AnimaniaConfig.PLAY_TIMER.get();
            case "laidTimer" -> AnimaniaConfig.LAID_TIMER.get();
            case "fancyEggs" -> AnimaniaConfig.FANCY_EGGS.get();
            case "fancyEggsRotate" -> AnimaniaConfig.FANCY_EGGS_ROTATE.get();
            case "animalsSleep" -> AnimaniaConfig.ANIMALS_SLEEP.get();
            case "allowMobRiding" -> AnimaniaConfig.ALLOW_MOB_RIDING.get();
            case "allowTroughAutomation" -> AnimaniaConfig.ALLOW_TROUGH_AUTOMATION.get();
            case "enableNaturalSpawns" -> AnimaniaConfig.ENABLE_NATURAL_SPAWNS.get();
            default -> "?";
        };
    }

    @Override
    public void onClose() {
        minecraft.setScreen(parent);
    }

    @Override
    public boolean isPauseScreen() { return false; }
}
