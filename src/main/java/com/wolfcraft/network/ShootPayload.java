package com.wolfcraft.network;

import com.wolfcraft.WolfCraftMod;
import io.netty.buffer.ByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.Identifier;

public record ShootPayload() implements CustomPacketPayload {
    public static final ShootPayload INSTANCE = new ShootPayload();
    public static final CustomPacketPayload.Type<ShootPayload> TYPE = new CustomPacketPayload.Type<>(
            Identifier.fromNamespaceAndPath(WolfCraftMod.MOD_ID, "shoot")
    );
    public static final StreamCodec<ByteBuf, ShootPayload> STREAM_CODEC = StreamCodec.unit(INSTANCE);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }
}
