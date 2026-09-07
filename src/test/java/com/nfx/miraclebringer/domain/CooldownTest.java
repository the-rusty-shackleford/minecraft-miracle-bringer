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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partitions: never had one; inside the cooldown / at its edge / past it;
 * a clock gone backwards; a cooldown of zero; the hours conversion at 0,
 * 24 and a fraction; the RI's edges.
 */
class CooldownTest {
    private static final long DAY = 24L * 3600L * 1000L;

    @Test
    void aFirstMiracleIsAlwaysReady() {
        assertTrue(Cooldown.ready(Cooldown.NEVER, 0L, DAY));
        assertTrue(Cooldown.ready(Cooldown.NEVER, 1_800_000_000_000L, DAY));
    }

    @Test
    void theCooldownHoldsUntilItHasElapsed() {
        long last = 1_800_000_000_000L;
        assertFalse(Cooldown.ready(last, last + 1, DAY));
        assertFalse(Cooldown.ready(last, last + DAY - 1, DAY), "a millisecond short");
        assertTrue(Cooldown.ready(last, last + DAY, DAY), "at the day");
        assertTrue(Cooldown.ready(last, last + 2 * DAY, DAY));
    }

    @Test
    void aClockGoneBackwardsDoesNotLockThePlayerOut() {
        assertTrue(Cooldown.ready(1_800_000_000_000L, 1_700_000_000_000L, DAY));
    }

    @Test
    void noCooldownIsAlwaysReady() {
        assertTrue(Cooldown.ready(1_800_000_000_000L, 1_800_000_000_000L, 0L));
    }

    @Test
    void hoursConvert() {
        assertEquals(0L, Cooldown.hoursToMillis(0.0));
        assertEquals(DAY, Cooldown.hoursToMillis(24.0));
        assertEquals(1_800_000L, Cooldown.hoursToMillis(0.5));
    }

    @Test
    void theRepresentationInvariantIsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> Cooldown.ready(0L, 1L, -1L));
        assertThrows(IllegalArgumentException.class, () -> Cooldown.hoursToMillis(-1.0));
        assertThrows(IllegalArgumentException.class, () -> Cooldown.hoursToMillis(Double.POSITIVE_INFINITY));
    }
}
