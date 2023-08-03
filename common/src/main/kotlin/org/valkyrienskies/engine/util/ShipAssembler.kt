package org.valkyrienskies.engine.util

import com.google.common.collect.Sets
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.level.ChunkPos
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.state.BlockState
import org.joml.AxisAngle4d
import org.joml.Matrix4d
import org.joml.Vector3d
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.datastructures.DenseBlockPosSet
import org.valkyrienskies.core.impl.game.ships.ShipObjectServer
import org.valkyrienskies.core.impl.networking.simple.sendToClient
import org.valkyrienskies.core.impl.util.logger
import org.valkyrienskies.mod.common.assembly.createNewShipWithBlocks
import org.valkyrienskies.mod.common.executeIf
import org.valkyrienskies.mod.common.isTickingChunk
import org.valkyrienskies.mod.common.networking.PacketRestartChunkUpdates
import org.valkyrienskies.mod.common.networking.PacketStopChunkUpdates
import org.valkyrienskies.mod.common.playerWrapper
import org.valkyrienskies.mod.common.util.toJOML
import org.valkyrienskies.mod.util.relocateBlock
import org.valkyrienskies.mod.util.updateBlock
import kotlin.collections.set
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.round
import kotlin.math.sign

object ShipAssembler {
    // 收集范围内的方块
fun collectBlocksInRange(
    level: ServerLevel,
    center: BlockPos,
    range_y: Int,
    range_y_: Int,
    range_z: Int,
    range_z_: Int,
    range_x: Int,
    range_x_: Int,
    axis: Direction.Axis,
    predicate: (BlockState) -> Boolean,
    shipSteeringDirection: Direction
): ServerShip? {
    val blocks = DenseBlockPosSet()

    for (x in center.x - range_z..center.x + range_z_) {
        for (y in center.y - range_y_..center.y + range_y) {
            for (z in center.z - range_x_..center.z + range_x) {
                // 根据轴方向调整坐标值
                val pos = when (axis) {
                    Direction.Axis.X -> BlockPos(x, y, z)
                    Direction.Axis.Z -> BlockPos(x, y, z)
                    Direction.Axis.Y -> BlockPos(x, z, y)
                }

                // 根据船舵的朝向调整坐标值
                val adjustedPos = when (shipSteeringDirection) {
                    Direction.SOUTH -> BlockPos(center.x - (pos.x - center.x), pos.y, center.z + (pos.z - center.z))
                    Direction.NORTH -> BlockPos(center.x + (pos.x - center.x), pos.y, center.z - (pos.z - center.z))
                    Direction.EAST -> BlockPos(center.x + (pos.z - center.z), pos.y, center.z + (pos.x - center.x))
                    Direction.WEST -> BlockPos(center.x - (pos.z - center.z), pos.y, center.z - (pos.x - center.x))
                    else -> pos
                }

                val state = level.getBlockState(adjustedPos)
                if (predicate(state)) {
                    blocks.add(adjustedPos.toJOML())
                }
            }
        }
    }

    return if (blocks.isNotEmpty()) {
        createNewShipWithBlocks(center, blocks, level)
    } else {
        null
    }
}


    // 将数字四舍五入到最接近的倍数
    private fun roundToNearestMultipleOf(number: Double, multiple: Double) = multiple * round(number / multiple)

    // 根据 https://gamedev.stackexchange.com/questions/83601/from-3d-rotation-snap-to-nearest-90-directions 修改
    // 将旋转角度调整到最接近的90度方向
    private fun snapRotation(direction: AxisAngle4d): AxisAngle4d {
        val x = abs(direction.x)
        val y = abs(direction.y)
        val z = abs(direction.z)
        val angle = roundToNearestMultipleOf(direction.angle, PI / 2)

        return if (x > y && x > z) {
            direction.set(angle, direction.x.sign, 0.0, 0.0)
        } else if (y > x && y > z) {
            direction.set(angle, 0.0, direction.y.sign, 0.0)
        } else {
            direction.set(angle, 0.0, 0.0, direction.z.sign)
        }
    }

