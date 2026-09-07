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

import com.nfx.miraclebringer.domain.Cooldown;
import com.nfx.miraclebringer.domain.Lethality;
import com.nfx.miraclebringer.domain.Odds;
import com.nfx.miraclebringer.domain.Ward;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectCategory;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;

import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.neoforge.event.entity.living.LivingDamageEvent;
import net.neoforged.neoforge.event.entity.living.LivingIncomingDamageEvent;
import net.neoforged.neoforge.event.entity.living.MobEffectEvent;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;

import java.util.List;

/**
 * Where the game meets the rules.
 *
 * <p>The miracle is judged in the pre-damage event, which the game fires
 * after armour and enchantments have taken their share and before
 * absorption has, so the damage there is what will come off absorption and
 * health together; zeroing it there is the blow refused. It runs before the
 * game's own totem check, so a totem is only spent when no miracle came.
 * Every cause of damage in the game passes through this event; there is no
 * other door.
 *
 * <p>The blessing refuses damage in the incoming-damage event, the first the
 * game fires, and refuses harmful effects as they are offered; a per-tick
 * pass puts out fire, refills air, thaws, and clears any harm that arrived
 * another way. The kill command is let through both when the server keeps
 * that exemption.
 */
public final class MiracleHandler {
    private MiracleHandler() {}

    /** The max-absorption modifier behind the golden hearts. */
    static final ResourceLocation GOLDEN_HEARTS =
            ResourceLocation.fromNamespaceAndPath(MiracleBringer.MOD_ID, "golden_hearts");

    @SubscribeEvent
    public static void onDamagePre(LivingDamageEvent.Pre event) {
        if (!(event.getEntity() instanceof Player player) || !(player.level() instanceof ServerLevel level)) {
            return;
        }
        if (player.hasEffect(ModEffects.BLESSING)) {
            return;
        }
        DamageSource source = event.getSource();
        if (Ward.letsThrough(isKillCommand(source), MiracleConfig.KILL_COMMAND_EXEMPT.get())) {
            return;
        }
        float health = player.getHealth();
        if (!(health > 0.0f) || !Lethality.lethal(health, player.getAbsorptionAmount(), event.getNewDamage())) {
            return;
        }
        long now = System.currentTimeMillis();
        MiracleMemory memory = player.getData(ModAttachments.MEMORY);
        if (!Cooldown.ready(memory.lastMiracleMillis(), now, Cooldown.hoursToMillis(MiracleConfig.COOLDOWN_HOURS.get()))) {
            return;
        }
        if (!Odds.granted(player.getRandom().nextDouble(), MiracleConfig.CHANCE.get())) {
            return;
        }
        event.setNewDamage(0.0f);
        bless(level, player, now, memory);
    }

    /**
     * effects: the blow refused, restores {@code player} to full health with
     * golden hearts, applies the blessing for the configured time, clears
     * fire and every harmful effect, records the time against the cooldown,
     * summons the visitor and sounds the blessing
     */
    static void bless(ServerLevel level, Player player, long now, MiracleMemory memory) {
        player.setHealth(player.getMaxHealth());
        grantGoldenHearts(player, MiracleConfig.ABSORPTION.get().floatValue());
        int duration = MiracleConfig.DURATION_TICKS.get();
        // Icon shown, the effect's own swirl of particles not: it hangs in the
        // player's own view, and the visitor is the sign.
        player.addEffect(new MobEffectInstance(ModEffects.BLESSING, duration, 0, false, false, true));
        clearHarm(player);
        player.setData(ModAttachments.MEMORY, memory.granted(now));
        MiracleVisitor.summon(level, player, level.getGameTime() + duration);
        level.playSound(null, player.blockPosition(), ModSounds.BLESSING.get(), SoundSource.PLAYERS, 1.0f, 1.0f);
        MiracleBringer.LOGGER.info("a miracle for {}", player.getName().getString());
    }

    @SubscribeEvent
    public static void onIncomingDamage(LivingIncomingDamageEvent event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player) || !entity.hasEffect(ModEffects.BLESSING)) {
            return;
        }
        if (!Ward.letsThrough(isKillCommand(event.getSource()), MiracleConfig.KILL_COMMAND_EXEMPT.get())) {
            event.setCanceled(true);
        }
    }

    @SubscribeEvent
    public static void onEffectApplicable(MobEffectEvent.Applicable event) {
        LivingEntity entity = event.getEntity();
        if (entity.level().isClientSide || !(entity instanceof Player) || !entity.hasEffect(ModEffects.BLESSING)) {
            return;
        }
        if (Ward.refusesEffect(event.getEffectInstance().getEffect().value().getCategory() == MobEffectCategory.HARMFUL)) {
            event.setResult(MobEffectEvent.Applicable.Result.DO_NOT_APPLY);
        }
    }

    @SubscribeEvent
    public static void onPlayerTick(PlayerTickEvent.Post event) {
        Player player = event.getEntity();
        if (player.level().isClientSide || !player.hasEffect(ModEffects.BLESSING)) {
            return;
        }
        clearHarm(player);
    }

    /**
     * effects: gives {@code player} at least {@code amount} of absorption.
     * Absorption is capped by the max-absorption attribute, zero unless
     * raised, which vanilla's absorption effect does only while it lasts;
     * these hearts are to stay until spent, so the cap is raised by a
     * modifier of this mod's that outlives the blessing.
     */
    static void grantGoldenHearts(Player player, float amount) {
        AttributeInstance cap = player.getAttribute(Attributes.MAX_ABSORPTION);
        if (cap != null) {
            cap.removeModifier(GOLDEN_HEARTS);
            if (amount > 0.0f) {
                cap.addPermanentModifier(new AttributeModifier(GOLDEN_HEARTS, amount, AttributeModifier.Operation.ADD_VALUE));
            }
        }
        player.setAbsorptionAmount(Math.max(player.getAbsorptionAmount(), amount));
    }

    /** effects: puts out fire, refills air, thaws, and removes every harmful effect */
    private static void clearHarm(Player player) {
        player.clearFire();
        player.setAirSupply(player.getMaxAirSupply());
        player.setTicksFrozen(0);
        List<MobEffectInstance> harmful = player.getActiveEffects().stream()
                .filter(e -> e.getEffect().value().getCategory() == MobEffectCategory.HARMFUL)
                .toList();
        for (MobEffectInstance instance : harmful) {
            player.removeEffect(instance.getEffect());
        }
    }

    private static boolean isKillCommand(DamageSource source) {
        return source.is(DamageTypes.GENERIC_KILL);
    }
}
