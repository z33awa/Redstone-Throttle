package com.z33awa.redstonethrottle.content.modulator;

import java.util.List;

import com.simibubi.create.content.kinetics.KineticNetwork;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.content.kinetics.base.DirectionalShaftHalvesBlockEntity;
import com.simibubi.create.content.kinetics.base.GeneratingKineticBlockEntity;
import com.simibubi.create.content.kinetics.base.IRotate;
import com.simibubi.create.content.kinetics.base.KineticBlockEntity;
import com.simibubi.create.content.kinetics.gearbox.GearboxBlockEntity;
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
        STRENGTH, FIXED, MULTIPLIER;

        public static Mode of(int ordinal) {
            return values()[Mth.clamp(ordinal, 0, values().length - 1)];
        }
    }

    private static final float STRENGTH_RATE = 0.5f;
    private static final float STRESS_CAPACITY = 256.0f;
    public static final int MAX_RATE = 256;
    public static final int MIN_RATE = 1;
    public static final int MAX_MULTIPLIER = 256;
    public static final int MIN_MULTIPLIER = 1;
    public static final int MAX_INITIAL_SPEED = 256;
    public static final int MIN_INITIAL_SPEED = 0;
    public static final int MAX_INTERVAL_TICKS = 200;
    public static final int MIN_INTERVAL_TICKS = 6;

    private Mode mode = Mode.STRENGTH;
    private int lockedRate = 16;
    private int strengthMultiplier = 16;
    private int initialSpeed = 0;
    private int intervalTicks = 20;

    private int tickCounter;
    private boolean wasSignalActive;
    private int prevSignalDir;
    private float offset;
    private float cachedGenerated;
    private int overstressTicks;
    private int normalTicks;
    private boolean inputIsOverstressed;
    private static final int OVERSTRESS_HYSTERESIS = 10;

    public RedstoneSpeedModulatorBlockEntity(BlockEntityType<?> type, BlockPos pos, BlockState state) {
        super(type, pos, state);
    }

    @Override
    public void onLoad() {
        super.onLoad();
        if (level != null && !level.isClientSide) {
            updateGeneratedRotation();
        }
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
        float inputSpeed = readInputSpeed(facing);

        int inputSign = Math.abs(inputSpeed) < 0.01f ? 0 : (inputSpeed > 0 ? 1 : -1);
        float inputAbs = Math.abs(inputSpeed);

        float rawDesired;
        if (mode == Mode.MULTIPLIER) {
            if (inputSign == 0) {
                rawDesired = 0f;
            } else {
                int dir = initialSpeed >= 0 ? 1 : -1;
                float delta = (leftSig - rightSig) * (float) strengthMultiplier;
                float targetAbs = Math.abs(initialSpeed) + delta;
                targetAbs = Mth.clamp(targetAbs, 0f, maxSpeed);
                rawDesired = dir * targetAbs;
            }
        } else {
            // STRENGTH / FIXED: accumulate offset over time
            int interval = Math.max(MIN_INTERVAL_TICKS, intervalTicks);
            float offsetMin = -inputAbs;
            float offsetMax = maxSpeed - inputAbs;

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

                if (inputSign != 0 && delta != 0f) {
                    offset = Mth.clamp(offset + delta, offsetMin, offsetMax);
                    setChanged();
                }
            }

            if (inputSign != 0) {
                offset = Mth.clamp(offset, offsetMin, offsetMax);
            }

            if (inputSign == 0) {
                rawDesired = 0f;
            } else {
                float targetAbs = inputAbs + offset;
                targetAbs = Mth.clamp(targetAbs, 0f, maxSpeed);
                rawDesired = inputSign * targetAbs;
            }
        }

        // Gearbox reverses direction when crossing perpendicular axes
        rawDesired *= getGearDirectionMultiplier(facing);

        float desired = Math.round(rawDesired);

        if (Math.abs(desired - cachedGenerated) >= 0.5f) {
            cachedGenerated = desired;
            updateGeneratedRotation();
        }

        // Reset stress hysteresis when output stops or switches direction
        if (Math.abs(cachedGenerated) < 0.01f) {
            overstressTicks = 0;
            normalTicks = 0;
            inputIsOverstressed = false;
        }

        propagateStressToInput();
    }

    /**
     * Returns the world direction of the left redstone input face for the given facing.
     * The left input is the model's west face (side_redstone_west texture) after blockstate rotation.
     */
    public static Direction redstoneLeft(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.WEST;
            case SOUTH -> Direction.WEST;
            case EAST -> Direction.SOUTH;
            case WEST -> Direction.SOUTH;
            case UP -> Direction.EAST;
            case DOWN -> Direction.WEST;
        };
    }

    /**
     * Returns the world direction of the right redstone input face for the given facing.
     * The right input is the model's east face (side_redstone_east texture) after blockstate rotation.
     */
    public static Direction redstoneRight(Direction facing) {
        return switch (facing) {
            case NORTH -> Direction.EAST;
            case SOUTH -> Direction.EAST;
            case EAST -> Direction.NORTH;
            case WEST -> Direction.NORTH;
            case UP -> Direction.WEST;
            case DOWN -> Direction.EAST;
        };
    }

    private float readInputSpeed(Direction facing) {
        BlockPos neighborPos = worldPosition.relative(facing.getOpposite());
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (!(be instanceof KineticBlockEntity kbe))
            return 0f;

        // Verify the neighbour actually has a shaft pointing toward us.
        BlockState neighborState = kbe.getBlockState();
        Block neighborBlock = neighborState.getBlock();
        Direction towardModulator = facing;

        if (!(neighborBlock instanceof IRotate rotate)
            || !rotate.hasShaftTowards(level, neighborPos, neighborState, towardModulator))
            return 0f;

        return kbe.getTheoreticalSpeed();
    }

    /** Read the speed of the kinetic block adjacent to the BACK face. Safe for client-side use (renderer). */
    public float readInputSpeed() {
        if (level == null) return 0f;
        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        BlockPos neighborPos = worldPosition.relative(facing.getOpposite());
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (!(be instanceof KineticBlockEntity kbe)) return 0f;

        BlockState neighborState = kbe.getBlockState();
        Block neighborBlock = neighborState.getBlock();
        Direction towardModulator = facing;

        if (!(neighborBlock instanceof IRotate rotate)
            || !rotate.hasShaftTowards(level, neighborPos, neighborState, towardModulator))
            return 0f;

        return kbe.getSpeed() * getGearDirectionMultiplier(facing);
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
        if (level == null || level.isClientSide) {
            lastCapacityProvided = STRESS_CAPACITY;
            return STRESS_CAPACITY;
        }

        float maxOutput = STRESS_CAPACITY;
        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        BlockPos neighborPos = worldPosition.relative(facing.getOpposite());
        BlockEntity be = level.getBlockEntity(neighborPos);

        if (be instanceof KineticBlockEntity kbe && kbe.hasNetwork()) {
            KineticNetwork inputNetwork = kbe.getOrCreateNetwork();
            float inputAvailable = Math.max(0, inputNetwork.calculateCapacity() - inputNetwork.calculateStress());
            float outputSpeed = Math.abs(cachedGenerated);
            if (outputSpeed > 0.01f) {
                maxOutput = Math.min(STRESS_CAPACITY, inputAvailable / outputSpeed);
            }
        } else {
            maxOutput = 0f;
        }

        lastCapacityProvided = maxOutput;
        return maxOutput;
    }

    /**
     * Returns -1 if the input passes through a gearbox that reverses direction across perpendicular axes,
     * otherwise 1. The gearbox direction logic matches RotationPropagator.getAxisModifier.
     */
    private float getGearDirectionMultiplier(Direction facing) {
        BlockPos neighborPos = worldPosition.relative(facing.getOpposite());
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (!(be instanceof GearboxBlockEntity gbe) || !gbe.hasSource())
            return 1f;

        Direction sourceDir = ((DirectionalShaftHalvesBlockEntity) gbe).getSourceFacing();
        Direction towardModulator = facing;
        if (towardModulator.getAxis() == sourceDir.getAxis())
            return towardModulator != sourceDir ? -1f : 1f;
        return towardModulator.getAxisDirection() == sourceDir.getAxisDirection() ? -1f : 1f;
    }

    private void propagateStressToInput() {
        Direction facing = getBlockState().getValue(RedstoneSpeedModulatorBlock.FACING);
        BlockPos neighborPos = worldPosition.relative(facing.getOpposite());
        BlockEntity be = level.getBlockEntity(neighborPos);
        if (!(be instanceof KineticBlockEntity kbe) || !kbe.hasNetwork()) {
            inputIsOverstressed = false;
            overstressTicks = 0;
            normalTicks = 0;
            return;
        }
        if (!hasNetwork()) {
            inputIsOverstressed = false;
            overstressTicks = 0;
            normalTicks = 0;
            return;
        }

        boolean outputOverStressed = getOrCreateNetwork().calculateStress() > getOrCreateNetwork().calculateCapacity()
            && IRotate.StressImpact.isEnabled();

        if (outputOverStressed) {
            overstressTicks++;
            normalTicks = 0;
        } else {
            normalTicks++;
            overstressTicks = 0;
        }

        KineticNetwork inputNet = kbe.getOrCreateNetwork();

        if (overstressTicks >= OVERSTRESS_HYSTERESIS && !inputIsOverstressed) {
            inputIsOverstressed = true;
            overstressTicks = OVERSTRESS_HYSTERESIS;
            // Force input network overstress via zero capacity on all members
            for (KineticBlockEntity member : inputNet.members.keySet()) {
                member.updateFromNetwork(0, 1, inputNet.getSize());
            }
        } else if (normalTicks >= OVERSTRESS_HYSTERESIS && inputIsOverstressed) {
            inputIsOverstressed = false;
            normalTicks = OVERSTRESS_HYSTERESIS;
            // updateNetwork() skips sync when currentStress==newStress (no member map changed).
            // Call updateFromNetwork on each member directly with the real capacity/stress.
            float cap = inputNet.calculateCapacity();
            float str = inputNet.calculateStress();
            int sz = inputNet.getSize();
            for (KineticBlockEntity member : inputNet.members.keySet()) {
                member.updateFromNetwork(cap, str, sz);
            }
        }
    }

    @Override
    public void remove() {
        super.remove();
    }

    @Override
    protected void write(CompoundTag tag, HolderLookup.Provider registries, boolean clientPacket) {
        tag.putByte("Mode", (byte) mode.ordinal());
        tag.putInt("LockedRate", lockedRate);
        tag.putInt("StrengthMultiplier", strengthMultiplier);
        tag.putInt("InitialSpeed", initialSpeed);
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
        strengthMultiplier = Mth.clamp(tag.getInt("StrengthMultiplier"), MIN_MULTIPLIER, MAX_MULTIPLIER);
        if (strengthMultiplier == 0) strengthMultiplier = 16;
        initialSpeed = Mth.clamp(tag.getInt("InitialSpeed"), MIN_INITIAL_SPEED, MAX_INITIAL_SPEED);
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

    /** Server-side: apply settings received from the GUI packet. */
    public void applySettings(Mode mode, int lockedRate, int intervalTicks, int strengthMultiplier, int initialSpeed) {
        this.mode = mode;
        this.lockedRate = Mth.clamp(lockedRate, MIN_RATE, MAX_RATE);
        this.strengthMultiplier = Mth.clamp(strengthMultiplier, MIN_MULTIPLIER, MAX_MULTIPLIER);
        this.initialSpeed = Mth.clamp(initialSpeed, MIN_INITIAL_SPEED, MAX_INITIAL_SPEED);
        this.intervalTicks = Mth.clamp(intervalTicks, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
        setChanged();
        sendData();
    }

    @Override
    public boolean addToGoggleTooltip(List<Component> tooltip, boolean isPlayerSneaking) {
        boolean added = super.addToGoggleTooltip(tooltip, isPlayerSneaking);
        added = true;

        tooltip.add(Component.literal(" ")
            .append(Component.translatable("gui.goggles.redstone_throttle.output"))
            .withStyle(ChatFormatting.GRAY));
        tooltip.add(Component.literal("    ")
            .append(Component.literal(String.format("%.0f", Math.abs(cachedGenerated))).withStyle(ChatFormatting.AQUA))
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
                .append(Component.translatable("gui.goggles.redstone_throttle.rpm_per_step").withStyle(ChatFormatting.DARK_GRAY)));
        }

        if (mode == Mode.MULTIPLIER) {
            tooltip.add(Component.literal("    ")
                .append(Component.literal(initialSpeed + " RPM").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" " + Component.translatable("gui.goggles.redstone_throttle.initial_speed").getString()).withStyle(ChatFormatting.DARK_GRAY)));
            tooltip.add(Component.literal("    ")
                .append(Component.literal(strengthMultiplier + " ×").withStyle(ChatFormatting.AQUA))
                .append(Component.literal(" " + Component.translatable("gui.goggles.redstone_throttle.multiplier").getString()).withStyle(ChatFormatting.DARK_GRAY)));
        }

        if (mode != Mode.MULTIPLIER) {
            tooltip.add(Component.literal("    ").append(
                Component.translatable("gui.goggles.redstone_throttle.interval",
                    String.format("%.2f", intervalTicks / 20.0f)).withStyle(ChatFormatting.DARK_GRAY)));
        }

        return added;
    }
}
