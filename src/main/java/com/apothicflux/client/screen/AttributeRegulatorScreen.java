package com.apothicflux.client.screen;

import com.apothicflux.menu.AttributeRegulatorMenu;
import com.apothicflux.networking.ModMessages;
import com.apothicflux.networking.ServerboundC2SUpdateAttributesPacket;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Inventory;
import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.api.distmarker.OnlyIn;

import java.util.function.Predicate;

/**
 * 属性调节器 GUI 界面 — 使用自定义纹理。
 * 纹理路径: apothicflux:textures/gui/flux_stats_bookshelf_tier_gui.png
 *
 * 布局说明（基于 186x137 纹理）：
 * - 能量槽：leftPos+7, topPos+12, 宽=6, 最大高=112
 * - 滑块：leftPos+16, 四行 Y=12/35/58/81, 宽=120
 * - 输入框：leftPos+144, 四行 Y=13/35/57/79, 宽=32, 高=18
 * - 勾选框：纹理内建底图位于 (16,104) 和 (79,104)，选中时叠加覆盖层
 */
@OnlyIn(Dist.CLIENT)
public class AttributeRegulatorScreen extends AbstractContainerScreen<AttributeRegulatorMenu>
{
    private static final ResourceLocation TEXTURE =
            new ResourceLocation("apothicflux", "textures/gui/flux_stats_bookshelf_tier_gui.png");

    // 窗口尺寸
    private static final int GUI_WIDTH  = 186;
    private static final int GUI_HEIGHT = 137;

    // ---- 能量槽参数（相对于 leftPos/topPos） ----
    private static final int ENERGY_X = 7;
    private static final int ENERGY_Y = 12;
    private static final int ENERGY_W = 6;
    private static final int ENERGY_MAX_H = 112;

    // ---- 滑块参数 ----
    private static final int SLIDER_X = 16;
    private static final int SLIDER_W = 120;
    private static final int[] SLIDER_Y = {12, 35, 58, 81};

    // ---- 输入框参数 ----
    private static final int EDIT_X = 144;
    private static final int EDIT_W = 30;
    private static final int EDIT_H = 9;
    private static final int[] EDIT_Y = {13, 35, 57, 79};

    // ---- 勾选框参数（纹理内建底图坐标） ----
    private static final int CHECKBOX_SIZE = 20;
    private static final int[] CHECKBOX_X = {16, 79};
    private static final int CHECKBOX_Y = 104;

    // ---- UI 组件 ----
    private AttributeSlider eternaSlider;
    private AttributeSlider quantaSlider;
    private AttributeSlider arcanaSlider;
    private AttributeSlider cluesSlider;
    private EditBox eternaEdit;
    private EditBox quantaEdit;
    private EditBox arcanaEdit;
    private EditBox cluesEdit;

    /** 自定义勾选框状态（取代原版 Checkbox） */
    private boolean treasureEnabled = false;
    private boolean calibrationEnabled = false;

    private Button saveButton;

    // ---- 缓存 ----
    private boolean cachedTreasure = false;
    private boolean cachedCalibration = false;
    private int cachedClues = 0;

    /** 防递归同步标志 */
    private boolean isSyncing = false;

    /** 正在被拖拽的滑块 */
    private AttributeSlider draggingSlider = null;

    // ---- 上一次服务端数据快照 ----
    private int lastEternaData = -1;
    private int lastQuantaData = -1;
    private int lastArcanaData = -1;
    private int lastCluesData = -1;
    private int lastTreasureData = -1;
    private int lastCalibrationData = -1;

    private static final int MAX_ENERGY = 100_000_000;

    public AttributeRegulatorScreen(AttributeRegulatorMenu menu, Inventory playerInv, Component title)
    {
        super(menu, playerInv, title);
        this.imageWidth = GUI_WIDTH;
        this.imageHeight = GUI_HEIGHT;
    }

