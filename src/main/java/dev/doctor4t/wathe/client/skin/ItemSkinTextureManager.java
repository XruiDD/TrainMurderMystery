package dev.doctor4t.wathe.client.skin;

import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.Sprite;
import org.jetbrains.annotations.Nullable;

import java.util.concurrent.ConcurrentHashMap;

public class ItemSkinTextureManager extends RemoteTextureLoader {
    private static final ItemSkinTextureManager INSTANCE = new ItemSkinTextureManager();

    private final ConcurrentHashMap<String, ItemSkinQuadGenerator.SkinQuadData> quadCache =
            new ConcurrentHashMap<>();

    private ItemSkinTextureManager() {
        super("skin_cache", "skins/");
    }

    public static ItemSkinTextureManager getInstance() {
        return INSTANCE;
    }

    @Override
    protected void onTextureDecoded(String textureUrl, NativeImage image) {
        Sprite dummySprite = MinecraftClient.getInstance()
                .getBakedModelManager().getMissingModel().getParticleSprite();
        ItemSkinQuadGenerator.SkinQuadData quads = ItemSkinQuadGenerator.generate(image, dummySprite);
        quadCache.put(textureUrl, quads);
    }

    public @Nullable ItemSkinQuadGenerator.SkinQuadData getQuads(String textureUrl) {
        return quadCache.get(textureUrl);
    }

    @Override
    public void clearAll() {
        super.clearAll();
        quadCache.clear();
    }
}
