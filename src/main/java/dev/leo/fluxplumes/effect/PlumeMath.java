package dev.leo.fluxplumes.effect;

/**
 * Pure maths for the effects, kept free of Minecraft classes so it is easy to unit test and to mirror in GLSL.
 */
public final class PlumeMath {
    private PlumeMath() {}

    /**
     * Shock diamond brightness (0..1) at a distance along the plume axis.
     *
     * @param distance  blocks from the nozzle along the exhaust direction
     * @param nozzleDia nozzle diameter in blocks (sets the diamond spacing scale)
     * @param thrust    0..1 throttle; diamonds sharpen and spread out as thrust rises, and vanish at idle
     */
    public static float shockDiamond(float distance, float nozzleDia, float thrust) {
        if (thrust <= 0.05f || distance < 0f) return 0f;
        float spacing = nozzleDia * (1.2f + 1.8f * thrust);
        float phase = (float) (distance / spacing * Math.PI * 2.0);
        float wave = 0.5f + 0.5f * (float) Math.cos(phase);           // 1 at each diamond centre
        float sharp = (float) Math.pow(wave, 3.0 + 3.0 * thrust);    // narrower diamonds at high thrust
        float decay = (float) Math.exp(-distance / (spacing * 6.0f)); // fade down the plume
        float gate = Math.min(1f, (thrust - 0.05f) / 0.3f);           // fade in from idle
        return sharp * decay * gate;
    }

    /** 0..1 strength of the ground-smoke effect, given the raycast hit distance and the max range. */
    public static float groundProximity(float hitDistance, float range) {
        if (range <= 0f || hitDistance >= range) return 0f;
        float t = 1f - Math.max(0f, hitDistance) / range;
        return t * t;
    }

    /** Linear blend of two 0xRRGGBB colours. */
    public static int lerpColor(int a, int b, float t) {
        t = Math.max(0f, Math.min(1f, t));
        int r = (int) (((a >> 16) & 0xFF) * (1 - t) + ((b >> 16) & 0xFF) * t);
        int g = (int) (((a >> 8) & 0xFF) * (1 - t) + ((b >> 8) & 0xFF) * t);
        int bl = (int) ((a & 0xFF) * (1 - t) + (b & 0xFF) * t);
        return (r << 16) | (g << 8) | bl;
    }
}
