package com.apothicflux.client.screen;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

/**
 * 自定义滑块，使用自定义 GUI 纹理渲染把手。
 * 滑块底轨已包含在背景纹理中，本控件仅绘制可拖动的把手。
 */
@OnlyIn(Dist.CLIENT)
public class AttributeSlider extends AbstractSliderButton
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("apothicflux", "textures/gui/flux_stats_bookshelf_tier_gui.png");

    private final float min;
    private final float max;
    private final float step;
    private Runnable onValueChanged;

    public AttributeSlider(int x, int y, int width, int height,
                           float min, float max, float initial,
                           float step)
    {
        super(x, y, width, height, Component.empty(), 0.0D);
        this.min = min;
        this.max = max;
        this.step = step;

        // 计算初始 value (0.0 ~ 1.0)
        double normalized = (initial - min) / (max - min);
        this.value = Math.max(0.0D, Math.min(1.0D, normalized));

        updateMessage();
    }

    public void setOnValueChanged(Runnable callback)
    {
        this.onValueChanged = callback;
    }

    public float getFloatValue()
    {
        float raw = (float) (min + value * (max - min));
        return Math.round(raw / step) * step;
    }

    public int getIntValue()
    {
        return Math.round(getFloatValue());
    }

    public void setFloatValue(float val)
    {
        float clamped = Math.max(min, Math.min(max, val));
        double newValue = (clamped - min) / (max - min);
        if (Math.abs(this.value - newValue) > 0.0001D)
        {
            this.value = newValue;
            applyValue();
            updateMessage();
            if (onValueChanged != null)
                onValueChanged.run();
        }
    }

    // ===== 公开内部状态供 EditBox 使用 =====

    public float getMin()     { return min; }
    public float getMax()     { return max; }
    public float getStep()    { return step; }

    // ===== 内部覆盖 =====

    @Override
    protected void updateMessage()
    {
        float current = getFloatValue();
        if (step >= 1.0F)
            this.setMessage(Component.literal(String.valueOf(getIntValue())));
        else
            this.setMessage(Component.literal(String.format("%.1f", current)));
    }

    @Override
    protected void applyValue()
    {
        updateMessage();
        if (onValueChanged != null)
            onValueChanged.run();
    }

    @Override
    public void onClick(double mouseX, double mouseY)
    {
        super.onClick(mouseX, mouseY);
    }

    @Override
    protected void onDrag(double mouseX, double mouseY, double dragX, double dragY)
    {
        this.value = (mouseX - (double)(this.getX() + 4)) / (double)(this.width - 8);
        this.value = Math.max(0.0D, Math.min(1.0D, this.value));
        this.applyValue();
        this.updateMessage();
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (button == 0 && this.isActive())
        {
            this.onDrag(mouseX, mouseY, dragX, dragY);
            return true;
        }
        return false;
    }

    @Override
    public void onRelease(double mouseX, double mouseY)
    {
        super.onRelease(mouseX, mouseY);
    }

    // ==================== 自定义渲染 ====================

    /**
     * 完全重写渲染：不调用 super.renderWidget()，仅使用纹理绘制把手。
     * 滑块底轨已包含在 GUI 背景纹理中。
     */
    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        // 计算把手 X 坐标（基于当前 value）
        int handleX = this.getX() + (int)(this.value * (this.width - 8));

        // 判断悬停/聚焦/拖动状态以选择纹理区域
        boolean hovered = this.isHoveredOrFocused();

        // 绘制把手：未选中 U=195，选中/悬停 U=205；V=0；宽=8，高=19
        guiGraphics.blit(TEXTURE, handleX, this.getY(),
                hovered ? 205 : 195, 0, 8, 19);
    }
}