package dev.leo.fluxplumes.client;

import dev.leo.fluxplumes.FluxPlumes;
import dev.leo.fluxplumes.PlumeStyle;
import it.unimi.dsi.fastutil.ints.Int2ObjectOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.AbstractTickableSoundInstance;
import net.minecraft.client.resources.sounds.SoundInstance;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.neoforged.neoforge.registries.DeferredRegister;

import java.util.function.Supplier;

/** Looping thruster sounds keyed by plume style, managed per compartment. */
public final class PlumeSound {
    public static final DeferredRegister<SoundEvent> REGISTER =
            DeferredRegister.create(Registries.SOUND_EVENT, FluxPlumes.MODID);

    public static final Supplier<SoundEvent> ROCKET = REGISTER.register("rocket", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(FluxPlumes.MODID, "rocket")));
    public static final Supplier<SoundEvent> AFTERBURNER = REGISTER.register("afterburner", () -> SoundEvent.createVariableRangeEvent(
            ResourceLocation.fromNamespaceAndPath(FluxPlumes.MODID, "afterburner")));

    /** Seconds for the spool-up ramp from silence to full volume. */
    private static final float FADE_IN_SECONDS = 0.8f;
    /** Seconds for the spool-down ramp from full volume to silence. */
    private static final float FADE_OUT_SECONDS = 1.2f;

    private static final Int2ObjectOpenHashMap<LoopingSound> ACTIVE = new Int2ObjectOpenHashMap<>();
    private static final it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap LAST_SEEN = new it.unimi.dsi.fastutil.ints.Int2LongOpenHashMap();

    static {
        LAST_SEEN.defaultReturnValue(-1L);
    }

    private PlumeSound() {}

    public static SoundEvent forStyle(PlumeStyle style) {
        return switch (style) {
            case ROCKET, DEFAULT, FLARE -> ROCKET.get();
            case SR71 -> AFTERBURNER.get();
        };
    }

    /**
     * Call each frame while the compartment is firing. When {@code active} flips to false the
     * sound is not cut off: it spools down over {@link #FADE_OUT_SECONDS} and removes itself.
     */
    public static void update(int key, SoundEvent event, double x, double y, double z, boolean active) {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null || event == null) {
            return;
        }
        if (!active) {
            var inst = ACTIVE.get(key);
            if (inst != null) {
                inst.beginFadeOut();
            }
            LAST_SEEN.remove(key);
            return;
        }
        LAST_SEEN.put(key, System.nanoTime());
        var inst = ACTIVE.get(key);
        if (inst == null || inst.isStopped()) {
            if (inst != null) {
                ACTIVE.remove(key);
            }
            inst = new LoopingSound(event, x, y, z, () -> ACTIVE.remove(key));
            mc.getSoundManager().play(inst);
            ACTIVE.put(key, inst);
        } else {
            inst.beginFadeIn();
            inst.moveTo(x, y, z);
        }
    }

    /** Starts the spool-down for sounds whose compartment stopped rendering for more than a second. */
    public static void sweep() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) {
            return;
        }
        long now = System.nanoTime();
        var it = ACTIVE.int2ObjectEntrySet().iterator();
        while (it.hasNext()) {
            var e = it.next();
            int key = e.getIntKey();
            LoopingSound inst = e.getValue();
            if (inst.isStopped()) {
                it.remove();
                LAST_SEEN.remove(key);
                continue;
            }
            long last = LAST_SEEN.get(key);
            if (last < 0 || now - last > 1_000_000_000L) {
                inst.beginFadeOut();
                LAST_SEEN.remove(key);
            }
        }
    }

    public static void stopAll() {
        var mc = Minecraft.getInstance();
        if (mc == null || mc.getSoundManager() == null) {
            ACTIVE.clear();
            return;
        }
        for (var inst : ACTIVE.values()) {
            mc.getSoundManager().stop(inst);
        }
        ACTIVE.clear();
    }

    /**
     * Tickable looping sound with a smooth spool-up/spool-down envelope. {@code SoundEngine}
     * re-reads the position, volume and pitch of
     * {@link net.minecraft.client.resources.sounds.TickableSoundInstance}s every tick; instead of
     * snapping values between frames, {@code tick()} exponentially interpolates them toward stored
     * targets. That smooths out the tick updates for continuous thruster playback, so there are no
     * audible seam restarts or looping-tick stutter at loop points or spool changes. When a
     * fade-out reaches silence the instance stops itself and is dropped from {@link #ACTIVE} via
     * {@code onFullyStopped}.
     */
    private static final class LoopingSound extends AbstractTickableSoundInstance {
        private final Runnable onFullyStopped;
        private final float basePitch;
        // Interpolation targets - the public volume/pitch/position ease toward these every tick.
        private float targetVolume;
        private float targetPitch;
        private double targetX;
        private double targetY;
        private double targetZ;
        private long lastTick = System.nanoTime();

        LoopingSound(SoundEvent event, double x, double y, double z, Runnable onFullyStopped) {
            super(event, SoundSource.BLOCKS, RandomSource.create());
            // Slight per-instance detune so identical thrusters on one aircraft don't phase-lock
            // and land their loop-point seams on the same tick (a leading cause of audible restarts).
            this.basePitch = 0.95f + 0.1f * this.random.nextFloat();
            this.volume = 0.0f; // start silent; tick() eases us up over FADE_IN_SECONDS
            this.pitch = this.basePitch;
            this.targetVolume = 1f;
            this.targetPitch = this.basePitch;
            this.x = (float) x;
            this.y = (float) y;
            this.z = (float) z;
            this.targetX = x;
            this.targetY = y;
            this.targetZ = z;
            this.looping = true;
            this.attenuation = SoundInstance.Attenuation.LINEAR;
            this.onFullyStopped = onFullyStopped;
        }

        /** The engine skips sounds that start at zero volume unless we opt in to starting silent. */
        @Override
        public boolean canStartSilent() {
            return true;
        }

        @Override
        public void tick() {
            long now = System.nanoTime();
            float dt = (now - lastTick) / 1_000_000_000f;
            lastTick = now;
            if (dt <= 0f || dt > 0.25f) {
                dt = 0.05f; // clamp weird deltas (pause, hitch, first tick)
            }
            // Frame-rate independent exponential approach: covers >99.9% of the gap to the target
            // within FADE_IN_SECONDS of audio. Volume and pitch both ease, so spool-up/spool-down
            // and throttle changes never snap or restart the loop audibly.
            float envelope = 1f - (float) Math.pow(0.001, dt / FADE_IN_SECONDS);
            volume += (targetVolume - volume) * envelope;
            pitch += (targetPitch - pitch) * envelope;

            if (volume < 0.004f) {
                this.stop();
                onFullyStopped.run();
                return;
            }

            // Smooth positional follow keeps LINEAR attenuation continuous on fast-moving thrusters
            // (no teleport pops between updates).
            float move = 1f - (float) Math.pow(0.001, dt);
            this.x += (float) ((targetX - x) * move);
            this.y += (float) ((targetY - y) * move);
            this.z += (float) ((targetZ - z) * move);
        }

        /** Eases back up to full volume and the per-thruster pitch (no hard restart). */
        void beginFadeIn() {
            targetVolume = 1f;
            targetPitch = basePitch;
        }

        /** Eases down to silence; the instance stops itself once it gets there. */
        void beginFadeOut() {
            targetVolume = 0f;
        }

        void moveTo(double nx, double ny, double nz) {
            targetX = nx;
            targetY = ny;
            targetZ = nz;
        }
    }
}
