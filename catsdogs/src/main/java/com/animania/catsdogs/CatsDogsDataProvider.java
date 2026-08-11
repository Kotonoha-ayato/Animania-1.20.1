package com.animania.catsdogs;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

/** Deterministic registry manifest proving Cats&Dogs participates in Forge datagen. */
public final class CatsDogsDataProvider implements DataProvider {
    private final PackOutput output;

    public CatsDogsDataProvider(PackOutput output) { this.output = output; }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("module", AnimaniaCatsDogs.MOD_ID);
        manifest.addProperty("schema", 1);
        JsonArray entities = new JsonArray();
        CatsDogsLegacyIds.ALL.forEach(entities::add);
        manifest.add("entity_ids", entities);
        JsonArray items = new JsonArray();
        CatsDogsContent.ITEM_IDS.forEach(items::add);
        manifest.add("item_ids", items);
        return DataProvider.saveStable(cache, manifest,
                output.createPathProvider(PackOutput.Target.DATA_PACK, "animania_manifest")
                        .json(new ResourceLocation(AnimaniaCatsDogs.MOD_ID, "registry")));
    }

    @Override
    public String getName() { return "Animania Cats&Dogs registry manifest"; }
}
