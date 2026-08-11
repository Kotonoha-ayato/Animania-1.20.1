package com.animania.client;

import com.mojang.logging.LogUtils;
import net.minecraft.client.model.geom.ModelLayerLocation;
import net.minecraft.resources.ResourceLocation;
import org.slf4j.Logger;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Client-only diagnostics for the 1.20.1 renderer migration.
 *
 * <p>The markers deliberately use INFO/WARN/ERROR rather than Forge's debug
 * logger level, so an ordinary development client records enough information
 * to diagnose a missing layer or purple/black texture without a custom
 * log4j configuration. Texture results are de-duplicated per entity/request
 * pair because {@code getTextureLocation} is called every render frame.</p>
 */
public final class AnimaniaClientDiagnostics {
    private static final Logger LOGGER = LogUtils.getLogger();
    private static final Set<String> REPORTED_TEXTURE_RESOLUTIONS = ConcurrentHashMap.newKeySet();

    private AnimaniaClientDiagnostics() {
    }

    public static void layerDefinitions(String module, int legacyCount, int nativeCount) {
        LOGGER.info("[ANIMANIA_CLIENT_LAYERS] module={} legacy_layers={} native_layers={}",
                module, legacyCount, nativeCount);
    }

    public static void rendererRegistrations(String module, int animalCount, int vehicleCount) {
        LOGGER.info("[ANIMANIA_CLIENT_RENDERERS] module={} animals={} vehicles={}",
                module, animalCount, vehicleCount);
    }

    public static ModelLayerLocation requireLayer(String module, String entityId, ModelLayerLocation layer) {
        if (layer != null) {
            return layer;
        }
        LOGGER.error("[ANIMANIA_CLIENT_MISSING_LAYER] module={} entity={}; no registered ModelLayerLocation exists",
                module, entityId);
        throw new IllegalStateException("Missing Animania model layer for " + module + ':' + entityId);
    }

    public static void textureResolution(ResourceLocation entityId, ResourceLocation requested,
                                         ResourceLocation selected, String result) {
        String fingerprint = entityId + "|" + requested + "|" + selected + "|" + result;
        if (!REPORTED_TEXTURE_RESOLUTIONS.add(fingerprint)) {
            return;
        }
        if ("requested".equals(result)) {
            LOGGER.info("[ANIMANIA_CLIENT_TEXTURE] entity={} requested={} selected={}",
                    entityId, requested, selected);
        } else if ("default_missing".equals(result)) {
            LOGGER.error("[ANIMANIA_CLIENT_TEXTURE_MISSING] entity={} requested={} fallback={} result={}; Minecraft will render the missing-texture checkerboard",
                    entityId, requested, selected, result);
        } else {
            LOGGER.warn("[ANIMANIA_CLIENT_TEXTURE_FALLBACK] entity={} requested={} selected={} result={}",
                    entityId, requested, selected, result);
        }
    }
}
