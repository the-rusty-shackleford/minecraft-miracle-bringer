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
import com.nfx.miraclebringer.ModAttachments;
import com.nfx.miraclebringer.ModEffects;
import com.nfx.miraclebringer.ModEntities;
import com.nfx.miraclebringer.domain.Cooldown;
import com.nfx.miraclebringer.domain.Visit;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Holder;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.BeforeBatch;
import net.minecraft.gametest.framework.GameTest;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Difficulty;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.damagesource.DamageType;
import net.minecraft.world.damagesource.DamageTypes;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.monster.Zombie;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Arrow;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.common.NeoForge;
import net.neoforged.neoforge.event.tick.PlayerTickEvent;
import net.neoforged.neoforge.gametest.GameTestHolder;
import net.neoforged.neoforge.gametest.PrefixGameTestTemplate;

import java.util.ArrayList;
import java.util.List;

/**
 * The miracle on a real server. The chance is config, shared by every test
 * running at once, so the tests are grouped in batches the framework runs
 * one after another, each batch setting the config it needs first.
 *
 * <p>{@code granted}: the chance is certain. Every damage type the game
 * registers -- and any a mod adds, since the sweep reads the registry --
 * is answered with the blessing, except the kill command; the real causes
 * (an arrow, an explosion, a zombie's blow, the kill command through the
 * command dispatcher) are tried through their own code paths; the
 * blessing refuses every damage type but the kill command and every
 * harmful effect; the visitor appears and rises; a totem is not spent.
 * {@code denied}: the chance is nil, a lethal blow kills, a totem works.
 * {@code cooldown}: the second miracle waits a day. {@code no_exemption}:
 * the kill command is answered like anything else.
 *
 * <p>Mock players have no connection and are not in the level's player
 * list; the visitor cannot find one and leaves at once, which is right for
 * the sweeps. The visitor's own test places a real player.
 */
@GameTestHolder(MiracleBringer.MOD_ID)
@PrefixGameTestTemplate(false)
public final class MiracleGameTests {
    private static final int ARENA_SIZE = 9;
    private static final Vec3 STAND = new Vec3(4.5, 1.0, 4.5);
    private static final float LETHAL = 1000.0f;

    public MiracleGameTests() {}

    // --- batches ---------------------------------------------------------------

    @BeforeBatch(batch = "granted")
    public static void certain(ServerLevel level) {
        configure(level, 1.0, 0.0, true);
    }

    @BeforeBatch(batch = "denied")
    public static void never(ServerLevel level) {
        configure(level, 0.0, 0.0, true);
    }

    @BeforeBatch(batch = "cooldown")
    public static void onceADay(ServerLevel level) {
        configure(level, 1.0, 24.0, true);
    }

    @BeforeBatch(batch = "no_exemption")
    public static void killCommandToo(ServerLevel level) {
        configure(level, 1.0, 0.0, false);
    }

    private static void configure(ServerLevel level, double chance, double cooldownHours, boolean killExempt) {
        level.getServer().setDifficulty(Difficulty.NORMAL, true);   // peaceful would zero mob damage
        MiracleConfig.CHANCE.set(chance);
        MiracleConfig.COOLDOWN_HOURS.set(cooldownHours);
        MiracleConfig.KILL_COMMAND_EXEMPT.set(killExempt);
    }

    // --- granted ---------------------------------------------------------------

