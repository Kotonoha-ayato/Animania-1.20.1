package com.animania.farm;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class FarmHiveItemRendererTest {
    @Test
    void bothHiveItemsSelectTheirNativeRenderer() throws Exception {
        for (String id : new String[]{"hive", "wild_hive"}) {
            var stream = getClass().getResourceAsStream(
                    "/assets/animania_farm/models/item/" + id + ".json");
            assertNotNull(stream, id);
            try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
                assertEquals("minecraft:builtin/entity",
                        JsonParser.parseReader(reader).getAsJsonObject().get("parent").getAsString(), id);
            }
        }
    }
}
