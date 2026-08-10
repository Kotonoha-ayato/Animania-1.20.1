package com.animania.client.manual;

import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.packs.resources.Resource;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Resource-driven native handbook screen; no Patchouli dependency. */
public final class ManualScreen extends Screen {
    private int page;
    private final List<ManualPage> pages;

    private ManualScreen() {
        super(Component.translatable("item.animania.manual"));
        this.pages = loadPages();
    }

    public static void open() {
        Minecraft.getInstance().setScreen(new ManualScreen());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        renderBackground(graphics);
        int left = width / 2 - 140;
        int top = height / 2 - 90;
        graphics.fill(left, top, left + 280, top + 180, 0xF0101010);
        graphics.drawString(font, Component.translatable("item.animania.manual"), left + 14, top + 12, 0xFFE0A030);
        ManualPage current = pages.get(page);
        graphics.drawString(font, Component.literal("- ").append(current.title()), left + 14, top + 34, 0xFFFFFFFF);
        graphics.drawWordWrap(font, current.body(), left + 14, top + 56, 250, 0xFFFFFFFF);
        graphics.drawString(font, Component.literal("< / >  " + (page + 1) + "/" + pages.size()), left + 14, top + 158, 0xFFAAAAAA);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (keyCode == 263) page = Math.max(0, page - 1);
        if (keyCode == 262) page = Math.min(pages.size() - 1, page + 1);
        if (keyCode == 256) onClose();
        return true;
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private static List<ManualPage> loadPages() {
        List<ManualPage> result = new ArrayList<>();
        var manager = Minecraft.getInstance().getResourceManager();
        // Base pages live at `<namespace>:manual/...`; the legacy addon packs
        // retain their `animania/manual/<addon>/...` path.  Read both modern
        // layouts so installing an addon actually extends the in-game book.
        Map<ResourceLocation, Resource> resources = new LinkedHashMap<>();
        resources.putAll(manager.listResources("manual", id -> id.getPath().endsWith(".json")));
        resources.putAll(manager.listResources("animania/manual", id -> id.getPath().endsWith(".json")));
        resources.entrySet().stream().sorted(Map.Entry.comparingByKey(Comparator.comparing(ResourceLocation::toString))).forEach(entry -> {
            try (var reader = new InputStreamReader(entry.getValue().open(), StandardCharsets.UTF_8)) {
                JsonElement parsed = JsonParser.parseReader(reader);
                if (!parsed.isJsonObject()) return;
                JsonObject object = parsed.getAsJsonObject();
                String name = object.has("name") ? object.get("name").getAsString() : entry.getKey().getPath();
                List<String> lines = new ArrayList<>();
                if (object.has("contents")) collectContents(object.get("contents"), lines);
                if (lines.isEmpty()) collectContents(parsed, lines);
                MutableComponent body = Component.empty();
                for (int i = 0; i < lines.size(); i++) {
                    if (i > 0) body.append(Component.literal("\n"));
                    body.append(asComponent(lines.get(i)));
                }
                result.add(new ManualPage(asComponent(name), body));
            } catch (Exception ignored) {
                // A malformed optional page must not prevent the handbook or
                // the client from opening; the remaining pages still render.
            }
        });
        if (result.isEmpty()) {
            result.add(new ManualPage(Component.literal("Animania"), Component.translatable("manual.animania.page.0")));
        }
        return result;
    }

    private static void collectContents(JsonElement element, List<String> lines) {
        if (element == null || element.isJsonNull()) return;
        if (element.isJsonPrimitive() && element.getAsJsonPrimitive().isString()) {
            lines.add(element.getAsString());
        } else if (element.isJsonArray()) {
            JsonArray array = element.getAsJsonArray();
            array.forEach(child -> collectContents(child, lines));
        } else if (element.isJsonObject()) {
            JsonObject object = element.getAsJsonObject();
            if (object.has("contents")) collectContents(object.get("contents"), lines);
        }
    }

    private static Component asComponent(String raw) {
        if (raw == null || raw.isBlank()) return Component.empty();
        String value = raw;
        if (value.startsWith("@")) {
            int hash = value.indexOf('#');
            value = hash >= 0 ? value.substring(hash + 1).replaceFirst("^-\\s*", "") : value.replaceAll("^@[^@]+@", "");
        }
        if (value.matches("[a-zA-Z0-9_.-]+")) return Component.translatable(value);
        return Component.literal(value);
    }

    private record ManualPage(Component title, Component body) { }
}
