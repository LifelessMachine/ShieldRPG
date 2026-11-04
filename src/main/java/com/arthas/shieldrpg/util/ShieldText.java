package com.arthas.shieldrpg.util;

import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.MutableComponent;

public final class ShieldText {
    private ShieldText() {}

    public static MutableComponent gradient(String text, int from, int to) {
        MutableComponent result = Component.empty();
        int count = text.codePointCount(0, text.length());
        int index = 0;
        int offset = 0;
        while (offset < text.length()) {
            int codePoint = text.codePointAt(offset);
            offset += Character.charCount(codePoint);
            double t = count <= 1 ? 0.0 : (double) index / (count - 1);
            index++;
            result.append(Component.literal(new String(Character.toChars(codePoint))).withColor(lerp(from, to, t)));
        }
        return result;
    }

    public static int lerp(int from, int to, double t) {
        int r = channel(from >> 16, to >> 16, t);
        int g = channel(from >> 8, to >> 8, t);
        int b = channel(from, to, t);
        return (r << 16) | (g << 8) | b;
    }

    private static int channel(int from, int to, double t) {
        return (int) Math.round((from & 0xFF) + ((to & 0xFF) - (from & 0xFF)) * t);
    }
}
