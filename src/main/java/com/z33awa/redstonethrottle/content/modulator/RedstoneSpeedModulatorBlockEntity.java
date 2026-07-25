package com.z33awa.redstonethrottle.content.modulator;

import java.util.List;

import com.simibubi.create.content.kinetics.RotationPropagator;
import com.simibubi.create.content.kinetics.transmission.SplitShaftBlockEntity;
import com.simibubi.create.infrastructure.config.AllConfigs;

import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;

public class RedstoneSpeedModulatorBlockEntity extends SplitShaftBlockEntity {

    public enum Mode {
        STRENGTH, FIXED, MULTIPLIER;

        public static Mode of(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }
    }

    private static final float STRENGTH_RATE = 0.5f;
    public static final int MAX_RATE = 256;
    public static final int MIN_RATE = 1;
    public static final int MAX_MULTIPLIER = 256;
    public static final int MIN_MULTIPLIER = 1;
    public static final int MAX_INITIAL_SPEED = 256;
    public static final int MIN_INITIAL_SPEED = 0;
    public static final int MAX_INTERVAL_TICKS = 200;
    public static final int MIN_INTERVAL_TICKS = 10;

    private Mode mode = Mode.STRENGTH;
    private int lockedRate = 16;
    private int strengthMultiplier = 16;
    private int initialSpeed;
    private int intervalTicks = 20;

    private int tickCounter;
    private int propagationCooldown;
    private boolean wasSignalActive;
    private int previousSignalDirection;
    private float offset;
    private float outputSpeed;

    public RedstoneSpeedModulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void tick() {
        super.tick();
        if (level == null || level.isClientSide)
            return;
        if (propagationCooldown > 0)
            propagationCooldown--;

        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        Direction inputFace = facing.getOpposite();
        boolean validInput = hasSource() && getSourceFacing() == inputFace;
        float inputSpeed = validInput ? getTheoreticalSpeed() : 0f;

        Direction increaseFace = RedstoneSpeedModulatorBlock.increaseFace(getBlockState());
        Direction decreaseFace = increaseFace.getOpposite();
        int increaseSignal = level.getSignal(worldPosition.relative(increaseFace), increaseFace);
        int decreaseSignal = level.getSignal(worldPosition.relative(decreaseFace), decreaseFace);

        int signalDifference = increaseSignal - decreaseSignal;
        int signalDirection = Integer.signum(signalDifference);
        boolean signalActive = signalDirection != 0;
        boolean justActivated = signalActive && !wasSignalActive;
        boolean directionChanged = signalActive && wasSignalActive
            && signalDirection != previousSignalDirection;
        wasSignalActive = signalActive;
        if (signalActive)
            previousSignalDirection = signalDirection;

        float maxSpeed = AllConfigs.server().kinetics.maxRotationSpeed.get().floatValue();
        float inputAbs = Math.abs(inputSpeed);
        int inputSign = inputAbs < 0.01f ? 0 : inputSpeed > 0 ? 1 : -1;
        float desired;

        if (mode == Mode.MULTIPLIER) {
            float targetAbs = initialSpeed + signalDifference * (float) strengthMultiplier;
            desired = inputSign * Mth.clamp(targetAbs, 0f, maxSpeed);
        } else {
            int interval = Math.max(MIN_INTERVAL_TICKS, intervalTicks);
            tickCounter++;
            if (justActivated || directionChanged || tickCounter >= interval) {
                tickCounter = 0;
                float delta;
                if (mode == Mode.STRENGTH) {
                    delta = signalDifference * STRENGTH_RATE;
                } else {
                    delta = ((increaseSignal > 0 ? 1 : 0) - (decreaseSignal > 0 ? 1 : 0))
                        * (float) lockedRate;
                }

                if (inputSign != 0 && delta != 0f) {
                    offset = Mth.clamp(offset + delta, -inputAbs, maxSpeed - inputAbs);
                    setChanged();
                }
            }

            if (inputSign == 0) {
                desired = 0f;
            } else {
                offset = Mth.clamp(offset, -inputAbs, maxSpeed - inputAbs);
                desired = inputSign * Mth.clamp(inputAbs + offset, 0f, maxSpeed);
            }
        }

        desired = Math.round(desired);
        if (Math.abs(desired - outputSpeed) >= 0.5f) {
            boolean magnitudeChanged = Math.abs(Math.abs(desired) - Math.abs(outputSpeed)) >= 0.5f;
            boolean stopping = Math.abs(desired) < 0.5f && Math.abs(outputSpeed) >= 0.5f;

            // The modifier is unsigned: an upstream direction change already travels through
            // the existing network and only requires a renderer/data update here.
            if (!magnitudeChanged) {
                outputSpeed = desired;
                setChanged();
                sendData();
                return;
            }

            // Rebuilding briefly takes the downstream tree through zero speed. Coalesce rapid
            // changes so Create's flicker protection cannot accumulate and destroy gearboxes.
            // A requested stop remains immediate.
            if (propagationCooldown > 0 && !stopping)
                return;

            outputSpeed = desired;
            rebuildPropagationTree();
            propagationCooldown = MIN_INTERVAL_TICKS;
            setChanged();
            sendData();
        }
    }

    private void rebuildPropagationTree() {
        if (level == null || level.isClientSide)
            return;
        RotationPropagator.handleRemoved(level, worldPosition, this);
        attachKinetics();
    }

