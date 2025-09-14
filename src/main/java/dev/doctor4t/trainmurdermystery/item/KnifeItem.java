package dev.doctor4t.trainmurdermystery.item;

import dev.doctor4t.trainmurdermystery.TrainMurderMystery;
import dev.doctor4t.trainmurdermystery.entity.PlayerBodyEntity;
import dev.doctor4t.trainmurdermystery.index.TrainMurderMysteryEntities;
import dev.doctor4t.trainmurdermystery.index.TrainMurderMysterySounds;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.UseAction;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.world.World;

public class KnifeItem extends Item {
    public KnifeItem(Settings settings) {
        super(settings);
    }

    @Override
    public UseAction getUseAction(ItemStack stack) {
        return UseAction.SPEAR;
    }

    @Override
    public void onStoppedUsing(ItemStack stack, World world, LivingEntity user, int remainingUseTicks) {
        if (remainingUseTicks < getMaxUseTime(stack, user) - 10 && user instanceof PlayerEntity attacker) {
            HitResult collision = ProjectileUtil.getCollision(attacker, entity -> entity.isAlive() && entity.isAttackable(), 2f);
            if (collision instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity killedPlayer && TrainMurderMystery.shouldRestrictPlayerOptions(killedPlayer)) {
                killedPlayer.damage(attacker.getWorld().getDamageSources().genericKill(), 9999999f);

                PlayerBodyEntity body = TrainMurderMysteryEntities.PLAYER_BODY.create(world);
                body.setPlayerUuid(killedPlayer.getUuid());

                body.refreshPositionAndAngles(killedPlayer.getX(), killedPlayer.getY(), killedPlayer.getZ(), killedPlayer.getHeadYaw(), 0f);
                body.setYaw(killedPlayer.getHeadYaw());
                body.setHeadYaw(killedPlayer.getHeadYaw());
                killedPlayer.getWorld().spawnEntity(body);

                user.swingHand(Hand.MAIN_HAND);
                killedPlayer.playSound(TrainMurderMysterySounds.ITEM_KNIFE_STAB, 1.0f, 1.0f);
                if (!attacker.isCreative()) attacker.getItemCooldownManager().set(this, 3600); // 3 minutes
            }

        }
    }

    @Override
    public int getMaxUseTime(ItemStack stack, LivingEntity user) {
        return 72000;
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack itemStack = user.getStackInHand(hand);
        user.setCurrentHand(hand);
        user.playSound(TrainMurderMysterySounds.ITEM_KNIFE_PREPARE, 1.0f, 1.0f);
        return TypedActionResult.consume(itemStack);
    }
}
