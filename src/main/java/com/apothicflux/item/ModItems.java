package com.apothicflux.item;

import com.apothicflux.ApothicFlux;
import com.apothicflux.block.ModBlocks;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModItems
{
    public static final DeferredRegister<Item> ITEMS = DeferredRegister.create(ForgeRegistries.ITEMS, ApothicFlux.MODID);

    public static final RegistryObject<Item> ATTRIBUTE_REGULATOR = ITEMS.register("attribute_regulator",
            () -> new BlockItem(ModBlocks.ATTRIBUTE_REGULATOR.get(), new Item.Properties())
    );
}