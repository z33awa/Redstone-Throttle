package com.example.aerothrottle.content.modulator;

import java.util.List;

import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneSpeedModulatorBlockEntity extends GeneratingKineticBlockEntity {

    public enum Mode {
        STRENGTH, FIXED;

        public static Mode of(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }
    }

    private static final float STRENGTH_RATE = 0.5f;
    private static final float STRESS_CAPACITY = 256.0f;
    public static final int MAX_RATE = 256;
    public static final int MIN_RATE = 1;
    public static final int MAX_INTERVAL_TICKS = 200;
    public static final int MIN_INTERVAL_TICKS = 6;

    private Mode mode = Mode.STRENGTH;
    private int lockedRate = 16;
    private int intervalTicks = 20;

    private int tickCounter;
    private boolean wasSignalActive;
    private int prevSignalDir;
    private float offset;
    private float cachedGenerated;

    public RedstoneSpeedModulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;

        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        Direction leftDir = redstoneLeft(facing);
        Direction rightDir = redstoneRight(facing);

        int leftSig = level.getSignal(worldPosition.relative(leftDir), leftDir);
        int rightSig = level.getSignal(worldPosition.relative(rightDir), rightDir);

        int signalDir = Integer.signum(leftSig - rightSig);
        boolean signalActive = signalDir != 0;
        boolean justActivated = signalActive && !wasSignalActive;
        boolean directionChanged = signalActive && wasSignalActive && signalDir != prevSignalDir;
        wasSignalActive = signalActive;
        if (signalActive) prevSignalDir = signalDir;

        float maxSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get().floatValue();
        int interval = Math.max(MIN_INTERVAL_TICKS, intervalTicks);

        // Read input first so we can clamp offset to its useful range.
        float inputSpeed = readInputSpeed(facing);
        float offsetMin = -maxSpeed;
        float offsetMax = maxSpeed;
        if (inputSpeed > 0f) offsetMin = -inputSpeed;
        if (inputSpeed < 0f) offsetMax = -inputSpeed;

        tickCounter++;
        if (justActivated || directionChanged || tickCounter >= interval) {
            tickCounter = 0;

            float delta;
            if (mode == Mode.STRENGTH) {
                delta = (leftSig - rightSig) * STRENGTH_RATE;
            } else {
                int leftActive = leftSig > 0 ? 1 : 0;
                int rightActive = rightSig > 0 ? 1 : 0;
                delta = (leftActive - rightActive) * (float) lockedRate;
            }

            if (delta != 0f) {
                offset = Mth.clamp(offset + delta, offsetMin, offsetMax);
                setChanged();
            }
        }

        // Keep offset within useful bounds even if input changes without redstone input.
        offset = Mth.clamp(offset, offsetMin, offsetMax);

        // Output is gated by input: no input → no output. The modulator is a transformer, not a generator.
        float rawDesired;
        if (Math.abs(inputSpeed) < 0.01f) {
            rawDesired = 0f;
        } else {
            rawDesired = Mth.clamp(inputSpeed + offset, -maxSpeed, maxSpeed);
            if (inputSpeed > 0f && rawDesired < 0f) rawDesired = 0f;
            if (inputSpeed < 0f && rawDesired > 0f) rawDesired = 0f;
        }

        // Snap to integer RPM so goggle / speedometer / our display all agree.
        float desired = Math.round(rawDesired);

        if (Math.abs(desired - cachedGenerated) >= 0.5f) {
            cachedGenerated = desired;
            updateGeneratedRotation();
        }
    }

    /** Two horizontal directions perpendicular to the shaft axis, used as redstone inputs. */
    public static Direction redstoneLeft(Direction facing) {
        if (facing.getAxis() == Axis.Y) return Direction.EAST;
        return facing.getCounterClockWise();
    }

    public static Direction redstoneRight(Direction facing) {
        if (facing.getAxis() == Axis.Y) return Direction.WEST;
        return facing.getClockWise();
    }

    public void onRedstoneChanged() {
        // hook for Block.neighborChanged — tick() polls each frame; reserved for future use.
    }

    private float readInputSpeed(Direction facing) {
        BlockEntity be = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
        if (!(be instanceof KineticBlockEntity kbe))
            return 0f;
        Block block = kbe.getBlockState().getBlock();
        if (!(block instanceof IRotate rotate))
            return 0f;
        if (rotate.getRotationAxis(kbe.getBlockState()) != facing.getAxis())
            return 0f;
        return kbe.getTheoreticalSpeed();
    }

    /** Read the speed of the kinetic block adjacent to the BACK face. Safe for client-side use (renderer). */
    public float readInputSpeed() {
        if (level == null) return 0f;
        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        BlockEntity be = level.getBlockEntity(worldPosition.relative(facing.getOpposite()));
        if (!(be instanceof KineticBlockEntity kbe)) return 0f;
        Block block = kbe.getBlockState().getBlock();
        if (!(block instanceof IRotate rotate)) return 0f;
        if (rotate.getRotationAxis(kbe.getBlockState()) != facing.getAxis()) return 0f;
        return kbe.getSpeed();
    }

    @Override
    public float getGeneratedSpeed() {
        return cachedGenerated;
    }

    @Override
    public float calculateStressApplied() {
        this.lastStressApplied = 0f;
        return 0f;
    }

    @Override
    public float calculateAddedStressCapacity() {
        this.lastCapacityProvided = STRESS_CAPACITY;
        return STRESS_CAPACITY;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putByte("Mode", (byte) mode.ordinal());
        tag.putInt("LockedRate", lockedRate);
        tag.putInt("IntervalTicks", intervalTicks);
        tag.putFloat("Offset", offset);
        tag.putFloat("Generated", cachedGenerated);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        mode = Mode.of(tag.getByte("Mode"));
        lockedRate = Mth.clamp(tag.getInt("LockedRate"), MIN_RATE, MAX_RATE);
        if (lockedRate == 0) lockedRate = 16;
        intervalTicks = Mth.clamp(tag.getInt("IntervalTicks"), MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        if (intervalTicks == 0) intervalTicks = 20;
        offset = tag.getFloat("Offset");
        cachedGenerated = tag.getFloat("Generated");
        super.read(tag, registries, clientPacket);
    }

    public Mode getMode() {
        return mode;
    }

    public int getLockedRate() {
        return lockedRate;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    public float getOffset() {
        return offset;
    }

    /** Server-side: apply settings received from the GUI packet. */
    public void applySettings(Mode mode, int lockedRate, int intervalTicks) {
        this.mode = mode;
        this.lockedRate = Mth.clamp(lockedRate, MIN_RATE, MAX_RATE);
        this.intervalTicks = Mth.clamp(intervalTicks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        setChanged();
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal(" ")
            .append(Component.translatable("gui.goggles.aero_throttle.output"))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("    ")
            .append(Component.literal(String.format("%.0f", Math.abs(cachedGenerated))).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" RPM").withStyle(ChatFormatting.DARK_GRAY)));

        tooltip.add(Component.literal(" ")
            .append(Component.translatable("gui.goggles.aero_throttle.mode"))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("    ").append(
            Component.translatable(mode == Mode.STRENGTH
                ? "gui.goggles.aero_throttle.mode.strength"
                : "gui.goggles.aero_throttle.mode.fixed").withStyle(ChatFormatting.AQUA)));

        if (mode == Mode.FIXED) {
            tooltip.add(Component.literal("    ")
                .append(Component.literal(lockedRate + " ").withStyle(ChatFormatting.AQUA))
                .append(Component.translatable("gui.goggles.aero_throttle.rpm_per_step").withStyle(ChatFormatting.DARK_GRAY)));
        }

        tooltip.add(Component.literal("    ").append(
            Component.translatable("gui.goggles.aero_throttle.interval",
                String.format("%.2f", intervalTicks / 20.0f)).withStyle(ChatFormatting.DARK_GRAY)));

        return true;
    }
}