    @GameTest(template = "arena", batch = "granted")
    public void everyDamageTypeIsAnsweredButTheKillCommand(GameTestHelper helper) {
        layFloor(helper);
        List<String> notAnswered = new ArrayList<>();
        List<String> tried = new ArrayList<>();
        for (Holder.Reference<DamageType> type : helper.getLevel().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).holders().toList()) {
            Player victim = mock(helper, 6.0f);
            boolean kill = type.is(DamageTypes.GENERIC_KILL);
            victim.hurt(new DamageSource(type), LETHAL);
            tried.add(type.key().location().toString());
            boolean saved = victim.getHealth() == victim.getMaxHealth() && victim.hasEffect(ModEffects.BLESSING)
                    && victim.getAbsorptionAmount() >= 20.0f;
            if (kill ? saved : !saved) {
                notAnswered.add(type.key().location() + (kill ? " (the kill command should have killed)" : ""));
            }
        }
        helper.assertTrue(tried.size() >= 40, "the registry holds the vanilla damage types, saw " + tried.size());
        helper.assertTrue(notAnswered.isEmpty(), "wrong outcome for " + notAnswered);
        helper.succeed();
    }

    @GameTest(template = "arena", batch = "granted", timeoutTicks = 100)
    @SuppressWarnings("removal")
    public void realCausesAreAnsweredThroughTheirOwnPaths(GameTestHelper helper) {
        layFloor(helper);
        ServerLevel level = helper.getLevel();

        // Each victim stands apart: a blessed player refuses a blow, and an
        // arrow that meets one first bounces off and never reaches the next.

        // An explosion, through the level -- first, while it has one victim.
        ServerPlayer blown = placed(helper, 1.0f, new Vec3(4.5, 1.0, 7.5));
        Vec3 at = blown.position();
        level.explode(null, at.x + 0.5, at.y, at.z, 3.0f, Level.ExplosionInteraction.NONE);
        helper.assertTrue(blown.hasEffect(ModEffects.BLESSING), "an explosion was answered");

        // A zombie's blow, through its own attack.
        ServerPlayer bitten = placed(helper, 1.0f, new Vec3(1.5, 1.0, 1.5));
        Zombie zombie = helper.spawnWithNoFreeWill(EntityType.ZOMBIE, new BlockPos(1, 1, 2));
        zombie.doHurtTarget(bitten);
        helper.assertTrue(bitten.hasEffect(ModEffects.BLESSING), "a zombie's blow was answered");

        // An arrow, through the projectile's own hit, along a lane of its own.
        ServerPlayer shot = placed(helper, 1.0f, new Vec3(6.5, 1.0, 4.5));
        Arrow arrow = new Arrow(level, shot.getX() - 2.5, shot.getEyeY(), shot.getZ(), new ItemStack(Items.ARROW), null);
        arrow.setBaseDamage(50.0);
        arrow.shoot(1.0, 0.0, 0.0, 2.0f, 0.0f);
        level.addFreshEntity(arrow);
        helper.runAtTickTime(5, () -> helper.assertTrue(shot.hasEffect(ModEffects.BLESSING), "an arrow was answered"));

        // The kill command, through the dispatcher: not answered, by the exemption.
        ServerPlayer killed = placed(helper, 20.0f, new Vec3(7.5, 1.0, 1.5));
        helper.runAtTickTime(6, () -> {
            // By UUID: every placed player shares the mock's name.
            level.getServer().getCommands().performPrefixedCommand(level.getServer().createCommandSourceStack(),
                    "kill " + killed.getUUID());
            helper.assertFalse(killed.hasEffect(ModEffects.BLESSING), "the kill command is not answered");
            helper.assertTrue(killed.getHealth() <= 0.0f, "the kill command kills");
            helper.succeed();
        });
    }

    @GameTest(template = "arena", batch = "granted")
    public void theBlessingRefusesEveryDamageTypeButTheKillCommand(GameTestHelper helper) {
        layFloor(helper);
        List<String> wrong = new ArrayList<>();
        for (Holder.Reference<DamageType> type : helper.getLevel().registryAccess()
                .registryOrThrow(Registries.DAMAGE_TYPE).holders().toList()) {
            Player blessed = mock(helper, 20.0f);
            blessed.addEffect(new MobEffectInstance(ModEffects.BLESSING, 200));
            blessed.hurt(new DamageSource(type), LETHAL);
            boolean untouched = blessed.getHealth() == 20.0f;
            if (type.is(DamageTypes.GENERIC_KILL) ? untouched : !untouched) {
                wrong.add(type.key().location().toString());
            }
        }
        helper.assertTrue(wrong.isEmpty(), "wrong outcome for " + wrong);
        helper.succeed();
    }

    @GameTest(template = "arena", batch = "granted")
    public void theBlessingRefusesHarmAndKeepsGood(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 1.0f);
        player.addEffect(new MobEffectInstance(MobEffects.POISON, 600));
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.hasEffect(ModEffects.BLESSING), "blessed");
        helper.assertFalse(player.hasEffect(MobEffects.POISON), "the poison it had is gone");
        helper.assertFalse(player.addEffect(new MobEffectInstance(MobEffects.WITHER, 100)), "wither is refused");
        helper.assertFalse(player.hasEffect(MobEffects.WITHER), "and absent");
        helper.assertTrue(player.addEffect(new MobEffectInstance(MobEffects.REGENERATION, 100)), "regeneration is welcome");
        helper.succeed();
    }

    @GameTest(template = "arena", batch = "granted")
    public void theBlessingPutsOutFireAndRefillsAirEachTick(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 20.0f);
        player.addEffect(new MobEffectInstance(ModEffects.BLESSING, 200));
        player.setRemainingFireTicks(100);
        player.setAirSupply(0);
        player.setTicksFrozen(100);
        NeoForge.EVENT_BUS.post(new PlayerTickEvent.Post(player));
        helper.assertValueEqual(player.getRemainingFireTicks(), 0, "fire out");
        helper.assertValueEqual(player.getAirSupply(), player.getMaxAirSupply(), "air refilled");
        helper.assertValueEqual(player.getTicksFrozen(), 0, "thawed");
        helper.succeed();
    }

    @GameTest(template = "arena", batch = "granted", timeoutTicks = 400)
    @SuppressWarnings("removal")
    public void theVisitorAppearsBeforeThePlayerAndRisesWhenTheBlessingEnds(GameTestHelper helper) {
        layFloor(helper);
        MiracleConfig.DURATION_TICKS.set(60);
        ServerPlayer player = placed(helper, 1.0f);
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.hasEffect(ModEffects.BLESSING), "blessed");
        var visitors = helper.getLevel().getEntities(ModEntities.VISITOR.get(), helper.getBounds(), e -> true);
        helper.assertValueEqual(visitors.size(), 1, "one visitor");
        var visitor = visitors.get(0);
        Vec3 expected = player.position().add(Visit.DISTANCE, 0.0, 0.0);   // the player faces +X
        helper.assertTrue(visitor.position().distanceTo(expected) < 0.5, "he stands before the player, at " + visitor.position() + " for " + expected);
        double restY = visitor.getY();
        helper.runAtTickTime(30, () -> {
            helper.assertTrue(!visitor.isRemoved() && !visitor.isAscending(), "hovering through the blessing");
            helper.assertTrue(Math.abs(visitor.getY() - restY) <= Visit.BOB_AMPLITUDE + 0.01, "no higher than the bob");
        });
        helper.runAtTickTime(60 + 20, () -> {
            helper.assertTrue(visitor.isAscending(), "rising once the blessing ended");
            helper.assertTrue(visitor.getY() > restY + 0.5, "and higher already, at " + (visitor.getY() - restY));
        });
        helper.runAtTickTime(60 + Visit.ASCENT_TICKS + 5, () -> {
            helper.assertTrue(visitor.isRemoved(), "gone after the ascent");
            MiracleConfig.DURATION_TICKS.set(200);
            helper.succeed();
        });
    }

    @GameTest(template = "arena", batch = "granted")
    public void aTotemIsNotSpentWhenTheMiracleComes(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 1.0f);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.hasEffect(ModEffects.BLESSING), "blessed");
        helper.assertTrue(player.getOffhandItem().is(Items.TOTEM_OF_UNDYING), "the totem is still there");
        helper.succeed();
    }

    // --- denied ----------------------------------------------------------------

    @GameTest(template = "arena", batch = "denied")
    public void withNoChanceALethalBlowKills(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 1.0f);
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.getHealth() <= 0.0f, "dead");
        helper.assertFalse(player.hasEffect(ModEffects.BLESSING), "no blessing");
        helper.succeed();
    }

    @GameTest(template = "arena", batch = "denied")
    public void withNoChanceATotemStillWorks(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 1.0f);
        player.setItemInHand(InteractionHand.OFF_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.getHealth() > 0.0f, "alive by the totem");
        helper.assertTrue(player.getOffhandItem().isEmpty(), "the totem is spent");
        helper.assertFalse(player.hasEffect(ModEffects.BLESSING), "no blessing");
        helper.succeed();
    }

    // --- cooldown --------------------------------------------------------------

    @GameTest(template = "arena", batch = "cooldown")
    public void theSecondMiracleWaitsADay(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 1.0f);
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.hasEffect(ModEffects.BLESSING), "the first miracle comes");
        helper.assertTrue(player.getData(ModAttachments.MEMORY).lastMiracleMillis() != Cooldown.NEVER, "and is remembered");
        player.removeEffect(ModEffects.BLESSING);
        player.setAbsorptionAmount(0.0f);
        player.setHealth(1.0f);
        player.invulnerableTime = 0;                       // vanilla's half-second damage cooldown, or the blow is ignored
        player.hurt(player.damageSources().generic(), LETHAL);
        helper.assertTrue(player.getHealth() <= 0.0f, "the second, the same day, does not");
        helper.succeed();
    }

    // --- no exemption ----------------------------------------------------------

    @GameTest(template = "arena", batch = "no_exemption")
    public void withoutTheExemptionEvenTheKillCommandIsAnswered(GameTestHelper helper) {
        layFloor(helper);
        Player player = mock(helper, 1.0f);
        player.hurt(player.damageSources().genericKill(), LETHAL);
        helper.assertTrue(player.hasEffect(ModEffects.BLESSING), "the kill command is answered");
        player.hurt(player.damageSources().genericKill(), LETHAL);
        helper.assertValueEqual(player.getHealth(), player.getMaxHealth(), "and refused while blessed");
        helper.succeed();
    }

    // --- fixtures --------------------------------------------------------------

    private static void layFloor(GameTestHelper helper) {
        for (int x = 0; x < ARENA_SIZE; x++) {
            for (int z = 0; z < ARENA_SIZE; z++) {
                helper.setBlock(new BlockPos(x, 0, z), Blocks.SMOOTH_STONE);
            }
        }
    }

    /** A survival mock player in the arena at {@code health}, facing +X. */
    private static Player mock(GameTestHelper helper, float health) {
        Player player = helper.makeMockPlayer(GameType.SURVIVAL);
        stand(helper, player);
        player.setHealth(health);
        return player;
    }

    /** A real, placed survival player in the arena at {@code health}, facing +X. */
    private static ServerPlayer placed(GameTestHelper helper, float health) {
        return placed(helper, health, STAND);
    }

    @SuppressWarnings("removal")
    private static ServerPlayer placed(GameTestHelper helper, float health, Vec3 where) {
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        player.setGameMode(GameType.SURVIVAL);
        // A placed player is never ticked, so its three seconds of spawn
        // invulnerability would never run out; the field is private, and a
        // test is the one place reflection into vanilla is fair.
        try {
            var field = ServerPlayer.class.getDeclaredField("spawnInvulnerableTime");
            field.setAccessible(true);
            field.setInt(player, 0);
        } catch (ReflectiveOperationException e) {
            helper.fail("could not clear spawn invulnerability: " + e);
        }
        stand(helper, player, where);
        player.setHealth(health);
        return player;
    }

    private static void stand(GameTestHelper helper, Player player) {
        stand(helper, player, STAND);
    }

    private static void stand(GameTestHelper helper, Player player, Vec3 where) {
        Vec3 at = helper.absoluteVec(where);
        player.moveTo(at.x, at.y, at.z, -90.0f, 0.0f);
        player.setYHeadRot(-90.0f);
        player.setYBodyRot(-90.0f);
    }
}
