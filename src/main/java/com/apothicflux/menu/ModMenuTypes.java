package com.apothicflux.menu;

import com.apothicflux.ApothicFlux;
import net.minecraft.world.inventory.MenuType;
import net.minecraftforge.common.extensions.IForgeMenuType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModMenuTypes
{
    public static final DeferredRegister<MenuType<?>> MENU_TYPES =
            DeferredRegister.create(ForgeRegistries.MENU_TYPES, ApothicFlux.MODID);

    public static final RegistryObject<MenuType<AttributeRegulatorMenu>> ATTRIBUTE_REGULATOR_MENU =
            MENU_TYPES.register("attribute_regulator",
                    () -> IForgeMenuType.create(AttributeRegulatorMenu::new)
            );
}