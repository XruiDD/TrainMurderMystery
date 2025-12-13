package dev.doctor4t.trainmurdermystery.config;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

import java.util.ArrayList;
import java.util.List;

public class TMMServerConfig {
    // 惩罚类型枚举
    public enum ShootInnocentPunishment {
        VANILLA,
        PREVENT_GUN_PICKUP,
        KILL_SHOOTER
    }

    public static ConfigClassHandler<TMMServerConfig> HANDLER = ConfigClassHandler.createBuilder(TMMServerConfig.class)
            .id(TMM.id("server_config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("trainmurdermystery-server.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry(comment = "禁用的角色ID列表")
    public List<String> disabledRoles = new ArrayList<>();

    @SerialEntry(comment = "默认武器反击机率 (0.0-1.0)")
    public float backfireChance = 0f;

    @SerialEntry(comment = "默认杀手比例 (1个杀手对应X个玩家)")
    public int killerRatio = 6;

    @SerialEntry(comment = "默认杀手数量 (0=使用比例)")
    public int killerCount = 0;

    // AutoStart 设置
    @SerialEntry(comment = "自动开始游戏模式")
    public String autoStartGameMode = "trainmurdermystery:murder";

    @SerialEntry(comment = "自动开始倒计时秒数 (0=禁用)")
    public int autoStartSeconds = 0;

    // Bound 设置
    @SerialEntry(comment = "默认是否启用观察者边界限制")
    public boolean bound = true;

    @SerialEntry(comment = "射杀无辜玩家的惩罚 (DEFAULT/PREVENT_GUN_PICKUP/KILL_SHOOTER)")
    public ShootInnocentPunishment shootInnocentPunishment = ShootInnocentPunishment.KILL_SHOOTER;
}
