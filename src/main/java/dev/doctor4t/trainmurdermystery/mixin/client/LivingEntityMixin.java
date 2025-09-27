package dev.doctor4t.trainmurdermystery.mixin.client;

import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import dev.doctor4t.trainmurdermystery.client.TMMClient;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;

import java.util.HashMap;
import java.util.UUID;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends Entity {
    public LivingEntityMixin(EntityType<?> type, World world) {
        super(type, world);
    }

    @WrapMethod(method = "getMainHandStack")
    public ItemStack tmm$replaceItemInHandWithPsychosisItem(Operation<ItemStack> original) {
        if (TMMClient.moodComponent != null && TMMClient.moodComponent.isLowerThanMid()) { // make sure it's only the main hand item that's being replaced
            HashMap<UUID, ItemStack> psychosisItems = TMMClient.moodComponent.getPsychosisItems();
            UUID uuid = this.getUuid();
            if (psychosisItems.containsKey(uuid)) {
                return psychosisItems.get(uuid);
            }
        }

        return original.call();
    }
}
