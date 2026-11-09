package com.grandline;

import java.util.ArrayList;
import java.util.List;
import net.minecraftforge.event.TickEvent;
import net.minecraftforge.event.server.ServerStoppedEvent;
import net.minecraftforge.eventbus.api.SubscribeEvent;
import net.minecraftforge.fml.common.Mod;

/** Pequeno agendador de tarefas por tick (usado para animar os golpes). */
@Mod.EventBusSubscriber(modid = GrandLine.MODID)
public final class Scheduler {
    private static final class Task {
        int delay; final Runnable run;
        Task(int d, Runnable r) { delay = d; run = r; }
    }

    private static final List<Task> TASKS = new ArrayList<>();

    private Scheduler() {}

    public static void later(int ticks, Runnable r) { TASKS.add(new Task(Math.max(0, ticks), r)); }

    @SubscribeEvent
    public static void tick(TickEvent.ServerTickEvent e) {
        if (e.phase != TickEvent.Phase.END || TASKS.isEmpty()) return;
        List<Task> snapshot = new ArrayList<>(TASKS);
        TASKS.clear();
        List<Task> keep = new ArrayList<>();
        for (Task t : snapshot) {
            if (t.delay-- <= 0) {
                try { t.run.run(); } catch (Exception ex) { ex.printStackTrace(); }
            } else keep.add(t);
        }
        TASKS.addAll(keep);
    }

    @SubscribeEvent
    public static void stopped(ServerStoppedEvent e) { TASKS.clear(); }
}
