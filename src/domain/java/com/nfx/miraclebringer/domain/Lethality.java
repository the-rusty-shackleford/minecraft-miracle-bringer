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
 * Whether a blow would kill: damage is taken from absorption first, then
 * health, and the game counts a player dead at zero health. Judged where
 * the game judges it, after armour and enchantments have taken their share
 * and before absorption has, which is where the damage number still
 * includes what absorption will soak.
 */
public final class Lethality {
    private Lethality() {}

    /**
     * requires: {@code health > 0}, {@code absorption >= 0}, {@code damage >= 0}, all finite<br>
     * effects: returns whether {@code damage}, taken from {@code absorption}
     * then {@code health}, leaves no health
     */
    public static boolean lethal(float health, float absorption, float damage) {
        if (!(health > 0.0f) || !(absorption >= 0.0f) || !(damage >= 0.0f)
                || Float.isInfinite(health) || Float.isInfinite(absorption) || Float.isInfinite(damage)) {
            throw new IllegalArgumentException("need health > 0, absorption >= 0, damage >= 0, all finite; were "
                    + health + ", " + absorption + ", " + damage);
        }
        return damage >= absorption + health;
    }
}
