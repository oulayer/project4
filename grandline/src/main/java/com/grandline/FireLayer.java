package com.grandline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.math.Axis;
import net.minecraft.client.model.PlayerModel;
import net.minecraft.client.model.geom.ModelPart;
import net.minecraft.client.player.AbstractClientPlayer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.entity.RenderLayerParent;
import net.minecraft.client.renderer.entity.layers.RenderLayer;
import net.minecraft.util.Mth;

/**
 * Camada extra do jogador: corpo de fogo/gelo (Logia) e o braco de fogo que se estica nos golpes.
 * Tudo e desenhado como caixas emissivas sobre as partes do modelo do jogador.
 */
public class FireLayer extends RenderLayer<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> {
    // duracao e tamanho por tipo de animacao (1 jab, 2 cruzado, 3 roda, 4 gancho, 5 final, 6 higan, 7 hiken)
    private static final float[] DUR = {8, 8, 8, 10, 8, 14, 8, 12};
    private static final float[] SIZE = {0, 0.30f, 0.32f, 0f, 0.42f, 0.62f, 0.20f, 0.5f};

    public FireLayer(RenderLayerParent<AbstractClientPlayer, PlayerModel<AbstractClientPlayer>> parent) {
        super(parent);
    }

    @Override
    public void render(PoseStack ps, MultiBufferSource buf, int light, AbstractClientPlayer pl, float limb, float limbAmt,
                       float pt, float age, float netYaw, float pitch) {
        ClientVisuals.Vis v = ClientVisuals.get(pl.getUUID());
        if (v == null || pl.isInvisible()) return;
        PlayerModel<AbstractClientPlayer> m = getParentModel();
        VertexConsumer vc = buf.getBuffer(RenderType.eyes(FireRender.TEX));
        float t = age;

        if ((v.flags & ClientVisuals.FIRE) != 0) body(ps, vc, m, t, 1f, 0.55f, 0.12f);
        if ((v.flags & ClientVisuals.ICE) != 0) body(ps, vc, m, t, 0.55f, 0.85f, 1f);

        if (v.anim > 0) {
            float dur = DUR[Math.min(v.anim, DUR.length - 1)];
            float prog = (pl.level().getGameTime() + pt - v.animStart) / dur;
            if (prog >= 0f && prog <= 1f) {
                float k = Mth.sin(prog * Mth.PI);
                if (v.anim == 3) wheel(ps, vc, m, t, prog);
                else arm(ps, vc, m, t, k, SIZE[Math.min(v.anim, SIZE.length - 1)]);
            }
        }
    }

    /** Casca de chamas em 3 camadas sobre cabeca, tronco, bracos e pernas + coroa de fogo. */
    private void body(PoseStack ps, VertexConsumer vc, PlayerModel<AbstractClientPlayer> m, float t, float r, float g, float b) {
        for (int layer = 0; layer < 3; layer++) {
            float inf = 0.02f + layer * 0.06f;
            float a = 0.85f - layer * 0.22f;
            float tt = t * (1f + layer * 0.3f) + layer * 4;
            float gg = g + layer * 0.08f;
            part(ps, vc, m.head, -4, -8, -4, 4, 0, 4, inf, r, gg, b, a, tt);
            part(ps, vc, m.body, -4, 0, -2, 4, 12, 2, inf, r, gg, b, a, tt + 2);
            part(ps, vc, m.rightArm, -3, -2, -2, 1, 10, 2, inf, r, gg, b, a, tt + 4);
            part(ps, vc, m.leftArm, -1, -2, -2, 3, 10, 2, inf, r, gg, b, a, tt + 6);
            part(ps, vc, m.rightLeg, -2, 0, -2, 2, 12, 2, inf, r, gg, b, a, tt + 1);
            part(ps, vc, m.leftLeg, -2, 0, -2, 2, 12, 2, inf, r, gg, b, a, tt + 3);
        }
        // coroa de fogo sobre a cabeca
        float flick = 1f + 0.25f * Mth.sin(t * 0.6f);
        part(ps, vc, m.head, -3, -8f - 12f * flick, -3, 3, -8, 3, 0f, r, g + 0.1f, b, 0.75f, t * 1.4f);
        part(ps, vc, m.head, -1.5f, -8f - 20f * flick, -1.5f, 1.5f, -8, 1.5f, 0f, r, g + 0.25f, b + 0.1f, 0.6f, t * 1.8f + 3);
    }

    private static void part(PoseStack ps, VertexConsumer vc, ModelPart part, float x0, float y0, float z0, float x1, float y1, float z1,
                             float inf, float r, float g, float b, float a, float t) {
        ps.pushPose();
        part.translateAndRotate(ps);
        FireRender.pxBox(ps, vc, x0, y0, z0, x1, y1, z1, inf, r, g, b, a, t);
        ps.popPose();
    }

    /** Braco direito em chamas com um punho gigante e um rastro que se estica na direcao do soco. */
    private void arm(PoseStack ps, VertexConsumer vc, PlayerModel<AbstractClientPlayer> m, float t, float k, float size) {
        float s = 4f + size * 16f * k;      // meia-largura do punho em pixels
        float len = 6f + size * 60f * k;    // comprimento do rastro
        ps.pushPose();
        m.rightArm.translateAndRotate(ps);
        float a = 0.35f + 0.65f * k;
        // manga de fogo no braco inteiro
        FireRender.pxBox(ps, vc, -3, -2, -2, 1, 10, 2, 0.10f, 1f, 0.5f, 0.08f, a, t);
        FireRender.pxBox(ps, vc, -3, -2, -2, 1, 10, 2, 0.20f, 1f, 0.35f, 0.04f, a * 0.6f, t + 4);
        // punho incandescente
        FireRender.pxBox(ps, vc, -1 - s, 10, -s, -1 + s, 10 + 2 * s, s, 0f, 1f, 0.95f, 0.65f, a, t * 1.3f);
        FireRender.pxBox(ps, vc, -1 - s * 1.4f, 9, -s * 1.4f, -1 + s * 1.4f, 10 + 2.6f * s, s * 1.4f, 0f, 1f, 0.55f, 0.10f, a * 0.7f, t * 1.5f + 2);
        // rastro
        FireRender.pxBox(ps, vc, -1 - s * 0.8f, 10 + 2 * s, -s * 0.8f, -1 + s * 0.8f, 10 + 2 * s + len, s * 0.8f, 0f, 1f, 0.40f, 0.05f, a * 0.55f, t + 7);
        ps.popPose();
    }

    /** Roda de chamas girando na cintura (golpe 3). */
    private void wheel(PoseStack ps, VertexConsumer vc, PlayerModel<AbstractClientPlayer> m, float t, float prog) {
        float radius = (18f + 40f * Mth.sin(prog * Mth.PI)); // pixels
        float a = 0.9f * Mth.sin(prog * Mth.PI) + 0.1f;
        ps.pushPose();
        m.body.translateAndRotate(ps);
        ps.translate(0, 6f / 16f, 0);
        ps.mulPose(Axis.YP.rotationDegrees(prog * 600f));
        for (int i = 0; i < 3; i++) {
            ps.pushPose();
            ps.mulPose(Axis.YP.rotationDegrees(i * 60f));
            FireRender.pxBox(ps, vc, -radius, -2, -3, radius, 2, 3, 0f, 1f, 0.55f, 0.10f, a, t + i * 3);
            FireRender.pxBox(ps, vc, -radius * 0.8f, -1, -2, radius * 0.8f, 1, 2, 0f, 1f, 0.95f, 0.6f, a, t * 1.4f + i);
            ps.popPose();
        }
        ps.popPose();
    }
}
