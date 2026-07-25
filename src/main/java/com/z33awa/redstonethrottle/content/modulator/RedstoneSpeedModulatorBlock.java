package com.z33awa.redstonethrottle.content.modulator;

import com.z33awa.redstonethrottle.client.ScreenOpener;
import com.z33awa.redstonethrottle.registry.ModBlockEntities;
import com.simibubi.create.content.kinetics.base.DirectionalKineticBlock;
import com.simibubi.create.foundation.block.IBE;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Direction.Axis;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Mirror;
import net.minecraft.world.level.block.Rotation;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition.Builder;
import net.minecraft.world.level.block.state.properties.IntegerProperty;
import net.minecraft.world.phys.BlockHitResult;

public class RedstoneSpeedModulatorBlock extends DirectionalKineticBlock
    implements IBE<RedstoneSpeedModulatorBlockEntity> {

    public static final IntegerProperty ROLL = IntegerProperty.create("roll", 0, 3);

    public RedstoneSpeedModulatorBlock(Properties properties) {
        super(properties);
        registerDefaultState(defaultBlockState()
            .setValue(FACING, Direction.NORTH)
            .setValue(ROLL, 0));
    }

    @Override
    public Axis getRotationAxis(BlockState state) {
        return state.getValue(FACING).getAxis();
    }

    @Override
    public boolean hasShaftTowards(LevelReader world, BlockPos pos, BlockState state, Direction face) {
        // Shaft topology must not depend on the current output speed. Create traverses these
        // connections after a source is removed to clear every downstream block; hiding the
        // output shaft at zero speed would leave the old output network rotating indefinitely.
        return face.getAxis() == getRotationAxis(state);
    }

    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        BlockState state = super.getStateForPlacement(context);
        return state == null ? null : state.setValue(ROLL, 0);
    }

    @Override
    protected void createBlockStateDefinition(Builder<Block, BlockState> builder) {
        builder.add(ROLL);
        super.createBlockStateDefinition(builder);
    }

    @Override
    public BlockState getRotatedBlockState(BlockState state, Direction targetedFace) {
        Direction facing = state.getValue(FACING);
        if (targetedFace.getAxis() == facing.getAxis())
            return state.setValue(ROLL, (state.getValue(ROLL) + 1) & 3);
        return super.getRotatedBlockState(state, targetedFace);
    }

    @Override
    public BlockState rotate(BlockState state, Rotation rotation) {
        Direction oldFacing = state.getValue(FACING);
        Direction oldIncrease = increaseFace(state);
        Direction newFacing = rotation.rotate(oldFacing);
        Direction newIncrease = rotation.rotate(oldIncrease);
        return state.setValue(FACING, newFacing)
            .setValue(ROLL, findRoll(newFacing, newIncrease));
    }

    @Override
    @SuppressWarnings("deprecation")
    public BlockState mirror(BlockState state, Mirror mirror) {
        Direction newFacing = mirror.mirror(state.getValue(FACING));
        Direction newIncrease = mirror.mirror(increaseFace(state));
        return state.setValue(FACING, newFacing)
            .setValue(ROLL, findRoll(newFacing, newIncrease));
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

    public static Direction increaseFace(BlockState state) {
        return increaseFace(state.getValue(FACING), state.getValue(ROLL));
    }

    public static Direction increaseFace(Direction facing, int roll) {
        int normalizedRoll = Math.floorMod(roll, 4);
        if (facing.getAxis() == Axis.Y) {
            return switch (normalizedRoll) {
                case 0 -> Direction.EAST;
                case 1 -> Direction.SOUTH;
                case 2 -> Direction.WEST;
                default -> Direction.NORTH;
            };
        }
        return switch (normalizedRoll) {
            case 0 -> facing.getClockWise();
            case 1 -> Direction.UP;
            case 2 -> facing.getCounterClockWise();
            default -> Direction.DOWN;
        };
    }

    private static int findRoll(Direction facing, Direction increaseFace) {
        for (int roll = 0; roll < 4; roll++)
            if (increaseFace(facing, roll) == increaseFace)
                return roll;
        return 0;
    }
}
