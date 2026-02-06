package dev.doctor4t.wathe.util;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheGameModes;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerVeteranComponent;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.network.PacketByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.network.codec.PacketCodecs;
import net.minecraft.network.packet.CustomPayload;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.NotNull;

public record KnifeStabPayload(int target) implements CustomPayload {
    public static final Id<KnifeStabPayload> ID = new Id<>(Wathe.id("knifestab"));
    public static final PacketCodec<PacketByteBuf, KnifeStabPayload> CODEC = PacketCodec.tuple(PacketCodecs.INTEGER, KnifeStabPayload::target, KnifeStabPayload::new);

    @Override
    public Id<? extends CustomPayload> getId() {
        return ID;
    }

    public static class Receiver implements ServerPlayNetworking.PlayPayloadHandler<KnifeStabPayload> {
        @Override
        public void receive(@NotNull KnifeStabPayload payload, ServerPlayNetworking.@NotNull Context context) {
            ServerPlayerEntity player = context.player();
            if (!(player.getServerWorld().getEntityById(payload.target()) instanceof ServerPlayerEntity target)) return;
            if (target.distanceTo(player) > 3.0) return;

            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(context.player().getWorld());
            Role playerRole = gameComponent.getRole(player);

            // Check if player is a veteran with limited stab uses
            if (playerRole == WatheRoles.VETERAN) {
                PlayerVeteranComponent veteranComponent = PlayerVeteranComponent.KEY.get(player);
                if (!veteranComponent.hasStabUsesLeft()) {
                    return; // No stabs left, cannot use knife
                }
                veteranComponent.useStab();

                // Remove knife if no uses left
                if (!veteranComponent.hasStabUsesLeft()) {
                    // Remove knife from player's inventory
                    for (int i = 0; i < player.getInventory().size(); i++) {
                        ItemStack stack = player.getInventory().getStack(i);
                        if (stack.isOf(WatheItems.KNIFE)) {
                            player.getInventory().removeStack(i);
                            break;
                        }
                    }
                }
            }

            ServerPlayerEntity recordTarget = target instanceof ServerPlayerEntity serverTarget ? serverTarget : null;
            GameRecordManager.recordItemUse(player, Registries.ITEM.getId(WatheItems.KNIFE), recordTarget, null);

            GameFunctions.killPlayer(target, true, player, GameConstants.DeathReasons.KNIFE);
            target.playSound(WatheSounds.ITEM_KNIFE_STAB, 1.0f, 1.0f);
            player.swingHand(Hand.MAIN_HAND);
            if (!player.isCreative() && gameComponent.getGameMode() != WatheGameModes.LOOSE_ENDS) {
                // Calculate current excess players
                int totalPlayers = context.player().getServerWorld().getPlayers().size();
                int killerCount = gameComponent.getAllKillerTeamPlayers().size();
                int killerRatio = gameComponent.getKillerDividend();
                int excessPlayers = Math.max(0, totalPlayers - (killerCount * killerRatio));
                // Calculate dynamic cooldown (reduce by 5 seconds per excess player, minimum 10 seconds)
                int baseCooldown = GameConstants.ITEM_COOLDOWNS.get(WatheItems.KNIFE);
                int cooldownReductionPerExcess = GameConstants.getInTicks(0, 5); // 5 seconds per excess player
                int adjustedCooldown = Math.max(GameConstants.getInTicks(0, 10), baseCooldown - (excessPlayers * cooldownReductionPerExcess));
                player.getItemCooldownManager().set(WatheItems.KNIFE, adjustedCooldown);
            }
        }
    }
}
