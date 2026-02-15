package dev.doctor4t.wathe.item;

import dev.doctor4t.wathe.index.WatheDataComponentTypes;
import dev.doctor4t.wathe.item.component.WalkieTalkieComponent;
import dev.doctor4t.wathe.util.AdventureUsable;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.TypedActionResult;
import net.minecraft.world.World;

import java.util.List;

public class WalkieTalkieItem extends Item implements AdventureUsable {
    public WalkieTalkieItem(Settings settings) {
        super(settings);
    }

    @Override
    public TypedActionResult<ItemStack> use(World world, PlayerEntity user, Hand hand) {
        ItemStack stack = user.getStackInHand(hand);

        if (world.isClient) {
            WalkieTalkieComponent component = stack.getOrDefault(WatheDataComponentTypes.WALKIE_TALKIE, WalkieTalkieComponent.DEFAULT);
            WalkieTalkieItemClient.openScreen(component.channel(), hand);
        }
        return TypedActionResult.success(stack);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, List<Text> tooltip, TooltipType type) {
        WalkieTalkieComponent component = stack.getOrDefault(WatheDataComponentTypes.WALKIE_TALKIE, WalkieTalkieComponent.DEFAULT);
        tooltip.add(Text.translatable("tooltip.wathe.walkie_talkie.channel", component.channel()).formatted(Formatting.GRAY));
        tooltip.add(Text.translatable("tooltip.wathe.walkie_talkie.hold").formatted(Formatting.DARK_GRAY));
        tooltip.add(Text.translatable("tooltip.wathe.walkie_talkie.right_click").formatted(Formatting.DARK_GRAY));
    }
}
