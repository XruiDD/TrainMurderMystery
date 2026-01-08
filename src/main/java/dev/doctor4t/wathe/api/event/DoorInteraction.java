package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.block.SmallDoorBlock;
import dev.doctor4t.wathe.block.TrainDoorBlock;
import dev.doctor4t.wathe.block_entity.DoorBlockEntity;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

/**
 * 门交互事件。
 * 在玩家尝试与门交互时触发，可用于修改或取消交互行为。
 *
 * <p>事件处理流程：</p>
 * <ol>
 *   <li>事件按注册顺序调用各监听器</li>
 *   <li>第一个返回非PASS结果的监听器决定最终结果</li>
 *   <li>如果所有监听器都返回PASS，则执行原有逻辑</li>
 * </ol>
 *
 * <p>支持的交互类型包括：普通开关门、使用钥匙、撬锁、撬棍炸门、被炸开的门等。
 * 可通过 {@link DoorInteractionContext#getInteractionType()} 获取具体类型。</p>
 *
 * <p>使用示例：</p>
 * <pre>{@code
 * DoorInteractionEvent.EVENT.register(context -> {
 *     // MASTER_KEY可以开任何需要钥匙的门
 *     if (context.getHandItem().isOf(MyItems.MASTER_KEY)) {
 *         if (context.requiresKey() && !context.isOpen()) {
 *             return DoorInteractionResult.ALLOW;
 *         }
 *     }
 *     return DoorInteractionResult.PASS;
 * });
 * }</pre>
 */
public interface DoorInteraction {

    /**
     * 门交互事件实例。
     *
     * <p>返回规则：</p>
     * <ul>
     *   <li>PASS: 继续检查后续监听器，最终执行原逻辑</li>
     *   <li>ALLOW: 立即允许操作，执行开门/关门</li>
     *   <li>DENY: 立即拒绝操作，返回ActionResult.FAIL</li>
     *   <li>HANDLED: 交互已被处理，返回ActionResult.SUCCESS，不执行门逻辑</li>
     * </ul>
     */
    Event<DoorInteraction> EVENT = createArrayBacked(
            DoorInteraction.class,
            listeners -> context -> {
                for (DoorInteraction listener : listeners) {
                    DoorInteractionResult result = listener.onInteract(context);
                    if (result.shouldTerminate()) {
                        return result;
                    }
                }
                return DoorInteractionResult.PASS;
            }
    );

    /**
     * 处理门交互事件。
     *
     * @param context 交互上下文，包含所有相关信息
     * @return 交互结果
     */
    DoorInteractionResult onInteract(DoorInteractionContext context);

    /**
     * 门交互事件的返回结果。
     * 使用三态设计，允许监听器选择不处理某些交互。
     */
    enum DoorInteractionResult {
        /**
         * 不处理此交互，继续执行原逻辑和后续监听器。
         */
        PASS,

        /**
         * 允许此交互，跳过后续检查直接执行操作（开门/关门）。
         * 例如：MASTER_KEY 可以开任何门。
         */
        ALLOW,

        /**
         * 拒绝此交互，阻止操作执行，返回 ActionResult.FAIL。
         * 例如：某些角色禁止使用撬锁工具。
         */
        DENY,

        /**
         * 交互已被处理，返回 ActionResult.SUCCESS，不执行后续门逻辑。
         * 例如：附属模组完全自定义处理了交互（如修复门、设置门状态等）。
         */
        HANDLED;

        /**
         * 检查结果是否应该终止事件链。
         *
         * @return 如果不是PASS则返回true
         */
        public boolean shouldTerminate() {
            return this != PASS;
        }
    }

    /**
     * 门交互上下文，包含交互的所有相关信息。
     * 使用不可变设计，确保事件处理期间数据一致性。
     */
    final class DoorInteractionContext {
        private final World world;
        private final BlockPos pos;
        private final BlockPos lowerPos;
        private final BlockState state;
        private final DoorBlockEntity entity;
        private final PlayerEntity player;
        private final ItemStack handItem;
        private final DoorInteractionType interactionType;
        private final DoorType doorType;

        // 缓存的门状态信息
        private final String keyName;
        private final boolean isOpen;
        private final boolean isJammed;
        private final boolean isBlasted;
        private final boolean requiresKey;

        public DoorInteractionContext(
                @NotNull World world,
                @NotNull BlockPos pos,
                @NotNull BlockPos lowerPos,
                @NotNull BlockState state,
                @NotNull DoorBlockEntity entity,
                @NotNull PlayerEntity player,
                @NotNull ItemStack handItem,
                @NotNull DoorInteractionType interactionType,
                @NotNull DoorType doorType
        ) {
            this.world = world;
            this.pos = pos;
            this.lowerPos = lowerPos;
            this.state = state;
            this.entity = entity;
            this.player = player;
            this.handItem = handItem.copy(); // 防止外部修改
            this.interactionType = interactionType;
            this.doorType = doorType;

            // 缓存门状态
            this.keyName = entity.getKeyName();
            this.isOpen = entity.isOpen();
            this.isJammed = entity.isJammed();
            this.isBlasted = entity.isBlasted();
            this.requiresKey = !this.keyName.isEmpty();
        }

        // === 基本信息访问器 ===

        public @NotNull World getWorld() {
            return world;
        }

