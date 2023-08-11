package org.valkyrienskies.engine.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.damagesource.DamageSource
import net.minecraft.world.entity.Entity
import net.minecraft.world.entity.projectile.Projectile
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.engine.EngineConfig
import org.valkyrienskies.engine.ship.EngineShipControl
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos

class BalloonBlock(properties: Properties) : Block(properties) {

    override fun fallOn(level: Level, state: BlockState, blockPos: BlockPos, entity: Entity, f: Float) {
        entity.causeFallDamage(f, 0.2f, DamageSource.FALL)  // 使实体受到摔落伤害
    }

    override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level.isClientSide) return  // 如果是客户端，直接返回
        level as ServerLevel

        val ship = level.getShipObjectManagingPos(pos) ?: level.getShipManagingPos(pos) ?: return  // 获取飞船对象，如果不存在则返回
        EngineShipControl.getOrCreate(ship).balloons += 1  // 获取或创建引擎控制，并将气球数量加1
    }

    override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        super.destroy(level, pos, state)

        if (level.isClientSide) return  // 如果是客户端，直接返回
        level as ServerLevel

        level.getShipManagingPos(pos)?.getAttachment<EngineShipControl>()?.let {
            it.balloons -= 1  // 将气球数量减1
        }
    }

    override fun onProjectileHit(level: Level, state: BlockState, hit: BlockHitResult, projectile: Projectile) {
        if (level.isClientSide) return  // 如果是客户端，直接返回

        level.destroyBlock(hit.blockPos, false)  // 摧毁击中的气球方块
        Direction.values().forEach {  // 遍历周围的方向
            val neighbor = hit.blockPos.relative(it)  // 获取相邻方块的位置
            if (level.getBlockState(neighbor).block == this &&
                level.random.nextFloat() < EngineConfig.SERVER.popSideBalloonChance
            ) {
                level.destroyBlock(neighbor, false)  // 根据一定的概率摧毁相邻的气球方块
            }
        }
    }
}
