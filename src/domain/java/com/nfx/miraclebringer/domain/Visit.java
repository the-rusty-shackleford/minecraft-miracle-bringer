/*
 * Miracle Bringer - once in a while, a lethal blow is answered with a blessing.
 * Copyright (C) 2026 Rusty Shackleford and nfx
 *
 * This program is free software: you can redistribute it and/or modify it
 * under the terms of the GNU Affero General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or (at your
 * option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU Affero General Public License
 * for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package com.nfx.miraclebringer.domain;

/**
 * The visitor's movement, over ticks: where he appears, how he hovers, how
 * he rises and when he is gone.
 *
 * <p>He appears {@link #DISTANCE} blocks in front of the player along the
 * horizontal look, at the player's feet; a player looking straight up or
 * down has no horizontal look, and then he appears to the south. He hovers
 * with a slow bob for the blessing's length, then rises, starting gently
 * and gaining speed, and is gone {@link #ASCENT_TICKS} after he started.
 */
public final class Visit {
    private Visit() {}

    /** Blocks in front of the player he appears. */
    public static final double DISTANCE = 2.5;
    /** Ticks the ascent takes before he is gone. */
    public static final int ASCENT_TICKS = 80;
    /** Blocks per tick he rises at first, and the most he rises per tick. */
    public static final double RISE_START = 0.04;
    public static final double RISE_MAX = 0.55;
    /** Blocks per tick per tick he gains. */
    public static final double RISE_GAIN = 0.012;
    /** The hover's amplitude in blocks and its period in ticks. */
    public static final double BOB_AMPLITUDE = 0.08;
    public static final int BOB_PERIOD = 60;

    /**
     * effects: returns the horizontal offset from the player, as
     * {@code (dx, dz)}, at which he appears: {@link #DISTANCE} along the
     * look's horizontal part, or due south if there is none
     */
    public static double[] appearanceOffset(double lookX, double lookZ) {
        double length = Math.sqrt(lookX * lookX + lookZ * lookZ);
        if (!(length > 1e-6)) {
            return new double[] {0.0, DISTANCE};
        }
        return new double[] {lookX / length * DISTANCE, lookZ / length * DISTANCE};
    }

    /** effects: returns the hover's height above rest at {@code tick} */
    public static double bob(int tick) {
        return BOB_AMPLITUDE * Math.sin(2.0 * Math.PI * tick / BOB_PERIOD);
    }

    /**
     * requires: {@code ticksSinceEnd >= 0}<br>
     * effects: returns blocks per tick he rises, {@code ticksSinceEnd} after
     * the blessing ended: gaining from {@link #RISE_START} to {@link #RISE_MAX}
     */
    public static double riseSpeed(int ticksSinceEnd) {
        if (ticksSinceEnd < 0) {
            throw new IllegalArgumentException("ticksSinceEnd must be >= 0, was " + ticksSinceEnd);
        }
        return Math.min(RISE_MAX, RISE_START + RISE_GAIN * ticksSinceEnd);
    }

    /** effects: returns whether he is gone, {@code ticksSinceEnd} after the blessing ended */
    public static boolean gone(int ticksSinceEnd) {
        return ticksSinceEnd >= ASCENT_TICKS;
    }
}