    @Override
    public float getRotationSpeedModifier(Direction face) {
        if (!hasSource())
            return 1f;

        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        Direction inputFace = facing.getOpposite();
        if (getSourceFacing() != inputFace)
            return 0f;
        if (face == inputFace)
            return 1f;
        if (face != facing)
            return 0f;

        float inputAbs = Math.abs(getTheoreticalSpeed());
        return inputAbs < 0.01f ? 0f : Math.abs(outputSpeed) / inputAbs;
    }

    public float getOutputSpeed() {
        return outputSpeed;
    }

    public float getInputSpeed() {
        if (!hasSource())
            return 0f;
        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        return getSourceFacing() == facing.getOpposite() ? getTheoreticalSpeed() : 0f;
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putByte("Mode", (byte) mode.ordinal());
        tag.putInt("LockedRate", lockedRate);
        tag.putInt("StrengthMultiplier", strengthMultiplier);
        tag.putInt("InitialSpeed", initialSpeed);
        tag.putInt("IntervalTicks", intervalTicks);
        tag.putFloat("Offset", offset);
        tag.putFloat("OutputSpeed", outputSpeed);
        super.write(tag, registries, clientPacket);
    }

    @Override
    protected void read(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        mode = tag.contains("Mode") ? Mode.of(tag.getByte("Mode")) : Mode.STRENGTH;
        lockedRate = tag.contains("LockedRate")
            ? Mth.clamp(tag.getInt("LockedRate"), MIN_RATE, MAX_RATE) : 16;
        strengthMultiplier = tag.contains("StrengthMultiplier")
            ? Mth.clamp(tag.getInt("StrengthMultiplier"), MIN_MULTIPLIER, MAX_MULTIPLIER) : 16;
        initialSpeed = tag.contains("InitialSpeed")
            ? Mth.clamp(tag.getInt("InitialSpeed"), MIN_INITIAL_SPEED, MAX_INITIAL_SPEED) : 0;
        intervalTicks = tag.contains("IntervalTicks")
            ? Mth.clamp(tag.getInt("IntervalTicks"), MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS) : 20;
        offset = tag.getFloat("Offset");
        outputSpeed = tag.contains("OutputSpeed") ? tag.getFloat("OutputSpeed") : tag.getFloat("Generated");
        super.read(tag, registries, clientPacket);
    }

    public Mode getMode() {
        return mode;
    }

    public int getLockedRate() {
        return lockedRate;
    }

    public int getStrengthMultiplier() {
        return strengthMultiplier;
    }

    public int getInitialSpeed() {
        return initialSpeed;
    }

    public int getIntervalTicks() {
        return intervalTicks;
    }

    public float getOffset() {
        return offset;
    }

    public void applySettings(Mode mode, int lockedRate, int intervalTicks, int strengthMultiplier,
                              int initialSpeed) {
        this.mode = mode;
        this.lockedRate = Mth.clamp(lockedRate, MIN_RATE, MAX_RATE);
        this.strengthMultiplier = Mth.clamp(strengthMultiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.initialSpeed = Mth.clamp(initialSpeed, MIN_INITIAL_SPEED, MAX_INITIAL_SPEED);
        this.intervalTicks = Mth.clamp(intervalTicks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        tickCounter = this.intervalTicks;
        setChanged();
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        super.addToGoggleTooltip(tooltip, isPlayerSneaking);

        tooltip.add(Component.literal(" ")
            .append(Component.translatable("gui.goggles.redstone_throttle.output"))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("    ")
            .append(Component.literal(String.format("%.0f", Math.abs(outputSpeed))).withStyle(ChatFormatting.AQUA))
            .append(Component.literal(" RPM").withStyle(ChatFormatting.DARK_GRAY)));

        tooltip.add(Component.literal(" ")
            .append(Component.translatable("gui.goggles.redstone_throttle.mode"))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("    ").append(
            Component.translatable(mode == Mode.STRENGTH
                ? "gui.goggles.redstone_throttle.mode.strength"
                : mode == Mode.FIXED
                    ? "gui.goggles.redstone_throttle.mode.fixed"
                    : "gui.goggles.redstone_throttle.mode.multiplier").withStyle(ChatFormatting.AQUA)));

        if (mode == Mode.FIXED) {
            tooltip.add(Component.literal("    ")
                .append(Component.literal(lockedRate + " ").withStyle(ChatFormatting.AQUA))
                .append(Component.translatable("gui.goggles.redstone_throttle.rpm_per_step")
                    .withStyle(ChatFormatting.DARK_GRAY)));
        } else if (mode == Mode.MULTIPLIER) {
            tooltip.add(Component.literal("    ")
                .append(Component.literal(initialSpeed + " RPM").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" "
                    + Component.translatable("gui.goggles.redstone_throttle.initial_speed").getString())
                    .withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(Component.literal("    ")
                .append(Component.literal(strengthMultiplier + " ×").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" "
                    + Component.translatable("gui.goggles.redstone_throttle.multiplier").getString())
                    .withStyle(ChatFormatting.DARK_GRAY)));
        } else {
            tooltip.add(Component.literal("    ").append(
                Component.translatable("gui.goggles.redstone_throttle.interval",
                    String.format("%.2f", intervalTicks / 20.0f)).withStyle(ChatFormatting.DARK_GRAY)));
        }

        if (mode == Mode.FIXED) {
            tooltip.add(Component.literal("    ").append(
                Component.translatable("gui.goggles.redstone_throttle.interval",
                    String.format("%.2f", intervalTicks / 20.0f)).withStyle(ChatFormatting.DARK_GRAY)));
        }

        return true;
    }
}
