package com.apothicflux.block.entity;

import com.apothicflux.ApothicFlux;
import com.apothicflux.block.ModBlocks;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlockEntities
{
    public static final DeferredRegister<BlockEntityType<?>> BLOCK_ENTITIES = DeferredRegister.create(ForgeRegistries.BLOCK_ENTITY_TYPES, ApothicFlux.MODID);

    public static final RegistryObject<BlockEntityType<AttributeRegulatorBlockEntity>> ATTRIBUTE_REGULATOR_BE =
            BLOCK_ENTITIES.register("attribute_regulator",
                    () -> BlockEntityType.Builder.of(
                            AttributeRegulatorBlockEntity::new,
                            ModBlocks.ATTRIBUTE_REGULATOR.get()
                    ).build(null)
            );
}