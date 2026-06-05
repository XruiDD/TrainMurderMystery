package dev.doctor4t.wathe.client.model.cosmetic;

import dev.doctor4t.wathe.Wathe;
import net.fabricmc.fabric.api.client.model.loading.v1.ModelLoadingPlugin;
import net.minecraft.client.MinecraftClient;
import net.minecraft.resource.ResourceManager;
import net.minecraft.util.Identifier;

import java.util.HashMap;
import java.util.Map;

/**
 * Loads every cosmetic item model shipped in the runtime pack (models/item/cosmetic/&lt;id&gt;.json in
 * the wathe namespace) and stores its baked form in {@link CosmeticModelStore}, keyed by cosmeticId.
 *
 * <p>Model SELECTION is not done here: a mixin into ItemRenderer.getModel returns the stored model for
 * any stack carrying the cosmetic SKIN component, regardless of which item it is. This makes ANY item
 * (vanilla or modded) skinnable purely by data - no hardcoded list of skinnable items, no per-item
 * model wrapping. Conceptually a backport of 1.21.4's minecraft:item_model component to 1.21.1.
 */
public class CosmeticModelLoadingPlugin implements ModelLoadingPlugin {
    private static final String COSMETIC_DIR = "models/item/cosmetic";
    private static final String SUFFIX = ".json";

    @Override
    public void onInitializeModelLoader(Context ctx) {
        CosmeticModelStore.clear();

        ResourceManager rm = MinecraftClient.getInstance().getResourceManager();
        Map<Identifier, String> cosmeticByModelId = new HashMap<>();
        rm.findResources(COSMETIC_DIR, path -> path.getPath().endsWith(SUFFIX)).forEach((resId, res) -> {
            if (!Wathe.MOD_ID.equals(resId.getNamespace())) return;
            String p = resId.getPath();                                       // models/item/cosmetic/<id>.json
            String modelPath = p.substring("models/".length(), p.length() - SUFFIX.length()); // item/cosmetic/<id>
            String cosmeticId = modelPath.substring(modelPath.lastIndexOf('/') + 1);
            cosmeticByModelId.put(Identifier.of(Wathe.MOD_ID, modelPath), cosmeticId);
        });

        // Make the vanilla model loader bake every cosmetic model (Indigo-wrapped, renders correctly)...
        ctx.addModels(cosmeticByModelId.keySet());

        // ...and capture each baked result into the store under its cosmeticId.
        ctx.modifyModelAfterBake().register((model, context) -> {
            String cosmeticId = cosmeticByModelId.get(context.resourceId());
            if (cosmeticId != null && model != null) {
                CosmeticModelStore.put(cosmeticId, model);
            }
            return model;
        });
    }
}
