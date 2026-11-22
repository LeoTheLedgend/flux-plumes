package dev.leo.fluxplumes.client;

import dev.alyti.fluxnautics.client.render.ThrusterCompartmentCache;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;

import java.util.Objects;

/**
 * How hot each plume has got since it was lit, 0..1. A fresh ignition starts at 0 (rocket-like flame) and heats up
 * to 1 (full afterburner with diamonds); it cools while the thruster is off. Render thread only.
 */
public final class PlumeHeat {
    /** Heat of the compartment being drawn right now; uploaded as uFPHeat. */
    public static float current;

    /** Seconds from ignition to fully hot. */
    private static final double RISE = 3.5;
    /** Seconds from fully hot to cold once the thruster stops. */
    private static final double FALL = 1.5;

    private static final Int2ObjectOpenHashMap<State> STATES = new Int2ObjectOpenHashMap<>();

    private static final class State {
        float heat;
        double last = -1;
    }

    private PlumeHeat() {}

    public static float update(ThrusterCompartmentCache.Compartment compartment, float length, float span) {
        float thrust = length / Math.max(span, 1f);
        State s = STATES.computeIfAbsent(Objects.hash(compartment.anchor.pos, compartment.groupId), k -> new State());
        double now = System.nanoTime() / 1.0e9;
        if (s.last < 0) {
            s.last = now;
        } else {
            double dt = now - s.last;
            if (dt >= 0.002) {
                if (dt > 0.5) {
                    // Nothing was drawn for a while: the thruster was off (or out of view), so it cooled down.
                    s.heat = (float) Math.max(0.0, s.heat - dt / FALL);
                } else if (thrust > 0.12f) {
                    s.heat = (float) Math.min(1.0, s.heat + dt / RISE);
                } else {
                    s.heat = (float) Math.max(0.0, s.heat - dt / FALL);
                }
                s.last = now;
            }
        }
        current = s.heat;
        return s.heat;
    }
}
