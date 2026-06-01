package com.apothicflux.block;

import com.apothicflux.ApothicFlux;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.material.MapColor;
import net.minecraft.world.level.material.PushReaction;
import net.minecraftforge.registries.DeferredRegister;
import net.minecraftforge.registries.ForgeRegistries;
import net.minecraftforge.registries.RegistryObject;

public class ModBlocks
{
    public static final DeferredRegister<Block> BLOCKS = DeferredRegister.create(ForgeRegistries.BLOCKS, ApothicFlux.MODID);

    public static final RegistryObject<Block> ATTRIBUTE_REGULATOR = BLOCKS.register("attribute_regulator",
            () -> new AttributeRegulatorBlock(BlockBehaviour.Properties.of()
                    .mapColor(MapColor.METAL)
                    .strength(6.0F, 1200.0F)
                    .sound(SoundType.WOOD)
                    .noOcclusion()
                    .requiresCorrectToolForDrops()
            )
    );
}