package dev.doctor4t.trainmurdermystery.command.argument;

import com.mojang.brigadier.context.CommandContext;
import com.mojang.brigadier.exceptions.CommandSyntaxException;
import com.mojang.brigadier.suggestion.SuggestionProvider;
import com.mojang.brigadier.suggestion.Suggestions;
import com.mojang.brigadier.suggestion.SuggestionsBuilder;
import dev.doctor4t.trainmurdermystery.api.Role;
import dev.doctor4t.trainmurdermystery.api.TMMRoles;
import net.minecraft.command.CommandSource;
import net.minecraft.server.command.ServerCommandSource;

import java.util.concurrent.CompletableFuture;

public class RoleSuggestionProvider implements SuggestionProvider<ServerCommandSource> {
    @Override
    public CompletableFuture<Suggestions> getSuggestions(CommandContext<ServerCommandSource> context, SuggestionsBuilder builder) {
        if (TMMRoles.ROLES.isEmpty()) return Suggestions.empty();

        for (Role role : TMMRoles.ROLES) {
            if (TMMRoles.SPECIAL_ROLES.contains(role)) continue;
            if (role != null && CommandSource.shouldSuggest(builder.getRemaining(), role.identifier().getPath())) {
                builder.suggest(role.identifier().getPath());
            }
        }
        return builder.buildFuture();
    }
}
