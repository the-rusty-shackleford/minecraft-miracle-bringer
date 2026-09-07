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

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import com.nfx.miraclebringer.MiracleBringer;
import com.nfx.miraclebringer.MiracleVisitor;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.renderer.LightTexture;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * The halo: a flat golden ring floating a little above the head, turning
 * slowly, lit from within so it glows in the dark. Drawn as a strip of
 * quads on both faces, textured with the mod's small gold swatch, with the
 * light forced to full so no shadow dims it.
 */
public final class HaloLayer extends RenderLayer<MiracleVisitor, PlayerModel<MiracleVisitor>> {

    private static final ResourceLocation TEXTURE =
            ResourceLocation.fromNamespaceAndPath(MiracleBringer.MOD_ID, "textures/entity/halo.png");
    /** Model units, 16 to a block: the ring's radii and how far above the head's top it floats. */
    private static final float INNER = 4.2f;
    private static final float OUTER = 5.6f;
    private static final float ABOVE_HEAD = 3.0f;
    private static final float HEAD_TOP = 8.0f;
    private static final int SEGMENTS = 24;
    private static final float TILT_DEGREES = 12.0f;
    private static final float TURN_DEGREES_PER_TICK = 1.5f;

    public HaloLayer(RenderLayerParent<MiracleVisitor, PlayerModel<MiracleVisitor>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack poseStack, MultiBufferSource buffer, int packedLight, MiracleVisitor entity,
                       float limbSwing, float limbSwingAmount, float partialTick, float ageInTicks,
                       float netHeadYaw, float headPitch) {
        poseStack.pushPose();
        // The entity frame here has y growing downward, 16 units to a block,
        // with the origin at the model's chest height; the head's top is
        // HEAD_TOP above it. The head's own rotation is deliberately not
        // followed: a halo hangs in the air above the head, it does not
        // turn with it.
        poseStack.translate(0.0f, -(HEAD_TOP + ABOVE_HEAD) / 16.0f, 0.0f);
        poseStack.mulPose(Axis.YP.rotationDegrees(ageInTicks * TURN_DEGREES_PER_TICK));
        poseStack.mulPose(Axis.XP.rotationDegrees(TILT_DEGREES));
        VertexConsumer consumer = buffer.getBuffer(RenderType.entityTranslucentEmissive(TEXTURE));
        Matrix4f pose = poseStack.last().pose();
        int light = LightTexture.FULL_BRIGHT;
        for (int i = 0; i < SEGMENTS; i++) {
            double a0 = 2.0 * Math.PI * i / SEGMENTS;
            double a1 = 2.0 * Math.PI * (i + 1) / SEGMENTS;
            float u0 = (float) i / SEGMENTS;
            float u1 = (float) (i + 1) / SEGMENTS;
            float ox0 = (float) (Math.cos(a0) * OUTER) / 16.0f, oz0 = (float) (Math.sin(a0) * OUTER) / 16.0f;
            float ox1 = (float) (Math.cos(a1) * OUTER) / 16.0f, oz1 = (float) (Math.sin(a1) * OUTER) / 16.0f;
            float ix0 = (float) (Math.cos(a0) * INNER) / 16.0f, iz0 = (float) (Math.sin(a0) * INNER) / 16.0f;
            float ix1 = (float) (Math.cos(a1) * INNER) / 16.0f, iz1 = (float) (Math.sin(a1) * INNER) / 16.0f;
            // Top face, then the same quad wound the other way for the underside.
            vertex(consumer, pose, ox0, oz0, u0, 0.0f, light);
            vertex(consumer, pose, ox1, oz1, u1, 0.0f, light);
            vertex(consumer, pose, ix1, iz1, u1, 1.0f, light);
            vertex(consumer, pose, ix0, iz0, u0, 1.0f, light);
            vertex(consumer, pose, ix0, iz0, u0, 1.0f, light);
            vertex(consumer, pose, ix1, iz1, u1, 1.0f, light);
            vertex(consumer, pose, ox1, oz1, u1, 0.0f, light);
            vertex(consumer, pose, ox0, oz0, u0, 0.0f, light);
        }
        poseStack.popPose();
    }

    private static void vertex(VertexConsumer consumer, Matrix4f pose, float x, float z, float u, float v, int light) {
        consumer.addVertex(pose, x, 0.0f, z)
                .setColor(255, 235, 150, 235)
                .setUv(u, v)
                .setOverlay(OverlayTexture.NO_OVERLAY)
                .setLight(light)
                .setNormal(0.0f, 1.0f, 0.0f);
    }
}
