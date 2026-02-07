package dev.doctor4t.wathe.mixin;

import com.llamalad7.mixinextras.injector.ModifyExpressionValue;
import com.llamalad7.mixinextras.injector.ModifyReturnValue;
import com.llamalad7.mixinextras.injector.wrapmethod.WrapMethod;
import com.llamalad7.mixinextras.injector.wrapoperation.Operation;
import com.mojang.datafixers.util.Either;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.event.AllowPlayerPunching;
import dev.doctor4t.wathe.block.entity.SeatEntity;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapEnhancementsWorldComponent;
import dev.doctor4t.wathe.cca.PlayerMoodComponent;
import dev.doctor4t.wathe.cca.PlayerPoisonComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.MovementConfig;
import dev.doctor4t.wathe.game.GameConstants;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import dev.doctor4t.wathe.item.CocktailItem;
import dev.doctor4t.wathe.util.PoisonUtils;
import dev.doctor4t.wathe.util.Scheduler;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.component.type.FoodComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityType;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.Registries;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.Unit;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.World;
import org.jetbrains.annotations.NotNull;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.UUID;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin extends LivingEntity {
    @Shadow
    public abstract float getAttackCooldownProgress(float baseTime);

    @Shadow
    public abstract boolean isCreative();

    @Unique
    private Scheduler.ScheduledTask poisonSleepTask;

    protected PlayerEntityMixin(EntityType<? extends LivingEntity> entityType, World world) {
        super(entityType, world);
    }


    @ModifyReturnValue(method = "getMovementSpeed", at = @At("RETURN"))
    public float wathe$overrideMovementSpeed(float original) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(this.getWorld());
        if (gameComponent.isRunning() && !this.isCreative()) {
            MovementConfig movement = MapEnhancementsWorldComponent.KEY.get(this.getWorld()).getMovementConfig();
            return this.isSprinting() ? 0.1f * movement.sprintSpeedMultiplier() : 0.07f * movement.walkSpeedMultiplier();
        } else {
            return original;
        }
    }

    @Inject(method = "tickMovement", at = @At("HEAD"))
    public void wathe$limitSprint(CallbackInfo ci) {
        GameWorldComponent gameComponent = GameWorldComponent.KEY.get(this.getWorld());
        if (GameFunctions.isPlayerAliveAndSurvival((PlayerEntity) (Object) this) && gameComponent != null && gameComponent.isRunning()) {
            Role role = gameComponent.getRole((PlayerEntity) (Object) this);
            PlayerStaminaComponent staminaComponent = PlayerStaminaComponent.KEY.get(this);
            PlayerMoodComponent moodComponent = PlayerMoodComponent.KEY.get(this);

            // 极低心情时（低于抑郁阈值）无法疾跑
            if (moodComponent.isLowerThanDepressed()) {
                this.setSprinting(false);
            }

            if (role != null && role.getMaxSprintTime() >= 0) {
                int maxSprintTime = role.getMaxSprintTime();
                float sprintingTicks = staminaComponent.getSprintingTicks();
                boolean exhausted = staminaComponent.isExhausted();
                // 疲惫机制
                if (sprintingTicks <= 0) {
                    this.setSprinting(false);
                    exhausted = true; // 进入疲惫状态
                }

                // 疲惫状态下阻止疾跑，直到休息5秒
                if (exhausted) {
                    this.setSprinting(false);
                    // 休息5秒后解除疲惫（每tick恢复0.25，100 ticks * 0.25 = 25）
                    if (sprintingTicks >= staminaComponent.getExhaustionRecoveryTicks() * 0.25f) {
                        exhausted = false;
                    }
                }
                if (this.isSprinting()) {
                    sprintingTicks = Math.max(sprintingTicks - 1, 0);
                } else {
                    sprintingTicks = Math.min(sprintingTicks + 0.25f, maxSprintTime);
                }


                staminaComponent.setSprintingTicks(sprintingTicks);
                staminaComponent.setMaxSprintTime(maxSprintTime);
                staminaComponent.setExhausted(exhausted);
            } else {
                // 角色没有体力限制（如杀手），确保 maxSprintTime = -1 被同步到客户端
                if (staminaComponent.getMaxSprintTime() != -1) {
                    staminaComponent.setMaxSprintTime(-1);
                    staminaComponent.setExhausted(false);
                }
            }
        }
    }

    @WrapMethod(method = "attack")
    public void attack(Entity target, Operation<Void> original) {
        PlayerEntity self = (PlayerEntity) (Object) this;

        if (getMainHandStack().isOf(WatheItems.BAT) && target instanceof PlayerEntity playerTarget && self instanceof ServerPlayerEntity serverPlayer&& this.getAttackCooldownProgress(0.5F) >= 1f) {
            GameFunctions.killPlayer((ServerPlayerEntity)playerTarget, true, serverPlayer, GameConstants.DeathReasons.BAT);
            serverPlayer.getServerWorld().playSound(self,
                    playerTarget.getX(), playerTarget.getEyeY(), playerTarget.getZ(),
                    WatheSounds.ITEM_BAT_HIT, SoundCategory.PLAYERS,
                    3f, 1f);
            return;
        }

        if (self.isCreative() || this.getMainHandStack().isOf(WatheItems.KNIFE)
                || (target instanceof PlayerEntity playerTarget && AllowPlayerPunching.EVENT.invoker().allowPunching(self, playerTarget))) {
            original.call(target);
        }
    }

    @Inject(method = "eatFood", at = @At(value = "INVOKE", target = "Lnet/minecraft/entity/player/HungerManager;eat(Lnet/minecraft/component/type/FoodComponent;)V", shift = At.Shift.AFTER))
    private void wathe$poisonedFoodEffect(@NotNull World world, ItemStack stack, FoodComponent foodComponent, CallbackInfoReturnable<ItemStack> cir) {
        if (world.isClient) return;
        String poisoner = stack.getOrDefault(WatheDataComponentTypes.POISONER, null);
        if (poisoner != null) {
            NbtCompound recordExtra = new NbtCompound();
            recordExtra.putString("source", "food");
            recordExtra.putString("item", Registries.ITEM.getId(stack.getItem()).toString());
            int poisonTicks = PlayerPoisonComponent.KEY.get(this).poisonTicks;
            if (poisonTicks == -1) {
                PlayerPoisonComponent.KEY.get(this).setPoisonTicks(
                        world.getRandom().nextBetween(PlayerPoisonComponent.clampTime.getLeft(), PlayerPoisonComponent.clampTime.getRight()),
                        UUID.fromString(poisoner),
                        recordExtra
                );
            } else {
                PlayerPoisonComponent.KEY.get(this).setPoisonTicks(
                        MathHelper.clamp(poisonTicks - world.getRandom().nextBetween(100, 300), 0, PlayerPoisonComponent.clampTime.getRight()),
                        UUID.fromString(poisoner),
                        recordExtra
                );
            }
        }
    }

    @Inject(method = "wakeUp(ZZ)V", at = @At("HEAD"))
    private void wathe$poisonSleep(boolean skipSleepTimer, boolean updateSleepingPlayers, CallbackInfo ci) {
        if (this.poisonSleepTask != null) {
            this.poisonSleepTask.cancel();
            this.poisonSleepTask = null;
        }
    }

    @Inject(method = "trySleep", at = @At("TAIL"))
    private void wathe$poisonSleepMessage(BlockPos pos, CallbackInfoReturnable<Either<PlayerEntity.SleepFailureReason, Unit>> cir) {
        PlayerEntity self = (PlayerEntity) (Object) (this);
        if (cir.getReturnValue().right().isPresent() && self instanceof ServerPlayerEntity serverPlayer) {
            if (this.poisonSleepTask != null) this.poisonSleepTask.cancel();

            this.poisonSleepTask = Scheduler.schedule(
                    () -> PoisonUtils.bedPoison(serverPlayer),
                    40
            );
        }
    }

    @Inject(method = "canConsume(Z)Z", at = @At("HEAD"), cancellable = true)
    private void wathe$allowEatingRegardlessOfHunger(boolean ignoreHunger, @NotNull CallbackInfoReturnable<Boolean> cir) {
        cir.setReturnValue(true);
    }

    @Inject(method = "eatFood", at = @At("HEAD"))
    private void wathe$eat(World world, ItemStack stack, FoodComponent foodComponent, @NotNull CallbackInfoReturnable<ItemStack> cir) {
        if (!(stack.getItem() instanceof CocktailItem)) {
            PlayerMoodComponent.KEY.get(this).eatFood();
        }
    }

    @ModifyExpressionValue(method = "tick", at = @At(value = "INVOKE", target = "Lnet/minecraft/world/World;isDay()Z"))
    private boolean wathe$cancelWakingUpPlayers(boolean original) {
        return false;
    }

    @ModifyReturnValue(method = "shouldDismount", at = @At("RETURN"))
    private boolean wathe$delaySeatDismount(boolean original) {
        if (original && this.getVehicle() instanceof SeatEntity seatEntity) {
            return seatEntity.getRidingTicks() >= 5;
        }
        return original;
    }
}
