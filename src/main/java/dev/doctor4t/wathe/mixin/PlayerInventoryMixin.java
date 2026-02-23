package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.cca.TrainWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Final;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;

@Mixin(PlayerInventory.class)
public class PlayerInventoryMixin {
    @Shadow
    @Final
    public PlayerEntity player;

    @WrapMethod(method = "scrollInHotbar")
    private void wathe$invalid(double scrollAmount, @NotNull Operation<Void> original) {
        int oldSlot = this.player.getInventory().selectedSlot;
        original.call(scrollAmount);
        PlayerPsychoComponent component = PlayerPsychoComponent.KEY.get(this.player);
        if (component.getPsychoTicks() > 0 &&
                (this.player.getInventory().getStack(oldSlot).isOf(WatheItems.BAT)) &&
                (!this.player.getInventory().getStack(this.player.getInventory().selectedSlot).isOf(WatheItems.BAT))
        ) this.player.getInventory().selectedSlot = oldSlot;
    }

    /**
     * 游戏运行期间，限制 getEmptySlot 只返回快捷栏槽位 (0-8)，
     * 防止拾取物品进入玩家无法访问的背包区域 (9-35)。
     */
    @WrapMethod(method = "getEmptySlot")
    private int wathe$restrictEmptySlotToHotbar(@NotNull Operation<Integer> original) {
        int slot = original.call();
        if (wathe$shouldRestrictToHotbar() && slot > 8) {
            return -1;
        }
        return slot;
    }

    /**
     * 游戏运行期间，限制 getOccupiedSlotWithRoomForStack 只返回快捷栏槽位 (0-8)，
     * 防止堆叠物品进入玩家无法访问的背包区域 (9-35)。
     */
    @WrapMethod(method = "getOccupiedSlotWithRoomForStack")
    private int wathe$restrictOccupiedSlotToHotbar(ItemStack stack, @NotNull Operation<Integer> original) {
        int slot = original.call(stack);
        if (wathe$shouldRestrictToHotbar() && slot > 8) {
            return -1;
        }
        return slot;
    }

    /**
     * 判断是否应该将物品操作限制在快捷栏范围内。
     * 仅在游戏进行中、HUD 激活、且玩家正在游戏（存活且非旁观/创造模式）时生效。
     */
    @Unique
    private boolean wathe$shouldRestrictToHotbar() {
        if (this.player == null || this.player.getWorld() == null) return false;
        TrainWorldComponent train = TrainWorldComponent.KEY.get(this.player.getWorld());
        if (train == null || !train.hasHud()) return false;
        return GameFunctions.isPlayerPlayingAndAlive(this.player);
    }
}