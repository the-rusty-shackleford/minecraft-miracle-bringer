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
 * What the blessing lets through: nothing, but for the kill command when
 * the server keeps that exemption. Damage of every other kind is refused,
 * and so is every harmful effect.
 */
public final class Ward {
    private Ward() {}

    /**
     * effects: returns whether a blessed player takes the blow: only a kill
     * command, and only while the exemption is kept
     */
    public static boolean letsThrough(boolean killCommand, boolean killCommandExempt) {
        return killCommand && killCommandExempt;
    }

    /** effects: returns whether a blessed player refuses an effect: every harmful one */
    public static boolean refusesEffect(boolean harmful) {
        return harmful;
    }
}
