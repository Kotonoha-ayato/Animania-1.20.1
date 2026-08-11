package com.animania.extra;

import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

final class ExtraHamsterWheelItemRendererTest {
    @Test
    void itemModelSelectsTheNativeCustomRenderer() throws Exception {
        var stream = getClass().getResourceAsStream(
                "/assets/animania_extra/models/item/hamster_wheel.json");
        assertNotNull(stream);
        try (var reader = new InputStreamReader(stream, StandardCharsets.UTF_8)) {
            assertEquals("minecraft:builtin/entity",
                    JsonParser.parseReader(reader).getAsJsonObject().get("parent").getAsString());
        }
    }
}
