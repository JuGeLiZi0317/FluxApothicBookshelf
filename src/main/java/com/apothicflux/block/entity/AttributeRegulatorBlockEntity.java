package com.apothicflux.block.entity;

import com.apothicflux.menu.AttributeRegulatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.Connection;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.network.protocol.game.ClientboundBlockEntityDataPacket;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.ContainerData;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraftforge.common.capabilities.Capability;
import net.minecraftforge.common.capabilities.ForgeCapabilities;
import net.minecraftforge.common.util.LazyOptional;
import net.minecraftforge.energy.EnergyStorage;
import net.minecraftforge.energy.IEnergyStorage;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class AttributeRegulatorBlockEntity extends BlockEntity implements ContainerData, MenuProvider
{
    // 能量存储：容量 100,000,000 FE，最大输入 8000 FE/t，不允许提取能量
    public final EnergyStorage energyStorage = new EnergyStorage(100000000, 8000, 0, 0);
    private LazyOptional<IEnergyStorage> lazyEnergy = LazyOptional.of(() -> this.energyStorage);

    // 神化属性值
    private float eterna = 0.0F;
    private float quanta = 0.0F;
    private float arcana = 0.0F;
    private float clues = 0.0F;

    // 勾选框状态
    private boolean treasureEnchantments = false;
    private boolean calibration = false;

    // ContainerData slot 索引常量
    private static final int SLOT_ENERGY = 0;
    private static final int SLOT_ETERNA = 1;
    private static final int SLOT_QUANTA = 2;
    private static final int SLOT_ARCANA = 3;
    private static final int SLOT_CLUES = 4;
    private static final int SLOT_TREASURE = 5;
    private static final int SLOT_CALIBRATION = 6;
    private static final int DATA_SLOT_COUNT = 7;

    public AttributeRegulatorBlockEntity(BlockPos pos, BlockState state)
    {
        super(ModBlockEntities.ATTRIBUTE_REGULATOR_BE.get(), pos, state);
    }

    // ==================== Getters / Setters ====================

    public float getEterna() { return eterna; }
    public void setEterna(float eterna) { this.eterna = eterna; }

    public float getQuanta() { return quanta; }
    public void setQuanta(float quanta) { this.quanta = quanta; }

    public float getArcana() { return arcana; }
    public void setArcana(float arcana) { this.arcana = arcana; }

    public float getClues() { return clues; }
    public void setClues(float clues) { this.clues = clues; }

    public boolean hasTreasureEnchantments() { return treasureEnchantments; }
    public void setTreasureEnchantments(boolean value) { this.treasureEnchantments = value; }

    public boolean hasCalibration() { return calibration; }
    public void setCalibration(boolean value) { this.calibration = value; }

    /**
     * @return 当前存储的能量（FE）
     */
    public int getEnergyStored()
    {
        return energyStorage.getEnergyStored();
    }

    /**
     * @return 最大可存储能量（FE）
     */
    public int getMaxEnergyStored()
    {
        return energyStorage.getMaxEnergyStored();
    }

    // ==================== ContainerData 接口 (7 个数据槽) ====================

    @Override
    public int get(int index)
    {
        switch (index)
        {
            case SLOT_ENERGY: return getEnergyStored();
            case SLOT_ETERNA: return (int) (eterna * 100.0F);                   // float → int (保留两位小数)
            case SLOT_QUANTA: return (int) (quanta * 100.0F);
            case SLOT_ARCANA: return (int) (arcana * 100.0F);
            case SLOT_CLUES:  return (int) (clues * 100.0F);
            case SLOT_TREASURE: return treasureEnchantments ? 1 : 0;            // boolean → int
            case SLOT_CALIBRATION: return calibration ? 1 : 0;
            default: return 0;
        }
    }

    @Override
    public void set(int index, int value)
    {
        switch (index)
        {
            case SLOT_ENERGY:
                // 能量由 FE 管道管理，客户端不可写
                break;
            case SLOT_ETERNA:
                this.eterna = value / 100.0F;
                setChanged();
                break;
            case SLOT_QUANTA:
                this.quanta = value / 100.0F;
                setChanged();
                break;
            case SLOT_ARCANA:
                this.arcana = value / 100.0F;
                setChanged();
                break;
            case SLOT_CLUES:
                this.clues = value / 100.0F;
                setChanged();
                break;
            case SLOT_TREASURE:
                this.treasureEnchantments = value != 0;
                setChanged();
                break;
            case SLOT_CALIBRATION:
                this.calibration = value != 0;
                setChanged();
                break;
        }
    }

    @Override
    public int getCount()
    {
        return DATA_SLOT_COUNT;
    }

    // ==================== MenuProvider 接口 ====================

    @Override
    public Component getDisplayName()
    {
        return Component.translatable("container.apothicflux.attribute_regulator");
    }

    @Nullable
    @Override
    public AbstractContainerMenu createMenu(int containerId, Inventory playerInv, Player player)
    {
        // 将 BlockEntity 自身作为 ContainerData 传入 Menu
        return new AttributeRegulatorMenu(containerId, playerInv, worldPosition, this);
    }

    // ==================== Capability (FE) ====================

    @Override
    public @NotNull <T> LazyOptional<T> getCapability(@NotNull Capability<T> cap, @Nullable Direction side)
    {
        if (cap == ForgeCapabilities.ENERGY)
        {
            return lazyEnergy.cast();
        }
        return super.getCapability(cap, side);
    }

    @Override
    public void invalidateCaps()
    {
        super.invalidateCaps();
        this.lazyEnergy.invalidate();
    }

    @Override
    public void reviveCaps()
    {
        super.reviveCaps();
        this.lazyEnergy = LazyOptional.of(() -> this.energyStorage);
    }

    // ==================== 服务端 Tick ====================

    /**
     * 服务端每 tick 调用：消耗 10 FE
     * @return true 表示能量 > 0（激活状态），false 表示能量耗尽（未激活）
     */
    public boolean tickServer()
    {
        if (this.energyStorage.getEnergyStored() >= 10)
        {
            this.energyStorage.extractEnergy(10, false);
            setChanged();
            return true;
        }
        return false;
    }

    // ==================== 数据同步 ====================

    @Override
    public CompoundTag getUpdateTag()
    {
        CompoundTag tag = super.getUpdateTag();
        saveAdditional(tag);
        return tag;
    }

    @Override
    public void handleUpdateTag(CompoundTag tag)
    {
        super.handleUpdateTag(tag);
        load(tag);
    }

    @Nullable
    @Override
    public Packet<ClientGamePacketListener> getUpdatePacket()
    {
        return ClientboundBlockEntityDataPacket.create(this);
    }

    @Override
    public void onDataPacket(Connection net, ClientboundBlockEntityDataPacket pkt)
    {
        super.onDataPacket(net, pkt);
    }

    // ==================== NBT 序列化 ====================

    @Override
    protected void saveAdditional(CompoundTag nbt)
    {
        super.saveAdditional(nbt);
        nbt.putInt("energy", this.energyStorage.getEnergyStored());
        nbt.putFloat("eterna", this.eterna);
        nbt.putFloat("quanta", this.quanta);
        nbt.putFloat("arcana", this.arcana);
        nbt.putFloat("clues", this.clues);
        nbt.putBoolean("treasureEnchantments", this.treasureEnchantments);
        nbt.putBoolean("calibration", this.calibration);
    }

    @Override
    public void load(CompoundTag nbt)
    {
        super.load(nbt);
        if (nbt.contains("energy"))
        {
            this.energyStorage.deserializeNBT(nbt.get("energy"));
        }
        if (nbt.contains("eterna"))  this.eterna = nbt.getFloat("eterna");
        if (nbt.contains("quanta"))  this.quanta = nbt.getFloat("quanta");
        if (nbt.contains("arcana"))  this.arcana = nbt.getFloat("arcana");
        if (nbt.contains("clues"))   this.clues = nbt.getFloat("clues");
        if (nbt.contains("treasureEnchantments")) this.treasureEnchantments = nbt.getBoolean("treasureEnchantments");
        if (nbt.contains("calibration")) this.calibration = nbt.getBoolean("calibration");
    }
}