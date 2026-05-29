package com.z33awa.redstonethrottle.network;

import com.z33awa.redstonethrottle.RedstoneThrottleMod;
import com.z33awa.redstonethrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;

import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record UpdateModulatorPacket(BlockPos pos, byte mode, int lockedRate, int intervalTicks, int strengthMultiplier, int initialSpeed)
    implements CustomPacketPayload {

    public static final Type<UpdateModulatorPacket> TYPE = new Type<>(
        ResourceLocation.fromNamespaceAndPath(RedstoneThrottleMod.MOD_ID, "update_modulator"));

    public static final StreamCodec<RegistryFriendlyByteBuf, UpdateModulatorPacket> STREAM_CODEC =
        StreamCodec.composite(
            BlockPos.STREAM_CODEC, UpdateModulatorPacket::pos,
            ByteBufCodecs.BYTE,    UpdateModulatorPacket::mode,
            ByteBufCodecs.VAR_INT, UpdateModulatorPacket::lockedRate,
            ByteBufCodecs.VAR_INT, UpdateModulatorPacket::intervalTicks,
            ByteBufCodecs.VAR_INT, UpdateModulatorPacket::strengthMultiplier,
            ByteBufCodecs.VAR_INT, UpdateModulatorPacket::initialSpeed,
            UpdateModulatorPacket::new);

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    public static void handle(UpdateModulatorPacket pkt, IPayloadContext ctx) {
        if (pkt == null || ctx == null) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet: packet/context was null");
            return;
        }

        ctx.enqueueWork(() -> applySettings(pkt, ctx))
            .exceptionally(throwable -> {
                RedstoneThrottleMod.LOGGER.error(
                    "Failed to process modulator update packet at {}",
                    pkt.pos(),
                    throwable);
                return null;
            });
    }

    private static void applySettings(UpdateModulatorPacket pkt, IPayloadContext ctx) {
        if (pkt == null || pkt.pos() == null) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet: missing target position");
            return;
        }

        if (ctx.player() == null) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet at {}: missing player context", pkt.pos());
            return;
        }
        var player = ctx.player();

        if (!(player.level() instanceof Level level)) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet at {}: invalid player level for {}", pkt.pos(), player.getName().getString());
            return;
        }

        if (!level.isLoaded(pkt.pos())) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet: position not loaded {}", pkt.pos());
            return;
        }

        BlockEntity blockEntity = level.getBlockEntity(pkt.pos());
        if (!(blockEntity instanceof RedstoneSpeedModulatorBlockEntity modulator)) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet: no modulator block entity at {}", pkt.pos());
            return;
        }

        // Distance check: 8 blocks
        if (player.distanceToSqr(pkt.pos().getX() + 0.5, pkt.pos().getY() + 0.5, pkt.pos().getZ() + 0.5) > 64) {
            RedstoneThrottleMod.LOGGER.warn("Ignored modulator update packet: player {} too far from {}", player.getName().getString(), pkt.pos());
            return;
        }

        modulator.applySettings(
            RedstoneSpeedModulatorBlockEntity.Mode.of(pkt.mode()),
            pkt.lockedRate(),
            pkt.intervalTicks(),
            pkt.strengthMultiplier(),
            pkt.initialSpeed());
    }
}
