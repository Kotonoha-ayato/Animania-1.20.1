package com.animania.api;

import com.animania.api.data.SpeciesDefinition;
import com.animania.api.data.AnimalSnapshot;
import com.animania.api.interfaces.IAnimaniaAnimal;
import net.minecraftforge.fml.ModList;
import net.minecraft.resources.ResourceLocation;

import java.util.Collection;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiPredicate;
import java.util.function.BooleanSupplier;

/** Versioned facade for addon discovery and species metadata. */
public final class AnimaniaApi {
    public static final String API_VERSION = "3.0.0";
    private static final Map<ResourceLocation, SpeciesDefinition> SPECIES = new LinkedHashMap<>();
    private static final Map<String, BooleanSupplier> TAMING_RULES = new LinkedHashMap<>();
    private static final Map<String, BiPredicate<ResourceLocation, net.minecraft.world.item.ItemStack>> FOOD_RULES = new LinkedHashMap<>();

    private AnimaniaApi() {
    }

    public static synchronized void registerSpecies(SpeciesDefinition definition) {
        SpeciesDefinition previous = SPECIES.putIfAbsent(definition.id(), definition);
        if (previous != null && !previous.equals(definition)) {
            throw new IllegalStateException("Duplicate Animania species id: " + definition.id());
        }
    }

    public static synchronized Optional<SpeciesDefinition> species(ResourceLocation id) {
        return Optional.ofNullable(SPECIES.get(id));
    }

    public static synchronized Collection<SpeciesDefinition> species() {
        return Collections.unmodifiableCollection(SPECIES.values());
    }

    /** Resolve the registered species for an entity without exposing internals. */
    public static synchronized Optional<SpeciesDefinition> speciesOf(IAnimaniaAnimal animal) {
        return speciesOf((com.animania.api.IAnimaniaAnimal) animal);
    }

    /**
     * Resolve the registered species for any implementation of the stable
     * root API.  The overload retaining the historical interfaces-package
     * signature above keeps already compiled addons binary compatible.
     */
    public static synchronized Optional<SpeciesDefinition> speciesOf(com.animania.api.IAnimaniaAnimal animal) {
        if (animal == null) return Optional.empty();
        ResourceLocation id = animal.typeId();
        if (id != null) return species(id);
        AnimalSnapshot snapshot = animal.snapshot();
        return snapshot == null ? Optional.empty() : species(snapshot.type());
    }

    /** Stable ID view used by addons when building tabs, probes, or JEI lists. */
    public static synchronized Set<ResourceLocation> speciesIds() {
        return Collections.unmodifiableSet(new java.util.LinkedHashSet<>(SPECIES.keySet()));
    }

    /** Return the species registered by one addon namespace. */
    public static synchronized Collection<SpeciesDefinition> speciesForAddon(String namespace) {
        if (namespace == null || namespace.isBlank()) return List.of();
        return Collections.unmodifiableList(SPECIES.values().stream()
                .filter(definition -> namespace.equals(definition.id().getNamespace()))
                .toList());
    }

    /** Whether a concrete species id is known, without throwing on absent addons. */
    public static synchronized boolean hasSpecies(ResourceLocation id) {
        return id != null && SPECIES.containsKey(id);
    }

    /**
     * Register an addon-owned breeding rule without making Base depend on the
     * addon implementation.  The supplier is evaluated on the server when a
     * pair is checked, so a live Forge config reload is respected.
     */
    public static synchronized void registerTamingRequirement(String namespace, BooleanSupplier requirement) {
        if (namespace == null || namespace.isBlank() || requirement == null) return;
        TAMING_RULES.put(namespace, requirement);
    }

    /** Whether animals in the namespace must be tamed before breeding. */
    public static synchronized boolean requiresTaming(ResourceLocation id) {
        if (id == null) return false;
        BooleanSupplier rule = TAMING_RULES.get(id.getNamespace());
        return rule != null && rule.getAsBoolean();
    }

    /**
     * Register addon-owned food matching without coupling Base to an addon
     * config class. The predicate is evaluated server-side at interaction
     * time, so Forge config reloads take effect immediately.
     */
    public static synchronized void registerFoodMatcher(String namespace,
            BiPredicate<ResourceLocation, net.minecraft.world.item.ItemStack> matcher) {
        if (namespace == null || namespace.isBlank() || matcher == null) return;
        FOOD_RULES.put(namespace, matcher);
    }

    /** Resolve an addon food rule for an entity; returns false when absent. */
    public static synchronized boolean matchesRegisteredFood(ResourceLocation entityId,
            net.minecraft.world.item.ItemStack stack) {
        if (entityId == null || stack == null || stack.isEmpty()) return false;
        BiPredicate<ResourceLocation, net.minecraft.world.item.ItemStack> matcher = FOOD_RULES.get(entityId.getNamespace());
        return matcher != null && matcher.test(entityId, stack);
    }

    public static boolean isAnimaniaAnimal(Object value) {
        return value instanceof com.animania.api.IAnimaniaAnimal;
    }

    public static boolean isAddonLoaded(String modId) {
        return ModList.get().isLoaded(modId);
    }
}
