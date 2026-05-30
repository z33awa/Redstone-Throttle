package com.z33awa.redstonethrottle.client;

import com.z33awa.redstonethrottle.content.modulator.RedstoneSpeedModulatorBlockEntity;
import com.z33awa.redstonethrottle.content.modulator.RedstoneSpeedModulatorBlockEntity.Mode;
import com.z33awa.redstonethrottle.network.UpdateModulatorPacket;
import com.mojang.blaze3d.platform.Lighting;

import com.simibubi.create.AllSoundEvents;
import com.simibubi.create.foundation.gui.AllIcons;

import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;

import com.z33awa.redstonethrottle.registry.ModBlocks;

import net.minecraft.world.item.ItemStack;

import com.mojang.blaze3d.systems.RenderSystem;

import net.minecraft.ChatFormatting;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.narration.NarrationElementOutput;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.util.Mth;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import net.neoforged.neoforge.network.PacketDistributor;

import java.util.List;
import java.util.Optional;

import static com.z33awa.redstonethrottle.content.modulator.RedstoneSpeedModulatorBlockEntity.*;

public class ModulatorScreen extends Screen {

    // ═══════════════════════════════════════════════════════════════
    //  TEXTURE  —  like Create's AllGuiTextures
    //  Your PNG:  230×170  (transparent padding around artwork)
    //  Artwork:   174×116  at top-left
    //  ⚠ TEX_W / TEX_H MUST equal the actual PNG file dimensions.
    //  If Minecraft resizes NPOT → power-of-2, set these to the
    //  power-of-2 size (e.g. 256×256) and pad the PNG accordingly.
    // ═══════════════════════════════════════════════════════════════
    private static final ResourceLocation GUI_TEXTURE =
            ResourceLocation.fromNamespaceAndPath("redstone_throttle", "textures/gui/modulator.png");

    private static final int TEX_W = 230;   // PNG file width
    private static final int TEX_H = 170;   // PNG file height
    private static final int ART_X = 0;     // artwork X within PNG
    private static final int ART_Y = 0;     // artwork Y within PNG
    private static final int ART_W = 174;   // artwork width
    private static final int ART_H = 116;   // artwork height

    // ── Overall scale & position ──────────────────────────────────
    private static final float UI_SCALE   = 1.0f;  // 1.0=原大小, 越大越放大
    private static final int   LEFT_SHIFT = 0;    // px 左移（0=居中）

    private static final int WIDTH  = (int)(ART_W * UI_SCALE);
    private static final int HEIGHT = (int)(ART_H * UI_SCALE);

    // ═══════════════════════════════════════════════════════════════
    //  POSITION TWEAKS  (original unscaled px, auto-multiplied by UI_SCALE)
    // ═══════════════════════════════════════════════════════════════
    private static final int _TITLE_Y = 3;
    private static final int _TITLE_X = -4;    // X offset from centre
    private static final int _MODE_Y  = 25;
    private static final int _MODE_X  = -4;    // X offset from centre
    private static final int _TAB_Y   = 22;
    private static final int _TAB_W   = 50;
    private static final int _TAB_H   = 16;
    private static final int _LABEL_X  = 40;
    private static final int _VALUE_CX = 25;
    private static final int _FIXED_RATE_Y   = 47;
    private static final int _FIXED_INTERVAL_Y = 69;
    private static final int _STRENGTH_INTERVAL_Y = 47;
    private static final int _MULT_INITIAL_Y    = 47;
    private static final int _MULT_MULTIPLIER_Y = 69;
    private static final int _CONFIRM_X    = 141;
    private static final int _CONFIRM_Y    = 93;
    private static final int _CONFIRM_SIZE = 16;
    private static final int _PREVIEW_X    = 180;
    private static final int _PREVIEW_Y    = 62;
    private static final int _PREVIEW_SIZE = 80;

