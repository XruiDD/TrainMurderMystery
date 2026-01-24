package dev.doctor4t.wathe.game;

import dev.doctor4t.wathe.Wathe;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.ShopEntry;
import net.minecraft.item.Item;
import net.minecraft.util.Identifier;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

public interface GameConstants {
    // Logistics
    int FADE_TIME = 40;
    int FADE_PAUSE = 20;

    // Blocks
    int DOOR_AUTOCLOSE_TIME = getInTicks(0, 5);

    // Items
    Map<Item, Integer> ITEM_COOLDOWNS = new HashMap<>();

    static void init() {
        ITEM_COOLDOWNS.put(WatheItems.KNIFE, getInTicks(1, 0));
        ITEM_COOLDOWNS.put(WatheItems.REVOLVER, getInTicks(0, 10));
        ITEM_COOLDOWNS.put(WatheItems.DERRINGER, getInTicks(0, 1));
        ITEM_COOLDOWNS.put(WatheItems.GRENADE, getInTicks(3, 0));
        ITEM_COOLDOWNS.put(WatheItems.LOCKPICK, getInTicks(3, 0));
        ITEM_COOLDOWNS.put(WatheItems.CROWBAR, getInTicks(0, 10));
        ITEM_COOLDOWNS.put(WatheItems.BODY_BAG, getInTicks(5, 0));
        ITEM_COOLDOWNS.put(WatheItems.PSYCHO_MODE, getInTicks(5, 0));
        ITEM_COOLDOWNS.put(WatheItems.BLACKOUT, getInTicks(3, 0));
    }

    int JAMMED_DOOR_TIME = getInTicks(1, 0);

    // Corpses
    int TIME_TO_DECOMPOSITION = getInTicks(1, 0);
    int DECOMPOSING_TIME = getInTicks(4, 0);

    // Task Variables
    float MOOD_GAIN = 0.5f;
    float MOOD_DRAIN = 1f / getInTicks(4, 0);
    int MIN_TIME_TO_FIRST_TASK = getInTicks(0, 40);
    int MAX_TIME_TO_FIRST_TASK = getInTicks(1, 20);
    int MIN_TASK_COOLDOWN = getInTicks(0, 30);
    int MAX_TASK_COOLDOWN = getInTicks(1, 0);
    int TASK_INTERVAL_PER_ACTIVE_TASK = getInTicks(0, 10); // 每个活跃任务额外增加20秒间隔
    int SLEEP_TASK_DURATION = getInTicks(0, 8);
    int OUTSIDE_TASK_DURATION = getInTicks(0, 8);
    float MID_MOOD_THRESHOLD = 0.55f;
    float DEPRESSIVE_MOOD_THRESHOLD = 0.2f;
    float ITEM_PSYCHOSIS_CHANCE = .5f; // in percent
    int ITEM_PSYCHOSIS_REROLL_TIME = 200;

    // Shop Variables
    // Default shop entries - empty by default, populated by BuildShopEntries event
    List<ShopEntry> SHOP_ENTRIES = new ArrayList<>();
    int MONEY_START = 100;
    Function<Long, Integer> PASSIVE_MONEY_TICKER = time -> {
        if (time % getInTicks(0, 10) == 0) {
            return 5;
        }
        return 0;
    };
    int MONEY_PER_KILL = 100;
    int PSYCHO_MODE_ARMOUR = 1;

    // Timers
    int PSYCHO_TIMER = getInTicks(0, 30);
    int FIRECRACKER_TIMER = getInTicks(0, 15);
    int BLACKOUT_MIN_DURATION = getInTicks(0, 30);
    int BLACKOUT_MAX_DURATION = getInTicks(0, 40);
    int TIME_ON_CIVILIAN_KILL = getInTicks(0, 30);

    static int getInTicks(int minutes, int seconds) {
        return (minutes * 60 + seconds) * 20;
    }

    interface DeathReasons {
        Identifier GENERIC = Wathe.id("generic");
        Identifier KNIFE = Wathe.id("knife_stab");
        Identifier GUN = Wathe.id("gun_shot");
        Identifier GUN_BACKFIRE = Wathe.id("gun_shot_backfire");
        Identifier BAT = Wathe.id("bat_hit");
        Identifier GRENADE = Wathe.id("grenade");
        Identifier POISON = Wathe.id("poison");
        Identifier FELL_OUT_OF_TRAIN = Wathe.id("fell_out_of_train");
        Identifier ESCAPED = Wathe.id("escaped");
        Identifier SHOT_INNOCENT = Wathe.id("shot_innocent");
        Identifier MENTAL_BREAKDOWN = Wathe.id("mental_breakdown");
    }
}