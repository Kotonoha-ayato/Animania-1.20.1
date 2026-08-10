package com.animania.migrator;

import org.junit.jupiter.api.Test;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.*;

class ConfigMigratorTest {
    @Test
    void migratesKnownKeysAndReportsUnknownKeysWithoutOverwriting() throws Exception {
        Path input = Files.createTempDirectory("animania-old");
        Path output = Files.createTempDirectory("animania-new");
        Files.writeString(input.resolve("animania.cfg"), "hungerUpdateInterval=1200\nhivePlayermadeHoneyRate=500\nhiveSpawning=false\nspawnProbabilityCows=11\nallowEggThrowing=true\nnumberRabbitFamilies=4\ncatFood=[\"minecraft:cod\"]\nlegacyThing=true\n");
        ConfigMigrator.main(new String[]{"--input", input.toString(), "--output", output.toString()});
        Path config = output.resolve("animania-common.toml");
        Path farm = output.resolve("animania_farm-common.toml");
        Path catsDogs = output.resolve("animania_catsdogs-common.toml");
        Path report = output.resolve("animania-config-migration-report.json");
        assertTrue(Files.readString(config).contains("hungerInterval=1200"));
        assertFalse(Files.readString(config).contains("hivePlayerHoneyRate"));
        assertTrue(Files.readString(farm).contains("hivePlayerHoneyRate=500"));
        assertTrue(Files.readString(farm).contains("hiveSpawning=false"));
        assertTrue(Files.readString(farm).contains("spawnProbabilityCows=11"));
        assertTrue(Files.readString(farm).contains("allowEggThrowing=true"));
        assertTrue(Files.readString(output.resolve("animania_extra-common.toml")).contains("numberRabbitFamilies=4"));
        assertTrue(Files.readString(catsDogs).contains("catFood=[\"minecraft:cod\"]"));
        assertTrue(Files.readString(report).contains("unmigratable"));
        assertThrows(IllegalStateException.class, () -> ConfigMigrator.main(new String[]{"--input", input.toString(), "--output", output.toString()}));
    }
}
