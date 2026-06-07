package dev.doctor4t.wathe.mixin;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.event.AllowPlayerPunching;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.MapEnhancementsWorldComponent;
import dev.doctor4t.wathe.cca.PlayerStaminaComponent;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.GravityConfig;
import dev.doctor4t.wathe.config.datapack.MapEnhancementsConfiguration.JumpConfig;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.InteractionSwingTracker;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.attribute.EntityAttribute;
import net.minecraft.entity.attribute.EntityAttributeInstance;
import net.minecraft.entity.attribute.EntityAttributeModifier;
import net.minecraft.entity.attribute.EntityAttributes;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundEvent;
import net.minecraft.util.Hand;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(LivingEntity.class)
public abstract class LivingEntityMixin extends EntityMixin implements InteractionSwingTracker {
    @Unique
    private static final EntityAttributeModifier WEAPON_KNOCKBACK_MODIFIER = new EntityAttributeModifier(Wathe.id("weapon_knockback_modifier"), .5f, EntityAttributeModifier.Operation.ADD_VALUE);

    // 记录最近一次"右键交互（开门等）触发的挥手"所在 tick，用于区分左右键挥手
    @Unique
    private long wathe$lastInteractionSwingTick = Long.MIN_VALUE;

    @Unique
    private static final EntityAttributeModifier DISABLE_JUMP_MODIFIER = new EntityAttributeModifier(
        Wathe.id("disable_jump_modifier"), -1.0, EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL);

    @Unique
    private float wathe$lastGravityMultiplier = Float.NaN;

    @Unique
    private Boolean wathe$lastJumpDisabled = null;

    @Shadow
    protected boolean jumping;

    @Shadow
    public abstract void playSound(@Nullable SoundEvent sound);

    @Shadow
    public abstract @Nullable EntityAttributeInstance getAttributeInstance(RegistryEntry<EntityAttribute> attribute);

    // 标记由右键交互触发的服务端挥手：原版交互成功时服务端调用 swingHand(hand, true)（fromServerPlayer=true），
    // 左键攻击/空挥走 swingHand(hand) → swingHand(hand, false)，永远不传 true。
    // 据此区分左右键，使 ServerPlayerEntity 的攻击冷却重置只跳过右键交互、保留左键挥击的冷却。
    @Inject(method = "swingHand(Lnet/minecraft/util/Hand;Z)V", at = @At("HEAD"))
    private void wathe$trackInteractionSwing(Hand hand, boolean fromServerPlayer, CallbackInfo ci) {
        if (fromServerPlayer && (Object) this instanceof PlayerEntity player && player.getMainHandStack().isOf(WatheItems.BAT)) {
            this.wathe$lastInteractionSwingTick = player.getWorld().getTime();
        }
    }

    @Override
    public long wathe$lastInteractionSwingTick() {
        return this.wathe$lastInteractionSwingTick;
    }

