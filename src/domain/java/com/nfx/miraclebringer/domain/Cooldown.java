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
 * One miracle per player per cooldown, on the wall clock: the server's
 * uptime is not a player's day, and a day is what Rusty asked for.
 *
 * <p>Times are epoch milliseconds. {@link #NEVER} marks a player who has
 * had no miracle. A clock that has gone backwards (a machine's time reset)
 * counts as ready rather than locking a player out until the old time comes
 * round again.
 */
public final class Cooldown {
    private Cooldown() {}

    /** The last-miracle time of a player who has never had one. */
    public static final long NEVER = Long.MIN_VALUE;

    /**
     * requires: {@code cooldownMillis >= 0}<br>
     * effects: returns whether a miracle may come at {@code now} for a
     * player whose last was at {@code last}: never had one, the cooldown
     * elapsed, the clock went backwards, or no cooldown at all
     */
    public static boolean ready(long last, long now, long cooldownMillis) {
        if (cooldownMillis < 0) {
            throw new IllegalArgumentException("cooldownMillis must be >= 0, was " + cooldownMillis);
        }
        if (last == NEVER || cooldownMillis == 0 || now < last) {
            return true;
        }
        return now - last >= cooldownMillis;
    }

    /** effects: returns {@code hours} as milliseconds, rounded down */
    public static long hoursToMillis(double hours) {
        if (!(hours >= 0.0) || Double.isInfinite(hours)) {
            throw new IllegalArgumentException("hours must be finite and >= 0, was " + hours);
        }
        return (long) (hours * 3600.0 * 1000.0);
    }
}
