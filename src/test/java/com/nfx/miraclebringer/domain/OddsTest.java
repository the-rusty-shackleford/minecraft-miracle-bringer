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

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Partitions: chance 0 / 1 / between, sample under / at / over the chance;
 * sample at 0; the RI's edges.
 */
class OddsTest {

    @Test
    void zeroNeverAndOneAlways() {
        assertFalse(Odds.granted(0.0, 0.0));
        assertFalse(Odds.granted(0.999, 0.0));
        assertTrue(Odds.granted(0.0, 1.0));
        assertTrue(Odds.granted(0.999, 1.0));
    }

    @Test
    void theSampleMustFallUnderTheChance() {
        assertTrue(Odds.granted(0.0199, 0.02), "just under two percent");
        assertFalse(Odds.granted(0.02, 0.02), "at the chance is not under it");
        assertFalse(Odds.granted(0.5, 0.02));
    }

    @Test
    void theRepresentationInvariantIsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> Odds.granted(0.5, 1.5));
        assertThrows(IllegalArgumentException.class, () -> Odds.granted(0.5, -0.1));
        assertThrows(IllegalArgumentException.class, () -> Odds.granted(1.0, 0.5), "a sample of one is outside [0, 1)");
        assertThrows(IllegalArgumentException.class, () -> Odds.granted(Double.NaN, 0.5));
    }
}
