package com.animania.client.manual;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertTrue;

/** Resource and API regression coverage for the native handbook. */
class ManualScreenTest {
    @Test
    void nativeManualLoadsBaseAndAddonResourceLayoutsWithoutPatchouli() throws Exception {
        String screen = Files.readString(Path.of("src/main/java/com/animania/client/manual/ManualScreen.java"));
        String item = Files.readString(Path.of("src/main/java/com/animania/common/item/ManualItem.java"));
        assertTrue(screen.contains("listResources(\"manual\""));
        assertTrue(screen.contains("listResources(\"animania/manual\""));
        assertTrue(screen.contains("JsonParser"));
        assertTrue(item.contains("ManualScreen.open"));
        assertTrue(!screen.contains("patchouli"));
    }

    @Test
    void allBaseManualPagesAreValidJson() throws Exception {
        Path manual = Path.of("src/main/resources/assets/animania/manual");
        long pages = Files.walk(manual).filter(path -> path.toString().endsWith(".json")).peek(path -> {
            try {
                com.google.gson.JsonParser.parseString(Files.readString(path));
            } catch (Exception error) {
                throw new AssertionError("invalid manual page " + path, error);
            }
        }).count();
        assertTrue(pages >= 10, "base manual page set unexpectedly small");
    }
}
