package dev.doctor4t.wathe.client.skin;

/**
 * 玩家实体皮肤（64×64 PNG）远程加载器。无 quad 生成，复用 {@link RemoteTextureLoader} 三段流程。
 */
public class PlayerSkinTextureManager extends RemoteTextureLoader {
    private static final PlayerSkinTextureManager INSTANCE = new PlayerSkinTextureManager();

    private PlayerSkinTextureManager() {
        super("player_skin_cache", "player_skins/");
    }

    public static PlayerSkinTextureManager getInstance() {
        return INSTANCE;
    }
}
