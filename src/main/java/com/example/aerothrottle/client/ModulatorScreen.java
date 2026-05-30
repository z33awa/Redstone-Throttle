package com.example.aerothrottle.client;

import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;
import com.example.aerothrottle.content.modulator.RedstoneSpeedModulatorBlockEntity.Mode;
import com.example.aerothrottle.network.UpdateModulatorPacket;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.CycleButton;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import net.neoforged.neoforge.network.PacketDistributor;

public class ModulatorScreen extends Screen {

    private static final int WIDTH = 220;
    private static final int HEIGHT = 170;
    public static final int MAX_INTERVAL_TICKS = 200;
    public static final int MIN_INTERVAL_TICKS = 6;

    private final BlockPos pos;
    private Mode mode;
    private int lockedRate;
    private int strengthMultiplier;
    private int initialSpeed;
    private int intervalTicks;

    private RateSlider rateSlider;
    private InitialSpeedSlider initialSpeedSlider;
    private MultiplierSlider multiplierSlider;
    private IntervalSlider intervalSlider;

    public ModulatorScreen(RedstoneSpeedModulatorBlockEntity be) {
        super(Component.translatable("gui.aero_throttle.title"));
        this.pos = be.getBlockPos();
        this.mode = be.getMode();
        this.lockedRate = be.getLockedRate();
        this.strengthMultiplier = be.getStrengthMultiplier();
        this.initialSpeed = be.getInitialSpeed();
        this.intervalTicks = be.getIntervalTicks();
    }

    @Override
    protected void init() {
        super.init();
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;

        CycleButton<Mode> modeButton = CycleButton.<Mode>builder(this::modeLabel)
            .withValues(Mode.values())
            .withInitialValue(mode)
            .create(left + 20, top + 30, WIDTH - 40, 20,
                Component.translatable("gui.aero_throttle.mode"),
                (btn, value) -> {
                    mode = value;
                    boolean isMul = (mode == Mode.MULTIPLIER);
                    rateSlider.visible = (mode == Mode.FIXED);
                    initialSpeedSlider.visible = isMul;
                    multiplierSlider.visible = isMul;
                    intervalSlider.visible = !isMul;
                    sendUpdate();
                });
        addRenderableWidget(modeButton);

        rateSlider = new RateSlider(left + 20, top + 60, WIDTH - 40, 20, lockedRate);
        rateSlider.visible = (mode == Mode.FIXED);
        addRenderableWidget(rateSlider);

        initialSpeedSlider = new InitialSpeedSlider(left + 20, top + 60, WIDTH - 40, 20, initialSpeed);
        initialSpeedSlider.visible = (mode == Mode.MULTIPLIER);
        addRenderableWidget(initialSpeedSlider);

        multiplierSlider = new MultiplierSlider(left + 20, top + 90, WIDTH - 40, 20, strengthMultiplier);
        multiplierSlider.visible = (mode == Mode.MULTIPLIER);
        addRenderableWidget(multiplierSlider);

        intervalSlider = new IntervalSlider(left + 20, top + 90, WIDTH - 40, 20, intervalTicks);
        intervalSlider.visible = (mode != Mode.MULTIPLIER);
        addRenderableWidget(intervalSlider);

        addRenderableWidget(Button.builder(
            Component.translatable("gui.done"),
            btn -> onClose())
            .bounds(left + (WIDTH - 80) / 2, top + HEIGHT - 30, 80, 20)
            .build());
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // Solid dim instead of renderBackground() — vanilla applies a blur to the world
        // backdrop that bleeds into screen-space text on some setups.
        graphics.fill(0, 0, this.width, this.height, 0xA0000000);
        int left = (this.width - WIDTH) / 2;
        int top = (this.height - HEIGHT) / 2;
        graphics.fill(left, top, left + WIDTH, top + HEIGHT, 0xFF181820);
        graphics.fill(left, top, left + WIDTH, top + 1, 0xFFFFFFFF);
        graphics.fill(left, top + HEIGHT - 1, left + WIDTH, top + HEIGHT, 0xFFFFFFFF);
        graphics.fill(left, top, left + 1, top + HEIGHT, 0xFFFFFFFF);
        graphics.fill(left + WIDTH - 1, top, left + WIDTH, top + HEIGHT, 0xFFFFFFFF);
        graphics.drawCenteredString(this.font, this.title, left + WIDTH / 2, top + 10, 0xFFFFFF);
        super.render(graphics, mouseX, mouseY, partialTick);
    }

    @Override
    public void renderBackground(GuiGraphics graphics, int mouseX, int mouseY, float partialTick) {
        // intentionally empty — render() draws its own backdrop without blur
    }

    @Override
    public boolean isPauseScreen() {
        return false;
    }

