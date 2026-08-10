package com.animania.common.advancement;

import com.animania.Animania;
import com.google.gson.JsonObject;
import com.google.gson.JsonSyntaxException;
import net.minecraft.advancements.CriteriaTriggers;
import net.minecraft.advancements.critereon.AbstractCriterionTriggerInstance;
import net.minecraft.advancements.critereon.ContextAwarePredicate;
import net.minecraft.advancements.critereon.DeserializationContext;
import net.minecraft.advancements.critereon.SerializationContext;
import net.minecraft.advancements.critereon.SimpleCriterionTrigger;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.GsonHelper;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.registries.ForgeRegistries;

/** Modern, server-authoritative replacement for Animania's 1.12 feed criterion. */
public final class FeedAnimalTrigger extends SimpleCriterionTrigger<FeedAnimalTrigger.Instance> {
    public static final ResourceLocation ID = new ResourceLocation(Animania.MOD_ID, "feed_animal");
    public static final FeedAnimalTrigger INSTANCE = CriteriaTriggers.register(new FeedAnimalTrigger());

    private FeedAnimalTrigger() {}

    public static void bootstrap() {
        // Loading this class registers INSTANCE before advancements are decoded.
    }

    @Override
    public ResourceLocation getId() {
        return ID;
    }

    public void trigger(ServerPlayer player, ItemStack fedItem, ResourceLocation entityId) {
        trigger(player, instance -> instance.matches(fedItem, entityId));
    }

    @Override
    protected Instance createInstance(JsonObject json, ContextAwarePredicate player,
                                      DeserializationContext context) {
        ResourceLocation entityId = new ResourceLocation(GsonHelper.getAsString(json, "entity"));
        if (!ForgeRegistries.ENTITY_TYPES.containsKey(entityId)) {
            throw new JsonSyntaxException("Unknown entity '" + entityId + "'");
        }
        ResourceLocation itemId = null;
        if (json.has("itemstack")) {
            JsonObject itemJson = GsonHelper.getAsJsonObject(json, "itemstack");
            itemId = new ResourceLocation(GsonHelper.getAsString(itemJson, "item"));
            if (!ForgeRegistries.ITEMS.containsKey(itemId)) {
                throw new JsonSyntaxException("Unknown item '" + itemId + "'");
            }
        }
        return new Instance(player, entityId, itemId);
    }

    public static final class Instance extends AbstractCriterionTriggerInstance {
        private final ResourceLocation entityId;
        private final ResourceLocation itemId;

        private Instance(ContextAwarePredicate player, ResourceLocation entityId, ResourceLocation itemId) {
            super(ID, player);
            this.entityId = entityId;
            this.itemId = itemId;
        }

        boolean matches(ItemStack fedItem, ResourceLocation actualEntityId) {
            if (!entityId.equals(actualEntityId)) return false;
            if (itemId == null) return true;
            Item expected = ForgeRegistries.ITEMS.getValue(itemId);
            return expected != null && fedItem.is(expected);
        }

        @Override
        public JsonObject serializeToJson(SerializationContext context) {
            JsonObject json = super.serializeToJson(context);
            json.addProperty("entity", entityId.toString());
            if (itemId != null) {
                JsonObject item = new JsonObject();
                item.addProperty("item", itemId.toString());
                json.add("itemstack", item);
            }
            return json;
        }
    }
}
