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
 * Partitions: damage under health / equal / over; absorption zero / some,
 * with damage under, equal to and over the sum; the RI's edges.
 */
class LethalityTest {

    @Test
    void withoutAbsorptionDamageAtHealthKills() {
        assertFalse(Lethality.lethal(10.0f, 0.0f, 9.99f));
        assertTrue(Lethality.lethal(10.0f, 0.0f, 10.0f), "exactly to zero is dead");
        assertTrue(Lethality.lethal(10.0f, 0.0f, 100.0f));
    }

    @Test
    void absorptionSoaksFirst() {
        assertFalse(Lethality.lethal(10.0f, 4.0f, 13.99f), "absorption and health together hold it");
        assertTrue(Lethality.lethal(10.0f, 4.0f, 14.0f));
        assertFalse(Lethality.lethal(1.0f, 20.0f, 20.5f), "a golden-hearted player at one heart survives a 20");
    }

    @Test
    void zeroDamageNeverKills() {
        assertFalse(Lethality.lethal(0.5f, 0.0f, 0.0f));
    }

    @Test
    void theRepresentationInvariantIsEnforced() {
        assertThrows(IllegalArgumentException.class, () -> Lethality.lethal(0.0f, 0.0f, 1.0f), "already dead is not a question");
        assertThrows(IllegalArgumentException.class, () -> Lethality.lethal(10.0f, -1.0f, 1.0f));
        assertThrows(IllegalArgumentException.class, () -> Lethality.lethal(10.0f, 0.0f, -1.0f));
        assertThrows(IllegalArgumentException.class, () -> Lethality.lethal(10.0f, 0.0f, Float.POSITIVE_INFINITY));
        assertThrows(IllegalArgumentException.class, () -> Lethality.lethal(Float.NaN, 0.0f, 1.0f));
    }
}
