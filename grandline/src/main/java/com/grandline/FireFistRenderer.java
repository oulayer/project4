package com.grandline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.EntityRendererProvider;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;

/** Modelo do Hiken: punho cerrado de fogo (palma, dedos, polegar) envolto em camadas de chama animada. */
public class FireFistRenderer extends EntityRenderer<FireFistEntity> {
    public FireFistRenderer(EntityRendererProvider.Context ctx) {
        super(ctx);
    }

    @Override
    public ResourceLocation getTextureLocation(FireFistEntity e) {
        return FireRender.TEX;
    }

    @Override
    protected int getBlockLightLevel(FireFistEntity e, net.minecraft.core.BlockPos pos) {
        return 15;
    }

    @Override
    public void render(FireFistEntity e, float yaw, float pt, PoseStack ps, MultiBufferSource buf, int light) {
        float t = e.tickCount + pt;
        float s = e.isBig() ? 1.7f : 1.0f;
        ps.pushPose();
        ps.translate(0, e.getBbHeight() / 2f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(-Mth.lerp(pt, e.yRotO, e.getYRot())));
        ps.mulPose(Axis.XP.rotationDegrees(Mth.lerp(pt, e.xRotO, e.getXRot())));
        ps.scale(s, s, s);
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(FireRender.TEX));

        // miolo incandescente: palma + 4 dedos + polegar (a frente do modelo e +Z)
        FireRender.box(ps, vc, -0.30f, -0.30f, -0.45f, 0.30f, 0.30f, 0.20f, 1f, 0.95f, 0.65f, 1f, t);
        for (int i = 0; i < 4; i++) {
            float x = -0.27f + i * 0.18f;
            FireRender.box(ps, vc, x, -0.28f, 0.20f, x + 0.15f, 0.10f, 0.50f, 1f, 0.85f, 0.45f, 1f, t + i);
        }
        FireRender.box(ps, vc, 0.30f, -0.22f, 0.02f, 0.46f, 0.00f, 0.34f, 1f, 0.80f, 0.40f, 1f, t + 2);

        // camadas de chama maiores e mais transparentes, pulsando
        for (int i = 0; i < 3; i++) {
            float k = 0.42f + i * 0.20f + 0.05f * Mth.sin(t * 0.9f + i * 1.7f);
            FireRender.box(ps, vc, -k, -k, -0.70f - i * 0.1f, k, k, 0.62f + i * 0.12f,
                    1f, 0.55f - i * 0.12f, 0.10f, 0.60f - i * 0.15f, t * 1.2f + i * 3);
        }
        // cauda de fogo atras do punho (-Z)
        for (int i = 0; i < 3; i++) {
            float w = 0.30f - i * 0.07f;
            float z0 = -0.7f - i * 0.75f;
            FireRender.box(ps, vc, -w, -w, z0 - 0.75f, w, w, z0, 1f, 0.45f - i * 0.1f, 0.05f, 0.55f - i * 0.13f, t + i * 5);
        }
        ps.popPose();
        super.render(e, yaw, pt, ps, buf, light);
    }
}
