package dev.doctor4t.trainmurdermystery.game;

public interface GameConstants {
    int DOOR_AUTOCLOSE_TIME = getInTicks(0, 5);
    int KNIFE_COOLDOWN = getInTicks(3, 0);
    int JAMMED_DOOR_TIME = getInTicks(1, 0);
    int LOCKPICK_JAM_COOLDOWN = getInTicks(5, 0);

    static int getInTicks(int minutes, int seconds) {
        return (minutes * 60 + seconds) * 20;
    }
}