    @Override
    protected void init()
    {
        super.init();

        int left = this.leftPos;
        int top  = this.topPos;

        final float ZERO = 0.0F;

        // ===== 过滤器 =====
        Predicate<String> numberFilter = s -> {
            if (s.isEmpty()) return true;
            if (s.contains("-")) return false;
            long dots = s.chars().filter(c -> c == '.').count();
            if (dots > 1) return false;
            return s.chars().allMatch(c -> Character.isDigit(c) || c == '.');
        };

        // ===== 创建 4 行控件 =====
        eternaSlider = createSliderRow(left, top, 0, 0.0F, 100.0F, ZERO, 0.5F);
        eternaEdit   = createEditBox(left, top, 0, eternaSlider, numberFilter);

        quantaSlider = createSliderRow(left, top, 1, 0.0F, 100.0F, ZERO, 0.5F);
        quantaEdit   = createEditBox(left, top, 1, quantaSlider, numberFilter);

        arcanaSlider = createSliderRow(left, top, 2, 0.0F, 100.0F, ZERO, 0.5F);
        arcanaEdit   = createEditBox(left, top, 2, arcanaSlider, numberFilter);

        cluesSlider  = createSliderRow(left, top, 3, 0.0F, 15.0F, ZERO, 1.0F);
        cluesEdit    = createEditBox(left, top, 3, cluesSlider, numberFilter);

        // ===== 勾选框（自定义，无原版 Checkbox 控件） =====
        // 状态由点击事件维护，渲染在 renderBg 中处理
        this.treasureEnabled = false;
        this.calibrationEnabled = false;

        // ===== 保存按钮（位于勾选框下方） =====
        int saveBtnX = left + (GUI_WIDTH - 60) / 2;
        int saveBtnY = top + CHECKBOX_Y + CHECKBOX_SIZE + 12;
        saveButton = addRenderableWidget(Button.builder(
                Component.literal("保存"),
                (btn) -> onSaveClicked()
        ).bounds(saveBtnX, saveBtnY, 60, 20).build());

        resetLastDataMarkers();
    }

    private void resetLastDataMarkers()
    {
        this.lastEternaData = -1;
        this.lastQuantaData = -1;
        this.lastArcanaData = -1;
        this.lastCluesData = -1;
        this.lastTreasureData = -1;
        this.lastCalibrationData = -1;
    }

    // ==================== 辅助创建方法 ====================

    /**
     * 创建一行中的滑块
     */
    private AttributeSlider createSliderRow(int left, int top, int rowIndex,
                                            float min, float max, float initial,
                                            float step)
    {
        int sliderY = top + SLIDER_Y[rowIndex];

        AttributeSlider slider = new AttributeSlider(
                left + SLIDER_X, sliderY, SLIDER_W, 16,
                min, max, initial, step
        );

        slider.setOnValueChanged(() -> {
            if (isSyncing) return;
            isSyncing = true;
            updateEditBoxFromSlider(slider);
            isSyncing = false;
        });

        return addRenderableWidget(slider);
    }

    /**
     * 创建一行中的输入框
     */
    private EditBox createEditBox(int left, int top, int rowIndex,
                                  AttributeSlider slider,
                                  Predicate<String> filter)
    {
        int editX = left + EDIT_X + 1;
        int editY = top + EDIT_Y[rowIndex] + 4;

        EditBox edit = new EditBox(this.font, editX, editY, EDIT_W, EDIT_H,
                Component.empty());

        // 去除原版黑底白边
        edit.setBordered(false);

        // 字体 Y 轴微调以居中于输入框（字体默认 9px，输入框 18px，偏移 4~5）
        // 通过修改 textY 偏移来实现：EditBox 内部使用 y + (height - 9) / 2
        // 这里只需保持默认行为即可，因为 setBordered(false) 后 EditBox 自己会居中

        String initialText = formatSliderValue(slider);
        edit.setValue(initialText);

        edit.setFilter(filter);
        edit.setMaxLength(6);
        edit.setResponder(text -> onEditBoxChanged(text, slider));

        return addRenderableWidget(edit);
    }

    /**
     * 输入框内容变化回调
     */
    private void onEditBoxChanged(String text, AttributeSlider slider)
    {
        if (isSyncing) return;
        if (text.isEmpty()) return;

        try
        {
            float val = Float.parseFloat(text);
            if (val < 0) return;
            if (val > slider.getMax())
            {
                val = slider.getMax();
                isSyncing = true;
                String formatted = (slider.getStep() >= 1.0F)
                        ? String.valueOf((int) slider.getMax())
                        : String.format("%.1f", slider.getMax());
                EditBox target = getEditBoxForSlider(slider);
                if (target != null) target.setValue(formatted);
                isSyncing = false;
            }

            isSyncing = true;
            slider.setFloatValue(val);
            isSyncing = false;
        }
        catch (NumberFormatException ignored) {}
    }

