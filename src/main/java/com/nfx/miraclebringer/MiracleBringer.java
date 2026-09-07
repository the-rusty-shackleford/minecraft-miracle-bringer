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

import com.mojang.logging.LogUtils;
import net.neoforged.bus.api.IEventBus;
import net.neoforged.fml.ModContainer;
import net.neoforged.fml.common.Mod;
import net.neoforged.fml.config.ModConfig;
import net.neoforged.neoforge.common.NeoForge;
import org.slf4j.Logger;

/**
 * Miracle Bringer: once in a while, a lethal blow is answered with a
 * blessing.
 *
 * <p>When a blow would take a player's last health, from any cause, there is
 * a chance -- two percent unless the server says otherwise, once a day per
 * player -- that the blow is refused instead: full health, golden hearts,
 * and ten seconds of Jesus' Blessing, under which no damage lands and no
 * harm takes, but for a kill command if the server keeps that exemption. A
 * visitor with a halo appears before the player for those seconds and
 * rises out of sight when they end.
 *
 * <p>The decisions are in {@code domain}, pure and tested; {@link MiracleHandler}
 * reads the game into them and applies what they say.
 */
@Mod(MiracleBringer.MOD_ID)
public final class MiracleBringer {
    public static final String MOD_ID = "miraclebringer";
    public static final Logger LOGGER = LogUtils.getLogger();

    public MiracleBringer(IEventBus modBus, ModContainer container) {
        ModEffects.register(modBus);
        ModEntities.register(modBus);
        ModSounds.register(modBus);
        ModAttachments.register(modBus);
        container.registerConfig(ModConfig.Type.COMMON, MiracleConfig.SPEC);
        // Game-bus events, not mod-bus ones.
        NeoForge.EVENT_BUS.register(MiracleHandler.class);
    }
}
