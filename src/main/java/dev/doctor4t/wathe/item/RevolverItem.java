package dev.doctor4t.wathe.item;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.client.WatheClient;
import dev.doctor4t.wathe.client.particle.HandParticle;
import dev.doctor4t.wathe.client.render.WatheRenderLayers;
import dev.doctor4t.wathe.game.GameFunctions;
import dev.doctor4t.wathe.util.GunShootPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.block.BedBlock;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.BedPart;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.projectile.ProjectileUtil;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.hit.EntityHitResult;
import net.minecraft.util.hit.HitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.Optional;
import org.jetbrains.annotations.NotNull;

public class RevolverItem extends Item {
    public RevolverItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(@NotNull World world, @NotNull PlayerEntity user, Hand hand) {
        if (world.isClient) {
            HitResult collision = getGunTarget(user);
            int targetId = resolveTargetFromHitResult(world, collision);
            ClientPlayNetworking.send(new GunShootPayload(targetId));
            user.setPitch(user.getPitch() - 4);
            spawnHandParticle();
        }
        return TypedActionResult.consume(user.getStackInHand(hand));
    }

    /**
     * 从 HitResult 解析目标玩家 ID
     * 处理直接命中实体和命中床上睡觉的玩家两种情况
     * @return 目标玩家 ID，未找到返回 -1
     */
    public static int resolveTargetFromHitResult(World world, HitResult collision) {
        if (collision instanceof EntityHitResult entityHitResult) {
            return entityHitResult.getEntity().getId();
        } else if (collision instanceof BlockHitResult blockHitResult) {
            Optional<PlayerEntity> sleepingPlayer = findSleepingPlayerOnBed(world, blockHitResult);
            if (sleepingPlayer.isPresent()) {
                return sleepingPlayer.get().getId();
            }
        }
        return -1;
    }

    /**
     * 检测床方块上是否有睡觉的玩家
     * @return 睡觉的玩家，如果没有则返回 Optional.empty()
     */
    public static Optional<PlayerEntity> findSleepingPlayerOnBed(World world, BlockHitResult blockHitResult) {
        BlockPos blockPos = blockHitResult.getBlockPos();
        BlockState state = world.getBlockState(blockPos);

        if (!(state.getBlock() instanceof BedBlock)) {
            return Optional.empty();
        }

        BedPart part = state.get(BedBlock.PART);
        Direction facing = state.get(BedBlock.FACING);
        BlockPos headPos = (part == BedPart.HEAD) ? blockPos : blockPos.offset(facing);

        for (PlayerEntity player : world.getPlayers()) {
            if (!player.isSleeping()) {
                continue;
            }
            Optional<BlockPos> sleepingPosOpt = player.getSleepingPosition();
            if (sleepingPosOpt.isEmpty()) {
                continue;
            }
            BlockPos sleepingPos = sleepingPosOpt.get();
            if (sleepingPos.equals(headPos) || sleepingPos.equals(blockPos)) {
                return Optional.of(player);
            }
        }
        return Optional.empty();
    }

    public static void spawnHandParticle() {
        HandParticle handParticle = new HandParticle()
                .setTexture(Wathe.id("textures/particle/gunshot.png"))
                .setPos(0.1f, 0.275f, -0.2f)
                .setMaxAge(3)
                .setSize(0.5f)
                .setVelocity(0f, 0f, 0f)
                .setLight(15, 15)
                .setAlpha(1f, 0.1f)
                .setRenderLayer(WatheRenderLayers::additive);
        WatheClient.handParticleManager.spawn(handParticle);
    }

    public static HitResult getGunTarget(PlayerEntity user) {
        return ProjectileUtil.getCollision(user, entity -> entity instanceof PlayerEntity player && GameFunctions.isPlayerAliveAndSurvival(player) && !GameFunctions.isPlayerSpectatingOrCreative(player), 30f);
    }
}