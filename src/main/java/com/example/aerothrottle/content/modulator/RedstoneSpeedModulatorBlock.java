package com.example.aerothrottle.content.modulator;

import com.example.aerothrottle.client.ScreenOpener;
import com.example.aerothrottle.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

public class RedstoneSpeedModulatorBlock extends DirectionalKineticBlock implements IBE<RedstoneSpeedModulatorBlockEntity> {

    public RedstoneSpeedModulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH));
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        return face == state.getValue(FACING);
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (!player.isShiftKeyDown()) return InteractionResult.PASS;
        if (!level.isClientSide) return InteractionResult.SUCCESS;
        RedstoneSpeedModulatorBlockEntity be = getBlockEntity(level, pos);
        if (be == null) return InteractionResult.PASS;
        ScreenOpener.openModulatorScreen(be);
        return InteractionResult.SUCCESS;
    }

    @Override
    public Class<RedstoneSpeedModulatorBlockEntity> getBlockEntityClass() {
        return RedstoneSpeedModulatorBlockEntity.class;
    }

    @Override
    public BlockEntityType<? extends RedstoneSpeedModulatorBlockEntity> getBlockEntityType() {
        return ModBlockEntities.REDSTONE_SPEED_MODULATOR.get();
    }
}
