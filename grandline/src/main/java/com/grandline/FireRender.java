package com.grandline;

import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.texture.OverlayTexture;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix3f;
import org.joml.Matrix4f;

/** Desenho de caixas brilhantes com a textura de fogo animada (8 quadros empilhados). */
public final class FireRender {
    public static final ResourceLocation TEX = new ResourceLocation(GrandLine.MODID, "textures/entity/fire.png");
    public static final int FRAMES = 8;
    public static final int FULL_BRIGHT = 15728880;

    private FireRender() {}

    /** Caixa em pixels de modelo (1/16 de bloco), inflada em blocos. */
    public static void pxBox(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                             float inflate, float r, float g, float b, float a, float time) {
        box(ps, vc, x0 / 16f - inflate, y0 / 16f - inflate, z0 / 16f - inflate,
                x1 / 16f + inflate, y1 / 16f + inflate, z1 / 16f + inflate, r, g, b, a, time);
    }

    public static void box(PoseStack ps, VertexConsumer vc, float x0, float y0, float z0, float x1, float y1, float z1,
                           float r, float g, float b, float a, float time) {
        PoseStack.Pose pose = ps.last();
        Matrix4f m = pose.pose();
        Matrix3f n = pose.normal();
        int frame = ((int) (time * 0.5f)) % FRAMES;
        if (frame < 0) frame += FRAMES;
        float v0 = frame / (float) FRAMES, v1 = (frame + 1) / (float) FRAMES;
        // +Y e -Y
        quad(vc, m, n, x0, y1, z0, x1, y1, z0, x1, y1, z1, x0, y1, z1, 0, 1, 0, r, g, b, a, v0, v1);
        quad(vc, m, n, x0, y0, z1, x1, y0, z1, x1, y0, z0, x0, y0, z0, 0, -1, 0, r, g, b, a, v0, v1);
        // +Z e -Z
        quad(vc, m, n, x0, y0, z1, x0, y1, z1, x1, y1, z1, x1, y0, z1, 0, 0, 1, r, g, b, a, v0, v1);
        quad(vc, m, n, x1, y0, z0, x1, y1, z0, x0, y1, z0, x0, y0, z0, 0, 0, -1, r, g, b, a, v0, v1);
        // +X e -X
        quad(vc, m, n, x1, y0, z1, x1, y1, z1, x1, y1, z0, x1, y0, z0, 1, 0, 0, r, g, b, a, v0, v1);
        quad(vc, m, n, x0, y0, z0, x0, y1, z0, x0, y1, z1, x0, y0, z1, -1, 0, 0, r, g, b, a, v0, v1);
    }

    /** Desenha a face nos dois sentidos, assim nao depende de culling. */
    private static void quad(VertexConsumer vc, Matrix4f m, Matrix3f n,
                             float ax, float ay, float az, float bx, float by, float bz,
                             float cx, float cy, float cz, float dx, float dy, float dz,
                             float nx, float ny, float nz, float r, float g, float b, float a, float v0, float v1) {
        // v1 embaixo da face, v0 em cima: a chama sobe na textura
        vert(vc, m, n, ax, ay, az, 0, v1, nx, ny, nz, r, g, b, a);
        vert(vc, m, n, bx, by, bz, 1, v1, nx, ny, nz, r, g, b, a);
        vert(vc, m, n, cx, cy, cz, 1, v0, nx, ny, nz, r, g, b, a);
        vert(vc, m, n, dx, dy, dz, 0, v0, nx, ny, nz, r, g, b, a);
        vert(vc, m, n, dx, dy, dz, 0, v0, -nx, -ny, -nz, r, g, b, a);
        vert(vc, m, n, cx, cy, cz, 1, v0, -nx, -ny, -nz, r, g, b, a);
        vert(vc, m, n, bx, by, bz, 1, v1, -nx, -ny, -nz, r, g, b, a);
        vert(vc, m, n, ax, ay, az, 0, v1, -nx, -ny, -nz, r, g, b, a);
    }

    private static void vert(VertexConsumer vc, Matrix4f m, Matrix3f n, float x, float y, float z, float u, float v,
                             float nx, float ny, float nz, float r, float g, float b, float a) {
        vc.vertex(m, x, y, z).color(r, g, b, a).uv(u, v).overlayCoords(OverlayTexture.NO_OVERLAY)
                .uv2(FULL_BRIGHT).normal(n, nx, ny, nz).endVertex();
    }
}
