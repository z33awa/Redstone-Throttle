package com.example.aerothrottle.client;

import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlock;
import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;
import com.mojang.blaze3d.vertex.PoseStack;
import com.simibubi.create.AllPartialModels;
import com.simibubi.create.content.kinetics.base.KineticBlockEntityRenderer;

import net.createmod.catnip.animation.AnimationTickHolder;
import net.createmod.catnip.render.CachedBuffers;
import net.createmod.catnip.render.SuperByteBuffer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import net.minecraft.client.renderer.blockentity.BlockEntityRendererProvider;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Renders TWO half-shafts inside the modulator:
 *   - front half (along FACING)  spins at OUTPUT speed (be.getSpeed())
 *   - back  half (along FACING.getOpposite()) spins at INPUT speed read from neighbor BE
 * The encased block model covers the middle, only the ends are visible through the gearbox holes.
 */
public class RedstoneSpeedModulatorRenderer extends KineticBlockEntityRenderer<RedstoneSpeedModulatorBlockEntity> {

    public RedstoneSpeedModulatorRenderer(BlockEntityRendererProvider.Context context) {
        super(context);
    }

    @Override
    protected void renderSafe(RedstoneSpeedModulatorBlockEntity be, float partialTicks, PoseStack ms,
                              MultiBufferSource buffer, int light, int overlay) {
        // We intentionally do NOT defer to Flywheel — no Flywheel visual is registered for this BE.
        BlockState state = be.getBlockState();
        Direction facing = state.getValue(RedstoneSpeedModulatorBlock.FACING);
        Axis axis = facing.getAxis();
        BlockPos pos = be.getBlockPos();
        float time = AnimationTickHolder.getRenderTime(be.getLevel());
        float positionOffset = KineticBlockEntityRenderer.getRotationOffsetForPosition(be, pos, axis);

        // Front half — output side, spinning at our generated speed
        renderHalf(be, state, ms, buffer, light, axis, facing, be.getSpeed(), time, positionOffset);

        // Back half — input side, spinning at the upstream network's speed
        float inputSpeed = be.readInputSpeed();
        renderHalf(be, state, ms, buffer, light, axis, facing.getOpposite(), inputSpeed, time, positionOffset);
    }

    private static void renderHalf(RedstoneSpeedModulatorBlockEntity be, BlockState state, PoseStack ms,
                                   MultiBufferSource buffer, int light, Axis axis, Direction direction,
                                   float speed, float time, float positionOffset) {
        SuperByteBuffer shaft = CachedBuffers.partialFacing(AllPartialModels.SHAFT_HALF, state, direction);
        float angle = ((time * speed * 3f / 10f + positionOffset) % 360f) / 180f * (float) Math.PI;
        KineticBlockEntityRenderer.kineticRotationTransform(shaft, be, axis, angle, light);
        shaft.renderInto(ms, buffer.getBuffer(RenderType.solid()));
    }
}
