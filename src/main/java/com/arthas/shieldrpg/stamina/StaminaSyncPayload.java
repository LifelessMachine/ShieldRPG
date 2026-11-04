package com.arthas.shieldrpg.stamina;

import com.arthas.shieldrpg.ShieldRPG;

import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record StaminaSyncPayload(float value, float max) implements CustomPacketPayload {
    public static final Type<StaminaSyncPayload> TYPE =
            new Type<>(Identifier.fromNamespaceAndPath(ShieldRPG.MODID, "stamina_sync"));

    public static final StreamCodec<ByteBuf, StaminaSyncPayload> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.FLOAT, StaminaSyncPayload::value,
            ByteBufCodecs.FLOAT, StaminaSyncPayload::max,
            StaminaSyncPayload::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
