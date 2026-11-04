package com.arthas.shieldrpg.client;

import com.arthas.shieldrpg.ShieldRPG;
import com.arthas.shieldrpg.api.ShieldApi;
import com.arthas.shieldrpg.internal.SkillCooldownTracker;

import net.minecraft.client.Minecraft;
import net.minecraft.core.component.DataComponents;
import net.neoforged.api.distmarker.Dist;
import net.neoforged.bus.api.SubscribeEvent;
import net.neoforged.fml.common.EventBusSubscriber;
import net.neoforged.neoforge.client.IItemDecorator;
import net.neoforged.neoforge.client.event.RegisterItemDecorationsEvent;

@EventBusSubscriber(modid = ShieldRPG.MODID, value = Dist.CLIENT)
public final class SkillCooldownOverlay {
    private static final double TWO_PI = Math.PI * 2.0;
    private static final int OVERLAY_COLOR = 0x7FFFFFFF;

    private SkillCooldownOverlay() {}

    private static boolean isCovered(int col, int row, double startAngle) {
        double dx = col + 0.5 - 8.0;
        double dy = row + 0.5 - 8.0;
        double angle = Math.atan2(dx, -dy);
        if (angle < 0.0) {
            angle += TWO_PI;
        }
        return angle >= startAngle;
    }

    @SubscribeEvent
    public static void registerDecorations(RegisterItemDecorationsEvent event) {
        IItemDecorator overlay = (guiGraphics, font, stack, x, y) -> {
            var data = stack.get(DataComponents.CUSTOM_DATA);
            var level = Minecraft.getInstance().level;
            if (data == null || level == null) {
                return false;
            }
            var tag = data.copyTag();
            double end = tag.getDoubleOr(SkillCooldownTracker.END_KEY, 0.0);
            double length = tag.getDoubleOr(SkillCooldownTracker.LENGTH_KEY, 0.0);
            double remaining = end - level.getGameTime();
            if (length <= 0.0 || remaining <= 0.0) {
                return false;
            }

            double startAngle = (1.0 - Math.min(1.0, remaining / length)) * TWO_PI;
            for (int row = 0; row < 16; row++) {
                int runStart = -1;
                for (int col = 0; col <= 16; col++) {
                    boolean covered = col < 16 && isCovered(col, row, startAngle);
                    if (covered && runStart < 0) {
                        runStart = col;
                    } else if (!covered && runStart >= 0) {
                        guiGraphics.fill(x + runStart, y + row, x + col, y + row + 1, OVERLAY_COLOR);
                        runStart = -1;
                    }
                }
            }
            return true;
        };
        for (ShieldApi.ShieldEntry entry : ShieldApi.getShields()) {
            event.register(entry.item(), overlay);
        }
    }
}
