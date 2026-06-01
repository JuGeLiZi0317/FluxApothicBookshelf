package com.apothicflux.networking;

import net.minecraft.core.BlockPos;
import net.minecraft.network.FriendlyByteBuf;

public class ServerboundC2SUpdateAttributesPacket {
    private final BlockPos pos;
    private final float eterna;
    private final float quanta;
    private final float arcana;
    private final int clues;
    private final boolean treasureEnchantments;
    private final boolean calibration;

    public ServerboundC2SUpdateAttributesPacket(BlockPos pos, float eterna, float quanta, float arcana,
                                                int clues, boolean treasureEnchantments, boolean calibration) {
        this.pos = pos;
        this.eterna = eterna;
        this.quanta = quanta;
        this.arcana = arcana;
        this.clues = clues;
        this.treasureEnchantments = treasureEnchantments;
        this.calibration = calibration;
    }

    /**
     * 解码构造函数 — 从网络缓冲区读取数据
     */
    public ServerboundC2SUpdateAttributesPacket(FriendlyByteBuf buf) {
        this.pos = buf.readBlockPos();
        this.eterna = buf.readFloat();
        this.quanta = buf.readFloat();
        this.arcana = buf.readFloat();
        this.clues = buf.readVarInt();
        this.treasureEnchantments = buf.readBoolean();
        this.calibration = buf.readBoolean();
    }

    /**
     * 编码方法 — 将数据写入网络缓冲区
     */
    public void toBytes(FriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeFloat(eterna);
        buf.writeFloat(quanta);
        buf.writeFloat(arcana);
        buf.writeVarInt(clues);
        buf.writeBoolean(treasureEnchantments);
        buf.writeBoolean(calibration);
    }

    // ===== Getter 方法 =====
    public BlockPos getPos() { return pos; }
    public float getEterna() { return eterna; }
    public float getQuanta() { return quanta; }
    public float getArcana() { return arcana; }
    public int getClues() { return clues; }
    public boolean hasTreasureEnchantments() { return treasureEnchantments; }
    public boolean hasCalibration() { return calibration; }
}