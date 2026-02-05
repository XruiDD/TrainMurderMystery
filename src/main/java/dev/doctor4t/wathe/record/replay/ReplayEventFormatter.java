package dev.doctor4t.wathe.record.replay;

import dev.doctor4t.wathe.record.GameRecordEvent;
import dev.doctor4t.wathe.record.GameRecordManager;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.text.Text;
import org.jetbrains.annotations.Nullable;

/**
 * 回放事件格式化器接口
 * 用于将游戏记录事件转换为玩家可读的文本
 */
@FunctionalInterface
public interface ReplayEventFormatter {
    /**
     * 将事件格式化为可读文本
     *
     * @param event 游戏记录事件
     * @param match 对局记录
     * @param world 服务器世界
     * @return 格式化后的文本，如果返回 null 则跳过该事件
     */
    @Nullable
    Text format(GameRecordEvent event, GameRecordManager.MatchRecord match, ServerWorld world);
}
