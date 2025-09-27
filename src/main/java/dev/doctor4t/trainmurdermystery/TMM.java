package dev.doctor4t.trainmurdermystery;

import com.google.common.reflect.Reflection;
import dev.doctor4t.trainmurdermystery.command.*;
import dev.doctor4t.trainmurdermystery.index.*;
import dev.doctor4t.trainmurdermystery.util.*;
import net.fabricmc.api.ModInitializer;
import net.fabricmc.fabric.api.command.v2.CommandRegistrationCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.entity.Entity;
import net.minecraft.util.Identifier;
import net.minecraft.util.math.BlockPos;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class TMM implements ModInitializer {
    public static final String MOD_ID = "trainmurdermystery";
    public static final Logger LOGGER = LoggerFactory.getLogger(MOD_ID);

    public static Identifier id(String name) {
        return Identifier.of(MOD_ID, name);
    }

    @Override
    public void onInitialize() {
        // Registry initializers
        Reflection.initialize(TMMDataComponentTypes.class);
        TMMSounds.initialize();
        TMMEntities.initialize();
        TMMBlocks.initialize();
        TMMItems.initialize();
        TMMBlockEntities.initialize();
        TMMParticles.initialize();

        // Register commands
        CommandRegistrationCallback.EVENT.register(((dispatcher, registryAccess, environment) -> {
            GiveRoomKeyCommand.register(dispatcher);
            SetTrainSpeedCommand.register(dispatcher);
            StartCommand.register(dispatcher);
            StopCommand.register(dispatcher);
            TestStartGameCommand.register(dispatcher);
            ForceStop.register(dispatcher);
        }));

        PayloadTypeRegistry.playS2C().register(ShootMuzzleS2CPayload.ID, ShootMuzzleS2CPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(PoisonUtils.PoisonOverlayPayload.ID, PoisonUtils.PoisonOverlayPayload.CODEC);
        PayloadTypeRegistry.playS2C().register(GunDropPayload.ID, GunDropPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(KnifeStabPayload.ID, KnifeStabPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(GunShootPayload.ID, GunShootPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(StoreBuyPayload.ID, StoreBuyPayload.CODEC);
        PayloadTypeRegistry.playC2S().register(NoteEditPayload.ID, NoteEditPayload.CODEC);
        ServerPlayNetworking.registerGlobalReceiver(KnifeStabPayload.ID, new KnifeStabPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(GunShootPayload.ID, new GunShootPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(StoreBuyPayload.ID, new StoreBuyPayload.Receiver());
        ServerPlayNetworking.registerGlobalReceiver(NoteEditPayload.ID, new NoteEditPayload.Receiver());

        Scheduler.init();
    }

    public static boolean isSkyVisibleAdjacent(@NotNull Entity player) {
        var mutable = new BlockPos.Mutable();
        var playerPos = BlockPos.ofFloored(player.getEyePos());
        for (var x = -1; x <= 1; x += 2) {
            for (var z = -1; z <= 1; z += 2) {
                mutable.set(playerPos.getX() + x, playerPos.getY(), playerPos.getZ() + z);
                if (player.getWorld().isSkyVisible(mutable)) {
                    return true;
                }
            }
        }
        return false;
    }

    public static boolean isExposedToWind(@NotNull Entity player) {
        var mutable = new BlockPos.Mutable();
        var playerPos = BlockPos.ofFloored(player.getEyePos());
        for (var x = 0; x <= 10; x++) {
            mutable.set(playerPos.getX() - x, player.getEyePos().getY(), playerPos.getZ());
            if (!player.getWorld().isSkyVisible(mutable)) {
                return false;
            }
        }
        return true;
    }
}

// TORECORD: Fixing the mood system
// TORECORD: Better tasks: mood goes down gradually, completing tasks is a single action to bring it back up
//  (new task system is more meant to make players vulnerable to the hitman in a different way from splitting them up)
// TORECORD: - Get a snack from restaurant task (food platter block + food items)
// TORECORD: - Get a drink from the bar task (drink tray block + custom drink items)
// TORECORD: - Sleeping task requiring you to sleep for 8s
// TORECORD: - Get some fresh air reduced to going walking outside for 8s
// TODO: - Change mood down effect from speed to: (to prevent players being able to innocent each other on an easily observable change)
// TODO:    - Mid mood: Imagining random items in other player's hands (changes every so often) + a little more shake
// TODO:    - Low mood: Randomly seeing players as psycho + not seeing bodies + even more shake

// TORECORD: Fixing the detective
// TORECORD: New name display system to allow anyone to know player's names
// TORECORD: Remove revolver bullet count but make detectives drop the gun on innocent kill
//              to prevent detectives gunning down people and giving more weight to the choice as well as offer a chance to other players to make decisions
//              also sets mood to 0 as extra punishment
//              Cannot be picked up by: The person who shot wrongly or killers, if you already have a revolver
//              but then can't people force the hitman to try and pick up the gun to see if they are one? No I'll explain why later
// TORECORD: Revolver also has a really nice indicator if you are going to hit your target
// TORECORD: Make the detective drop the gun on killed (that the hitman cannot pick up, to prevent soft locking)
// TORECORD: Also removed cooldown in creative LMAO
// done: Remove body bags so make player corpses turn into skeletons after some time (since the detective role is no longer really a role and depends on who carries the gun, it's hard to keep the body bag item)

// TORECORD: Fixing the hitman
// TORECORD: Mood system shows up for the hitman as well, custom mood icon indicating no effect, but here to suggest how to play along and fake tasks
// TORECORD: Remove target system and make the win condition a kill count, turning him into a psycho
// TORECORD: New name display shows "psycho cohort" for other psychos, and instinct now shows other psychos instead of targets, game recognizes game, allowing them to scheme together
// TORECORD: You'll notice the killer doesn't start with the knife or the lockpick, because ITEM SHOP. Idea is we're gonna give the killer a bunch of tools and you are free to play how you want! And there's gonna be a lot of options
// TORECORD: - Fixing the knife (now with a kill indicator)
// TORECORD: - Grenade for clumped up people (foils the grouping up cheese)
//              Does not kill the hitman (idk they have shrapnel body armor or something)
//              Detail of the thrown grenade not having the pin or the handle
// TORECORD: - Poison (poisons the next food or drink item)
// TORECORD: - Scorpion (poisons the next person sleeping in the bed)
// TORECORD:    Getting food poisoned and then scorpion poisoned lowers the timer
// TORECORD: - Revolver
//          allows the hitman to potentially pass as a detective / passenger with a gun on top of giving a ranged option
//          double edged sword because just like for other passengers, you drop it when you shoot an innocent, which means that there is now one more gun in circulation people can use against you
//          counter to people trying to make you pick up the gun and confirm you are the hitman because remember you can't pick it up if you already have a revolver
//              so the killer can buy it and bluff that they already have one and use it as a justification
// TORECORD: - Crowbar (perma opening a door should be a hitman ability, allows for creative kills where you can push off players from train doors, as well as allowing passengers to use the exterior in order to give plausible deniability to hitmen using it to relocate)
// TORECORD: - Psycho mode (wanted to have an anonymous killer originally for the horror element, this also allows the hitman to go crazy how some wanted to)
//              Ambience can be heard by all players, so you know when to run. Also gives a use to rooms as you can hide in them
// TORECORD: - Light turn off item + true darkness (increases the horror aspect + amazing scenario of lights turning off and someone being dead when they turn back on + blackout and psycho mode)
//              True darkness doesn't work that well because of skylight seeping into the block, add a light barrier block that blocks it from entering
//              Disable name renderer for the duration of the blackout to prevent people seeing in the dark
// TORECORD: - Firecracker (luring people, shooting the gun in spectator often led to people rushing in from curiosity, allowing the hitman to manipulate players)
// TORECORD: - Body bag (can be used to clean up a kill, but very expensive)
// TORECORD: - Note (allows the hitman to leave messages, fun for encouraging the roleplay aspect)

// TORECORD: Fixing the map
// TODO: Rearrange the train cars to prevent all POIs being separated by all the sleeping cars
// TODO: Add light barrier blocks to the whole train
// TORECORD: Add barrier panel blocks to the lobby

// TORECORD: Polish
// TODO: Train chimney smoke + ringable horn, triggers game start in lobby and end of game
// TORECORD: Players collide with each other (Amy) + let's you step on them
// TODO: Louder footsteps
// TORECORD: System that remembers previous roles and allows cycling of roles (Amy)

// TODO:
// - Item shop sounds
// - Disable feature renderers on psychos