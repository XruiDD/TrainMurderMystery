package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.index.TMMItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.nbt.NbtElement;
import net.minecraft.nbt.NbtList;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.scoreboard.Scoreboard;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;

import java.util.*;

public class ScoreboardRoleSelectorComponent implements AutoSyncedComponent {
    public static final ComponentKey<ScoreboardRoleSelectorComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("rolecounter"), ScoreboardRoleSelectorComponent.class);
    public final Scoreboard scoreboard;
    public final MinecraftServer server;
    public final Map<Role, Map<UUID, Integer>> roleRounds = new HashMap<>();
    public final Map<Role, List<UUID>> forcedRoles = new HashMap<>();

    public ScoreboardRoleSelectorComponent(Scoreboard scoreboard, @Nullable MinecraftServer server) {
        this.scoreboard = scoreboard;
        this.server = server;
    }

    public Map<UUID, Integer> getRoundsForRole(Role role) {
        return roleRounds.computeIfAbsent(role, k -> new HashMap<>());
    }

    public List<UUID> getForcedForRole(Role role) {
        return forcedRoles.computeIfAbsent(role, k -> new ArrayList<>());
    }

    public int reset() {
        this.roleRounds.clear();
        this.forcedRoles.clear();
        return 1;
    }

    public void checkWeights(@NotNull ServerCommandSource source) {
        Map<Role, Double> roleTotals = new HashMap<>();
        for (Role role : TMMRoles.ROLES) {
            if (TMMRoles.SPECIAL_ROLES.contains(role)) continue;
            double total = 0d;
            for (ServerPlayerEntity player : source.getWorld().getPlayers()) {
                total += Math.exp(-getRoundsForRole(role).getOrDefault(player.getUuid(), 0) * 4);
            }
            roleTotals.put(role, total);
        }

        MutableText text = Text.literal("Role Weights:").formatted(Formatting.GRAY);
        for (ServerPlayerEntity player : source.getWorld().getPlayers()) {
            text = text.append("\n").append(player.getDisplayName());
            for (Role role : TMMRoles.ROLES) {
                if (TMMRoles.SPECIAL_ROLES.contains(role)) continue;
                int rounds = getRoundsForRole(role).getOrDefault(player.getUuid(), 0);
                double weight = Math.exp(-rounds * 4);
                double percent = weight / roleTotals.getOrDefault(role, 1.0) * 100;
                text.append(
                        Text.literal("\n  " + role.identifier().getPath() + " (").withColor(role.color())
                                .append(Text.literal("%d".formatted(rounds)).withColor(0x808080))
                                .append(Text.literal("): ").withColor(role.color()))
                                .append(Text.literal("%.2f%%".formatted(percent)).withColor(0x808080))
                );
            }
        }
        MutableText finalText = text;
        source.sendFeedback(() -> finalText, false);
    }

    public void setRoleRounds(@NotNull ServerCommandSource source, @NotNull ServerPlayerEntity player, @NotNull Role role, int times) {
        if (times < 0) times = 0;
        Map<UUID, Integer> rounds = getRoundsForRole(role);
        if (times == 0) rounds.remove(player.getUuid());
        else rounds.put(player.getUuid(), times);
        int finalTimes = times;
        String roleName = role.identifier().getPath();
        roleName = roleName.substring(0, 1).toUpperCase() + roleName.substring(1);
        String finalRoleName = roleName;
        source.sendFeedback(() -> Text.literal("Set ").formatted(Formatting.GRAY)
                .append(player.getDisplayName().copy().formatted(Formatting.YELLOW))
                .append(Text.literal("'s " + finalRoleName + " rounds to ").formatted(Formatting.GRAY))
                .append(Text.literal("%d".formatted(finalTimes)).withColor(0x808080))
                .append(Text.literal(".").formatted(Formatting.GRAY)), false);
    }

    @Deprecated
    public void setKillerRounds(@NotNull ServerCommandSource source, @NotNull ServerPlayerEntity player, int times) {
        setRoleRounds(source, player, TMMRoles.KILLER, times);
    }

    @Deprecated
    public void setVigilanteRounds(@NotNull ServerCommandSource source, @NotNull ServerPlayerEntity player, int times) {
        setRoleRounds(source, player, TMMRoles.VIGILANTE, times);
    }

    public int assignKillers(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int killerCount) {
        this.reduceRoleRounds(TMMRoles.KILLER);
        Map<UUID, Integer> killerRoundsMap = getRoundsForRole(TMMRoles.KILLER);
        List<UUID> forcedKillersList = getForcedForRole(TMMRoles.KILLER);
        ArrayList<UUID> killers = new ArrayList<>();
        for (UUID uuid : forcedKillersList) {
            killers.add(uuid);
            killerCount--;
            killerRoundsMap.put(uuid, killerRoundsMap.getOrDefault(uuid, 1) + 1);
        }
        forcedKillersList.clear();
        HashMap<ServerPlayerEntity, Float> map = new HashMap<>();
        float total = 0f;
        for (ServerPlayerEntity player : players) {
            float weight = (float) Math.exp(-killerRoundsMap.getOrDefault(player.getUuid(), 0) * 4);
            if (!GameWorldComponent.KEY.get(world).areWeightsEnabled()) weight = 1;
            map.put(player, weight);
            total += weight;
        }
        for (int i = 0; i < killerCount; i++) {
            float random = world.getRandom().nextFloat() * total;
            Iterator<Map.Entry<ServerPlayerEntity, Float>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<ServerPlayerEntity, Float> entry = iterator.next();
                random -= entry.getValue();
                if (random <= 0) {
                    killers.add(entry.getKey().getUuid());
                    total -= entry.getValue();
                    iterator.remove();
                    killerRoundsMap.put(entry.getKey().getUuid(), killerRoundsMap.getOrDefault(entry.getKey().getUuid(), 1) + 1);
                    break;
                }
            }
        }

        // Calculate excess players and adjust starting money
        int totalPlayers = players.size();
        int killerRatio = gameComponent.getKillerPlayerRatio();
        int excessPlayers = Math.max(0, totalPlayers - (killers.size() * killerRatio));
        int additionalMoneyPerExcess = 20; // 20 coins per excess player
        int dynamicStartingMoney = GameConstants.MONEY_START + (excessPlayers * additionalMoneyPerExcess);

        for (UUID killerUUID : killers) {
            gameComponent.addRole(killerUUID, TMMRoles.KILLER);
            PlayerEntity killer = world.getPlayerByUuid(killerUUID);
            if (killer != null) {
                PlayerShopComponent.KEY.get(killer).setBalance(dynamicStartingMoney);
            }
        }
        return killers.size();
    }

    private void reduceRoleRounds(Role role) {
        Map<UUID, Integer> rounds = getRoundsForRole(role);
        if (rounds.isEmpty()) return;
        int minimum = Integer.MAX_VALUE;
        for (Integer times : rounds.values()) minimum = Math.min(minimum, times);
        for (UUID uuid : rounds.keySet())
            rounds.put(uuid, rounds.get(uuid) - minimum);
    }

    @Deprecated
    private void reduceKillers() {
        reduceRoleRounds(TMMRoles.KILLER);
    }

    public void assignVigilantes(ServerWorld world, GameWorldComponent gameComponent, @NotNull List<ServerPlayerEntity> players, int vigilanteCount) {
        this.reduceRoleRounds(TMMRoles.VIGILANTE);
        Map<UUID, Integer> vigilanteRoundsMap = getRoundsForRole(TMMRoles.VIGILANTE);
        List<UUID> forcedVigilantesList = getForcedForRole(TMMRoles.VIGILANTE);
        ArrayList<ServerPlayerEntity> vigilantes = new ArrayList<>();
        for (UUID uuid : forcedVigilantesList) {
            PlayerEntity player = world.getPlayerByUuid(uuid);
            if (player instanceof ServerPlayerEntity serverPlayer && players.contains(serverPlayer) && !gameComponent.canUseKillerFeatures(player)) {
                gameComponent.addRole(player, TMMRoles.VIGILANTE);
                player.giveItemStack(new ItemStack(TMMItems.REVOLVER));
                vigilanteCount--;
                vigilanteRoundsMap.put(player.getUuid(), vigilanteRoundsMap.getOrDefault(player.getUuid(), 1) + 1);
            }
        }
        forcedVigilantesList.clear();
        HashMap<ServerPlayerEntity, Float> map = new HashMap<>();
        float total = 0f;
        for (ServerPlayerEntity player : players) {
            if (gameComponent.isRole(player, TMMRoles.KILLER)) continue;
            float weight = (float) Math.exp(-vigilanteRoundsMap.getOrDefault(player.getUuid(), 0) * 4);
            if (!GameWorldComponent.KEY.get(world).areWeightsEnabled()) weight = 1;
            map.put(player, weight);
            total += weight;
        }
        for (int i = 0; i < vigilanteCount; i++) {
            float random = world.getRandom().nextFloat() * total;
            Iterator<Map.Entry<ServerPlayerEntity, Float>> iterator = map.entrySet().iterator();

            while (iterator.hasNext()) {
                Map.Entry<ServerPlayerEntity, Float> entry = iterator.next();
                random -= entry.getValue();
                if (random <= 0) {
                    vigilantes.add(entry.getKey());
                    total -= entry.getValue();
                    iterator.remove();
                    vigilanteRoundsMap.put(entry.getKey().getUuid(), vigilanteRoundsMap.getOrDefault(entry.getKey().getUuid(), 1) + 1);
                    break;
                }
            }
        }
        for (ServerPlayerEntity player : vigilantes) {
            gameComponent.addRole(player, TMMRoles.VIGILANTE);
            player.giveItemStack(new ItemStack(TMMItems.REVOLVER));
        }
    }

    @Deprecated
    private void reduceVigilantes() {
        reduceRoleRounds(TMMRoles.VIGILANTE);
    }

    @Override
    public void writeToNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        NbtCompound rolesCompound = new NbtCompound();
        for (Map.Entry<Role, Map<UUID, Integer>> roleEntry : this.roleRounds.entrySet()) {
            NbtList roundsList = new NbtList();
            for (Map.Entry<UUID, Integer> detail : roleEntry.getValue().entrySet()) {
                NbtCompound compound = new NbtCompound();
                compound.putUuid("uuid", detail.getKey());
                compound.putInt("times", detail.getValue());
                roundsList.add(compound);
            }
            rolesCompound.put(roleEntry.getKey().identifier().toString(), roundsList);
        }
        tag.put("roleRounds", rolesCompound);

        // Backwards compatibility
        if (roleRounds.containsKey(TMMRoles.KILLER)) {
            NbtList killerRounds = new NbtList();
            for (Map.Entry<UUID, Integer> detail : getRoundsForRole(TMMRoles.KILLER).entrySet()) {
                NbtCompound compound = new NbtCompound();
                compound.putUuid("uuid", detail.getKey());
                compound.putInt("times", detail.getValue());
                killerRounds.add(compound);
            }
            tag.put("killerRounds", killerRounds);
        }
        if (roleRounds.containsKey(TMMRoles.VIGILANTE)) {
            NbtList vigilanteRounds = new NbtList();
            for (Map.Entry<UUID, Integer> detail : getRoundsForRole(TMMRoles.VIGILANTE).entrySet()) {
                NbtCompound compound = new NbtCompound();
                compound.putUuid("uuid", detail.getKey());
                compound.putInt("times", detail.getValue());
                vigilanteRounds.add(compound);
            }
            tag.put("vigilanteRounds", vigilanteRounds);
        }
    }

    @Override
    public void readFromNbt(@NotNull NbtCompound tag, RegistryWrapper.WrapperLookup registryLookup) {
        this.roleRounds.clear();

        // Read new format
        if (tag.contains("roleRounds")) {
            NbtCompound rolesCompound = tag.getCompound("roleRounds");
            for (String key : rolesCompound.getKeys()) {
                Role role = TMMRoles.getRole(Identifier.of(key));
                if (role == null) continue;
                Map<UUID, Integer> rounds = getRoundsForRole(role);
                for (NbtElement element : rolesCompound.getList(key, 10)) {
                    NbtCompound compound = (NbtCompound) element;
                    if (!compound.contains("uuid") || !compound.contains("times")) continue;
                    rounds.put(compound.getUuid("uuid"), compound.getInt("times"));
                }
            }
        }

        // Backwards compatibility: read old format if new format doesn't have killer/vigilante
        if (!roleRounds.containsKey(TMMRoles.KILLER) && tag.contains("killerRounds")) {
            Map<UUID, Integer> killerRoundsMap = getRoundsForRole(TMMRoles.KILLER);
            for (NbtElement element : tag.getList("killerRounds", 10)) {
                NbtCompound compound = (NbtCompound) element;
                if (!compound.contains("uuid") || !compound.contains("times")) continue;
                killerRoundsMap.put(compound.getUuid("uuid"), compound.getInt("times"));
            }
        }
        if (!roleRounds.containsKey(TMMRoles.VIGILANTE) && tag.contains("vigilanteRounds")) {
            Map<UUID, Integer> vigilanteRoundsMap = getRoundsForRole(TMMRoles.VIGILANTE);
            for (NbtElement element : tag.getList("vigilanteRounds", 10)) {
                NbtCompound compound = (NbtCompound) element;
                if (!compound.contains("uuid") || !compound.contains("times")) continue;
                vigilanteRoundsMap.put(compound.getUuid("uuid"), compound.getInt("times"));
            }
        }
    }
}