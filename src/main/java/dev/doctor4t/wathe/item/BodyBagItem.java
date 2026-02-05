package dev.doctor4t.wathe.item;

import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Vec3d;

public class BodyBagItem extends Item {
    public BodyBagItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity user, LivingEntity entity, Hand hand) {
        if (entity instanceof PlayerBodyEntity body) {
            Vec3d bodyPos = body.getPos();
            var bodyUuid = body.getUuid();
            body.discard();
            if (!user.getWorld().isClient) {
                user.getWorld().playSound(null, body.getX(), body.getY() + .1f, body.getZ(), SoundEvents.ITEM_BUNDLE_INSERT, SoundCategory.PLAYERS, 0.5f, 1f + user.getWorld().random.nextFloat() * .1f - .05f);
                if (user instanceof ServerPlayerEntity serverPlayer) {
                    NbtCompound extra = new NbtCompound();
                    extra.putUuid("body_uuid", bodyUuid);
                    GameRecordManager.putPos(extra, "body_pos", bodyPos);
                    GameRecordManager.recordItemUse(serverPlayer, Registries.ITEM.getId(this), null, extra);
                }
            }
            if (!user.isCreative()) {
                user.getStackInHand(hand).decrement(1);
                user.getItemCooldownManager().set(this, GameConstants.ITEM_COOLDOWNS.get(this));
            }

            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }
}
