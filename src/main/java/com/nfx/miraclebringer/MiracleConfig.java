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

import net.neoforged.neoforge.common.ModConfigSpec;

/** The knobs, in {@code config/miraclebringer-common.toml}. Read live. */
public final class MiracleConfig {
    private MiracleConfig() {}

    public static final ModConfigSpec SPEC;
    public static final ModConfigSpec.DoubleValue CHANCE;
    public static final ModConfigSpec.IntValue DURATION_TICKS;
    public static final ModConfigSpec.DoubleValue ABSORPTION;
    public static final ModConfigSpec.DoubleValue COOLDOWN_HOURS;
    public static final ModConfigSpec.BooleanValue KILL_COMMAND_EXEMPT;

    static {
        ModConfigSpec.Builder builder = new ModConfigSpec.Builder();
        builder.comment("The miracle.").push("miracle");
        CHANCE = builder
                .comment("The chance, 0 to 1, that a blow which would kill a player is answered with the blessing instead. 0.02 is two percent.")
                .defineInRange("chance", 0.02D, 0.0D, 1.0D);
        DURATION_TICKS = builder
                .comment("How long the blessing lasts, in ticks (20 a second).")
                .defineInRange("durationTicks", 200, 1, 72000);
        ABSORPTION = builder
                .comment("Golden hearts granted with the blessing, in health points (2 a heart). They stay until spent.")
                .defineInRange("absorption", 20.0D, 0.0D, 200.0D);
        COOLDOWN_HOURS = builder
                .comment("Hours a player must wait after a miracle before another can come, on the clock. 0 for no wait.")
                .defineInRange("cooldownHours", 24.0D, 0.0D, 8760.0D);
        KILL_COMMAND_EXEMPT = builder
                .comment("If true, /kill still kills: it neither triggers the miracle nor is refused by the blessing.")
                .define("killCommandExempt", true);
        builder.pop();
        SPEC = builder.build();
    }
}
