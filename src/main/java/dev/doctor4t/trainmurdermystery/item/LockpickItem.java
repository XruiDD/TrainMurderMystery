package dev.doctor4t.trainmurdermystery.item;

import dev.doctor4t.trainmurdermystery.block.SmallDoorBlock;
import dev.doctor4t.trainmurdermystery.block_entity.DoorBlockEntity;
import dev.doctor4t.trainmurdermystery.block_entity.SmallDoorBlockEntity;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import dev.doctor4t.trainmurdermystery.index.TrainMurderMysterySounds;
import net.minecraft.block.BlockState;
import net.minecraft.block.enums.DoubleBlockHalf;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.sound.SoundCategory;
import net.minecraft.util.ActionResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.world.World;

public class LockpickItem extends Item {
    public LockpickItem(Settings settings) {
        super(settings);
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();
        BlockPos pos = context.getBlockPos();
        BlockState state = world.getBlockState(pos);

        if (state.getBlock() instanceof SmallDoorBlock) {
            BlockPos lowerPos = state.get(SmallDoorBlock.HALF) == DoubleBlockHalf.LOWER ? pos : pos.down();
            if (world.getBlockEntity(lowerPos) instanceof SmallDoorBlockEntity entity) {
                if (player.isSneaking()) {
                    entity.setJammed(GameConstants.JAMMED_DOOR_TIME);

                    if (!player.isCreative()) player.getItemCooldownManager().set(this, GameConstants.LOCKPICK_JAM_COOLDOWN);

                    if (!world.isClient)
                        world.playSound(null, lowerPos.getX() + .5f, lowerPos.getY() + 1, lowerPos.getZ() + .5f, TrainMurderMysterySounds.ITEM_LOCKPICK_DOOR, SoundCategory.BLOCKS, 1f, 1f);
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        }

        return super.useOnBlock(context);
    }

}
