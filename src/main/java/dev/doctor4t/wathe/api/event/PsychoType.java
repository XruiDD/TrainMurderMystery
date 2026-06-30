package dev.doctor4t.wathe.api.event;

/**
 * 疯魔模式的可见性类型。
 * <p>
 * {@link #tracksCounter} — 是否计入全局 {@code psychosActive} 计数器。
 * 该计数器驱动客户端默认 Drone BGM（{@code AMBIENT_PSYCHO_DRONE}），
 * 表示「是否还有公开疯魔在进行」。
 * <p>
 * 事件 {@link PsychoModeEvents#ON_PSYCHO_START} / {@link PsychoModeEvents#ON_PSYCHO_END}
 * 对所有类型均派发（包括 {@link #SILENT}），由监听者自行按需过滤。
 */
public enum PsychoType {
    /** 公开疯魔：默认 Drone BGM。蝙蝠、商店疯魔药。 */
    PUBLIC(true),
    /** 可见静默：无默认 BGM（调用方自备，如 JESTER_MOMENT）。小丑疯魔时刻。 */
    VISIBLE_QUIET(false),
    /**
     * 静默疯魔：无 BGM（不计数器）。静语者静默疯魔。
     * <p>注意：猎魔人仍会因此<b>拿到猎魔枪</b>且可击杀该玩家；其「隐身」仅指
     * 客户端不对静语者做猎魔人透视高亮（见 NoellesrolesClient 的高亮跳过），
     * 即「能武装、可击杀，但无位置指引」。
     */
    SILENT(false);

    public final boolean tracksCounter;

    PsychoType(boolean tracksCounter) {
        this.tracksCounter = tracksCounter;
    }
}
