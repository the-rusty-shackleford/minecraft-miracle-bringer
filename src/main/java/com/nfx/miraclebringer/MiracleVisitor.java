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

import com.nfx.miraclebringer.domain.Visit;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.syncher.EntityDataAccessor;
import net.minecraft.network.syncher.EntityDataSerializers;
import net.minecraft.network.syncher.SynchedEntityData;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.HumanoidArm;
import net.minecraft.world.entity.Mob;
import net.minecraft.world.entity.MobSpawnType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

/**
 * The visitor: a figure in a robe with a halo who appears before a blessed
 * player, hovers facing them for the blessing, then rises out of sight.
 *
 * <p>A mob with no mind of its own -- no AI, no gravity, no hunger for
 * anything -- that cannot be hurt, pushed, leashed or clicked, and that is
 * never saved: a world reloaded mid-visit has no visitor, which is better
 * than one left standing for ever. The movement is {@link Visit}'s; this
 * class reads the clock and applies it.
 *
 * <p>RI: {@code ward} and {@code endsAt} are set on the server by
 * {@link #summon} before the entity is added; the client's copy has neither
 * and only renders.
 */
public final class MiracleVisitor extends Mob {

    private static final EntityDataAccessor<Boolean> DATA_ASCENDING =
            SynchedEntityData.defineId(MiracleVisitor.class, EntityDataSerializers.BOOLEAN);

    @Nullable
    private UUID ward = null;
    private long endsAt = 0L;
    private double restY = 0.0;
    private int ticksSinceEnd = 0;

    public MiracleVisitor(EntityType<? extends MiracleVisitor> type, Level level) {
        super(type, level);
        this.setNoGravity(true);
        this.setNoAi(true);
        this.setInvulnerable(true);
        this.setSilent(true);
        this.noPhysics = true;
    }

    /**
     * effects: places a visitor before {@code ward} -- {@link Visit#DISTANCE}
     * blocks along the horizontal look, at the player's feet, facing them
     * -- to hover until {@code endsAt} (game time) and then rise and go;
     * returns it, or null if the level refused it
     *
     * @param level  the player's level
     * @param ward   the blessed player
     * @param endsAt the game time the blessing ends
     */
    @Nullable
    public static MiracleVisitor summon(ServerLevel level, Player ward, long endsAt) {
        MiracleVisitor visitor = ModEntities.VISITOR.get().create(level);
        if (visitor == null) {
            return null;
        }
        Vec3 look = ward.getViewVector(1.0f);
        double[] offset = Visit.appearanceOffset(look.x, look.z);
        double x = ward.getX() + offset[0];
        double z = ward.getZ() + offset[1];
        visitor.ward = ward.getUUID();
        visitor.endsAt = endsAt;
        visitor.restY = ward.getY();
        visitor.moveTo(x, ward.getY(), z, 0.0f, 0.0f);
        visitor.face(ward);
        visitor.finalizeSpawn(level, level.getCurrentDifficultyAt(visitor.blockPosition()), MobSpawnType.EVENT, null);
        return level.addFreshEntity(visitor) ? visitor : null;
    }

    /** Whether he is on his way up; synced, for the client's particles. */
    public boolean isAscending() {
        return this.entityData.get(DATA_ASCENDING);
    }

    @Override
    protected void defineSynchedData(SynchedEntityData.Builder builder) {
        super.defineSynchedData(builder);
        builder.define(DATA_ASCENDING, false);
    }

    @Override
    public void tick() {
        super.tick();
        if (this.level().isClientSide) {
            if (this.isAscending()) {
                for (int i = 0; i < 2; i++) {
                    this.level().addParticle(ParticleTypes.END_ROD,
                            this.getRandomX(0.6), this.getRandomY(), this.getRandomZ(0.6), 0.0, -0.02, 0.0);
                }
            }
            return;
        }
        Player player = this.ward == null ? null : this.level().getPlayerByUUID(this.ward);
        if (player == null || player.isRemoved()) {
            this.discard();
            return;
        }
        if (!this.isAscending()) {
            this.face(player);
            if (this.level().getGameTime() >= this.endsAt) {
                this.entityData.set(DATA_ASCENDING, true);
                this.level().playSound(null, this.blockPosition(), ModSounds.ASCEND.get(), SoundSource.NEUTRAL, 1.0f, 1.0f);
            } else {
                this.setPos(this.getX(), this.restY + Visit.bob(this.tickCount), this.getZ());
                this.setDeltaMovement(Vec3.ZERO);
            }
            return;
        }
        // A mob with no mind never runs the step that applies its velocity,
        // so he is moved by position, as the hover is; the client interpolates.
        this.setPos(this.getX(), this.getY() + Visit.riseSpeed(this.ticksSinceEnd), this.getZ());
        this.ticksSinceEnd++;
        if (Visit.gone(this.ticksSinceEnd)) {
            this.discard();
        }
    }

    /** effects: turns body and head toward {@code player} */
    private void face(Player player) {
        double dx = player.getX() - this.getX();
        double dz = player.getZ() - this.getZ();
        float yaw = (float) (Math.toDegrees(Math.atan2(dz, dx)) - 90.0);
        this.setYRot(yaw);
        this.setYBodyRot(yaw);
        this.setYHeadRot(yaw);
        this.yRotO = yaw;
        this.yBodyRotO = yaw;
        this.yHeadRotO = yaw;
    }

    // --- nothing touches him ------------------------------------------------

    @Override
    public boolean hurt(DamageSource source, float amount) {
        return false;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return true;
    }

    @Override
    public boolean isPushable() {
        return false;
    }

    @Override
    protected void doPush(Entity entity) {}

    @Override
    public boolean isPickable() {
        return false;
    }

    @Override
    public boolean canBeLeashed() {
        return false;
    }

    @Override
    public boolean removeWhenFarAway(double distance) {
        return false;
    }

    @Override
    public void checkDespawn() {}

    @Override
    public HumanoidArm getMainArm() {
        return HumanoidArm.RIGHT;
    }

    @Override
    protected boolean shouldDespawnInPeaceful() {
        return false;
    }
}
