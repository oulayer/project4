package com.grandline;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3f;

/** Utilidades de particulas para as animacoes. */
public final class Fx {
    private Fx() {}

    public static DustParticleOptions dust(int rgb, float size) {
        return new DustParticleOptions(new Vector3f(((rgb >> 16) & 255) / 255f, ((rgb >> 8) & 255) / 255f, (rgb & 255) / 255f), size);
    }

    public static final DustParticleOptions DARK = dust(0x08080F, 1.7f);
    public static final DustParticleOptions CRIMSON = dust(0xB3101F, 1.9f);
    public static final DustParticleOptions VIOLET = dust(0x6A1FD0, 1.0f);
    public static final DustParticleOptions CYAN = dust(0x6FF2FF, 1.1f);

    public static void p(ServerLevel l, ParticleOptions o, Vec3 v) {
        l.sendParticles(o, v.x, v.y, v.z, 1, 0, 0, 0, 0);
    }

    public static void spread(ServerLevel l, ParticleOptions o, Vec3 v, int count, double spread, double speed) {
        l.sendParticles(o, v.x, v.y, v.z, count, spread, spread, spread, speed);
    }

    public static void ring(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n) {
        for (int i = 0; i < n; i++) {
            double a = Math.PI * 2 * i / n;
            p(l, o, c.add(Math.cos(a) * r, 0, Math.sin(a) * r));
        }
    }

    /** Explosao de fogo em camadas: nucleo quente, chama laranja, brasas vermelhas, lava e fumaca. */
    public static void fireBurst(ServerLevel l, Vec3 c, double r, int n) {
        sphere(l, ParticleTypes.FLAME, c, r, n);
        sphere(l, dust(0xFFE08A, 1.6f), c, r * 0.55, Math.max(4, n / 3));
        sphere(l, dust(0xFF4A00, 2.2f), c, r * 0.8, Math.max(4, n / 3));
        spread(l, dust(0xC21800, 0.9f), c, Math.max(4, n / 4), r * 0.9, 0);
        spread(l, ParticleTypes.LAVA, c, Math.max(2, n / 10), r * 0.6, 0);
        spread(l, ParticleTypes.LARGE_SMOKE, c.add(0, r * 0.4, 0), Math.max(2, n / 12), r * 0.5, 0.03);
    }

    /** Corte de fogo: arco com chama, nucleo amarelo, borda laranja e brasas. */
    public static void fireArc(ServerLevel l, Vec3 center, Vec3 u, Vec3 v, double radius, double a0, double a1, int n) {
        arc(l, ParticleTypes.FLAME, center, u, v, radius, a0, a1, n);
        arc(l, dust(0xFFD36A, 1.5f), center, u, v, radius * 0.92, a0, a1, n);
        arc(l, dust(0xFF4A00, 1.9f), center, u, v, radius * 1.05, a0, a1, n);
        arc(l, dust(0xC21800, 0.9f), center, u, v, radius * 1.15, a0, a1, Math.max(4, n / 2));
    }

    /** Esfera de particulas (espiral de Fibonacci). */
    public static void sphere(ServerLevel l, ParticleOptions o, Vec3 c, double r, int n) {
        double golden = Math.PI * (3 - Math.sqrt(5));
        for (int i = 0; i < n; i++) {
            double y = 1 - 2.0 * (i + 0.5) / n;
            double rad = Math.sqrt(Math.max(0, 1 - y * y));
            double a = golden * i;
            p(l, o, c.add(Math.cos(a) * rad * r, y * r, Math.sin(a) * rad * r));
        }
    }

    /** Arco no plano (u, v): pos = c + u*cos(a)*r + v*sin(a)*r. */
    public static void arc(ServerLevel l, ParticleOptions o, Vec3 c, Vec3 u, Vec3 v, double r, double a0, double a1, int n) {
        for (int i = 0; i <= n; i++) {
            double a = a0 + (a1 - a0) * i / n;
            p(l, o, c.add(u.scale(Math.cos(a) * r)).add(v.scale(Math.sin(a) * r)));
        }
    }

    public static void line(ServerLevel l, ParticleOptions o, Vec3 a, Vec3 b, double step) {
        Vec3 d = b.subtract(a);
        double len = d.length();
        if (len < 1.0E-4) return;
        Vec3 s = d.normalize().scale(step);
        Vec3 pos = a;
        for (double t = 0; t < len; t += step) {
            p(l, o, pos);
            pos = pos.add(s);
        }
    }
}
