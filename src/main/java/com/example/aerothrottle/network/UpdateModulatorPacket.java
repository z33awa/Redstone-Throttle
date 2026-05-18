package com.example.aerothrottle.network;

import com.example.aerothrottle.AeroThrottleMod;
import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateModulatorPacket(BlockPos pos, byte mode, int lockedRate, int intervalTicks)
    implements CustomPacketPayload {

    public static final Type<UpdateModulatorPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(AeroThrottleMod.MOD_ID, "update_modulator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateModulatorPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpdateModulatorPacket::pos,
            ByteBufCodecs.BYTE,    UpdateModulatorPacket::mode,
            ByteBufCodecs.VAR_INT, UpdateModulatorPacket::lockedRate,
            ByteBufCodecs.VAR_INT, UpdateModulatorPacket::intervalTicks,
            UpdateModulatorPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateModulatorPacket pkt, IPayloadContext ctx) {
        ctx.enqueueWork(() -> {
            if (!(ctx.player().level() instanceof Level level)) return;
            if (!level.isLoaded(pkt.pos)) return;
            BlockEntity be = level.getBlockEntity(pkt.pos);
            if (!(be instanceof RedstoneSpeedModulatorBlockEntity modulator)) return;
            // Distance check: 8 blocks
            if (ctx.player().distanceToSqr(pkt.pos.getX() + 0.5, pkt.pos.getY() + 0.5, pkt.pos.getZ() + 0.5) > 64) return;
            modulator.applySettings(
                RedstoneSpeedModulatorBlockEntity.Mode.of(pkt.mode),
                pkt.lockedRate,
                pkt.intervalTicks);
        });
    }
}
