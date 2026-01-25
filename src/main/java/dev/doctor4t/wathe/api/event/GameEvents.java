package dev.doctor4t.wathe.api.event;

import dev.doctor4t.wathe.api.GameMode;
import dev.doctor4t.wathe.cca.GameWorldComponent;
import dev.doctor4t.wathe.game.GameFunctions;
import net.fabricmc.fabric.api.event.Event;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.world.World;
import org.jetbrains.annotations.Nullable;

import static net.fabricmc.fabric.api.event.EventFactory.createArrayBacked;

public final class GameEvents {

    private GameEvents() {
    }

    public static final Event<OnGameStart> ON_GAME_START = createArrayBacked(OnGameStart.class, listeners -> (gameMode) -> {
        for (OnGameStart listener : listeners) {
            listener.onGameStart(gameMode);
        }
    });

    public static final Event<OnGameStop> ON_GAME_STOP = createArrayBacked(OnGameStop.class, listeners -> (gameMode) -> {
        for (OnGameStop listener : listeners) {
            listener.onGameStop(gameMode);
        }
    });

    public static final Event<OnFinishInitialize> ON_FINISH_INITIALIZE = createArrayBacked(OnFinishInitialize.class, listeners -> (world, gameWorldComponent) -> {
        for (OnFinishInitialize listener : listeners) {
            listener.onFinishInitialize(world, gameWorldComponent);
        }
    });

    public static final Event<OnFinishFinalize> ON_FINISH_FINALIZE = createArrayBacked(OnFinishFinalize.class, listeners -> (world, gameWorldComponent) -> {
        for (OnFinishFinalize listener : listeners) {
            listener.onFinishFinalize(world, gameWorldComponent);
        }
    });

    /**
     * Called after win condition is determined but BEFORE setRoundEndData is called.
     * This allows mods to react to the final winner information before the round end data is set.
     */
    public static final Event<OnWinDetermined> ON_WIN_DETERMINED = createArrayBacked(OnWinDetermined.class, listeners -> (world, gameComponent, winStatus, neutralWinner) -> {
        for (OnWinDetermined listener : listeners) {
            listener.onWinDetermined(world, gameComponent, winStatus, neutralWinner);
        }
    });

    public interface OnGameStart {
        void onGameStart(GameMode gameMode);
    }

    public interface OnGameStop {
        void onGameStop(GameMode gameMode);
    }

    public interface OnFinishInitialize {
        void onFinishInitialize(World world, GameWorldComponent gameComponent);
    }

    public interface OnFinishFinalize {
        void onFinishFinalize(World world, GameWorldComponent gameComponent);
    }

    /**
     * Called after win condition is determined but before round end data is set.
     */
    public interface OnWinDetermined {
        /**
         * Called when a winner has been determined.
         *
         * @param world         the server world
         * @param gameComponent the game world component
         * @param winStatus     the determined win status (KILLERS, PASSENGERS, TIME, NEUTRAL, etc.)
         * @param neutralWinner the neutral player who won (only non-null if winStatus is NEUTRAL)
         */
        void onWinDetermined(ServerWorld world, GameWorldComponent gameComponent, GameFunctions.WinStatus winStatus, @Nullable ServerPlayerEntity neutralWinner);
    }
}
