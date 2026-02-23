package dev.doctor4t.wathe.command;

import com.google.common.collect.ImmutableList;
import com.mojang.brigadier.CommandDispatcher;
import com.mojang.brigadier.arguments.StringArgumentType;
import dev.doctor4t.wathe.index.WatheItems;
import net.minecraft.command.argument.EntityArgumentType;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.LoreComponent;
import net.minecraft.item.ItemStack;
import net.minecraft.server.command.CommandManager;
import net.minecraft.server.command.ServerCommandSource;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.text.Style;
import net.minecraft.text.Text;

import java.util.Collection;

public class GiveRoomKeyCommand {
    public static void register(CommandDispatcher<ServerCommandSource> dispatcher) {
        dispatcher.register(
                CommandManager.literal("wathe:giveRoomKey")
                        .requires(source -> source.hasPermissionLevel(2))
                        .then(
                                CommandManager.argument("roomName", StringArgumentType.string())
                                        .executes(context -> giveRoomKey(
                                                context.getSource(),
                                                ImmutableList.of(context.getSource().getPlayerOrThrow()),
                                                StringArgumentType.getString(context, "roomName")
                                        ))
                                        .then(
                                                CommandManager.argument("targets", EntityArgumentType.players())
                                                        .executes(context -> giveRoomKey(
                                                                context.getSource(),
                                                                EntityArgumentType.getPlayers(context, "targets"),
                                                                StringArgumentType.getString(context, "roomName")
                                                        ))
                                        )
                        )
        );
    }

    private static int giveRoomKey(ServerCommandSource source, Collection<ServerPlayerEntity> targets, String roomName) {
        for (ServerPlayerEntity target : targets) {
            ItemStack itemStack = new ItemStack(WatheItems.KEY);
            itemStack.apply(DataComponentTypes.LORE, LoreComponent.DEFAULT, component -> new LoreComponent(Text.literal(roomName).getWithStyle(Style.EMPTY.withItalic(false).withColor(0xFF8C00))));
            target.giveItemStack(itemStack);
        }
        return 1;
    }
}
