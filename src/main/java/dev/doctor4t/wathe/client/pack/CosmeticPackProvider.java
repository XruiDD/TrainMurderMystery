package dev.doctor4t.wathe.client.pack;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.resource.*;
import net.minecraft.text.Text;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.function.Consumer;

public final class CosmeticPackProvider implements ResourcePackProvider {
    public static final String PACK_ID = "wathe_cosmetics_runtime";
    private final Path packDir;

    public CosmeticPackProvider(Path packDir) { this.packDir = packDir; }

    @Override
    public void register(Consumer<ResourcePackProfile> adder) {
        // The cache dir is empty until the pack has been downloaded (on join). An empty
        // dir has no pack.mcmeta, so ResourcePackProfile.create() returns null. Treat that
        // as the expected "not installed yet" state (INFO), not an error.
        Path meta = packDir.resolve("pack.mcmeta");
        if (!Files.exists(meta)) {
            Wathe.LOGGER.info("[CosmeticPack] runtime pack cache empty (no pack.mcmeta at {}); " +
                    "not registering yet - it will appear after the pack downloads on server join", packDir);
            return;
        }
        Wathe.LOGGER.info("[CosmeticPack] found pack.mcmeta at {}, creating profile", packDir);

        ResourcePackInfo info = new ResourcePackInfo(
                PACK_ID, Text.literal("SparkExpress Cosmetics"),
                ResourcePackSource.BUILTIN, Optional.empty());
        ResourcePackProfile profile = ResourcePackProfile.create(
                info,
                new ResourcePackProfile.PackFactory() {
                    @Override public ResourcePack open(ResourcePackInfo i) { return new DirectoryResourcePack(i, packDir); }
                    @Override public ResourcePack openWithOverlays(ResourcePackInfo i, ResourcePackProfile.Metadata m) { return new DirectoryResourcePack(i, packDir); }
                },
                ResourceType.CLIENT_RESOURCES,
                new ResourcePackPosition(true, ResourcePackProfile.InsertionPosition.TOP, false));
        if (profile != null) {
            adder.accept(profile);
            Wathe.LOGGER.info("[CosmeticPack] registered runtime cosmetic pack from {}", packDir);
        } else {
            Wathe.LOGGER.warn("[CosmeticPack] pack.mcmeta present at {} but profile creation failed " +
                    "(invalid pack.mcmeta / unsupported pack_format?)", packDir);
        }
    }
}
