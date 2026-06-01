package com.apothicflux.networking;

import com.apothicflux.ApothicFlux;
import com.apothicflux.block.entity.AttributeRegulatorBlockEntity;
import com.apothicflux.menu.AttributeRegulatorMenu;
import net.minecraft.core.BlockPos;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraftforge.network.NetworkEvent;
import net.minecraftforge.network.NetworkRegistry;
import net.minecraftforge.network.PacketDistributor;
import net.minecraftforge.network.simple.SimpleChannel;

import java.util.function.Supplier;

public class ModMessages {
    private static final String PROTOCOL_VERSION = "1.0";
    private static int packetId = 0;

    public static final SimpleChannel CHANNEL = NetworkRegistry.newSimpleChannel(
            new ResourceLocation(ApothicFlux.MODID, "main"),
            () -> PROTOCOL_VERSION,
            PROTOCOL_VERSION::equals,
            PROTOCOL_VERSION::equals
    );

    /**
     * 注册所有网络数据包。在 FMLCommonSetupEvent 中调用。
     */
    public static void register() {
        CHANNEL.messageBuilder(ServerboundC2SUpdateAttributesPacket.class, packetId++)
                .encoder(ServerboundC2SUpdateAttributesPacket::toBytes)
                .decoder(ServerboundC2SUpdateAttributesPacket::new)
                .consumerMainThread((packet, context) -> {
                    handleUpdateAttributes(packet, context);
                })
                .add();
    }

    /**
     * 处理客户端发来的属性更新请求
     */
    private static void handleUpdateAttributes(ServerboundC2SUpdateAttributesPacket packet, Supplier<NetworkEvent.Context> contextSupplier) {
        NetworkEvent.Context ctx = contextSupplier.get();
        ctx.enqueueWork(() -> {
            // 获取发送包的玩家
            ServerPlayer player = ctx.getSender();
            if (player == null) return;

            Level level = player.level();
            BlockPos pos = packet.getPos();

            // 检查距离：玩家必须在 8 格方块内才能修改
            if (!player.blockPosition().closerThan(pos, 8.0D)) return;

            // 获取 BlockEntity
            BlockEntity be = level.getBlockEntity(pos);
            if (!(be instanceof AttributeRegulatorBlockEntity regulator)) return;

            // 更新属性数值（客户端传来的值，限制在合理范围内）
            float eterna = Math.max(0.0F, Math.min(100.0F, packet.getEterna()));
            float quanta = Math.max(0.0F, Math.min(100.0F, packet.getQuanta()));
            float arcana = Math.max(0.0F, Math.min(100.0F, packet.getArcana()));
            int clues = Math.max(0, Math.min(15, packet.getClues()));

            regulator.setEterna(eterna);
            regulator.setQuanta(quanta);
            regulator.setArcana(arcana);
            regulator.setClues((float) clues);
            regulator.setTreasureEnchantments(packet.hasTreasureEnchantments());
            regulator.setCalibration(packet.hasCalibration());

            regulator.setChanged();

            // 同步到客户端（刷新 GUI）
            level.sendBlockUpdated(pos, level.getBlockState(pos), level.getBlockState(pos), 3);

            // 强制同步已打开的 GUI 菜单数据
            if (player.containerMenu instanceof AttributeRegulatorMenu menu &&
                menu.getPos().equals(pos))
            {
                for (int i = 0; i < regulator.getCount(); i++)
                {
                    menu.setData(i, regulator.get(i));
                }
                // 广播到客户端
                menu.broadcastChanges();
            }
        });
        ctx.setPacketHandled(true);
    }

    /**
     * 客户端 → 服务端：发送任意数据包
     */
    public static void sendToServer(Object packet) {
        CHANNEL.send(PacketDistributor.SERVER.noArg(), packet);
    }
}