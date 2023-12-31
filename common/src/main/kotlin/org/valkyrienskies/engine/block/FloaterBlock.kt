package org.valkyrienskies.engine.block

import net.minecraft.core.BlockPos
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.SoundType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import net.minecraft.world.level.material.Material
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.engine.ship.EngineShipControl
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos

class FloaterBlock : Block(
    Properties.of(Material.WOOD)
        .sound(SoundType.WOOL).strength(1.0f, 2.0f)
) {
    // 初始化块状态
    init {
        registerDefaultState(defaultBlockState().setValue(BlockStateProperties.POWER, 0))
    }

    // 创建块状态定义
    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        super.createBlockStateDefinition(builder)
        builder.add(BlockStateProperties.POWER)
    }

    // 当块被放置时的操作
    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        // 如果是客户端则返回
        if (level.isClientSide) return
        level as ServerLevel

        // 获取浮动器的电力
        val floaterPower = 15 - state.getValue(BlockStateProperties.POWER)

        // 获取船只对象
        val ship = level.getShipObjectManagingPos(pos) ?: level.getShipManagingPos(pos) ?: return
        // 获取或创建船只的引擎控制，增加浮动器的电力
        EngineShipControl.getOrCreate(ship).floaters += floaterPower
    }

    // 当邻居改变时的操作
    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        block: Block,
        fromPos: BlockPos,
        isMoving: Boolean
    ) {
        super.neighborChanged(state, level, pos, block, fromPos, isMoving)

        // 如果不是服务器端则返回
        if (level as? ServerLevel == null) return

        // 获取信号
        val signal = level.getBestNeighborSignal(pos)
        // 获取当前电力
        val currentPower = state.getValue(BlockStateProperties.POWER)

        // 获取船只管理位置的附件，增加浮动器的电力
        level.getShipManagingPos(pos)?.getAttachment<EngineShipControl>()?.let {
            it.floaters += (currentPower - signal)
        }

        // 设置块状态
        level.setBlock(pos, state.setValue(BlockStateProperties.POWER, signal), 2)
    }

    // 当块被破坏时的操作
    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        super.destroy(level, pos, state)

        // 如果是客户端则返回
        if (level.isClientSide) return
        level as ServerLevel

        // 获取浮动器的电力
        val floaterPower = 15 - state.getValue(BlockStateProperties.POWER)

        // 获取船只管理位置的附件，减少浮动器的电力
        level.getShipManagingPos(pos)?.getAttachment<EngineShipControl>()?.let {
            it.floaters -= floaterPower
        }
    }
}