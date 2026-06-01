package com.apothicflux;

import com.apothicflux.block.ModBlocks;
import com.apothicflux.block.entity.ModBlockEntities;
import com.apothicflux.client.screen.AttributeRegulatorScreen;
import com.apothicflux.item.ModItems;
import com.apothicflux.menu.ModMenuTypes;
import com.apothicflux.networking.ModMessages;
import com.mojang.logging.LogUtils;
import net.minecraft.client.gui.screens.MenuScreens;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.eventbus.api.IEventBus;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.event.lifecycle.FMLClientSetupEvent;
import net.minecraftforge.fml.event.lifecycle.FMLCommonSetupEvent;
import net.minecraftforge.fml.javafmlmod.FMLJavaModLoadingContext;
import org.slf4j.Logger;

@Mod(ApothicFlux.MODID)
public class ApothicFlux
{
    public static final String MODID = "apothicflux";
    private static final Logger LOGGER = LogUtils.getLogger();

    public ApothicFlux()
    {
        IEventBus modEventBus = FMLJavaModLoadingContext.get().getModEventBus();

        // 注册 DeferredRegister 到 Mod 事件总线
        ModBlocks.BLOCKS.register(modEventBus);
        ModBlockEntities.BLOCK_ENTITIES.register(modEventBus);
        ModItems.ITEMS.register(modEventBus);
        ModMenuTypes.MENU_TYPES.register(modEventBus);

        modEventBus.addListener(this::commonSetup);
        modEventBus.addListener(this::clientSetup);

        MinecraftForge.EVENT_BUS.register(this);
    }

    private void commonSetup(final FMLCommonSetupEvent event)
    {
        event.enqueueWork(() -> {
            ModMessages.register();
        });
        LOGGER.info("ApothicFlux is initializing...");
    }

    private void clientSetup(final FMLClientSetupEvent event)
    {
        LOGGER.info("ApothicFlux client setup...");
        event.enqueueWork(() ->
        {
            MenuScreens.register(
                    ModMenuTypes.ATTRIBUTE_REGULATOR_MENU.get(),
                    AttributeRegulatorScreen::new
            );
        });
    }
}
