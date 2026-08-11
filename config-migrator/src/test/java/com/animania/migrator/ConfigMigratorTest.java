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

    @Test
    void rewritesLegacyRegistryIdsAndKeepsExactSourceDerivedDefaults() throws Exception {
        Path input = Files.createTempDirectory("animania-old-ids");
        Path output = Files.createTempDirectory("animania-new-ids");
        Files.writeString(input.resolve("addons.cfg"), """
                chickenBed=animania:block_straw
                dogBed=animania:dog_pillow
                ferretFood=["animania:brown_egg","animania:peacock_egg_blue","animania:prime_mutton","animania:prime_rabbit","animania_prime_chicken"]
                """);
        ConfigMigrator.main(new String[]{"--input", input.toString(), "--output", output.toString()});
        String farm = Files.readString(output.resolve("animania_farm-common.toml"));
        String extra = Files.readString(output.resolve("animania_extra-common.toml"));
        String catsDogs = Files.readString(output.resolve("animania_catsdogs-common.toml"));
        assertTrue(farm.contains("chickenBed=\"animania:straw\""));
        assertTrue(extra.contains("animania_farm:brown_egg"));
        assertTrue(extra.contains("animania_extra:peacock_egg_blue"));
        assertTrue(extra.contains("animania_farm:raw_prime_mutton"));
        assertTrue(extra.contains("animania_extra:raw_prime_rabbit"));
        assertTrue(extra.contains("animania_farm:raw_prime_chicken"));
        assertTrue(catsDogs.contains("dogBed=\"animania_catsdogs:dog_pillow\""));
        assertTrue(farm.contains("simplecorn:corncob"));
        assertTrue(extra.contains("harvestcraft:cornitem"));
        assertTrue(catsDogs.contains("catFood=[\"minecraft:fish\"]"));
        assertTrue(catsDogs.contains("dogFood=[\"listAllbeefraw\"]"));
    }
}
