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
package com.nfx.miraclebringer.gametest;

import com.nfx.miraclebringer.MiracleBringer;
import com.nfx.miraclebringer.MiracleConfig;
import net.minecraft.client.CameraType;
import net.minecraft.client.Minecraft;
import net.minecraft.client.Screenshot;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.GameType;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.event.ClientTickEvent;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

/**
 * The end-to-end test, on film: in the {@code photoBooth} dev run the
 * chance is forced to certain, the player is set to survival at one
 * heart, a lethal blow is dealt through the real damage path, and the
 * frames land in {@code run/booth/screenshots/booth-*.png}: the moment of
 * the blessing (health, golden hearts, the effect, the visitor before the
 * player), the visitor from behind the player, and his ascent until he is
 * gone. Client only, active only under {@code miraclebringer.photobooth}.
 */
@EventBusSubscriber(modid = BoothMod.MOD_ID, value = Dist.CLIENT)
public final class PhotoBooth {
    private PhotoBooth() {}

    private static final boolean ACTIVE = Boolean.getBoolean("miraclebringer.photobooth");
    private static final int SETTLE = 30;
    private static final int DURATION = 200;

    private record Step(int at, Runnable action) {}

    private static List<Step> steps;
    private static int tick = 0;

    @SubscribeEvent
    public static void onClientTick(ClientTickEvent.Post event) {
        if (!ACTIVE) {
            return;
        }
        Minecraft mc = Minecraft.getInstance();
        LocalPlayer player = mc.player;
        if (player == null || mc.level == null) {
            return;
        }
        if (steps == null) {
            steps = script(mc, player);
        }
        tick++;
        for (Step step : steps) {
            if (step.at() == tick) {
                step.action().run();
            }
        }
    }

    private static List<Step> script(Minecraft mc, LocalPlayer player) {
        List<Step> s = new ArrayList<>();
        int[] t = {40};
        s.add(new Step(t[0], () -> {
            player.setYRot(0.0f);
            player.setYHeadRot(0.0f);
            player.setYBodyRot(0.0f);
            player.setXRot(0.0f);
            mc.options.setCameraType(CameraType.FIRST_PERSON);
            MiracleConfig.CHANCE.set(1.0);
            MiracleConfig.DURATION_TICKS.set(DURATION);
            MiracleConfig.COOLDOWN_HOURS.set(0.0);
            onServer(mc, sp -> {
                sp.setGameMode(GameType.SURVIVAL);
                sp.setHealth(1.0f);
            });
        }));
        s.add(new Step(t[0] += SETTLE, () -> shoot(mc, "booth-before")));
        s.add(new Step(t[0] += 1, () -> onServer(mc, sp -> sp.hurt(sp.damageSources().generic(), 1000.0f))));
        s.add(new Step(t[0] += 3, () -> shoot(mc, "booth-blessing")));
        s.add(new Step(t[0] += 30, () -> shoot(mc, "booth-visitor-first")));
        s.add(new Step(t[0] += 1, () -> mc.options.setCameraType(CameraType.THIRD_PERSON_BACK)));
        s.add(new Step(t[0] += SETTLE, () -> shoot(mc, "booth-visitor-third")));
        s.add(new Step(t[0] += 1, () -> mc.options.setCameraType(CameraType.FIRST_PERSON)));
        int end = 40 + SETTLE + 1 + DURATION;
        s.add(new Step(end + 12, () -> shoot(mc, "booth-ascend-1")));
        s.add(new Step(end + 40, () -> shoot(mc, "booth-ascend-2")));
        s.add(new Step(end + 70, () -> shoot(mc, "booth-ascend-3")));
        s.add(new Step(end + 95, () -> shoot(mc, "booth-after")));
        s.add(new Step(end + 110, mc::stop));
        return s;
    }

    /** Runs {@code action} on the integrated server's thread for this player. */
    private static void onServer(Minecraft mc, Consumer<ServerPlayer> action) {
        var server = mc.getSingleplayerServer();
        if (server == null || mc.player == null) {
            MiracleBringer.LOGGER.warn("photo booth: no integrated server");
            return;
        }
        var uuid = mc.player.getUUID();
        server.execute(() -> {
            ServerPlayer sp = server.getPlayerList().getPlayer(uuid);
            if (sp != null) {
                action.accept(sp);
            }
        });
    }

    private static void shoot(Minecraft mc, String name) {
        Screenshot.grab(mc.gameDirectory, name + ".png", mc.getMainRenderTarget(),
                message -> MiracleBringer.LOGGER.info("photo booth: {}", message.getString()));
    }
}
