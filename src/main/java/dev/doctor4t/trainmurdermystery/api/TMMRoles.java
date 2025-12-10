package dev.doctor4t.trainmurdermystery.api;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.client.gui.RoleAnnouncementTexts;
import dev.doctor4t.trainmurdermystery.game.GameConstants;
import net.minecraft.util.Identifier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

public class TMMRoles {
    public static final ArrayList<Role> ROLES = new ArrayList<>();
    public static final ArrayList<Role> SPECIAL_ROLES = new ArrayList<>();
    public static final ArrayList<Role> VANILLA_ROLES = new ArrayList<>();
    private static final Map<Identifier, Role> ROLES_REGISTRY = new HashMap<>();

    public static final Role DISCOVERY_CIVILIAN = new Role(TMM.id("discovery_civilian"), 0x36E51B, true, false, Role.MoodType.NONE, -1, true);
    public static final Role CIVILIAN = new Role(TMM.id("civilian"), 0x36E51B, true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), false);
    public static final Role VIGILANTE = new Role(TMM.id("vigilante"), 0x1B8AE5, true, false, Role.MoodType.REAL, GameConstants.getInTicks(0, 10), false);
    public static final Role KILLER = new Role(TMM.id("killer"), 0xC13838, false, true, Role.MoodType.FAKE, -1, true);
    public static final Role LOOSE_END = new Role(TMM.id("loose_end"), 0x9F0000, false, false, Role.MoodType.NONE, -1, false);


    static {
        SPECIAL_ROLES.add(LOOSE_END);
        SPECIAL_ROLES.add(DISCOVERY_CIVILIAN);
        SPECIAL_ROLES.add(CIVILIAN);
        VANILLA_ROLES.add(DISCOVERY_CIVILIAN);
        VANILLA_ROLES.add(CIVILIAN);
        VANILLA_ROLES.add(VIGILANTE);
        VANILLA_ROLES.add(KILLER);
        VANILLA_ROLES.add(LOOSE_END);
        registerRole(DISCOVERY_CIVILIAN);
        registerRole(CIVILIAN);
        registerRole(VIGILANTE);
        registerRole(KILLER);
        registerRole(LOOSE_END);
    }
    public static Role registerRole(Role role) {
        ROLES.add(role);
        ROLES_REGISTRY.put(role.identifier(),role);
        if (!VANILLA_ROLES.contains(role)){
            RoleAnnouncementTexts.RoleAnnouncementText roleAnnouncementText = new RoleAnnouncementTexts.RoleAnnouncementText(role.identifier(),role.color());
            RoleAnnouncementTexts.registerRoleAnnouncementText(roleAnnouncementText);
        }
        return role;
    }
    public static @Nullable Role getRole(Identifier id) {
        return ROLES_REGISTRY.get(id);
    }
}
