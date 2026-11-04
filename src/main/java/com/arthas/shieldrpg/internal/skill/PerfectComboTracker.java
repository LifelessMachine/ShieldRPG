package com.arthas.shieldrpg.internal.skill;

import com.arthas.shieldrpg.ShieldRPG;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.api.ShieldSkillContext;
import com.arthas.shieldrpg.api.ShieldTriggerIds;
import com.arthas.shieldrpg.api.ShieldTriggerListener;

public final class PerfectComboTracker implements ShieldTriggerListener {
    public static final String TRIGGER_ID = ShieldRPG.MODID + ":perfect_combo";
    private static final int WINDOW_TICKS = 200;
    private static final int COMBO_SIZE = 2;

    private record Combo(int count, long lastTick) {}

    private final Map<UUID, Combo> combos = new ConcurrentHashMap<>();

    @Override
    public String id() {
        return ShieldRPG.MODID + ":perfect_combo_tracker";
    }

    @Override
    public void onTrigger(ShieldSkillContext context) {
        if (!ShieldTriggerIds.PERFECT_GUARD.equals(context.trigger())) {
            return;
        }
        long now = context.player().level().getGameTime();
        Combo previous = combos.get(context.player().getUUID());
        int count = previous != null && now - previous.lastTick() <= WINDOW_TICKS ? previous.count() + 1 : 1;
        if (count >= COMBO_SIZE) {
            combos.remove(context.player().getUUID());
            ShieldApi.fireTrigger(context.player(), context.shield(), TRIGGER_ID, context.directSource(), context.attacker(),
                    context.amount(), Map.of("combo", COMBO_SIZE));
        } else {
            combos.put(context.player().getUUID(), new Combo(count, now));
        }
    }
}
