package com.animania.extra;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

/** Deterministic registry manifest proving Extra participates in Forge datagen. */
public final class ExtraDataProvider implements DataProvider {
    private final PackOutput output;

    public ExtraDataProvider(PackOutput output) { this.output = output; }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("module", AnimaniaExtra.MOD_ID);
        manifest.addProperty("schema", 1);
        JsonArray entities = new JsonArray();
        ExtraLegacyIds.ALL.forEach(entities::add);
        manifest.add("entity_ids", entities);
        JsonArray items = new JsonArray();
        ExtraContent.ITEM_IDS.forEach(items::add);
        manifest.add("item_ids", items);
        return DataProvider.saveStable(cache, manifest,
                output.createPathProvider(PackOutput.Target.DATA_PACK, "animania_manifest")
                        .json(new ResourceLocation(AnimaniaExtra.MOD_ID, "registry")));
    }

    @Override
    public String getName() { return "Animania Extra registry manifest"; }
}
