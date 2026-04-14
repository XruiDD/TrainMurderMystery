package dev.doctor4t.wathe.api.event;

/**
 * 疯魔模式的可见性类型。
 * <p>
 * {@link #tracksCounter} — 是否计入全局 {@code psychosActive} 计数器。
 * 该计数器驱动客户端默认 Drone BGM（{@code AMBIENT_PSYCHO_DRONE}），
 * 也被猎魔者等监听者用来判断「是否还有公开疯魔在进行」。
 * <p>
 * 事件 {@link PsychoModeEvents#ON_PSYCHO_START} / {@link PsychoModeEvents#ON_PSYCHO_END}
 * 对所有类型均派发（包括 {@link #SILENT}），由监听者自行按需过滤。
 */
public enum PsychoType {
    /** 公开疯魔：默认 Drone BGM。蝙蝠、商店疯魔药。 */
    PUBLIC(true),
    /** 可见静默：无默认 BGM（调用方自备，如 JESTER_MOMENT）。小丑疯魔时刻。 */
    VISIBLE_QUIET(false),
    /** 完全隐身：无 BGM，猎魔者等默认监听者应忽略。静语者静默疯魔。 */
    SILENT(false);

    public final boolean tracksCounter;

    PsychoType(boolean tracksCounter) {
        this.tracksCounter = tracksCounter;
    }
}
