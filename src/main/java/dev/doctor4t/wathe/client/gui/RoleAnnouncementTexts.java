package dev.doctor4t.wathe.client.gui;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.api.Role;
import dev.doctor4t.wathe.game.GameFunctions;
import net.minecraft.text.Text;
import net.minecraft.util.Identifier;
import org.apache.commons.lang3.NotImplementedException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;

public class RoleAnnouncementTexts {
    public static final ArrayList<RoleAnnouncementTexts.RoleAnnouncementText> ROLE_ANNOUNCEMENT_TEXTS = new ArrayList<>();
    private static final Map<Identifier, RoleAnnouncementText> ROLE_TEXT_REGISTRY = new HashMap<>();

    public static RoleAnnouncementTexts.RoleAnnouncementText registerRoleAnnouncementText(RoleAnnouncementTexts.RoleAnnouncementText role) {
        ROLE_ANNOUNCEMENT_TEXTS.add(role);
        if (role.identifier != null) {
            ROLE_TEXT_REGISTRY.put(role.identifier, role);
        }
        return role;
    }

    /**
     * Gets the RoleAnnouncementText associated with a Role by its identifier.
     * @param role the role
     * @return the associated RoleAnnouncementText, or BLANK if not found
     */
    public static @NotNull RoleAnnouncementText getForRole(@Nullable Role role) {
        if (role == null) return BLANK;
        return ROLE_TEXT_REGISTRY.getOrDefault(role.identifier(), BLANK);
    }

    /**
     * Gets the RoleAnnouncementText associated with a Role by its identifier.
     * @param identifier the identifier of the role
     * @return the associated RoleAnnouncementText, or BLANK if not found
     */
    public static @NotNull RoleAnnouncementText getForRole(@Nullable Identifier identifier) {
        if (identifier == null) return BLANK;
        return ROLE_TEXT_REGISTRY.getOrDefault(identifier, BLANK);
    }

    // Built-in role announcement texts
    public static final RoleAnnouncementText BLANK = registerRoleAnnouncementText(
            new RoleAnnouncementText(Wathe.id("blank"), 0xFFFFFF));
    public static final RoleAnnouncementText CIVILIAN = registerRoleAnnouncementText(
            new RoleAnnouncementText(Wathe.id("civilian"), 0x36E51B));
    public static final RoleAnnouncementText VIGILANTE = registerRoleAnnouncementText(
            new RoleAnnouncementText(Wathe.id("vigilante"), 0x1B8AE5));
    public static final RoleAnnouncementText KILLER = registerRoleAnnouncementText(
            new RoleAnnouncementText(Wathe.id("killer"), 0xC13838));
    public static final RoleAnnouncementText LOOSE_END = registerRoleAnnouncementText(
            new RoleAnnouncementText(Wathe.id("loose_end"), 0x9F0000));

    public static class RoleAnnouncementText {
        public final Identifier identifier;
        private final String name;
        public final int colour;
        public final Text roleText;
        public final Text titleText;
        public final Text welcomeText;
        public final Function<Integer, Text> premiseText;
        public final Function<Integer, Text> goalText;
        public final Text winText;

        /**
         * Creates a new RoleAnnouncementText with the specified properties.
         * @param identifier the unique identifier for this role (used for Role lookup)
         * @param colour the color used for this role's text
         */
        public RoleAnnouncementText(Identifier identifier, int colour) {
            this.identifier = identifier;
            this.name = identifier.getPath();
            this.colour = colour;
            this.roleText = Text.translatable("announcement.role." + this.name.toLowerCase()).withColor(this.colour);
            this.titleText = Text.translatable("announcement.title." + this.name.toLowerCase()).withColor(this.colour);
            this.welcomeText = Text.translatable("announcement.welcome", this.roleText).withColor(0xF0F0F0);
            this.premiseText = (count) -> Text.translatable(count == 1 ? "announcement.premise" : "announcement.premises", count);
            this.goalText = (count) -> Text.translatable((count == 1 ? "announcement.goal." : "announcement.goals.") + this.name.toLowerCase(), count).withColor(this.colour);
            this.winText = Text.translatable("announcement.win." + this.name.toLowerCase()).withColor(this.colour);
        }

        /**
         * Gets the end game text for this role based on the win status.
         * @param status the current win status
         * @param winner for NEUTRAL wins, this should be the winning role's winText
         * @return the text to display at end of game
         */
        public @Nullable Text getEndText(GameFunctions.@NotNull WinStatus status, Text winner) {
            return switch (status) {
                case NONE -> null;
                case PASSENGERS, TIME -> // Civilian faction wins - everyone sees "Civilians win"
                        CIVILIAN.winText;
                case KILLERS -> // Killer faction wins - everyone sees "Killers win"
                        KILLER.winText;
                case LOOSE_END -> Text.translatable("announcement.win." + LOOSE_END.name.toLowerCase(), winner).withColor(LOOSE_END.colour);
                case NEUTRAL -> // Neutral wins - winner parameter contains the winning role's custom win text
                        throw new NotImplementedException();
            };
        }
    }
}
