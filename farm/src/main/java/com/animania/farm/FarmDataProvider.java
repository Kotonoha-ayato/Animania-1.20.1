package com.animania.farm;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import net.minecraft.data.CachedOutput;
import net.minecraft.data.DataProvider;
import net.minecraft.data.PackOutput;
import net.minecraft.resources.ResourceLocation;

import java.util.concurrent.CompletableFuture;

/** Deterministic registry manifest proving Farm participates in Forge datagen. */
public final class FarmDataProvider implements DataProvider {
    private final PackOutput output;

    public FarmDataProvider(PackOutput output) { this.output = output; }

    @Override
    public CompletableFuture<?> run(CachedOutput cache) {
        JsonObject manifest = new JsonObject();
        manifest.addProperty("module", AnimaniaFarm.MOD_ID);
        manifest.addProperty("schema", 1);
        JsonArray entities = new JsonArray();
        FarmLegacyIds.ALL.forEach(entities::add);
        manifest.add("entity_ids", entities);
        JsonArray items = new JsonArray();
        FarmContent.ITEM_IDS.forEach(items::add);
        manifest.add("item_ids", items);
        return DataProvider.saveStable(cache, manifest,
                output.createPathProvider(PackOutput.Target.DATA_PACK, "animania_manifest")
                        .json(new ResourceLocation(AnimaniaFarm.MOD_ID, "registry")));
    }

    @Override
    public String getName() { return "Animania Farm registry manifest"; }
}