    // Scaled values (used in code)
    private static final int TITLE_Y = s(_TITLE_Y);
    private static final int TITLE_X = s(_TITLE_X);
    private static final int MODE_Y  = s(_MODE_Y);
    private static final int MODE_X  = s(_MODE_X);
    private static final int TAB_Y   = s(_TAB_Y);
    private static final int TAB_WIDTH  = s(_TAB_W);
    private static final int TAB_HEIGHT = s(_TAB_H);
    private static final int LABEL_X  = s(_LABEL_X);
    private static final int VALUE_CENTER_X_OFFSET = s(_VALUE_CX);
    private static final int FIXED_RATE_Y     = s(_FIXED_RATE_Y);
    private static final int FIXED_INTERVAL_Y = s(_FIXED_INTERVAL_Y);
    private static final int STRENGTH_INTERVAL_Y = s(_STRENGTH_INTERVAL_Y);
    private static final int MULT_INITIAL_Y   = s(_MULT_INITIAL_Y);
    private static final int MULT_MULTIPLIER_Y = s(_MULT_MULTIPLIER_Y);
    private static final int CONFIRM_X    = s(_CONFIRM_X);
    private static final int CONFIRM_Y    = s(_CONFIRM_Y);
    private static final int CONFIRM_SIZE = s(_CONFIRM_SIZE);
    private static final int PREVIEW_X    = s(_PREVIEW_X);
    private static final int PREVIEW_Y    = s(_PREVIEW_Y);
    private static final int PREVIEW_SIZE = s(_PREVIEW_SIZE);
    private static int s(int v) { return (int)(v * UI_SCALE); }

    // ---- Screen dim ----
    private static final int SCREEN_DIM_TOP = 0x40_101010;
    private static final int SCREEN_DIM_BOT = 0x50_101010;

    // ═══════════════════════════════════════════════════════════════
    //  COLOUR PALETTE
    // ═══════════════════════════════════════════════════════════════
    private static final int COLOR_TITLE = 0xFF_ECC048;
    private static final int COLOR_VALUE = 0xFF_FFFFFF;
    private static final int COLOR_LABEL = 0xFF_FFFFFF;
    private static final int COLOR_MODE  = 0xFF_FFFFFF;

    // ═══════════════════════════════════════════════════════════════
    //  STATE
    // ═══════════════════════════════════════════════════════════════
    private final BlockPos   pos;
    private final Level      level;
    private final BlockState blockState;
    private Mode mode;
    private int lockedRate;
    private int strengthMultiplier;
    private int initialSpeed;
    private int intervalTicks;

    private int guiLeft;
    private int guiTop;
    private long lastSoundMs;

