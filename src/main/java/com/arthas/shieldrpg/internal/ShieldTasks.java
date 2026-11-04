package com.arthas.shieldrpg.internal;

import java.util.ArrayList;
import java.util.List;

public final class ShieldTasks {

    private static final List<Runnable> NEXT = new ArrayList<>();

    private ShieldTasks() {}

    public static void runAfterThisTick(Runnable task) {
        NEXT.add(task);
    }

    public static void drain() {
        if (NEXT.isEmpty()) {
            return;
        }
        List<Runnable> now = new ArrayList<>(NEXT);
        NEXT.clear();
        for (Runnable task : now) {
            try {
                task.run();
            } catch (RuntimeException e) {
                com.arthas.shieldrpg.ShieldRPG.LOGGER.error("Shield task failed", e);
            }
        }
    }
}
