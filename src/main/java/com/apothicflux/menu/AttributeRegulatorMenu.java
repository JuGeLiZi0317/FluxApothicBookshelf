package com.apothicflux.menu;

import com.apothicflux.block.ModBlocks;
import com.apothicflux.block.entity.AttributeRegulatorBlockEntity;
import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.inventory.SimpleContainerData;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;

public class AttributeRegulatorMenu extends AbstractContainerMenu
{
    private final BlockPos pos;
    private final ContainerData data;

    // 服务端构造函数：从 BlockEntity 获取 ContainerData
    public AttributeRegulatorMenu(int containerId, Inventory playerInv, BlockPos pos, ContainerData data)
    {
        super(ModMenuTypes.ATTRIBUTE_REGULATOR_MENU.get(), containerId);
        this.pos = pos;
        this.data = data;

        // 添加 7 个数据槽：energy, eterna, quanta, arcana, clues, treasure, calibration
        addDataSlots(data);
    }

    // 客户端构造函数：通过 IContainerFactory 从网络缓存反序列化
    public AttributeRegulatorMenu(int containerId, Inventory playerInv, FriendlyByteBuf extraData)
    {
        this(containerId, playerInv, extraData.readBlockPos(), new SimpleContainerData(7));
    }

    @Override
    public boolean stillValid(Player player)
    {
        return stillValid(ContainerLevelAccess.create(player.level(), pos), player, ModBlocks.ATTRIBUTE_REGULATOR.get());
    }

    /**
     * 获取方块坐标
     */
    public BlockPos getPos()
    {
        return pos;
    }

    public int getData(int index)
    {
        return data.get(index);
    }

    /**
     * 服务端设置指定索引的数据槽值（由网络包处理器调用）
     */
    public void setData(int index, int value)
    {
        data.set(index, value);
    }

    @Override
    public ItemStack quickMoveStack(Player player, int index)
    {
        // 当前无物品槽位，直接返回空
        return ItemStack.EMPTY;
    }
}