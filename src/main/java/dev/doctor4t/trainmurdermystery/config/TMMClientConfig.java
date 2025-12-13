package dev.doctor4t.trainmurdermystery.config;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.isxander.yacl3.config.v2.api.ConfigClassHandler;
import dev.isxander.yacl3.config.v2.api.SerialEntry;
import dev.isxander.yacl3.config.v2.api.serializer.GsonConfigSerializerBuilder;
import net.fabricmc.loader.api.FabricLoader;

public class TMMClientConfig {
    public static ConfigClassHandler<TMMClientConfig> HANDLER = ConfigClassHandler.createBuilder(TMMClientConfig.class)
            .id(TMM.id("client_config"))
            .serializer(config -> GsonConfigSerializerBuilder.create(config)
                    .setPath(FabricLoader.getInstance().getConfigDir().resolve("trainmurdermystery-client.json5"))
                    .setJson5(true)
                    .build())
            .build();

    @SerialEntry(comment = "启用超级性能模式（降低渲染距离）")
    public boolean ultraPerfMode = false;
}