    // 解除船只的填充状态
    fun unfillShip(level: ServerLevel, ship: ServerShip, direction: Direction, shipCenter: BlockPos, center: BlockPos) {
        ship as ShipObjectServer
        ship.shipData.isStatic = true

        // 船只的旋转角度四舍五入到最接近的90度
        val shipToWorld = ship.transform.run {
            Matrix4d()
                .translate(positionInWorld)
                .rotate(snapRotation(AxisAngle4d(shipToWorldRotation)))
                .scale(shipToWorldScaling)
                .translate(-positionInShip.x(), -positionInShip.y(), -positionInShip.z())
        }


        val alloc0 = Vector3d()

        // 方向来自船只对齐的方向
        // 我们可以假设船只在船只空间中总是面向北方，因为它必须如此
        val rotation: Rotation = when (direction) {
            Direction.SOUTH -> Rotation.NONE // Bug in Direction.from2DDataValue() can return south/north as opposite
            Direction.NORTH -> Rotation.CLOCKWISE_180
            Direction.EAST -> Rotation.CLOCKWISE_90
            Direction.WEST -> Rotation.COUNTERCLOCKWISE_90
            else -> {
                Rotation.NONE
            }
        }

        val chunksToBeUpdated = mutableMapOf<ChunkPos, Pair<ChunkPos, ChunkPos>>()

        ship.activeChunksSet.forEach { chunkX, chunkZ ->
            chunksToBeUpdated[ChunkPos(chunkX, chunkZ)] =
                Pair(ChunkPos(chunkX, chunkZ), ChunkPos(chunkX, chunkZ))
        }

        val chunkPairs = chunksToBeUpdated.values.toList()
        val chunkPoses = chunkPairs.flatMap { it.toList() }
        val chunkPosesJOML = chunkPoses.map { it.toJOML() }

        // 将我们计划更新的所有块的列表发送给玩家，以便他们
        // 延迟所有更新，直到装配完成
        level.players().forEach { player ->
            PacketStopChunkUpdates(chunkPosesJOML).sendToClient(player.playerWrapper)
        }

        val toUpdate = Sets.newHashSet<Triple<BlockPos, BlockPos, BlockState>>()

        ship.activeChunksSet.forEach { chunkX, chunkZ ->
            val chunk = level.getChunk(chunkX, chunkZ)
            for (section in chunk.sections) {
                if (section == null || section.hasOnlyAir()) continue
                for (x in 0..15) {
                    for (y in 0..15) {
                        for (z in 0..15) {
                            val state = section.getBlockState(x, y, z)
                            if (state.isAir) continue

                            val realX = (chunkX shl 4) + x
                            val realY = section.bottomBlockY() + y
                            val realZ = (chunkZ shl 4) + z

                            val inWorldPos = shipToWorld.transformPosition(alloc0.set(realX + 0.5, realY + 0.5, realZ + 0.5)).floor()

                            val inWorldBlockPos = BlockPos(inWorldPos.x.toInt(), inWorldPos.y.toInt(), inWorldPos.z.toInt())
                            val inShipPos = BlockPos(realX, realY, realZ)

                            toUpdate.add(Triple(inShipPos, inWorldBlockPos, state))
                            level.relocateBlock(inShipPos, inWorldBlockPos, false, null, rotation)
                        }
                    }
                }
            }
        }
        // 我们在设置块后更新它们，以防止块破裂
        for (triple in toUpdate) {
            updateBlock(level, triple.first, triple.second, triple.third)
        }

        level.server.executeIf(
            // 如果所有修改的块都已加载并且
            // 块更新数据包已发送给玩家，此条件将返回true
            { chunkPoses.all(level::isTickingChunk) }
        ) {
            // 一旦所有的块更新都发送给玩家，我们就可以告诉他们重新开始块更新
            level.players().forEach { player ->
                PacketRestartChunkUpdates(chunkPosesJOML).sendToClient(player.playerWrapper)
            }
        }
    }
    private val logger by logger()
}
