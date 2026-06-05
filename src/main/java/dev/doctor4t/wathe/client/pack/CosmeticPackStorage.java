package dev.doctor4t.wathe.client.pack;

import com.google.gson.Gson;
import com.google.gson.JsonObject;
import dev.doctor4t.wathe.Wathe;
import org.jetbrains.annotations.Nullable;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;

public final class CosmeticPackStorage {
    private static final Gson GSON = new Gson();
    private final Path packDir;
    private final Path markerFile;

    public CosmeticPackStorage() {
        Path wathe = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("wathe");
        this.packDir = resolvePackDir();
        this.markerFile = wathe.resolve("cosmetic_pack.current.json");
        try { Files.createDirectories(packDir); } catch (Exception e) {
            Wathe.LOGGER.warn("[CosmeticPack] cannot create pack dir: {}", e.getMessage());
        }
    }

    public static Path resolvePackDir() {
        return net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir().resolve("wathe").resolve("cosmetic_pack");
    }

    public Path packDir() { return packDir; }

    public @Nullable String installedVersion() {
        try {
            if (!Files.exists(markerFile)) return null;
            JsonObject o = GSON.fromJson(Files.readString(markerFile), JsonObject.class);
            return o != null && o.has("version") ? o.get("version").getAsString() : null;
        } catch (Exception e) { return null; }
    }

    public void install(Path newRoot, String version) throws Exception {
        Path tmp = packDir.resolveSibling("cosmetic_pack.tmp");
        Path old = packDir.resolveSibling("cosmetic_pack.old");
        deleteRecursively(tmp);
        deleteRecursively(old);
        copyRecursively(newRoot, tmp);
        if (Files.exists(packDir)) Files.move(packDir, old, StandardCopyOption.REPLACE_EXISTING);
        Files.move(tmp, packDir, StandardCopyOption.REPLACE_EXISTING);
        JsonObject o = new JsonObject();
        o.addProperty("version", version);
        Files.writeString(markerFile, GSON.toJson(o));
        deleteRecursively(old);
    }

    private static void copyRecursively(Path src, Path dst) throws Exception {
        try (var walk = Files.walk(src)) {
            walk.forEach(p -> {
                try {
                    Path target = dst.resolve(src.relativize(p).toString());
                    if (Files.isDirectory(p)) Files.createDirectories(target);
                    else { Files.createDirectories(target.getParent()); Files.copy(p, target, StandardCopyOption.REPLACE_EXISTING); }
                } catch (Exception e) { throw new RuntimeException(e); }
            });
        }
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(java.util.Comparator.reverseOrder()).forEach(p -> {
                try { Files.delete(p); } catch (Exception ignored) {}
            });
        }
    }
}
