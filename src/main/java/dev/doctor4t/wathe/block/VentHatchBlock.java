package dev.doctor4t.wathe.block;

import com.mojang.serialization.MapCodec;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.index.WatheSounds;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.ShapeContext;
import net.minecraft.block.WallMountedBlock;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.StateManager;
import net.minecraft.state.property.BooleanProperty;
import net.minecraft.state.property.Properties;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.util.shape.VoxelShape;
import net.minecraft.world.BlockView;
import net.minecraft.world.World;
import net.minecraft.world.WorldView;

import java.util.HashMap;
import java.util.Map;

public class VentHatchBlock extends WallMountedBlock {

    public static final BooleanProperty OPEN = Properties.OPEN;

    // 存储锁定的通风舱口位置和解锁时间（游戏时间tick）
    private static final Map<BlockPos, Long> LOCKED_VENTS = new HashMap<>();
    private static final int LOCK_DURATION_TICKS = 600; // 30秒 = 600 ticks
    private static final VoxelShape NORTH_SHAPE = Block.createCuboidShape(1, 1, 15, 15, 15, 16);
    private static final VoxelShape EAST_SHAPE = Block.createCuboidShape(0, 1, 1, 1, 15, 15);
    private static final VoxelShape SOUTH_SHAPE = Block.createCuboidShape(1, 1, 0, 15, 15, 1);
    private static final VoxelShape WEST_SHAPE = Block.createCuboidShape(15, 1, 1, 16, 15, 15);
    private static final VoxelShape UP_SHAPE = Block.createCuboidShape(1, 0, 1, 15, 1, 15);
    private static final VoxelShape DOWN_SHAPE = Block.createCuboidShape(1, 15, 1, 15, 16, 15);
    private static final VoxelShape[] OPEN_WALL_SHAPES = {
            Block.createCuboidShape(1, 15, 0, 15, 16, 14),
            Block.createCuboidShape(2, 15, 1, 16, 16, 15),
            Block.createCuboidShape(1, 15, 2, 15, 16, 16),
            Block.createCuboidShape(0, 15, 1, 14, 16, 15)
    };
    private static final VoxelShape[] OPEN_CEILING_SHAPES = {
            Block.createCuboidShape(1, 2, 0, 15, 16, 1),
            Block.createCuboidShape(15, 2, 1, 16, 16, 15),
            Block.createCuboidShape(1, 2, 15, 15, 16, 16),
            Block.createCuboidShape(0, 2, 1, 1, 16, 15)
    };
    private static final VoxelShape[] OPEN_FLOOR_SHAPES = {
            Block.createCuboidShape(1, 0, 15, 15, 14, 16),
            Block.createCuboidShape(0, 0, 1, 1, 14, 15),
            Block.createCuboidShape(1, 0, 0, 15, 14, 1),
            Block.createCuboidShape(15, 0, 1, 16, 14, 15)
    };

    public VentHatchBlock(Settings settings) {
        super(settings);
        this.setDefaultState(super.getDefaultState().with(OPEN, false));
    }

    @Override
    protected MapCodec<? extends WallMountedBlock> getCodec() {
        return null;
    }

    @Override
    protected ActionResult onUse(BlockState state, World world, BlockPos pos, PlayerEntity player, BlockHitResult hit) {
        boolean hasCrowbar = player.getMainHandStack().isOf(WatheItems.CROWBAR);
        long currentTime = world.getTime();

        // 清理过期的锁定
        LOCKED_VENTS.entrySet().removeIf(entry -> entry.getValue() <= currentTime);

        boolean isLocked = LOCKED_VENTS.containsKey(pos);

        if (hasCrowbar) {
            player.getItemCooldownManager().set(WatheItems.CROWBAR, 6000);
            // 撬棍可以切换状态并锁定/刷新锁定时间
            boolean open = state.get(OPEN);
            world.setBlockState(pos, state.with(OPEN, !open));
            LOCKED_VENTS.put(pos, currentTime + LOCK_DURATION_TICKS);
            world.playSound(null, pos, WatheSounds.ITEM_CROWBAR_PRY, SoundCategory.BLOCKS, 2.5f, 1f);
            player.getItemCooldownManager().set(WatheItems.CROWBAR, 200);
            player.swingHand(Hand.MAIN_HAND, true);
            return ActionResult.success(world.isClient);
        } else if (isLocked) {
            // 锁定期间，非撬棍无法操作
            return ActionResult.FAIL;
        } else {
            // 未锁定，正常切换
            boolean open = state.get(OPEN);
            world.setBlockState(pos, state.with(OPEN, !open));
            SoundEvent sound = open ? SoundEvents.BLOCK_COPPER_TRAPDOOR_CLOSE : SoundEvents.BLOCK_COPPER_TRAPDOOR_OPEN;
            world.playSound(null, pos, sound, SoundCategory.BLOCKS, 1f, 1.125f);
            return ActionResult.success(world.isClient);
        }
    }

    @Override
    public VoxelShape getOutlineShape(BlockState state, BlockView world, BlockPos pos, ShapeContext context) {
        if (state.get(OPEN)) {
            Direction facing = state.get(FACING);
            return (switch (state.get(FACE)) {
                case CEILING -> OPEN_CEILING_SHAPES;
                case WALL -> OPEN_WALL_SHAPES;
                case FLOOR -> OPEN_FLOOR_SHAPES;
            })[facing.getHorizontal()];
        }
        return this.getShapeForState(state);
    }

    public VoxelShape getShapeForState(BlockState state) {
        return switch (VentHatchBlock.getDirection(state)) {
            case DOWN -> DOWN_SHAPE;
            case UP -> UP_SHAPE;
            case NORTH -> NORTH_SHAPE;
            case SOUTH -> SOUTH_SHAPE;
            case WEST -> WEST_SHAPE;
            case EAST -> EAST_SHAPE;
        };
    }

    @Override
    public boolean canPlaceAt(BlockState state, WorldView world, BlockPos pos) {
        return true;
    }

    @Override
    protected void appendProperties(StateManager.Builder<Block, BlockState> builder) {
        builder.add(FACE, FACING, OPEN);
    }
}
