package dev.doctor4t.wathe.client.model.cosmetic;

import net.minecraft.client.render.model.BakedModel;
import org.jetbrains.annotations.Nullable;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Holds the baked cosmetic item models for the current resource reload, keyed by cosmeticId.
 * Cleared and repopulated each reload by {@link CosmeticModelLoadingPlugin} (cleared at model-load
 * start, filled by its modifyModelAfterBake hook). Read at render time by the ItemRenderer mixin,
 * which returns the matching model for any stack carrying the cosmetic SKIN component.
 */
public final class CosmeticModelStore {
    private static final Map<String, BakedModel> MODELS = new ConcurrentHashMap<>();

    private CosmeticModelStore() {}

    public static void clear() {
        MODELS.clear();
    }

    public static void put(String cosmeticId, BakedModel model) {
        MODELS.put(cosmeticId, model);
    }

    public static @Nullable BakedModel get(String cosmeticId) {
        return MODELS.get(cosmeticId);
    }
}