    private Component modeLabel(Mode m) {
        String key = m == Mode.STRENGTH ? "gui.aero_throttle.mode.strength"
            : m == Mode.FIXED ? "gui.aero_throttle.mode.fixed"
            : "gui.aero_throttle.mode.multiplier";
        return Component.translatable("gui.aero_throttle.mode")
            .append(Component.literal(": "))
            .append(Component.translatable(key));
    }

    private static Component intervalLabel(int ticks) {
        String value = String.format("%dt (%.2fs)", ticks, ticks / 20.0);
        return Component.translatable("gui.aero_throttle.interval")
            .append(Component.literal(": " + value));
    }

    private void sendUpdate() {
        PacketDistributor.sendToServer(new UpdateModulatorPacket(
            pos, (byte) mode.ordinal(), lockedRate, intervalTicks, strengthMultiplier, initialSpeed));
    }

    private class RateSlider extends AbstractSliderButton {
        RateSlider(int x, int y, int w, int h, int initial) {
            super(x, y, w, h,
                Component.empty(),
                normalize(initial));
            updateMessage();
        }

        private static double normalize(int v) {
            int min = RedstoneSpeedModulatorBlockEntity.MIN_RATE;
            int max = RedstoneSpeedModulatorBlockEntity.MAX_RATE;
            return (Mth.clamp(v, min, max) - min) / (double) (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.aero_throttle.rate")
                .append(Component.literal(": " + lockedRate + " RPM/step")));
        }

        @Override
        protected void applyValue() {
            int min = RedstoneSpeedModulatorBlockEntity.MIN_RATE;
            int max = RedstoneSpeedModulatorBlockEntity.MAX_RATE;
            lockedRate = (int) Math.round(min + value * (max - min));
            sendUpdate();
        }
    }

    private class InitialSpeedSlider extends AbstractSliderButton {
        InitialSpeedSlider(int x, int y, int w, int h, int initial) {
            super(x, y, w, h,
                Component.empty(),
                normalize(initial));
            updateMessage();
        }

        private static double normalize(int v) {
            int min = RedstoneSpeedModulatorBlockEntity.MIN_INITIAL_SPEED;
            int max = RedstoneSpeedModulatorBlockEntity.MAX_INITIAL_SPEED;
            return (Mth.clamp(v, min, max) - min) / (double) (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.aero_throttle.initial_speed")
                .append(Component.literal(": " + initialSpeed + " RPM")));
        }

        @Override
        protected void applyValue() {
            int min = RedstoneSpeedModulatorBlockEntity.MIN_INITIAL_SPEED;
            int max = RedstoneSpeedModulatorBlockEntity.MAX_INITIAL_SPEED;
            initialSpeed = (int) Math.round(min + value * (max - min));
            sendUpdate();
        }
    }

    private class MultiplierSlider extends AbstractSliderButton {
        MultiplierSlider(int x, int y, int w, int h, int initial) {
            super(x, y, w, h,
                Component.empty(),
                normalize(initial));
            updateMessage();
        }

        private static double normalize(int v) {
            int min = RedstoneSpeedModulatorBlockEntity.MIN_MULTIPLIER;
            int max = RedstoneSpeedModulatorBlockEntity.MAX_MULTIPLIER;
            return (Mth.clamp(v, min, max) - min) / (double) (max - min);
        }

        @Override
        protected void updateMessage() {
            setMessage(Component.translatable("gui.aero_throttle.multiplier")
                .append(Component.literal(": " + strengthMultiplier + " ×")));
        }

        @Override
        protected void applyValue() {
            int min = RedstoneSpeedModulatorBlockEntity.MIN_MULTIPLIER;
            int max = RedstoneSpeedModulatorBlockEntity.MAX_MULTIPLIER;
            strengthMultiplier = (int) Math.round(min + value * (max - min));
            sendUpdate();
        }
    }

    private class IntervalSlider extends AbstractSliderButton {
        IntervalSlider(int x, int y, int w, int h, int initial) {
            super(x, y, w, h,
                Component.empty(),
                normalize(initial));
            updateMessage();
        }

        private static double normalize(int v) {
            return (Mth.clamp(v, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS) - MIN_INTERVAL_TICKS)
                / (double) (MAX_INTERVAL_TICKS - MIN_INTERVAL_TICKS);
        }

        @Override
        protected void updateMessage() {
            setMessage(intervalLabel(intervalTicks));
        }

        @Override
        protected void applyValue() {
            intervalTicks = (int) Math.round(MIN_INTERVAL_TICKS
                + value * (MAX_INTERVAL_TICKS - MIN_INTERVAL_TICKS));
            sendUpdate();
        }
    }
}
