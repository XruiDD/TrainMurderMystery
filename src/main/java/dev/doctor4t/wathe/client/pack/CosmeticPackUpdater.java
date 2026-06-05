package dev.doctor4t.wathe.client.pack;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.cosmetic.CosmeticApiClient;
import net.minecraft.client.MinecraftClient;
import net.minecraft.util.Util;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.Comparator;
import java.util.HexFormat;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public final class CosmeticPackUpdater {
    private static final HttpClient HTTP = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final CosmeticPackStorage storage;

    public CosmeticPackUpdater(CosmeticPackStorage storage) { this.storage = storage; }

    /**
     * Synchronous variant for STARTUP: fetch the manifest and, if changed, download + verify +
     * install into the pack dir, blocking the calling thread until done. Does NOT call
     * reloadResources() — it is meant to run in onInitializeClient, BEFORE Minecraft's initial
     * resource load, so the freshly-installed pack is picked up by that first load (no second
     * reload / no title-screen flicker). Bounded by the HTTP timeouts; on any failure it logs
     * and returns so startup proceeds with the cached (or empty) pack.
     */
    public void checkAndUpdateBlocking() {
        try {
            Wathe.LOGGER.info("[CosmeticPack] startup check (installed version: {})", storage.installedVersion());
            CosmeticApiClient.PackManifest manifest = CosmeticApiClient.fetchPackManifest().join();
            if (manifest == null) {
                Wathe.LOGGER.info("[CosmeticPack] no manifest from API - keeping current cache");
                return;
            }
            if (manifest.version().equals(storage.installedVersion())) {
                Wathe.LOGGER.info("[CosmeticPack] already up to date (version {})", manifest.version());
                return;
            }
            Wathe.LOGGER.info("[CosmeticPack] new version {} (installed {}); downloading {} (startup, blocking)",
                    manifest.version(), storage.installedVersion(), manifest.url());
            Path tmpRoot = downloadAndExtract(manifest);
            storage.install(tmpRoot, manifest.version());
            deleteRecursively(tmpRoot);
            Wathe.LOGGER.info("[CosmeticPack] installed version {} - will load in Minecraft's initial resource load", manifest.version());
        } catch (Exception e) {
            Wathe.LOGGER.warn("[CosmeticPack] startup update failed: {}", e.toString());
        }
    }

    public void checkAndUpdate() {
        Wathe.LOGGER.info("[CosmeticPack] checking for update (installed version: {})", storage.installedVersion());
        CosmeticApiClient.fetchPackManifest().thenAcceptAsync(manifest -> {
            if (manifest == null) {
                Wathe.LOGGER.info("[CosmeticPack] no manifest from API (none published / fetch failed) — keeping current cache");
                return;
            }
            if (manifest.version().equals(storage.installedVersion())) {
                Wathe.LOGGER.info("[CosmeticPack] already up to date (version {})", manifest.version());
                return;
            }
            Wathe.LOGGER.info("[CosmeticPack] new version {} (installed {}); downloading {}",
                    manifest.version(), storage.installedVersion(), manifest.url());
            try {
                Path tmpRoot = downloadAndExtract(manifest);
                storage.install(tmpRoot, manifest.version());
                deleteRecursively(tmpRoot);
                Wathe.LOGGER.info("[CosmeticPack] installed version {}; triggering silent resource reload", manifest.version());
                MinecraftClient.getInstance().execute(() -> MinecraftClient.getInstance().reloadResources());
            } catch (Exception e) {
                Wathe.LOGGER.warn("[CosmeticPack] update failed: {}", e.toString());
            }
        }, Util.getMainWorkerExecutor());
    }

    private Path downloadAndExtract(CosmeticApiClient.PackManifest m) throws Exception {
        HttpRequest req = HttpRequest.newBuilder().uri(URI.create(m.url()))
                .timeout(Duration.ofSeconds(30)).GET().build();
        HttpResponse<byte[]> res = HTTP.send(req, HttpResponse.BodyHandlers.ofByteArray());
        if (res.statusCode() != 200) throw new RuntimeException("zip HTTP " + res.statusCode() + " from " + m.url());
        byte[] zip = res.body();
        Wathe.LOGGER.info("[CosmeticPack] downloaded {} bytes", zip.length);

        String sha = HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(zip));
        if (!sha.equalsIgnoreCase(m.sha256())) {
            throw new RuntimeException("sha256 mismatch: got " + sha + ", manifest expected " + m.sha256());
        }
        Wathe.LOGGER.info("[CosmeticPack] sha256 verified");

        Path tmpRoot = net.fabricmc.loader.api.FabricLoader.getInstance().getGameDir()
                .resolve("wathe").resolve("cosmetic_pack.download");
        deleteRecursively(tmpRoot);
        Files.createDirectories(tmpRoot);
        int entries = 0;
        boolean hasMeta = false;
        try (ZipInputStream zis = new ZipInputStream(new java.io.ByteArrayInputStream(zip))) {
            ZipEntry e;
            while ((e = zis.getNextEntry()) != null) {
                Path out = tmpRoot.resolve(e.getName()).normalize();
                if (!out.startsWith(tmpRoot)) continue;
                if (e.isDirectory()) Files.createDirectories(out);
                else {
                    Files.createDirectories(out.getParent());
                    Files.copy(zis, out, StandardCopyOption.REPLACE_EXISTING);
                    entries++;
                    if ("pack.mcmeta".equals(e.getName())) hasMeta = true;
                }
            }
        }
        Wathe.LOGGER.info("[CosmeticPack] extracted {} files to {} (pack.mcmeta at root: {})", entries, tmpRoot, hasMeta);
        if (!hasMeta) {
            throw new RuntimeException("zip has no pack.mcmeta at root — bad pack (rebuild with build-pack.mjs)");
        }
        return tmpRoot;
    }

    private static void deleteRecursively(Path dir) throws Exception {
        if (!Files.exists(dir)) return;
        try (var walk = Files.walk(dir)) {
            walk.sorted(Comparator.reverseOrder()).forEach(p -> { try { Files.delete(p); } catch (Exception ignored) {} });
        }
    }
}
