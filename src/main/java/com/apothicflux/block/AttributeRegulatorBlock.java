package com.apothicflux.block;

import com.apothicflux.block.entity.AttributeRegulatorBlockEntity;
import dev.shadowsoffire.apotheosis.ench.api.IEnchantingBlock;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraftforge.network.NetworkHooks;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.Set;

public class AttributeRegulatorBlock extends Block implements EntityBlock, IEnchantingBlock
{
    // 激活状态属性：true = 有能量（激活），false = 无能量（未激活）
    public static final BooleanProperty ACTIVE = BooleanProperty.create("active");

    public AttributeRegulatorBlock(Properties properties)
    {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(ACTIVE, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder)
    {
        builder.add(ACTIVE);
    }

    @Nullable
    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state)
    {
        return new AttributeRegulatorBlockEntity(pos, state);
    }

    @Nullable
    @Override
    public <T extends BlockEntity> BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type)
    {
        // 仅在服务端 tick
        if (level.isClientSide)
            return null;

        return (lvl, pos, st, be) -> {
            if (be instanceof AttributeRegulatorBlockEntity regulator)
            {
                boolean active = regulator.tickServer();
                // 如果激活状态发生变化，更新 BlockState
                if (st.getValue(ACTIVE) != active)
                {
                    lvl.setBlock(pos, st.setValue(ACTIVE, active), 3);
                }
            }
        };
    }

    @Override
    public InteractionResult use(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand, BlockHitResult hit)
    {
        if (!level.isClientSide && player instanceof ServerPlayer serverPlayer)
        {
            if (level.getBlockEntity(pos) instanceof AttributeRegulatorBlockEntity be)
            {
                NetworkHooks.openScreen(serverPlayer, be, pos);
            }
        }
        return InteractionResult.SUCCESS;
    }

    // ==================== 神化 IEnchantingBlock 接口实现 ====================

    /**
     * 获取方块实体，用于读取动态属性值。当能量耗尽时返回 null（视为无属性）。
     */
    @Nullable
    private AttributeRegulatorBlockEntity getActiveBE(BlockState state, LevelReader level, BlockPos pos)
    {
        if (level.getBlockEntity(pos) instanceof AttributeRegulatorBlockEntity be)
        {
            // 只有方块处于激活状态（ACTIVE = true）时才返回 BE，否则视为无能量/未激活
            if (state.getValue(ACTIVE))
            {
                return be;
            }
        }
        return null;
    }

    /**
     * Forge 原版附魔台属性加成（同时也是神化 Eterna 的回退路径）。
     * 有能量时返回设定的 eterna 值，无能量时返回 0。
     */
    @Override
    public float getEnchantPowerBonus(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null ? be.getEterna() : 0.0F;
    }

    /**
     * 最大 Eterna 上限。返回与当前 eterna 相同的值（受能量约束）。
     */
    @Override
    public float getMaxEnchantingPower(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null ? be.getEterna() : 0.0F;
    }

    /**
     * Quanta（附魔等级变化率）。有能量时返回设定的 quanta 值。
     */
    @Override
    public float getQuantaBonus(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null ? be.getQuanta() : 0.0F;
    }

    /**
     * Arcana（附魔矩化）。有能量时返回设定的 arcana 值。
     */
    @Override
    public float getArcanaBonus(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null ? be.getArcana() : 0.0F;
    }

    /**
     * Quanta 修正（Rectification）。当校准（calibration）启用时返回 25%，否则返回 0。
     */
    @Override
    public float getQuantaRectification(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null && be.hasCalibration() ? 25.0F : 0.0F;
    }

    /**
     * 额外线索（Bonus Clues）。有能量时返回设定的 clues 值（整数）。
     */
    @Override
    public int getBonusClues(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null ? (int) be.getClues() : 0;
    }

    /**
     * 是否允许宝藏附魔（Treasure Enchantments）。
     */
    @Override
    public boolean allowsTreasure(BlockState state, LevelReader level, BlockPos pos)
    {
        AttributeRegulatorBlockEntity be = getActiveBE(state, level, pos);
        return be != null && be.hasTreasureEnchantments();
    }

    /**
     * 黑名单附魔列表。保持默认空集合。
     */
    @Override
    public Set<Enchantment> getBlacklistedEnchantments(BlockState state, LevelReader level, BlockPos pos)
    {
        return Collections.emptySet();
    }

    /**
     * 粒子效果。保持默认（仅在有能量时生成）。
     */
    @Override
    public void spawnTableParticle(BlockState state, Level level, RandomSource rand, BlockPos pos, BlockPos offset)
    {
        if (state.getValue(ACTIVE))
        {
            IEnchantingBlock.super.spawnTableParticle(state, level, rand, pos, offset);
        }
    }
}
