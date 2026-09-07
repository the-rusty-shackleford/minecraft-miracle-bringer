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
package com.nfx.miraclebringer.client;

import com.nfx.miraclebringer.MiracleBringer;
import com.nfx.miraclebringer.MiracleVisitor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelLayers;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.client.renderer.entity.MobRenderer;
import net.minecraft.resources.ResourceLocation;

/**
 * The visitor drawn with the player's own model in the mod's skin, a halo
 * layer above. The skin is a classic 64x64 player skin, so the player model
 * with wide arms fits it without a special case.
 */
public final class VisitorRenderer extends MobRenderer<MiracleVisitor, PlayerModel<MiracleVisitor>> {

    private static final ResourceLocation SKIN =
            ResourceLocation.fromNamespaceAndPath(MiracleBringer.MOD_ID, "textures/entity/visitor.png");

    public VisitorRenderer(EntityRendererProvider.Context context) {
        super(context, new PlayerModel<>(context.bakeLayer(ModelLayers.PLAYER), false), 0.5f);
        this.addLayer(new HaloLayer(this));
    }

    @Override
    public ResourceLocation getTextureLocation(MiracleVisitor entity) {
        return SKIN;
    }
}
