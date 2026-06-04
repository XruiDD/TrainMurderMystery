package dev.doctor4t.wathe.cosmetic;

import org.jetbrains.annotations.Nullable;

import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Server-side cache of the most recent {@link CosmeticApiClient.FetchedCosmetics} per player.
 * <p>
 * Used to restore the {@link dev.doctor4t.wathe.cca.PlayerCosmeticsComponent} after respawn,
 * because the component uses {@code RespawnCopyStrategy.NEVER_COPY} and the new
 * {@code ServerPlayerEntity} starts empty.
 * <p>
 * Populated on JOIN and ON_PSYCHO_START refetch. Cleared on DISCONNECT.
 */
public class CachedFetchedCosmetics {
    private static final ConcurrentHashMap<UUID, CosmeticApiClient.FetchedCosmetics> cache =
            new ConcurrentHashMap<>();

    public static void put(UUID player, CosmeticApiClient.FetchedCosmetics fetched) {
        cache.put(player, fetched);
    }

    public static @Nullable CosmeticApiClient.FetchedCosmetics get(UUID player) {
        return cache.get(player);
    }

    public static void remove(UUID player) {
        cache.remove(player);
    }

    public static void clear() {
        cache.clear();
    }
}
