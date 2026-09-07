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
 * Partitions: appearance along a look with a horizontal part / a straight-up
 * look / a diagonal one; the bob at rest, its peak, its trough, its period;
 * the rise at the start, while gaining, at and past the cap; gone at the
 * edge and not before.
 */
class VisitTest {
    private static final double EPS = 1e-9;

    @Test
    void heAppearsInFrontAlongTheHorizontalLook() {
        double[] off = Visit.appearanceOffset(1.0, 0.0);
        assertEquals(Visit.DISTANCE, off[0], EPS);
        assertEquals(0.0, off[1], EPS);
        double[] diag = Visit.appearanceOffset(3.0, 4.0);
        assertEquals(Visit.DISTANCE * 0.6, diag[0], EPS, "normalised, not scaled by the look's length");
        assertEquals(Visit.DISTANCE * 0.8, diag[1], EPS);
    }

    @Test
    void aVerticalLookPutsHimToTheSouth() {
        double[] off = Visit.appearanceOffset(0.0, 0.0);
        assertEquals(0.0, off[0], EPS);
        assertEquals(Visit.DISTANCE, off[1], EPS);
    }

    @Test
    void theBobIsASineOfThePeriod() {
        assertEquals(0.0, Visit.bob(0), EPS);
        assertEquals(Visit.BOB_AMPLITUDE, Visit.bob(Visit.BOB_PERIOD / 4), EPS, "the peak a quarter in");
        assertEquals(-Visit.BOB_AMPLITUDE, Visit.bob(3 * Visit.BOB_PERIOD / 4), EPS, "the trough");
        assertEquals(0.0, Visit.bob(Visit.BOB_PERIOD), EPS, "back to rest after a period");
    }

    @Test
    void theRiseGainsThenCaps() {
        assertEquals(Visit.RISE_START, Visit.riseSpeed(0), EPS);
        assertEquals(Visit.RISE_START + Visit.RISE_GAIN * 10, Visit.riseSpeed(10), EPS);
        int capAt = (int) Math.ceil((Visit.RISE_MAX - Visit.RISE_START) / Visit.RISE_GAIN);
        assertEquals(Visit.RISE_MAX, Visit.riseSpeed(capAt), EPS, "at the cap");
        assertEquals(Visit.RISE_MAX, Visit.riseSpeed(capAt + 100), EPS, "held there");
        assertTrue(capAt < Visit.ASCENT_TICKS, "he reaches full speed before he is gone");
    }

    @Test
    void heIsGoneWhenTheAscentEnds() {
        assertFalse(Visit.gone(0));
        assertFalse(Visit.gone(Visit.ASCENT_TICKS - 1));
        assertTrue(Visit.gone(Visit.ASCENT_TICKS));
    }

    @Test
    void theRiseRejectsANegativeTime() {
        assertThrows(IllegalArgumentException.class, () -> Visit.riseSpeed(-1));
    }
}