    // ── Constructor ───────────────────────────────────────────────
    public ModulatorScreen(RedstoneSpeedModulatorBlockEntity be) {
        super(Component.translatable("gui.redstone_throttle.title"));
        this.pos               = be.getBlockPos();
        this.level             = be.getLevel();
        this.blockState        = be.getBlockState();
        this.mode              = be.getMode();
        this.lockedRate        = be.getLockedRate();
        this.strengthMultiplier = be.getStrengthMultiplier();
        this.initialSpeed       = be.getInitialSpeed();
        this.intervalTicks      = be.getIntervalTicks();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INIT
    // ═══════════════════════════════════════════════════════════════
    @Override
    protected void init() {
        super.init();
        this.guiLeft = (this.width  - WIDTH)  / 2 - LEFT_SHIFT;
        this.guiTop  = (this.height - HEIGHT) / 2;
        clearWidgets();
        createConfirmButton();
    }

    private void createConfirmButton() {
        addRenderableWidget(new ConfirmButton(
                guiLeft + CONFIRM_X, guiTop + CONFIRM_Y, CONFIRM_SIZE));
    }

    // ═══════════════════════════════════════════════════════════════
    //  RENDER  — same order as Create's AbstractSimiScreen
    // ═══════════════════════════════════════════════════════════════
    @Override
    public void render(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        renderBackground(g, mouseX, mouseY, partialTick);
        // Texture — force full brightness then blit
        RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        g.blit(GUI_TEXTURE, guiLeft, guiTop, ART_X, ART_Y,
                ART_W, ART_H, TEX_W, TEX_H);

        g.drawCenteredString(font, title,
                guiLeft + WIDTH / 2 + TITLE_X, guiTop + TITLE_Y, COLOR_TITLE);

        Component modeText = Component.translatable(
                "gui.redstone_throttle.mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
        g.drawCenteredString(font, modeText,
                guiLeft + WIDTH / 2 + MODE_X, guiTop + MODE_Y, COLOR_MODE);

        drawValueLabels(g);
        renderBlockPreview(g);
        super.render(g, mouseX, mouseY, partialTick);
        renderVanillaTooltip(g, mouseX, mouseY);
    }

    @Override
    public void renderBackground(GuiGraphics g, int mouseX, int mouseY, float partialTick) {
        g.fillGradient(0, 0, width, height, SCREEN_DIM_TOP, SCREEN_DIM_BOT);
    }

    private void drawValueLabels(GuiGraphics g) {
        int labelX     = guiLeft + LABEL_X;
        int valCenterX = guiLeft + WIDTH / 2 + VALUE_CENTER_X_OFFSET;

        switch (mode) {
            case FIXED -> {
                g.drawString(font, Component.translatable("gui.redstone_throttle.rate"),
                        labelX, guiTop + FIXED_RATE_Y, COLOR_LABEL);
                g.drawCenteredString(font, lockedRate + " RPM",
                        valCenterX, guiTop + FIXED_RATE_Y, COLOR_VALUE);

                g.drawString(font, Component.translatable("gui.redstone_throttle.interval"),
                        labelX, guiTop + FIXED_INTERVAL_Y, COLOR_LABEL);
                g.drawCenteredString(font, String.format("%.1f s", intervalTicks / 20f),
                        valCenterX, guiTop + FIXED_INTERVAL_Y, COLOR_VALUE);
            }
            case STRENGTH -> {
                g.drawString(font, Component.translatable("gui.redstone_throttle.interval"),
                        labelX, guiTop + STRENGTH_INTERVAL_Y, COLOR_LABEL);
                g.drawCenteredString(font, String.format("%.1f s", intervalTicks / 20f),
                        valCenterX, guiTop + STRENGTH_INTERVAL_Y, COLOR_VALUE);
            }
            case MULTIPLIER -> {
                g.drawString(font, Component.translatable("gui.redstone_throttle.initial_speed"),
                        labelX, guiTop + MULT_INITIAL_Y, COLOR_LABEL);
                g.drawCenteredString(font, initialSpeed + " RPM",
                        valCenterX, guiTop + MULT_INITIAL_Y, COLOR_VALUE);

                g.drawString(font, Component.translatable("gui.redstone_throttle.multiplier"),
                        labelX, guiTop + MULT_MULTIPLIER_Y, COLOR_LABEL);
                g.drawCenteredString(font, strengthMultiplier + " ×",
                        valCenterX, guiTop + MULT_MULTIPLIER_Y, COLOR_VALUE);
            }
        }
    }

    private void renderBlockPreview(GuiGraphics g) {
        ItemStack stack = new ItemStack(ModBlocks.REDSTONE_SPEED_MODULATOR.get().asItem());
        g.pose().pushPose();
        g.pose().translate(guiLeft + PREVIEW_X, guiTop + PREVIEW_Y, 0);
        float s = PREVIEW_SIZE / 16f;
        g.pose().scale(s, s, 1f);
        Lighting.setupForFlatItems();
        g.renderItem(stack, 0, 0, 0);
        g.pose().popPose();
    }

    private boolean isOverModeBox(double mouseX, double mouseY) {
        Component t = Component.translatable("gui.redstone_throttle.mode." + mode.name().toLowerCase(java.util.Locale.ROOT));
        int tw = font.width(t);
        int x = guiLeft + WIDTH / 2 + MODE_X - tw / 2;
        int y = guiTop + MODE_Y;
        return mouseX >= x && mouseX <= x + tw && mouseY >= y && mouseY <= y + font.lineHeight;
    }

    private void renderVanillaTooltip(GuiGraphics g, int mouseX, int mouseY) {
        if (isOverModeBox(mouseX, mouseY)) {
            g.renderTooltip(font,
                    Component.translatable("gui.redstone_throttle.tooltip.switch_mode")
                            .withStyle(ChatFormatting.GRAY, ChatFormatting.ITALIC),
                    mouseX, mouseY);
            return;
        }
        String key = getRowLabelKeyAt(mouseX, mouseY);
        if (key == null) return;
        List<Component> lines = List.of(
                Component.translatable(key).withStyle(ChatFormatting.BLUE),
                Component.translatable("gui.redstone_throttle.tooltip.scroll")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC),
                Component.translatable("gui.redstone_throttle.tooltip.shift")
                        .withStyle(ChatFormatting.DARK_GRAY, ChatFormatting.ITALIC));
        g.renderTooltip(font, lines, Optional.empty(), mouseX, mouseY);
    }

    private String getRowLabelKeyAt(double mouseX, double mouseY) {
        if (mouseX < guiLeft + 10 || mouseX > guiLeft + WIDTH - 10) return null;
        return switch (mode) {
            case FIXED -> {
                if (inRow(mouseY, FIXED_RATE_Y))     yield "gui.redstone_throttle.rate";
                if (inRow(mouseY, FIXED_INTERVAL_Y)) yield "gui.redstone_throttle.interval";
                yield null;
            }
            case STRENGTH ->
                inRow(mouseY, STRENGTH_INTERVAL_Y) ? "gui.redstone_throttle.interval" : null;
            case MULTIPLIER -> {
                if (inRow(mouseY, MULT_INITIAL_Y))   yield "gui.redstone_throttle.initial_speed";
                if (inRow(mouseY, MULT_MULTIPLIER_Y)) yield "gui.redstone_throttle.multiplier";
                yield null;
            }
        };
    }

    private boolean inRow(double mouseY, int rowOffsetY) {
        return mouseY >= guiTop + rowOffsetY - 6 && mouseY <= guiTop + rowOffsetY + 14;
    }

    // ═══════════════════════════════════════════════════════════════
    //  INPUT
    // ═══════════════════════════════════════════════════════════════
    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (scrollY == 0) return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
        if (isOverTabArea(mouseX, mouseY)) {
            int next = mode.ordinal() + (scrollY > 0 ? -1 : 1);
            Mode[] vals = Mode.values();
            mode = vals[Mth.clamp(next, 0, vals.length - 1)];
            playScrollSound();
            sendUpdate();
            return true;
        }
        int step  = hasShiftDown() ? 10 : 1;
        int delta = scrollY > 0 ? step : -step;
        if (adjustValueAt(mouseX, mouseY, delta)) {
            playScrollSound();
            sendUpdate();
            return true;
        }
        return super.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    private boolean adjustValueAt(double mouseX, double mouseY, int delta) {
        switch (mode) {
            case FIXED -> {
                if (inRow(mouseY, FIXED_RATE_Y)) {
                    lockedRate = Mth.clamp(lockedRate + delta, MIN_RATE, MAX_RATE);
                    return true;
                }
                if (inRow(mouseY, FIXED_INTERVAL_Y)) {
                    intervalTicks = Mth.clamp(intervalTicks + delta, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
                    return true;
                }
            }
            case STRENGTH -> {
                if (inRow(mouseY, STRENGTH_INTERVAL_Y)) {
                    intervalTicks = Mth.clamp(intervalTicks + delta, MIN_INTERVAL_TICKS, MAX_INTERVAL_TICKS);
                    return true;
                }
            }
            case MULTIPLIER -> {
                if (inRow(mouseY, MULT_INITIAL_Y)) {
                    initialSpeed = Mth.clamp(initialSpeed + delta, MIN_INITIAL_SPEED, MAX_INITIAL_SPEED);
                    return true;
                }
                if (inRow(mouseY, MULT_MULTIPLIER_Y)) {
                    strengthMultiplier = Mth.clamp(strengthMultiplier + delta, MIN_MULTIPLIER, MAX_MULTIPLIER);
                    return true;
                }
            }
        }
        return false;
    }

    private boolean isOverTabArea(double mouseX, double mouseY) {
        int startX = guiLeft + (WIDTH - TAB_WIDTH * 3) / 2;
        return mouseX >= startX && mouseX <= startX + TAB_WIDTH * 3
                && mouseY >= guiTop + TAB_Y && mouseY <= guiTop + TAB_Y + TAB_HEIGHT;
    }

    @Override
    public boolean isPauseScreen() { return false; }

    // ═══════════════════════════════════════════════════════════════
    //  SOUND & NETWORK
    // ═══════════════════════════════════════════════════════════════
    private void playScrollSound() {
        long now = System.currentTimeMillis();
        if (now - lastSoundMs < 50) return;
        lastSoundMs = now;
        Minecraft.getInstance().getSoundManager()
                .play(SimpleSoundInstance.forUI(
                        net.minecraft.sounds.SoundEvents.NOTE_BLOCK_HAT.value(), 1.0f));
    }

    private void sendUpdate() {
        PacketDistributor.sendToServer(new UpdateModulatorPacket(
                pos, (byte) mode.ordinal(), lockedRate, intervalTicks, strengthMultiplier, initialSpeed));
    }

    @Override
    public void onClose() {
        AllSoundEvents.CONFIRM.playOnServer(level, pos);
        super.onClose();
    }

    // ═══════════════════════════════════════════════════════════════
    //  INNER — Confirm button (Create's I_CONFIRM icon)
    // ═══════════════════════════════════════════════════════════════
    private class ConfirmButton extends AbstractButton {
        ConfirmButton(int x, int y, int size) {
            super(x, y, size, size, Component.translatable("gui.done"));
        }

        @Override
        public void renderWidget(GuiGraphics g, int mx, int my, float pt) {
            if (isHoveredOrFocused()) {
                g.fill(getX() - 1, getY() - 1,
                        getX() + width + 1, getY() + height + 1, 0x90_9ABBd3);
            }
            AllIcons.I_CONFIRM.render(g, getX(), getY());
        }

        @Override
        public void onPress() { ModulatorScreen.this.onClose(); }

        @Override
        protected void updateWidgetNarration(NarrationElementOutput n) {
            defaultButtonNarrationText(n);
        }
    }
}