    private void updateEditBoxFromSlider(AttributeSlider slider)
    {
        String formatted = formatSliderValue(slider);
        EditBox target = getEditBoxForSlider(slider);
        if (target != null)
            target.setValue(formatted);
    }

    private String formatSliderValue(AttributeSlider slider)
    {
        if (slider.getStep() >= 1.0F)
            return String.valueOf(slider.getIntValue());
        else
            return String.format("%.1f", slider.getFloatValue());
    }

    private EditBox getEditBoxForSlider(AttributeSlider slider)
    {
        if (slider == eternaSlider) return eternaEdit;
        if (slider == quantaSlider) return quantaEdit;
        if (slider == arcanaSlider) return arcanaEdit;
        if (slider == cluesSlider)  return cluesEdit;
        return null;
    }

    // ==================== 保存逻辑 ====================

    private void onSaveClicked()
    {
        float eterna = eternaSlider.getFloatValue();
        float quanta = quantaSlider.getFloatValue();
        float arcana = arcanaSlider.getFloatValue();
        int   clues  = cluesSlider.getIntValue();

        boolean treasure    = this.treasureEnabled;
        boolean calibration = this.calibrationEnabled;

        this.cachedClues = clues;
        this.cachedTreasure = treasure;
        this.cachedCalibration = calibration;

        ModMessages.sendToServer(new ServerboundC2SUpdateAttributesPacket(
                this.menu.getPos(),
                eterna, quanta, arcana,
                clues, treasure, calibration
        ));
    }

    // ==================== 数据同步 ====================

    @Override
    protected void containerTick()
    {
        super.containerTick();

        // ---- 检查 Eterna (data index 1) ----
        int eternaData = this.menu.getData(1);
        if (eternaData != this.lastEternaData)
        {
            this.lastEternaData = eternaData;
            eternaSlider.setFloatValue(eternaData / 100.0F);
            updateEditBoxFromSlider(eternaSlider);
        }

        // ---- 检查 Quanta (data index 2) ----
        int quantaData = this.menu.getData(2);
        if (quantaData != this.lastQuantaData)
        {
            this.lastQuantaData = quantaData;
            quantaSlider.setFloatValue(quantaData / 100.0F);
            updateEditBoxFromSlider(quantaSlider);
        }

        // ---- 检查 Arcana (data index 3) ----
        int arcanaData = this.menu.getData(3);
        if (arcanaData != this.lastArcanaData)
        {
            this.lastArcanaData = arcanaData;
            arcanaSlider.setFloatValue(arcanaData / 100.0F);
            updateEditBoxFromSlider(arcanaSlider);
        }

        // ---- 检查 Clues (data index 4) ----
        int cluesData = this.menu.getData(4);
        if (cluesData != this.lastCluesData)
        {
            this.lastCluesData = cluesData;
            cluesSlider.setFloatValue(cluesData / 100.0F);
            updateEditBoxFromSlider(cluesSlider);
        }

        // ---- 检查 Treasure (data index 5) ----
        int treasureData = this.menu.getData(5);
        if (treasureData != this.lastTreasureData)
        {
            this.lastTreasureData = treasureData;
            this.treasureEnabled = treasureData != 0;
        }

        // ---- 检查 Calibration (data index 6) ----
        int calibrationData = this.menu.getData(6);
        if (calibrationData != this.lastCalibrationData)
        {
            this.lastCalibrationData = calibrationData;
            this.calibrationEnabled = calibrationData != 0;
        }
    }

