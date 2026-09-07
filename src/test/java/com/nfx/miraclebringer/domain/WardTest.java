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
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Partitions: the four combinations of kill command and exemption; harmful or not. */
class WardTest {

    @Test
    void onlyAnExemptKillCommandGetsThrough() {
        assertTrue(Ward.letsThrough(true, true));
        assertFalse(Ward.letsThrough(true, false), "the exemption withdrawn: even /kill is refused");
        assertFalse(Ward.letsThrough(false, true));
        assertFalse(Ward.letsThrough(false, false));
    }

    @Test
    void harmfulEffectsAreRefusedAndOthersAreNot() {
        assertTrue(Ward.refusesEffect(true));
        assertFalse(Ward.refusesEffect(false));
    }
}
