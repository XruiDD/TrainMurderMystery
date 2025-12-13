package dev.doctor4t.trainmurdermystery.cca;

import dev.doctor4t.trainmurdermystery.TMM;
import dev.doctor4t.trainmurdermystery.config.area.AreaConfiguration.FogConfig;
import dev.doctor4t.trainmurdermystery.config.area.AreaConfiguration.SnowParticlesConfig;
import dev.doctor4t.trainmurdermystery.config.area.AreaConfiguration.VisualConfig;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.util.StringIdentifiable;
import net.minecraft.world.World;
import org.ladysnake.cca.api.v3.component.ComponentKey;
import org.ladysnake.cca.api.v3.component.ComponentRegistry;
import org.ladysnake.cca.api.v3.component.sync.AutoSyncedComponent;
import org.ladysnake.cca.api.v3.component.tick.ClientTickingComponent;
import org.ladysnake.cca.api.v3.component.tick.ServerTickingComponent;

public class TrainWorldComponent implements AutoSyncedComponent, ServerTickingComponent, ClientTickingComponent {
    public static final ComponentKey<TrainWorldComponent> KEY = ComponentRegistry.getOrCreate(TMM.id("train"), TrainWorldComponent.class);

    private final World world;
    private int speed = 0; // im km/h
    private int time = 0;
    private boolean snow = true;
    private boolean fog = true;
    private boolean hud = true;
    private TimeOfDay timeOfDay = TimeOfDay.NIGHT;

    public TrainWorldComponent(World world) {
        this.world = world;
        // 使用硬编码默认值，游戏开始时会从 datapack 配置加载
    }

    private void sync() {
        TrainWorldComponent.KEY.sync(this.world);
    }

    public void setSpeed(int speed) {
        this.speed = speed;
        this.sync();
    }

    public int getSpeed() {
        return speed;
    }

    public float getTime() {
        return time;
    }

    public void setTime(int time) {
        this.time = time;
        this.sync();
    }

    public boolean isSnowing() {
        return snow;
    }

    public void setSnow(boolean snow) {
        this.snow = snow;
        this.sync();
    }

    public boolean isFoggy() {
        return fog;
    }

    public void setFog(boolean fog) {
        this.fog = fog;
        this.sync();
    }

    public boolean hasHud() {
        return hud;
    }

    public void setHud(boolean hud) {
        this.hud = hud;
        this.sync();
    }

    public TimeOfDay getTimeOfDay() {
        return timeOfDay;
    }

    public void setTimeOfDay(TimeOfDay timeOfDay) {
        this.timeOfDay = timeOfDay;
        this.sync();
    }

    @Override
    public void readFromNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        this.setSpeed(nbtCompound.getInt("Speed"));
        this.setTime(nbtCompound.getInt("Time"));
        this.setSnow(nbtCompound.getBoolean("Snow"));
        this.setFog(nbtCompound.getBoolean("Fog"));
        this.setHud(nbtCompound.getBoolean("Hud"));
        this.setTimeOfDay(parseTimeOfDaySafe(nbtCompound.getString("TimeOfDay")));
    }

    /**
     * 安全地解析 TimeOfDay 字符串，无效值返回默认值 NIGHT
     */
    private static TimeOfDay parseTimeOfDaySafe(String value) {
        if (value == null || value.isEmpty()) {
            return TimeOfDay.NIGHT;
        }
        try {
            return TimeOfDay.valueOf(value);
        } catch (IllegalArgumentException e) {
            TMM.LOGGER.warn("Invalid TimeOfDay value: '{}', using default NIGHT", value);
            return TimeOfDay.NIGHT;
        }
    }

    @Override
    public void writeToNbt(NbtCompound nbtCompound, RegistryWrapper.WrapperLookup wrapperLookup) {
        nbtCompound.putInt("Speed", speed);
        nbtCompound.putInt("Time", time);
        nbtCompound.putBoolean("Snow", snow);
        nbtCompound.putBoolean("Fog", fog);
        nbtCompound.putBoolean("Hud", hud);
        nbtCompound.putString("TimeOfDay", timeOfDay.name());
    }

    @Override
    public void clientTick() {
        tickTime();
    }

    private void tickTime() {
        if (speed > 0) {
            time++;
        } else {
            time = 0;
        }
    }

    @Override
    public void serverTick() {
        tickTime();

        ServerWorld serverWorld = (ServerWorld) world;
        serverWorld.setTimeOfDay(timeOfDay.time);
    }

    public void reset() {
        // 从 datapack 配置读取默认值
        AreasWorldComponent areas = AreasWorldComponent.KEY.get(this.world);
        SnowParticlesConfig snowConfig = areas.getSnowParticlesConfig();
        FogConfig fogConfig = areas.getFogConfig();
        VisualConfig visualConfig = areas.getVisualConfig();

        this.snow = snowConfig.enabled();
        this.fog = fogConfig.enabled();
        this.hud = visualConfig.hud();
        this.speed = visualConfig.trainSpeed();
        this.timeOfDay = parseTimeOfDaySafe(visualConfig.timeOfDay());
        this.time = 0;
        this.sync();
    }

    public enum TimeOfDay implements StringIdentifiable {
        DAY(6000),
        NIGHT(18000),
        SUNDOWN(12800);

        final int time;

        TimeOfDay(int time) {
            this.time = time;
        }

        @Override
        public String asString() {
            return this.name();
        }
    }

}
