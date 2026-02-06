package dev.doctor4t.wathe.record;

import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.api.event.GameEvents;
import dev.doctor4t.wathe.api.event.KillPlayer;
import dev.doctor4t.wathe.api.event.ShopPurchase;
import dev.doctor4t.wathe.api.event.TaskComplete;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;

public final class GameRecordHooks {
    private GameRecordHooks() {
    }

    public static void register() {
        GameEvents.ON_FINISH_INITIALIZE.register((world, gameComponent) -> {
            if (!(world instanceof ServerWorld serverWorld)) {
                return;
            }
            GameRecordManager.startMatch(serverWorld, gameComponent);
            GameRecordManager.recordMatchStart(serverWorld, gameComponent);
            GameRecordManager.recordRoleSnapshot(serverWorld, gameComponent);
        });

        ShopPurchase.AFTER.register((player, entry, index, pricePaid) -> {
            if (player == null || entry == null) {
                return;
            }
            GameRecordManager.recordShopPurchase(player, entry, index, pricePaid);
        });

        TaskComplete.EVENT.register((player, taskType) -> {
            String taskName = taskType == null ? "unknown" : taskType.name().toLowerCase();
            GameRecordManager.recordTaskComplete(player, taskName);
        });

        DoorInteraction.EVENT.register(context -> {
            if (!context.isServerSide()) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            if (!(context.getPlayer() instanceof ServerPlayerEntity serverPlayer)) {
                return DoorInteraction.DoorInteractionResult.PASS;
            }
            // 记录门交互事件，success 由实际门逻辑决定，这里先记录尝试
            GameRecordManager.recordDoorInteraction(
                serverPlayer,
                context.getLowerPos(),
                context.getInteractionType().name().toLowerCase(),
                context.getDoorType().name().toLowerCase(),
                true // 事件触发时假定成功，实际失败会被 DENY 结果阻止
            );
            return DoorInteraction.DoorInteractionResult.PASS;
        });
    }
}
