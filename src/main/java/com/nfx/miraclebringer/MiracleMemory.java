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
package com.nfx.miraclebringer;

import com.mojang.serialization.Codec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.nfx.miraclebringer.domain.Cooldown;

/**
 * What a player remembers of miracles: when the last one came, as epoch
 * milliseconds, or {@link Cooldown#NEVER}. Saved with the player and kept
 * through death, or a death would be the way round the cooldown.
 *
 * @param lastMiracleMillis when the last miracle came
 */
public record MiracleMemory(long lastMiracleMillis) {

    public static final MiracleMemory NONE = new MiracleMemory(Cooldown.NEVER);

    public static final Codec<MiracleMemory> CODEC = RecordCodecBuilder.create(i -> i.group(
            Codec.LONG.fieldOf("last_miracle_millis").forGetter(MiracleMemory::lastMiracleMillis)
    ).apply(i, MiracleMemory::new));

    /** effects: returns this with the last miracle at {@code millis} */
    public MiracleMemory granted(long millis) {
        return new MiracleMemory(millis);
    }
}
