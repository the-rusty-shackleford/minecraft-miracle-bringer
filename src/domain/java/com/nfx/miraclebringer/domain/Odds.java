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
 * The roll. A chance is a fraction in {@code [0, 1]}; a sample is a uniform
 * draw in {@code [0, 1)}. The miracle comes when the sample falls under the
 * chance, so a chance of 0 never grants and a chance of 1 always does.
 */
public final class Odds {
    private Odds() {}

    /**
     * requires: {@code 0 <= chance <= 1}, {@code 0 <= sample < 1}<br>
     * effects: returns whether the miracle is granted
     */
    public static boolean granted(double sample, double chance) {
        if (!(chance >= 0.0 && chance <= 1.0)) {
            throw new IllegalArgumentException("chance must be in [0, 1], was " + chance);
        }
        if (!(sample >= 0.0 && sample < 1.0)) {
            throw new IllegalArgumentException("sample must be in [0, 1), was " + sample);
        }
        return sample < chance;
    }
}
