package dev.doctor4t.wathe.block;

import dev.doctor4t.wathe.api.event.DoorInteraction;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import dev.doctor4t.wathe.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.entity.BlockEntityType;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

import java.util.function.Supplier;

public class TrainDoorBlock extends SmallDoorBlock {
    public TrainDoorBlock(Supplier<BlockEntityType<SmallDoorBlockEntity>> typeSupplier, Settings settings) {
        super(typeSupplier, settings);
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        BlockPos lowerPos = state.get(HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
        if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity) {
            // 确定交互类型
            DoorInteraction.DoorInteractionType interactionType = determineTrainDoorInteractionType(player, entity);

            // 构建上下文并触发事件
            DoorInteraction.DoorInteractionContext context = DoorInteraction.DoorInteractionContext.builder()
                    .world(world)
                    .pos(pos)
                    .lowerPos(lowerPos)
                    .state(state)
                    .entity(entity)
                    .player(player)
                    .handItem(player.getMainHandStack())
                    .interactionType(interactionType)
                    .doorType(DoorInteraction.DoorType.TRAIN_DOOR)
                    .build();

            DoorInteraction.DoorInteractionResult eventResult = DoorInteraction.EVENT.invoker().onInteract(context);

            // 根据事件结果处理
            if (eventResult == DoorInteraction.DoorInteractionResult.ALLOW) {
                if (interactionType == DoorInteraction.DoorInteractionType.USE_LOCKPICK) {
                    world.playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f, WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1f, 1f);
                }
                return open(state, world, entity, lowerPos);
            } else if (eventResult == DoorInteraction.DoorInteractionResult.DENY) {
                return ActionResult.FAIL;
            } else if (eventResult == DoorInteraction.DoorInteractionResult.HANDLED) {
                // 事件已处理，返回成功但不执行门逻辑
                return ActionResult.SUCCESS;
            }

            // PASS: 执行原有逻辑
            // 被炸开的门直接通过
            if (entity.isBlasted()) {
                return ActionResult.PASS;
            }

            if (player.isCreative() || GameWorldComponent.KEY.get(world).getGameStatus() == GameWorldComponent.GameStatus.INACTIVE) {
                return open(state, world, entity, lowerPos);
            } else {
                boolean hasLockpick = player.getMainHandStack().isOf(WatheItems.LOCKPICK);

                if (entity.isOpen()) {
                    return open(state, world, entity, lowerPos);
                } else {
                    if (hasLockpick) {
                        world.playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f, WatheSounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1f, 1f);
                        return open(state, world, entity, lowerPos);
                    } else {
                        if (!world.isClient) {
                            world.playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f, WatheSounds.BLOCK_DOOR_LOCKED, SoundCategory.BLOCKS, 1f, 1f);
                            player.sendMessage(Text.translatable("tip.door.locked"), true);
                        }
                        return ActionResult.FAIL;
                    }
                }
            }
        }

        return ActionResult.PASS;
    }

    /**
     * 确定TrainDoor的交互类型
     */
    private DoorInteraction.DoorInteractionType determineTrainDoorInteractionType(PlayerEntity player, DoorBlockEntity entity) {
        if (entity.isBlasted()) {
            return DoorInteraction.DoorInteractionType.BLASTED;
        }

        if (entity.isOpen()) {
            return DoorInteraction.DoorInteractionType.CLOSE;
        }

        if (player.getMainHandStack().isOf(WatheItems.LOCKPICK)) {
            return DoorInteraction.DoorInteractionType.USE_LOCKPICK;
        }

        return DoorInteraction.DoorInteractionType.INTERACT;
    }
}