        public @NotNull BlockPos getPos() {
            return pos;
        }

        public @NotNull BlockPos getLowerPos() {
            return lowerPos;
        }

        public @NotNull BlockState getState() {
            return state;
        }

        public @NotNull DoorBlockEntity getEntity() {
            return entity;
        }

        public @NotNull PlayerEntity getPlayer() {
            return player;
        }

        public @NotNull ItemStack getHandItem() {
            return handItem.copy();
        }

        public @NotNull DoorInteractionType getInteractionType() {
            return interactionType;
        }

        public @NotNull DoorType getDoorType() {
            return doorType;
        }

        // === 门状态访问器 ===

        public @NotNull String getKeyName() {
            return keyName;
        }

        public boolean isOpen() {
            return isOpen;
        }

        public boolean isJammed() {
            return isJammed;
        }

        public boolean isBlasted() {
            return isBlasted;
        }

        public boolean requiresKey() {
            return requiresKey;
        }

        // === 便捷方法 ===

        /**
         * 检查是否在服务端
         */
        public boolean isServerSide() {
            return !world.isClient();
        }

        /**
         * 检查玩家是否为创造模式
         */
        public boolean isCreative() {
            return player.isCreative();
        }

        /**
         * 获取手持物品的lore第一行（用于钥匙匹配）
         */
        @Nullable
        public String getHandItemLoreFirstLine() {
            LoreComponent lore = handItem.get(DataComponentTypes.LORE);
            if (lore != null && !lore.lines().isEmpty()) {
                return lore.lines().getFirst().getString();
            }
            return null;
        }

        /**
         * 检查手持物品是否是正确的钥匙
         */
        public boolean isCorrectKey() {
            if (!requiresKey) return false;
            String loreLine = getHandItemLoreFirstLine();
            return loreLine != null && loreLine.equals(keyName);
        }

        /**
         * 创建Builder
         */
        public static Builder builder() {
            return new Builder();
        }

        /**
         * 上下文构建器
         */
        public static class Builder {
            private World world;
            private BlockPos pos;
            private BlockPos lowerPos;
            private BlockState state;
            private DoorBlockEntity entity;
            private PlayerEntity player;
            private ItemStack handItem;
            private DoorInteractionType interactionType;
            private DoorType doorType;

            public Builder world(World world) {
                this.world = world;
                return this;
            }

            public Builder pos(BlockPos pos) {
                this.pos = pos;
                return this;
            }

            public Builder lowerPos(BlockPos lowerPos) {
                this.lowerPos = lowerPos;
                return this;
            }

            public Builder state(BlockState state) {
                this.state = state;
                return this;
            }

            public Builder entity(DoorBlockEntity entity) {
                this.entity = entity;
                return this;
            }

            public Builder player(PlayerEntity player) {
                this.player = player;
                return this;
            }

            public Builder handItem(ItemStack handItem) {
                this.handItem = handItem;
                return this;
            }

            public Builder interactionType(DoorInteractionType type) {
                this.interactionType = type;
                return this;
            }

            public Builder doorType(DoorType type) {
                this.doorType = type;
                return this;
            }

            public DoorInteractionContext build() {
                return new DoorInteractionContext(
                        world, pos, lowerPos, state, entity, player,
                        handItem, interactionType, doorType
                );
            }
        }



    }

    /**
     * 门类型枚举。
     */
    enum DoorType {
        /**
         * 小门（SmallDoorBlock）- 普通房间门
         */
        SMALL_DOOR,

        /**
         * 列车门（TrainDoorBlock）- 车厢连接门
         */
        TRAIN_DOOR;

        /**
         * 从方块获取门类型
         *
         * @param block 门方块
         * @return 门类型，如果不是门方块则返回null
         */
        @Nullable
        public static DoorType fromBlock(Block block) {
            if (block instanceof TrainDoorBlock) {
                return TRAIN_DOOR;
            } else if (block instanceof SmallDoorBlock) {
                return SMALL_DOOR;
            }
            return null;
        }
    }

    /**
     * 门交互类型枚举。
     * 描述玩家尝试对门执行的具体操作。
     */
    enum DoorInteractionType {
        /**
         * 尝试打开门（门当前关闭，无特殊工具）
         */
        OPEN,

        /**
         * 尝试关闭门（门当前打开）
         */
        CLOSE,

        /**
         * 使用正确的钥匙开门
         */
        USE_KEY,

        /**
         * 使用撬锁工具开门
         */
        USE_LOCKPICK,

        /**
         * 使用撬锁工具卡住门（蹲下+撬锁工具）
         */
        JAM_DOOR,

        /**
         * 使用撬棍炸开门
         */
        USE_CROWBAR,

        /**
         * 与被炸开的门交互（门已被炸开）
         */
        BLASTED,

        /**
         * 普通交互（无特殊工具，或钥匙不匹配）
         */
        INTERACT;

        /**
         * 检查此交互是否使用了工具
         */
        public boolean usesTool() {
            return this == USE_KEY || this == USE_LOCKPICK ||
                    this == USE_CROWBAR || this == JAM_DOOR;
        }

        /**
         * 检查此交互是否是开门操作
         */
        public boolean isOpening() {
            return this == OPEN || this == USE_KEY ||
                    this == USE_LOCKPICK || this == USE_CROWBAR;
        }
    }

}