    // ==================== 鼠标事件 ====================

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button)
    {
        if (button == 0)
        {
            // ---- 检测勾选框点击 ----
            int cbX1 = this.leftPos + CHECKBOX_X[0];
            int cbX2 = this.leftPos + CHECKBOX_X[1];
            int cbY  = this.topPos  + CHECKBOX_Y;

            if (mouseX >= cbX1 && mouseX < cbX1 + CHECKBOX_SIZE &&
                mouseY >= cbY  && mouseY < cbY  + CHECKBOX_SIZE)
            {
                this.treasureEnabled = !this.treasureEnabled;
                return true;
            }
            if (mouseX >= cbX2 && mouseX < cbX2 + CHECKBOX_SIZE &&
                mouseY >= cbY  && mouseY < cbY  + CHECKBOX_SIZE)
            {
                this.calibrationEnabled = !this.calibrationEnabled;
                return true;
            }

            // ---- 检测滑块按下 ----
            for (AttributeSlider slider : new AttributeSlider[]{eternaSlider, quantaSlider, arcanaSlider, cluesSlider})
            {
                if (slider != null && slider.isMouseOver(mouseX, mouseY))
                {
                    draggingSlider = slider;
                    break;
                }
            }
        }
        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseDragged(double mouseX, double mouseY, int button, double dragX, double dragY)
    {
        if (button == 0 && draggingSlider != null)
        {
            draggingSlider.mouseDragged(mouseX, mouseY, button, dragX, dragY);
            return true;
        }
        return super.mouseDragged(mouseX, mouseY, button, dragX, dragY);
    }

    @Override
    public boolean mouseReleased(double mouseX, double mouseY, int button)
    {
        draggingSlider = null;
        return super.mouseReleased(mouseX, mouseY, button);
    }

    // ==================== 渲染 ====================

    @Override
    protected void renderBg(GuiGraphics guiGraphics, float partialTick, int mouseX, int mouseY)
    {
        int x = this.leftPos;
        int y = this.topPos;

        // 1. 绘制完整主背景
        guiGraphics.blit(TEXTURE, x, y, 0, 0, GUI_WIDTH, GUI_HEIGHT);

        // 2. 能量槽动态填充（自下而上）
        int currentEnergy = this.menu.getData(0);
        float fillRatio = Math.min(1.0F, (float) currentEnergy / (float) MAX_ENERGY);
        int fillHeight = (int) (ENERGY_MAX_H * fillRatio);
        if (fillHeight > 0)
        {
            // 填充区：U=187, V=12, 宽=6, 最大高=112
            // 自下而上裁剪：绘制起点 Y 偏移 (ENERGY_MAX_H - fillHeight)，UV 的 V 也同步偏移
            int drawY = y + ENERGY_Y + (ENERGY_MAX_H - fillHeight);
            guiGraphics.blit(TEXTURE,
                    x + ENERGY_X, drawY,
                    187, 12 + (ENERGY_MAX_H - fillHeight),
                    ENERGY_W, fillHeight);
        }

        // 3. 绘制勾选框选中覆盖层（若有）
        if (treasureEnabled)
        {
            guiGraphics.blit(TEXTURE,
                    x + CHECKBOX_X[0], y + CHECKBOX_Y,
                    195, 22, 20, 20);
        }
        if (calibrationEnabled)
        {
            guiGraphics.blit(TEXTURE,
                    x + CHECKBOX_X[1], y + CHECKBOX_Y,
                    195, 22, 20, 20);
        }
    }

    @Override
    protected void renderLabels(GuiGraphics guiGraphics, int mouseX, int mouseY)
    {
        String[] labels = {"位阶", "量子化", "阿卡那", "线索"};
        int[] labelY = {17, 40, 63, 86};
        int labelX = 22;

        for (int i = 0; i < 4; i++)
        {
            guiGraphics.drawString(this.font, labels[i], labelX, labelY[i], 0xFFFFFF, false);
        }

        guiGraphics.drawString(this.font, "宝藏附魔", 38, 110, 0x404040, false);
        guiGraphics.drawString(this.font, "校准", 101, 110, 0x404040, false);
    }

    @Override
    public void render(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick)
    {
        super.render(guiGraphics, mouseX, mouseY, partialTick);

        // 能量条 Tooltip
        int barX = this.leftPos + ENERGY_X;
        int barY = this.topPos  + ENERGY_Y;
        if (mouseX >= barX && mouseX < barX + ENERGY_W &&
            mouseY >= barY && mouseY < barY + ENERGY_MAX_H)
        {
            int currentEnergy = this.menu.getData(0);
            guiGraphics.renderTooltip(this.font,
                    Component.literal(String.format("%,d / %,d FE", currentEnergy, MAX_ENERGY)),
                    mouseX, mouseY);
        }
    }

    @Override
    public void removed()
    {
        super.removed();
    }
}