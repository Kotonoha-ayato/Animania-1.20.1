package com.animania.common.advancement;

import com.animania.Animania;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.CriterionTrigger;
import net.minecraft.server.PlayerAdvancements;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.EntityPredicate;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.Map;
import java.util.Set;

/** Modern, server-authoritative replacement for Animania's 1.12 feed criterion. */
public final class FeedAnimalTrigger implements CriterionTrigger<FeedAnimalTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(Animania.MOD_ID, "feed_animal");
    public static final FeedAnimalTrigger INSTANCE = CriteriaTriggers.register(new FeedAnimalTrigger());

    private FeedAnimalTrigger() {}

    private final Map<PlayerAdvancements, Set<Listener<Instance>>> listeners = new IdentityHashMap<>();

    public static void bootstrap() {
        // Loading this class registers INSTANCE before advancements are decoded.
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public boolean trigger(ServerPlayer player, ItemStack fedItem, ResourceLocation entityId) {
        Set<Listener<Instance>> playerListeners = listeners.get(player.getAdvancements());
        if (playerListeners == null || playerListeners.isEmpty()) return false;
        // Listener.run mutates advancement progress and can unregister the
        // completed criterion, so iterate a stable snapshot.
        boolean matched = false;
        for (Listener<Instance> listener : new ArrayList<>(playerListeners)) {
            Instance instance = listener.getTriggerInstance();
            if (instance.matches(fedItem, entityId) && instance.matchesPlayer(player)) {
                listener.run(player.getAdvancements());
                matched = true;
            }
        }
        return matched;
    }

    @Override
    public Instance createInstance(JsonObject json, DeserializationContext context) {
        ContextAwarePredicate player = EntityPredicate.fromJson(json, "player", context);
        ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(json, "entity"));
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            throw new JsonSyntaxException("Unknown entity '" + entityId + "'");
        }
        ResourceLocation itemId = null;
        boolean optional = false;
        if (json.has("itemstack")) {
            JsonObject itemJson = GsonHelper.getAsJsonObject(json, "itemstack");
            itemId = new ResourceLocation(GsonHelper.getAsString(itemJson, "item"));
            if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                throw new JsonSyntaxException("Unknown item '" + itemId + "'");
            }
        } else if (json.has("optional")) {
            JsonObject itemJson = GsonHelper.getAsJsonObject(json, "optional");
            itemId = new ResourceLocation(GsonHelper.getAsString(itemJson, "item"));
            // Optional addon items must not make datapack loading fail when
            // that addon is absent. An unresolved ID simply never matches.
            optional = true;
        }
        return new Instance(player, entityId, itemId, optional);
    }

    @Override
    public void addPlayerListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        listeners.computeIfAbsent(advancements,
                ignored -> new HashSet<>()).add(listener);
    }

    @Override
    public void removePlayerListener(PlayerAdvancements advancements, Listener<Instance> listener) {
        Set<Listener<Instance>> playerListeners = listeners.get(advancements);
        if (playerListeners == null) return;
        playerListeners.remove(listener);
        if (playerListeners.isEmpty()) listeners.remove(advancements);
    }

    @Override
    public void removePlayerListeners(PlayerAdvancements advancements) {
        listeners.remove(advancements);
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final ResourceLocation entityId;
        private final ResourceLocation itemId;
        private final boolean optional;

        private Instance(ContextAwarePredicate player, ResourceLocation entityId, ResourceLocation itemId, boolean optional) {
            super(ID, player);
            this.entityId = entityId;
            this.itemId = itemId;
            this.optional = optional;
        }

        /** Public factory used by addon criteria and deterministic Forge tests. */
        public static Instance optional(ResourceLocation entityId, ResourceLocation itemId) {
            return new Instance(ContextAwarePredicate.ANY, entityId, itemId, true);
        }

        /** Exposed for deterministic compatibility/GameTest verification. */
        public boolean matches(ItemStack fedItem, ResourceLocation actualEntityId) {
            if (!entityId.equals(actualEntityId)) return false;
            if (itemId == null) return !optional;
            Item expected = ForgeRegistries.ITEMS.getValue(itemId);
            return expected != null && fedItem.is(expected);
        }

        public boolean isOptional() { return optional; }

        /** Verifies the vanilla player predicate used by the trigger. */
        public boolean matchesPlayer(ServerPlayer player) {
            return getPlayerPredicate().matches(EntityPredicate.createContext(player, player));
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("entity", entityId.toString());
            if (itemId != null) {
                JsonObject item = new JsonObject();
                item.addProperty("item", itemId.toString());
                json.add(optional ? "optional" : "itemstack", item);
            }
            return json;
        }
    }
}