    @Inject(method = "tick", at = @At("HEAD"))
    public void wathe$addKnockbackWithKnife(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            boolean shouldKnockback = player.getMainHandStack().isOf(WatheItems.KNIFE)
                    || AllowPlayerPunching.EVENT.invoker().allowPunching(player, player);
            updateAttribute(player.getAttributeInstance(EntityAttributes.GENERIC_ATTACK_KNOCKBACK), WEAPON_KNOCKBACK_MODIFIER, shouldKnockback);
        }
    }

    // 服务端应用重力配置 - 根据地图配置修改玩家重力
    @Inject(method = "tick", at = @At("HEAD"))
    public void wathe$applyGravityMultiplier(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            EntityAttributeInstance gravityAttr = player.getAttributeInstance(EntityAttributes.GENERIC_GRAVITY);
            if (gravityAttr == null) return;

            GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
            float targetMultiplier;
            if (gameComponent != null && gameComponent.isRunning() && GameFunctions.isPlayerAliveAndSurvival(player)) {
                GravityConfig gravityConfig = MapEnhancementsWorldComponent.KEY.get(player.getWorld()).getGravityConfig();
                targetMultiplier = gravityConfig.gravityMultiplier();
            } else {
                targetMultiplier = 1.0f;
            }

            // 仅在乘数发生变化时更新属性修改器
            if (targetMultiplier != wathe$lastGravityMultiplier) {
                // 移除旧的修改器
                if (gravityAttr.hasModifier(Wathe.id("map_gravity_modifier"))) {
                    gravityAttr.removeModifier(Wathe.id("map_gravity_modifier"));
                }
                // 仅在乘数不为 1.0 时添加修改器
                if (targetMultiplier != 1.0f) {
                    gravityAttr.addTemporaryModifier(new EntityAttributeModifier(
                        Wathe.id("map_gravity_modifier"),
                        targetMultiplier - 1.0f,
                        EntityAttributeModifier.Operation.ADD_MULTIPLIED_TOTAL
                    ));
                }
                wathe$lastGravityMultiplier = targetMultiplier;
            }
        }
    }

    // 服务端限制跳跃 - 通过 GENERIC_JUMP_STRENGTH 属性归零，覆盖客户端任何跳跃来源（按键、手柄模组等）
    // 属性是服务端权威并自动同步，客户端 LivingEntity.getJumpVelocity() 会读到 0
    // 判定逻辑必须与客户端 KeyBindingMixin 屏蔽跳跃键的条件保持一致
    @Inject(method = "tick", at = @At("HEAD"))
    public void wathe$applyJumpRestriction(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            EntityAttributeInstance jumpAttr = player.getAttributeInstance(EntityAttributes.GENERIC_JUMP_STRENGTH);
            if (jumpAttr == null) return;

            boolean shouldDisable = false;
            // GameFunctions.isPlayerPlayingAndAlive 已包含 isRunning + hasAnyRole + !isPlayerDead
            if (GameFunctions.isPlayerPlayingAndAlive(player)) {
                JumpConfig jumpConfig = MapEnhancementsWorldComponent.KEY.get(player.getWorld()).getJumpConfig();
                if (!jumpConfig.allowed()) {
                    shouldDisable = true;
                } else if (jumpConfig.staminaCost() > 0) {
                    PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
                    if (!stamina.isInfiniteStamina() && stamina.getSprintingTicks() < jumpConfig.staminaCost()) {
                        shouldDisable = true;
                    }
                }
            }

            if (wathe$lastJumpDisabled == null || wathe$lastJumpDisabled != shouldDisable) {
                if (jumpAttr.hasModifier(Wathe.id("disable_jump_modifier"))) {
                    jumpAttr.removeModifier(Wathe.id("disable_jump_modifier"));
                }
                if (shouldDisable) {
                    jumpAttr.addTemporaryModifier(DISABLE_JUMP_MODIFIER);
                }
                wathe$lastJumpDisabled = shouldDisable;
            }
        }
    }

    // 服务端限制跳跃 - 根据地图配置决定是否允许跳跃
    @Inject(method = "jump", at = @At("HEAD"), cancellable = true)
    public void wathe$restrictJump(CallbackInfo ci) {
        if ((Object) this instanceof PlayerEntity player) {
            // 仅在服务端检查
            if (!player.getWorld().isClient) {
                GameWorldComponent gameComponent = GameWorldComponent.KEY.get(player.getWorld());
                // 游戏进行中且玩家是存活的生存模式玩家时检查跳跃配置
                if (gameComponent != null && gameComponent.isRunning() && GameFunctions.isPlayerAliveAndSurvival(player)) {
                    JumpConfig jumpConfig = MapEnhancementsWorldComponent.KEY.get(player.getWorld()).getJumpConfig();
                    if (!jumpConfig.allowed()) {
                        // 不允许跳跃
                        ci.cancel();
                    } else if (jumpConfig.staminaCost() > 0) {
                        // 允许跳跃但消耗体力
                        PlayerStaminaComponent stamina = PlayerStaminaComponent.KEY.get(player);
                        if (stamina.isInfiniteStamina()) {
                            // 无限体力，允许跳跃且不消耗体力
                        } else if (stamina.getSprintingTicks() < jumpConfig.staminaCost()) {
                            // 体力不足，取消跳跃
                            ci.cancel();
                        } else {
                            // 扣除体力
                            stamina.setSprintingTicks(stamina.getSprintingTicks() - jumpConfig.staminaCost());
                        }
                    }
                    // allowed=true 且 staminaCost=0 → 自由跳跃，不取消
                }
            }
        }
    }

    @Unique
    private static void updateAttribute(EntityAttributeInstance attribute, EntityAttributeModifier modifier, boolean addOrKeep) {
        if (attribute != null) {
            boolean alreadyHasModifier = attribute.hasModifier(modifier.id());
            if (addOrKeep && !alreadyHasModifier) {
                attribute.addPersistentModifier(modifier);
            } else if (!addOrKeep && alreadyHasModifier) {
                attribute.removeModifier(modifier);
            }
        }
    }
}
