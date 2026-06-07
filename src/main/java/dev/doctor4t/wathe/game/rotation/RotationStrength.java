package dev.doctor4t.wathe.game.rotation;

/**
 * 轮换强度档位。OFF=纯随机（回滚用）。
 * 权重 = clamp(exp(lambda * debt), wMin, wMax)。参数见 spec §6.2，由仿真定标。
 */
public enum RotationStrength {
    OFF(0.0, 0.0, 0.0),
    LIGHT(1.0, 0.6, 1.5),
    MID(1.5, 0.4, 2.0),
    STRONG(2.0, 0.2, 4.0);

    private final double lambda;
    private final double wMin;
    private final double wMax;

    RotationStrength(double lambda, double wMin, double wMax) {
        this.lambda = lambda;
        this.wMin = wMin;
        this.wMax = wMax;
    }

    public double lambda() { return lambda; }
    public double wMin() { return wMin; }
    public double wMax() { return wMax; }
    public boolean isEnabled() { return this != OFF; }
}
