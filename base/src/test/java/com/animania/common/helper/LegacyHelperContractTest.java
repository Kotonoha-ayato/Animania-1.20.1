package com.animania.common.helper;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Registry/NBT parser and reflection cache compatibility coverage. */
class LegacyHelperContractTest {
    @Test
    void parserKeepsRegistryCountAndNbtSyntax() {
        StringParser.Parsed parsed = StringParser.parse("minecraft:wheat#3{foo:1b}");
        assertEquals("minecraft:wheat", parsed.id().toString());
        assertEquals(3, parsed.count());
        assertTrue(parsed.tag().contains("foo"));
    }

    @Test
    void utilitySourcesAreModernAndCached() throws Exception {
        String reflection = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/animania/common/helper/ReflectionUtil.java"));
        String registry = java.nio.file.Files.readString(java.nio.file.Path.of(
                "src/main/java/com/animania/common/helper/RegistryHelper.java"));
        assertTrue(reflection.contains("ConcurrentHashMap"));
        assertTrue(registry.contains("DeferredRegister"));
    }
}
