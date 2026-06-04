package dev.doctor4t.wathe.client.skin;

import dev.doctor4t.wathe.Wathe;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.util.Identifier;
import net.minecraft.util.Util;
import org.jetbrains.annotations.Nullable;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.time.Duration;
import java.util.HexFormat;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 通用远程贴图加载器：HTTP 拉 → 磁盘 SHA256 缓存 → NativeImage 解码 → GPU 注册。
 * 子类通过 {@link #onTextureDecoded} hook 在解码完成后做后处理（例如 ItemSkin 的 quad 生成）。
 * <p>
 * 缓存随 mod 版本变更自动失效（写一份 .skin_cache_version 标记文件）。
 */
public abstract class RemoteTextureLoader {
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    public enum TextureState { LOADING, READY, FAILED }

    private final ConcurrentHashMap<String, TextureState> textureStates = new ConcurrentHashMap<>();
    private final ConcurrentHashMap<String, Identifier> registeredTextures = new ConcurrentHashMap<>();
    private final String cacheSubdir;
    private final String texturePathPrefix;
    private Path cacheDir;

    /**
     * @param cacheSubdir       磁盘缓存目录（位于 {@code <runDir>/wathe/<cacheSubdir>}）
     * @param texturePathPrefix Identifier 路径前缀，例如 "skins/" 或 "player_skins/"
     */
    protected RemoteTextureLoader(String cacheSubdir, String texturePathPrefix) {
        this.cacheSubdir = cacheSubdir;
        this.texturePathPrefix = texturePathPrefix;
    }

    public void initialize() {
        Path watheDir = MinecraftClient.getInstance().runDirectory.toPath().resolve("wathe");
        cacheDir = watheDir.resolve(cacheSubdir);
        try {
            Files.createDirectories(cacheDir);
            invalidateCacheOnVersionChange(watheDir);
        } catch (Exception e) {
            Wathe.LOGGER.warn("[{}] Failed to create cache directory: {}", cacheSubdir, e.getMessage());
        }
    }

    private void invalidateCacheOnVersionChange(Path watheDir) {
        // Use one shared version marker across all loaders.
        Path versionFile = watheDir.resolve(".skin_cache_version");
        String currentVersion = Wathe.MOD_VERSION;
        try {
            if (Files.exists(versionFile)) {
                String cachedVersion = Files.readString(versionFile).trim();
                if (currentVersion.equals(cachedVersion)) return;
                Wathe.LOGGER.info("[{}] Mod version changed ({} -> {}), clearing cache",
                        cacheSubdir, cachedVersion, currentVersion);
            } else {
                Wathe.LOGGER.info("[{}] First run, initializing cache version marker", cacheSubdir);
            }
            try (var files = Files.list(cacheDir)) {
                files.filter(p -> p.toString().endsWith(".png")).forEach(p -> {
                    try { Files.delete(p); }
                    catch (Exception e) { Wathe.LOGGER.warn("[{}] Failed to delete {}: {}", cacheSubdir, p.getFileName(), e.getMessage()); }
                });
            }
            Files.writeString(versionFile, currentVersion);
        } catch (Exception e) {
            Wathe.LOGGER.warn("[{}] Failed cache invalidation: {}", cacheSubdir, e.getMessage());
        }
    }

    public TextureState getState(String textureUrl) {
        return textureStates.getOrDefault(textureUrl, TextureState.LOADING);
    }

    public @Nullable Identifier getTextureId(String textureUrl) {
        if (textureStates.get(textureUrl) != TextureState.READY) return null;
        return registeredTextures.get(textureUrl);
    }

    public void ensureLoaded(String textureUrl) {
        if (textureUrl == null || textureUrl.isEmpty()) return;
        if (cacheDir == null) return; // not yet initialized
        if (textureStates.putIfAbsent(textureUrl, TextureState.LOADING) != null) return;

        java.util.concurrent.CompletableFuture.supplyAsync(() -> {
            try {
                String hash = sha256(textureUrl);
                Path cachedFile = cacheDir.resolve(hash + ".png");
                if (Files.exists(cachedFile)) return Files.readAllBytes(cachedFile);

                HttpRequest request = HttpRequest.newBuilder()
                        .uri(URI.create(textureUrl))
                        .timeout(Duration.ofSeconds(10))
                        .GET()
                        .build();
                HttpResponse<byte[]> response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofByteArray());
                if (response.statusCode() != 200) throw new RuntimeException("HTTP " + response.statusCode());

                byte[] bytes = response.body();
                Files.write(cachedFile, bytes);
                return bytes;
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        }, Util.getMainWorkerExecutor()).thenAcceptAsync(bytes -> {
            try {
                InputStream stream = new ByteArrayInputStream(bytes);
                NativeImage image = NativeImage.read(stream);

                onTextureDecoded(textureUrl, image);

                NativeImageBackedTexture texture = new NativeImageBackedTexture(image);
                Identifier texId = Wathe.id(texturePathPrefix + sha256(textureUrl));
                MinecraftClient.getInstance().getTextureManager().registerTexture(texId, texture);
                registeredTextures.put(textureUrl, texId);
                textureStates.put(textureUrl, TextureState.READY);
            } catch (Exception e) {
                Wathe.LOGGER.warn("[{}] Failed to register texture for {}: {}",
                        cacheSubdir, textureUrl, e.getMessage());
                textureStates.put(textureUrl, TextureState.FAILED);
            }
        }, MinecraftClient.getInstance()).exceptionally(e -> {
            Wathe.LOGGER.warn("[{}] Failed to download {}: {}", cacheSubdir, textureUrl, e.getMessage());
            textureStates.put(textureUrl, TextureState.FAILED);
            return null;
        });
    }

    /**
     * Hook called on the main thread after NativeImage is decoded but before the GPU texture is
     * registered. Override to do post-decode work (e.g. quad generation). Default is no-op.
     */
    protected void onTextureDecoded(String textureUrl, NativeImage image) {}

    public void clearAll() {
        for (Identifier texId : registeredTextures.values()) {
            MinecraftClient.getInstance().getTextureManager().destroyTexture(texId);
        }
        registeredTextures.clear();
        textureStates.clear();
    }

    private static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes());
            return HexFormat.of().formatHex(hash);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
