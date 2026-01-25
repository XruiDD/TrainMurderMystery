package dev.doctor4t.wathe.client.gui;

import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.api.WatheRoles;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.cca.PlayerPsychoComponent;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.entity.NoteEntity;
import dev.doctor4t.wathe.entity.PlayerBodyEntity;
import dev.doctor4t.wathe.api.event.CanSeeBodyRole;
import dev.doctor4t.wathe.api.event.ShouldShowCohort;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.font.TextRenderer;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.render.RenderTickCounter;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Colors;
import net.minecraft.util.Formatting;
import net.minecraft.util.Identifier;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.MathHelper;
import net.minecraft.world.LightType;
import org.jetbrains.annotations.NotNull;

import java.util.UUID;

public class RoleNameRenderer {
    private static TrainRole targetRole = TrainRole.BYSTANDER;

    private static float nametagAlpha = 0f;
    private static float noteAlpha = 0f;
    private static float bodyRoleAlpha = 0f;
    private static Text nametag = Text.empty();
    private static Role bodyRole = null;
    private static PlayerBodyEntity targetBody = null;
    private static final Text[] note = new Text[]{Text.empty(), Text.empty(), Text.empty(), Text.empty()};

    public static void renderHud(TextRenderer renderer, @NotNull ClientPlayerEntity player, DrawContext context, RenderTickCounter tickCounter) {
        GameWorldComponent component = GameWorldComponent.KEY.get(player.getWorld());
        if (player.getWorld().getLightLevel(LightType.BLOCK, BlockPos.ofFloored(player.getEyePos())) < 3 && player.getWorld().getLightLevel(LightType.SKY, BlockPos.ofFloored(player.getEyePos())) < 10)
            return;
        float range = WatheClient.canSeeSpectatorInformation() ? 8f : 2f;
        Role targetPlayerRole = null;
        PlayerEntity targetPlayer = null; // 保存目标玩家引用，用于事件调用
        if (ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerEntity, range) instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof PlayerEntity target) {
            nametagAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, nametagAlpha, 1f);
            // Get target's role for spectator/creative mode display
            targetPlayerRole = component.getRole(target);
            targetPlayer = target; // 保存目标玩家
            nametag = target.getDisplayName();
            if (component.canUseKillerFeatures(target)) {
                targetRole = TrainRole.KILLER;
            } else {
                targetRole = TrainRole.BYSTANDER;
            }
            boolean shouldObfuscate = PlayerPsychoComponent.KEY.get(target).getPsychoTicks() > 0;
            nametag = shouldObfuscate ? Text.literal("urscrewed" + "X".repeat(player.getRandom().nextInt(8))).styled(style -> style.withFormatting(Formatting.OBFUSCATED, Formatting.DARK_RED)) : nametag;
        } else {
            nametagAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, nametagAlpha, 0f);
        }
        if (nametagAlpha > 0.05f) {
            context.getMatrices().push();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f + 6, 0);
            context.getMatrices().scale(0.6f, 0.6f, 1f);
            Role hudRole = targetPlayerRole != null ? targetPlayerRole : WatheRoles.CIVILIAN;
            // Draw role name for spectators/creative players
            if (hudRole != null && WatheClient.canSeeSpectatorInformation()) {
                Text roleName = Text.translatable("announcement.role." + hudRole.identifier().getPath());
                context.drawTextWithShadow(renderer, roleName, -renderer.getWidth(roleName) / 2, 0, hudRole.color() | (int) (nametagAlpha * 255.0F) << 24);
            }
            int nameWidth = renderer.getWidth(nametag);
            context.drawTextWithShadow(renderer, nametag, -nameWidth / 2, 16, MathHelper.packRgb(1f, 1f, 1f) | ((int) (nametagAlpha * 255) << 24));
            if (component.isRunning() && targetPlayer != null) {
                TrainRole playerRole = TrainRole.BYSTANDER;
                if (component.canUseKillerFeatures(player)) playerRole = TrainRole.KILLER;

                // 检查是否应该显示cohort提示
                boolean shouldShowCohort;

                // 先触发事件，允许附属mod自定义
                ShouldShowCohort.CohortResult eventResult = ShouldShowCohort.EVENT.invoker().getCohortResult(player, targetPlayer);
                if (eventResult != null) {
                    // 事件返回了明确结果，使用事件结果
                    shouldShowCohort = eventResult.shouldShow();
                } else {
                    // 事件未处理，使用默认逻辑：两个杀手之间显示cohort
                    shouldShowCohort = (playerRole == TrainRole.KILLER && targetRole == TrainRole.KILLER);
                }

                if (shouldShowCohort) {
                    context.getMatrices().translate(0, 20 + renderer.fontHeight, 0);
                    MutableText roleText = Text.translatable("game.tip.cohort");
                    int roleWidth = renderer.getWidth(roleText);
                    context.drawTextWithShadow(renderer, roleText, -roleWidth / 2, 0, MathHelper.packRgb(1f, 0f, 0f) | ((int) (nametagAlpha * 255) << 24));
                }
            }
            context.getMatrices().pop();
        }
        // 尸体角色显示逻辑：检测玩家是否在看尸体
        boolean canSeeFullBodyInfo = false;
        boolean canSeeEscapedDeathReason = false;
        if (ProjectileUtil.getCollision(player, entity -> entity instanceof PlayerBodyEntity, range) instanceof EntityHitResult ehr && ehr.getEntity() instanceof PlayerBodyEntity body) {
            UUID deadPlayerUuid = body.getPlayerUuid();
            Identifier currentDeathReason = body.getDeathReason();
            // 检查死因是否是"退出游戏"（escaped），所有人都能看到
            boolean isEscaped = currentDeathReason.getPath().equals("escaped");
            // 检查是否有权限查看尸体角色（旁观者/创造模式 或 通过 Event 允许）
            if (deadPlayerUuid != null && (WatheClient.canSeeSpectatorInformation() || CanSeeBodyRole.EVENT.invoker().canSee(MinecraftClient.getInstance().player))) {
                bodyRole = component.getRole(deadPlayerUuid);
                targetBody = body;
                bodyRoleAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, bodyRoleAlpha, 1f);
                canSeeFullBodyInfo = true;
            } else if (isEscaped) {
                // 退出游戏的死因，所有人都能看到（但只显示死因，不显示角色等信息）
                targetBody = body;
                bodyRoleAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, bodyRoleAlpha, 1f);
                canSeeEscapedDeathReason = true;
            } else {
                bodyRoleAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, bodyRoleAlpha, 0f);
            }
        } else {
            bodyRoleAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, bodyRoleAlpha, 0f);
        }
        // 渲染尸体角色和死亡信息
        if (bodyRoleAlpha > 0.05f && targetBody != null) {
            context.getMatrices().push();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f + 6, 0);
            context.getMatrices().scale(0.6f, 0.6f, 1f);
            if (canSeeFullBodyInfo && bodyRole != null) {
                // 渲染角色名称（仅当有权限查看完整信息时）
                Text roleName = Text.translatable("announcement.role." + bodyRole.identifier().getPath());
                context.drawTextWithShadow(renderer, roleName, -renderer.getWidth(roleName) / 2, 0, bodyRole.color() | (int) (bodyRoleAlpha * 255.0F) << 24);
                // 渲染死亡信息（死亡时间和死因）
                Identifier deathReason = targetBody.getDeathReason();
                Text deathInfo = Text.translatable("hud.body.death_info", targetBody.age / 20)
                        .append(Text.translatable("death_reason." + deathReason.getNamespace() + "." + deathReason.getPath()));
                context.drawTextWithShadow(renderer, deathInfo, -renderer.getWidth(deathInfo) / 2, 16, Colors.RED | (int) (bodyRoleAlpha * 255.0F) << 24);
            } else if (canSeeEscapedDeathReason) {
                // 仅渲染"退出游戏"死因（不显示角色和死亡时间）
                Identifier deathReason = targetBody.getDeathReason();
                Text deathReasonText = Text.translatable("death_reason." + deathReason.getNamespace() + "." + deathReason.getPath());
                context.drawTextWithShadow(renderer, deathReasonText, -renderer.getWidth(deathReasonText) / 2, 0, Colors.RED | (int) (bodyRoleAlpha * 255.0F) << 24);
            }
            context.getMatrices().pop();
        }
        if (ProjectileUtil.getCollision(player, entity -> entity instanceof NoteEntity, range) instanceof EntityHitResult entityHitResult && entityHitResult.getEntity() instanceof NoteEntity note) {
            noteAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, noteAlpha, 1f);
            nametagAlpha = MathHelper.lerp(tickCounter.getTickDelta(true), nametagAlpha, 0f);
            RoleNameRenderer.note[0] = Text.literal(note.getLines()[0]);
            RoleNameRenderer.note[1] = Text.literal(note.getLines()[1]);
            RoleNameRenderer.note[2] = Text.literal(note.getLines()[2]);
            RoleNameRenderer.note[3] = Text.literal(note.getLines()[3]);
        } else {
            noteAlpha = MathHelper.lerp(tickCounter.getTickDelta(true) / 4, noteAlpha, 0f);
        }
        if (noteAlpha > 0.05f) {
            context.getMatrices().push();
            context.getMatrices().translate(context.getScaledWindowWidth() / 2f, context.getScaledWindowHeight() / 2f + 6, 0);
            context.getMatrices().scale(0.6f, 0.6f, 1f);
            for (int i = 0; i < note.length; i++) {
                Text line = note[i];
                int lineWidth = renderer.getWidth(line);
                context.drawTextWithShadow(renderer, line, -lineWidth / 2, 16 + (i * (renderer.fontHeight + 2)), MathHelper.packRgb(1f, 1f, 1f) | ((int) (noteAlpha * 255) << 24));
            }
            context.getMatrices().pop();
        }
    }

    private enum TrainRole {
        KILLER,
        BYSTANDER
    }
}