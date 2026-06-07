package dev.doctor4t.wathe.game.rotation;

import java.util.ArrayList;
import java.util.List;
import java.util.function.DoubleSupplier;
import java.util.function.Predicate;
import java.util.function.ToDoubleFunction;

/**
 * 公平轮换纯算法。禁止依赖 Minecraft —— 随机源用 DoubleSupplier(产出 [0,1) 均匀值)解耦。
 */
public final class RoleRotation {
    private RoleRotation() {}

    /** debt = Σ 窗口内应得份额 − 实际落入该桶的局数。正=被亏待→加权，负=多当→降权。 */
    public static double debt(Iterable<GameEntry> window, RoleCategory category) {
        double expected = 0.0;
        int actual = 0;
        for (GameEntry e : window) {
            expected += e.shareFor(category);
            if (e.category() == category) actual++;
        }
        return expected - actual;
    }

    /** 权重 = clamp(exp(λ·debt), wMin, wMax)；OFF 档恒返回 1.0。 */
    public static double weight(double debt, RotationStrength strength) {
        if (!strength.isEnabled()) return 1.0;
        double w = Math.exp(strength.lambda() * debt);
        if (w < strength.wMin()) return strength.wMin();
        if (w > strength.wMax()) return strength.wMax();
        return w;
    }

    /**
     * 加权无放回抽样：从 pool 选 count 个，被选概率正比于权重。
     * Efraimidis–Spirakis：key = U^(1/w)，取 key 最大的 count 个。
     * 权重全相等时退化为均匀随机（OFF 档即此情形）。
     */
    public static <P> List<P> selectWeighted(List<P> pool, ToDoubleFunction<P> weightOf,
                                             int count, DoubleSupplier uniform01) {
        int take = Math.min(Math.max(count, 0), pool.size());
        List<P> result = new ArrayList<>(take);
        if (take == 0) return result;

        int n = pool.size();
        double[] keys = new double[n];
        Integer[] idx = new Integer[n];
        for (int i = 0; i < n; i++) {
            double w = Math.max(weightOf.applyAsDouble(pool.get(i)), 1e-9);
            double u = uniform01.getAsDouble();
            keys[i] = Math.pow(u, 1.0 / w);
            idx[i] = i;
        }
        java.util.Arrays.sort(idx, (a, b) -> Double.compare(keys[b], keys[a])); // key 降序
        for (int i = 0; i < take; i++) result.add(pool.get(idx[i]));
        return result;
    }

    /**
     * 从 available 里挑一个尽量"最近没用过"的元素：优先非 recent；若全是 recent 则退回全集。
     * 不修改 available；调用方负责移除返回值。available 为空返回 null。
     */
    public static <R> R pickAvoidingRecent(List<R> available, Predicate<R> isRecent,
                                           DoubleSupplier uniform01) {
        if (available.isEmpty()) return null;
        List<R> fresh = new ArrayList<>();
        for (R r : available) {
            if (!isRecent.test(r)) fresh.add(r);
        }
        List<R> pickFrom = fresh.isEmpty() ? available : fresh;
        int i = (int) (uniform01.getAsDouble() * pickFrom.size());
        if (i >= pickFrom.size()) i = pickFrom.size() - 1; // 防 u 恰为 1.0
        return pickFrom.get(i);
    }
}
