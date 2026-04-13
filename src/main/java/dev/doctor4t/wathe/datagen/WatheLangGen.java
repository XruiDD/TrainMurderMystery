package dev.doctor4t.wathe.datagen;

import dev.doctor4t.ratatouille.util.TextUtils;
import dev.doctor4t.wathe.index.WatheBlocks;
import dev.doctor4t.wathe.index.WatheEntities;
import dev.doctor4t.wathe.index.WatheItems;
import dev.doctor4t.wathe.util.WalkieTalkieChannelPayload;
import net.fabricmc.fabric.api.datagen.v1.FabricDataOutput;
import net.fabricmc.fabric.api.datagen.v1.provider.FabricLanguageProvider;
import net.minecraft.registry.RegistryWrapper;
import org.jetbrains.annotations.NotNull;

import java.util.concurrent.CompletableFuture;

public class WatheLangGen extends FabricLanguageProvider {

    public WatheLangGen(FabricDataOutput dataOutput, CompletableFuture<RegistryWrapper.WrapperLookup> registryLookup) {
        super(dataOutput, registryLookup);
    }

    @Override
    public void generateTranslations(RegistryWrapper.WrapperLookup wrapperLookup, @NotNull TranslationBuilder builder) {
        WatheBlocks.registrar.generateLang(wrapperLookup, builder);
        WatheItems.registrar.generateLang(wrapperLookup, builder);
        WatheEntities.registrar.generateLang(wrapperLookup, builder);

//        builder.add(WatheItems.LETTER.getTranslationKey() + ".instructions", "Instructions");
//        builder.add("tip.letter.killer.tooltip1", "Thank you for taking this job. Please eliminate the following targets:");
//        builder.add("tip.letter.killer.tooltip.target", "- %s");
//        builder.add("tip.letter.killer.tooltip2", "Please do so with the utmost discretion and do not get caught. Good luck.");
//        builder.add("tip.letter.killer.tooltip3", "");
//        builder.add("tip.letter.killer.tooltip4", "P.S.: Don't forget to use your instinct [Left Alt] and use the train's exterior to relocate.");
//
//        builder.add(WatheItems.LETTER.getTranslationKey() + ".notes", "Notes");
//        builder.add("tip.letter.detective.tooltip1", "Multiple homicides, several wealthy victims.");
//        builder.add("tip.letter.detective.tooltip2", "Have to be linked... Serial killer? Assassin? Killer?");
//        builder.add("tip.letter.detective.tooltip3", "Potential next victims frequent travelers of the Harpy Express.");
//        builder.add("tip.letter.detective.tooltip4", "Perfect situation to corner but need to keep targets safe.");

        builder.add("lobby.players.count", "Players boarded: %s / %s");
        builder.add("lobby.autostart.active", "Game will start once %s+ players are boarded");
        builder.add("lobby.autostart.time", "Game starting in %ss");
        builder.add("lobby.autostart.starting", "Game starting");
        builder.add("lobby.autostart.gamemode", "Current Mode: %s");
        builder.add("lobby.voting.active", "Map voting in progress");
        builder.add("lobby.voting.keybind_hint", "Press [%s] to open/close the voting screen");

        // Game modes
        builder.add("gamemode.wathe.murder", "Murder Mode");
        builder.add("gamemode.wathe.discovery", "Discovery Mode");
        builder.add("gamemode.wathe.loose_ends", "Loose Ends Mode");

        builder.add("announcement.role.civilian", "Civilian");
        builder.add("announcement.role.vigilante", "Vigilante");
        builder.add("announcement.role.veteran", "Veteran");
        builder.add("announcement.role.killer", "Killer");
        builder.add("announcement.role.loose_end", "Loose End");
        builder.add("announcement.title.civilian", "Civilians");
        builder.add("announcement.title.vigilante", "Vigilantes");
        builder.add("announcement.title.veteran", "Veterans");
        builder.add("announcement.title.killer", "Killers");
        builder.add("announcement.title.loose_end", "Loose Ends");
        builder.add("announcement.title.discovery_civilian", "Discovery Civilians");
        builder.add("announcement.title.no_role", "No Role");

        builder.add("announcement.welcome", "Welcome aboard, %s!");
        builder.add("announcement.premise", "There is a killer hiding on the train.");
        builder.add("announcement.premises", "There are %s killers hiding on the train.");
        builder.add("announcement.goal.civilian", "Stay safe and survive until the end of the journey.");
        builder.add("announcement.goal.vigilante", "Eliminate any murderers and protect the civilians.");
        builder.add("announcement.goal.veteran", "Use your combat experience to eliminate any threats. Your knife has limited uses.");
        builder.add("announcement.goal.killer", "Eliminate a passenger to succeed, before time runs out.");
        builder.add("announcement.goals.civilian", "Stay safe and survive until the end of the journey.");
        builder.add("announcement.goals.vigilante", "Eliminate any murderers and protect the civilians.");
        builder.add("announcement.goals.veteran", "Use your combat experience to eliminate any threats. Your knife has limited uses.");
        builder.add("announcement.goals.killer", "Eliminate all civilians before time runs out.");
        builder.add("announcement.win.civilian", "Passengers Win!");
        builder.add("announcement.win.vigilante", "Passengers Win!");
        builder.add("announcement.win.veteran", "Passengers Win!");
        builder.add("announcement.win.killer", "Killers Win!");
        builder.add("announcement.win.loose_end", "%s Wins!");
        builder.add("announcement.loose_ends.welcome", "Welcome aboard... Loose End.");
        builder.add("announcement.loose_ends.premise", "Everybody on the train has a derringer and a knife.");
        builder.add("announcement.loose_ends.goal", "Eliminate all loose ends before they come for you. Good luck.");
        builder.add("announcement.loose_ends.winner", "%s Wins!");

        builder.add("tip.letter.name", "Dear %s, your role is %s. Welcome aboard the Harpy Express!");
        builder.add("tip.letter.room", "Please find attached your ticket as well as the key for accessing");
        builder.add("tip.letter.room.grand_suite", "the Grand Suite");
        builder.add("tip.letter.room.cabin_suite", "your Cabin Suite");
        builder.add("tip.letter.room.twin_cabin", "your Twin Cabin");
        builder.add("tip.letter.tooltip1", "Enjoy your trip on the 1st of January 1923.");
        builder.add("tip.letter.tooltip2", "La Sirène wishes you a pleasant and safe voyage.");
        builder.add("tip.skin", "Skin: ");
        builder.add("tip.skin.loading", "Skin loading...");
        builder.add("tip.skin.failed", "Skin failed to load");
        builder.add("tip.skin.rarity.white", "Common");
        builder.add("tip.skin.rarity.green", "Custom");
        builder.add("tip.skin.rarity.blue", "Antique");
        builder.add("tip.skin.rarity.purple", "Unique");
        builder.add("tip.skin.rarity.orange", "Legendary");

        builder.add("itemGroup.wathe.building", "Wathe: Building Blocks");
        builder.add("itemGroup.wathe.decoration", "Wathe: Decoration & Functional");
        builder.add("itemGroup.wathe.equipment", "Wathe: Equipment");

        builder.add("container.cargo_box", "Cargo Box");
        builder.add("container.cabinet", "Cabinet");
        builder.add("subtitles.block.cargo_box.close", "Cargo Box closes");
        builder.add("subtitles.block.cargo_box.open", "Cargo Box opens");
        builder.add("subtitles.block.door.toggle", "Door operates");
        builder.add("subtitles.item.crowbar.pry", "Crowbar pries door");

        builder.add("tip.door.locked", "This door is locked and cannot be opened.");
        builder.add("tip.door.requires_key", "This door is locked and requires a key to be opened.");
        builder.add("tip.door.requires_different_key", "This door is locked and requires a different key to be opened.");
        builder.add("tip.door.jammed", "This door is jammed and cannot be opened at the moment!");
        builder.add("tip.derringer.used", "Used: cannot be shot anymore, get a kill for another chance!");

        builder.add("tip.cooldown", "On cooldown: %s");
        builder.add("tip.note", "I should write something first");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.KNIFE) + ".tooltip", "Right-click, hold for a second and get close to your victim\nAfter a kill, cannot be used for 1 minute\nAttack to knock back / push a player (no cooldown)");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.REVOLVER) + ".tooltip", "Point, right-click and shoot\nDrops if you kill an innocent");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.DERRINGER) + ".tooltip", "Point, right-click and shoot\nCan only be shot once, so make it count!\nShot is replenished after a kill");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.GRENADE) + ".tooltip", "Right-click to throw, explodes on impact\nGood to clear groups of people, but be wary of the blast radius!\nSingle use, 5 minute cooldown");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.PSYCHO_MODE) + ".tooltip", "\"Do you like hurting other people?\"\nHides your identity and allows you to go crazy with a bat for 30 seconds\nBat kills on full swing and cannot be unselected for the duration of the ability\nActivated instantly upon purchase, 5 minute cooldown");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.POISON_VIAL) + ".tooltip", "Slip in food or drinks to poison the next pickup");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.FIRECRACKER) + ".tooltip", "Detonates 15 seconds after being placed on ground\nGood to simulate gunshots and lure people");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.SCORPION) + ".tooltip", "Slip in a bed to poison the next person looking for a rest");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.LOCKPICK) + ".tooltip", "Use on any locked door to open it (no cooldown)\nSneak-use on a door to jam it for 1 minute (3 minute cooldown)");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.CROWBAR) + ".tooltip", "Use on any door to open it permanently");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.BODY_BAG) + ".tooltip", "Use on a dead body to bag it up and remove it\nSingle use, 5 minute cooldown");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.BLACKOUT) + ".tooltip", "Turn off all lights aboard for 15 to 20 seconds\nUse your instinct [left-alt] to see your targets in the dark\nActivated instantly on purchase, 5 minute cooldown");
        builder.add(TextUtils.getItemTranslationKey(WatheItems.NOTE) + ".tooltip", "Write a message and pin it for others to see\nSneak-use to write a message, then use on a wall or floor to place\nInvisible in hand");

        // Walkie-talkie
        builder.add("screen.wathe.walkie_talkie.title", "Walkie-Talkie");
        builder.add("screen.wathe.walkie_talkie.channel", "Channel");
        builder.add("tooltip.wathe.walkie_talkie.channel", "Channel: %s");
        builder.add("tooltip.wathe.walkie_talkie.hold", "Hold in hand to transmit");
        builder.add("tooltip.wathe.walkie_talkie.right_click", "Right-click to switch channel");

        builder.add("game.win.killers", "The killers reached their kill count, they win!");
        builder.add("game.win.passengers", "All killers were eliminated: the passengers win!");
        builder.add("game.win.time", "The killers ran out of time: the passengers win!");
        builder.add("game.win.loose_end", "They tied all of their loose ends!");

        builder.add("key.wathe.instinct", "Instinct");
        builder.add("key.wathe.map_vote", "Map Vote");
        builder.add("category.wathe.keybinds", "Spark Murder Mystery");

        builder.add("task.feel", "You feel like ");
        builder.add("task.fake", "You could fake ");
        builder.add("task.sleep", "getting some sleep.");
        builder.add("task.outside", "getting some fresh air.");
        builder.add("task.drink", "getting a drink.");
        builder.add("task.eat", "getting a snack.");
        builder.add("game.player.stung", "You feel something stinging you in your sleep.");
        builder.add("game.blackout.countdown", "§c⚡ Someone has sabotaged the train's power supply!§r Automatic power restoration in %s seconds!");
        builder.add("game.psycho_mode.time", "Psycho Mode: %ss");
        builder.add("game.psycho_mode.text", "Kill them all!");
        builder.add("game.psycho_mode.over", "Psycho Mode Over!");
        builder.add("game.tip.cohort", "Killer's Cohort");
        builder.add("game.start_error.not_enough_players", "Game cannot start: %s players minimum are required.");
        builder.add("game.start_error.game_running", "Game cannot start: a game is already running. Please try again from the lobby.");
        builder.add("game.start_error.voting_active", "Game cannot start: map voting is active. Please wait for voting to finish.");

        builder.add("wathe.gui.reset", "Clear");

        builder.add("wathe.map_variables.help", """
                    gameModeAndMapEffect: The default game mode and map effect the map will use (with auto-start or when using the train horn).
                    spawnPos: The spawn position and orientation players will be reset to once the game ends.
                    spectatorSpawnPos: The spawn position and orientation players will be set to when set as spectators at the start of a game.
                    readyArea: The lobby area which players need to be in to be selected for a game.
                    snowflakeCollider: A box that removes snow particles for players playing on \'Snow particles mode: Box Collider\'.
                    playAreaOffset: The offset players will be teleported by from the ready area into the play area.
                    playArea: The play area outside which players will be eliminated.
                    resetTemplateArea: The template that will be copied over the play area in order to reset the map.
                    resetPasteOffset: The offset at which the template should be pasted.
                """);
        builder.add("wathe.map_variables.set", "Map variable %s successfully set to %s");

        builder.add("wathe.game_settings.help", """
                    weights: The role weight system that keeps track of the roles players got and favorizes players who did not get vigilante / killer yet to get those roles. Disable if you wish for full-random.
                    autoStart: Time before the game starts automatically when enough players are boarded in seconds.
                    backfire: When a civilian shoots an innocent, chance said civilian will shoot themselves instead. 0 (default) is 0%, 1 is 100%.
                    roleDividend: The amount of players for which 1 of the role will be chosen (default: 5 for both roles). E.g: 4 -> 1 in 4 players will be that role, meaning 2 of that role for 8 people, 3 for 12, etc...
                    bounds: Enable or disable game bounds that limit spectators to the play area.
                    shootInnocentPunishment: Set the punishment when a player shoots an innocent. Options: preventGunPickup (drops gun), killShooter (backfire).
                    enableRole: Enable or disable specific roles. Use 'listRoles' to see all available roles and their status.
                    listRoles: Display all roles with their factions and enabled/disabled status. Click on non-special roles to toggle them.
                """);

        builder.add("commands.supporter_only", "Super silly supporter commands are reserved for Patreon and YouTube members; if you wanna try them out, please consider supporting! <3");

        builder.add("wathe.midnightconfig.title", "The Harpy Express - Final Voyage - Config");
        builder.add("wathe.midnightconfig.ultraPerfMode", "Ultra Performance Mode");
        builder.add("wathe.midnightconfig.ultraPerfMode.tooltip", "Disables scenery for a worse visual experience but maximum performance. Lowers render distance to 2.");
        builder.add("wathe.midnightconfig.disableScreenShake", "Disable Screen Shake");
        builder.add("wathe.midnightconfig.snowOptLevel", "Snow particles mode");
        builder.add("wathe.midnightconfig.snowOptLevel.tooltip", "How snow particle collisions will be processed.\n\n'Box Collider' may not work on all maps.");
        builder.add("wathe.midnightconfig.enum.SnowModeConfig.NO_OPTIMIZATION", "Default");
        builder.add("wathe.midnightconfig.enum.SnowModeConfig.BOX_COLLIDER", "Box Collider");
        builder.add("wathe.midnightconfig.enum.SnowModeConfig.TURN_OFF", "No particles");
        builder.add("wathe.midnightconfig.snowflakeChance", "Snowflake Particle Count (%)");
        builder.add("wathe.midnightconfig.snowflakeChance.tooltip", "Controls the spawn chance of snowflake particles.\n0 = Off, 100 = Maximum.");

        builder.add("wathe.argument.game_mode.invalid", "Game mode could not be found");
        builder.add("wathe.argument.map_effect.invalid", "Map effect could not be found");

        builder.add("credits.wathe.thank_you", "Thank you for playing The Last Voyage of the Harpy Express!\nMe and my team spent a lot of time working\non this mod and we hope you enjoy it.\nIf you do and wish to make a video or stream\nplease make sure to credit my channel,\nvideo and the mod page!\n - RAT / doctor4t \n Spark Edition V @mod_version@ | QQ Group: 1043700021 ");

        // Faction announcements
        builder.add("announcement.result.losers", "Losers");

        // Force role commands
        builder.add("commands.wathe.forcerole.invalid", "Invalid role name");
        builder.add("commands.wathe.forcerole.query", "%s is forced to role: %s");
        builder.add("commands.wathe.forcerole.query.none", "%s is not forced to any role");
        builder.add("commands.wathe.forcerole.success", "Forced %s to %s");
        builder.add("commands.wathe.setenabledrole.invalid", "Invalid role name");
        builder.add("commands.wathe.setenabledrole.success", "Role %s is now %s");
        builder.add("commands.wathe.setenabledrole.enabled", "enabled");
        builder.add("commands.wathe.setenabledrole.disabled", "disabled");

        // Faction names
        builder.add("faction.wathe.none", "None");
        builder.add("faction.wathe.civilian", "Civilian");
        builder.add("faction.wathe.killer", "Killer");
        builder.add("faction.wathe.neutral", "Neutral");

        // List roles UI
        builder.add("commands.wathe.listroles.header", "Roles:");
        builder.add("commands.wathe.listroles.enabled", "ON");
        builder.add("commands.wathe.listroles.disabled", "OFF");
        builder.add("commands.wathe.listroles.click_to_toggle", "Click to toggle");
        builder.add("commands.wathe.listroles.click_to_enable", "Click to enable");
        builder.add("commands.wathe.listroles.click_to_disable", "Click to disable");

        // Shoot innocent punishment
        builder.add("commands.wathe.gamesettings.shootinnocentpunishment.success", "Shoot innocent punishment set to: %s");
        builder.add("commands.wathe.gamesettings.shootinnocentpunishment.preventgunpickup", "Prevent Gun Pickup");
        builder.add("commands.wathe.gamesettings.shootinnocentpunishment.killshooter", "Kill Shooter");

        // Shop errors
        builder.add("shop.error.not_available", "Shop Not Available");
        builder.add("shop.error.invalid_item", "Invalid Item");
        builder.add("shop.error.on_cooldown", "Item On Cooldown");
        builder.add("shop.error.out_of_stock", "Out Of Stock");
        builder.add("shop.error.purchase_denied", "Purchase Denied");
        builder.add("shop.error.purchase_failed", "Purchase Failed");

        // Letter tips
        builder.add("tip.letter.killer.tooltip1", "Thank you for taking this job.");
        builder.add("tip.letter.killer.tooltip2", "Please do so with the utmost discretion and do not get caught. Good luck.");
        builder.add("tip.letter.killer.tooltip3", "P.S.: Don't forget to use your instinct [Left Alt] and use the train's exterior to relocate.");
        builder.add("tip.letter.vigilante.tooltip1", "Intel suggests there is a killer aboard this train.");
        builder.add("tip.letter.vigilante.tooltip2", "Your mission: eliminate any threats and protect the civilians.");
        builder.add("tip.letter.veteran.tooltip1", "Your military experience will be crucial on this journey.");
        builder.add("tip.letter.veteran.tooltip2", "You have a knife with limited uses. Make each strike count.");

        // Mood HUD
        builder.add("hud.mood.breakdown_warning", "MENTAL BREAKDOWN IMMINENT!");

        // Death reasons
        builder.add("hud.body.death_info", "Died %ss ago from ");
        builder.add("death_reason.wathe.generic", "Unknown");
        builder.add("death_reason.wathe.knife_stab", "Knife Stab");
        builder.add("death_reason.wathe.gun_shot", "Gunshot");
        builder.add("death_reason.wathe.gun_shot_backfire", "Gun Backfire");
        builder.add("death_reason.wathe.bat_hit", "Bat Hit");
        builder.add("death_reason.wathe.grenade", "Grenade Explosion");
        builder.add("death_reason.wathe.poison", "Poison");
        builder.add("death_reason.wathe.fell_out_of_train", "Fell off the Train");
        builder.add("death_reason.wathe.escaped", "Escaped");
        builder.add("death_reason.wathe.shot_innocent", "Accidental Kill");
        builder.add("death_reason.wathe.mental_breakdown", "Mental Breakdown");
        builder.add("death_reason.wathe.drowned", "Drowned");

        // Drowning warnings
        builder.add("warning.wathe.drowning.mild", "You're struggling to breathe...");
        builder.add("warning.wathe.drowning.severe", "You're about to suffocate!");
        builder.add("warning.wathe.drowning.critical", "You're drowning!!");

        // Disconnect messages
        builder.add("disconnect.wathe.version_mismatch", "Mod version mismatch! Server: %s, Client: %s");
        builder.add("disconnect.wathe.not_in_ready_area", "The game has started, but you were not in the ready area, so you cannot join!");

        // Replay
        builder.add("replay.title", "=== Match Replay ===");
        builder.add("replay.footer", "--- Replay End ---");
        builder.add("replay.death.wathe.knife_stab.killed", "%s was assassinated by %s with a §eknife§r");
        builder.add("replay.death.wathe.knife_stab.died", "%s was assassinated with a §eknife§r");
        builder.add("replay.death.wathe.gun_shot.killed", "%s was shot by %s with a §egun§r");
        builder.add("replay.death.wathe.gun_shot.died", "%s was shot with a §egun§r");
        builder.add("replay.death.wathe.gun_shot_backfire.died", "%s's §egun§r backfired");
        builder.add("replay.death.wathe.bat_hit.killed", "%s was killed by %s with a §ebat§r");
        builder.add("replay.death.wathe.bat_hit.died", "%s was killed with a §ebat§r");
        builder.add("replay.death.wathe.grenade.killed", "%s was blown up by %s's §egrenade§r");
        builder.add("replay.death.wathe.grenade.died", "%s was blown up by a §egrenade§r");
        builder.add("replay.death.wathe.poison.killed", "%s was §2poisoned§r by %s");
        builder.add("replay.death.wathe.poison.died", "%s died from §2poison§r");
        builder.add("replay.death.wathe.fell_out_of_train.killed", "%s was §7pushed§r off the train by %s");
        builder.add("replay.death.wathe.fell_out_of_train.died", "%s §7fell§r off the train");
        builder.add("replay.death.wathe.escaped.died", "%s §7escaped§r the game");
        builder.add("replay.death.wathe.shot_innocent.killed", "%s was §ckilled by mistake§r by %s");
        builder.add("replay.death.wathe.shot_innocent.died", "%s died from §ckilling an innocent§r");
        builder.add("replay.death.wathe.mental_breakdown.died", "%s suffered a §cmental breakdown§r");
        builder.add("replay.death.wathe.vanilla_death.died", "%s died in an §caccident§r");
        builder.add("replay.death.wathe.drowned.died", "%s §1drowned§r");
        builder.add("replay.death.unknown.killed", "%s was killed by %s");
        builder.add("replay.death.unknown.died", "%s died");
        builder.add("replay.shop_purchase", "%s purchased %s for §6%d§r coin");
        builder.add("replay.item_pickup", "%s picked up %s");
        builder.add("replay.item_pickup.multiple", "%s picked up %s x%d");
        builder.add("replay.poisoned.by", "%s was §2poisoned§r by %s");
        builder.add("replay.poisoned", "%s is §2poisoned§r");
        builder.add("replay.poisoned.wathe.food.by", "%s consumed food/drink §2poisoned§r by %s");
        builder.add("replay.poisoned.wathe.food", "%s consumed §2poisoned§r food/drink");
        builder.add("replay.poisoned.wathe.bed.by", "%s was stung by a scorpion §2placed§r by %s");
        builder.add("replay.poisoned.wathe.bed", "%s was §2stung by a scorpion§r while sleeping");
        builder.add("replay.platter_take.poisoned", "%1$s took %2$s §2poisoned§r by %3$s from the platter");
        builder.add("replay.skill.unknown.target", "%s used a §bskill§r on %s");
        builder.add("replay.skill.unknown", "%s used a §bskill§r");
        builder.add("replay.global.unknown", "A §dglobal event§r occurred");

        // Replay shield blocked
        builder.add("replay.shield_blocked.wathe.psycho_mode.by", "%s's §dPsycho Mode shield§r blocked %s's attack");
        builder.add("replay.shield_blocked.wathe.psycho_mode", "%s's §dPsycho Mode shield§r blocked fatal damage");
        builder.add("replay.shield_blocked.by", "%s's §eshield§r blocked %s's attack");
        builder.add("replay.shield_blocked", "%s's §eshield§r blocked fatal damage");

        // Map voting GUI
        builder.add("gui.wathe.map_voting.title", "Select Next Map");
        builder.add("gui.wathe.map_voting.selecting", "Selecting map...");
        builder.add("gui.wathe.map_voting.votes", "%s votes");
        builder.add("gui.wathe.map_voting.my_vote", "Voted");
        builder.add("gui.wathe.map_voting.player_range", "%1$s-%2$s players");
        builder.add("gui.wathe.map_voting.players", "Players online: %s");
        builder.add("gui.wathe.map_voting.voted_info", "Votes %s / Online %s");
        builder.add("gui.wathe.map_voting.unavailable", "Unavailable");
        builder.add("gui.wathe.map_voting.unavailable.min_players", "Requires at least %s players");
        builder.add("gui.wathe.map_voting.unavailable.max_players", "Maximum %s players");
    }
}
