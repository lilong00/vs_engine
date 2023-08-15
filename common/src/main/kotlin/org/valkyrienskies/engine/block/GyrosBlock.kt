// 导入相关的包
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
import org.valkyrienskies.engine.ship.Gyros
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos

// 定义一个稳定核心块的类
class GyrosBlock : Block(
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

        if (level.isClientSide) return
        level as ServerLevel

        val ship = level.getShipObjectManagingPos(pos) ?: level.getShipManagingPos(pos) ?: return
        Gyros.getOrCreate(ship).helms += 1
    }

    // 当块被破坏时的操作
    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        super.destroy(level, pos, state)

        if (level.isClientSide) return
        level as ServerLevel

        level.getShipManagingPos(pos)?.getAttachment<Gyros>()?.let { control ->

            if (control.helms <= 1 && control.seatedPlayer?.vehicle?.type == ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE) {
                control.seatedPlayer!!.unRide()
                control.seatedPlayer = null
            }

            control.helms -= 1
        }
    }
}