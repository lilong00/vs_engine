package org.valkyrienskies.engine.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.Level
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.DirectionalBlock
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.material.Material
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.engine.ship.ThrusterShipControl
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import java.util.*


class ThrusterBlock : DirectionalBlock(Properties.of(Material.BAMBOO)) {

    init {
        registerDefaultState(defaultBlockState().setValue(FACING, Direction.NORTH))
    }

    override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(FACING)
    }

    override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

    override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState {
        return defaultBlockState()
            .setValue(FACING, ctx.nearestLookingDirection.opposite)
    }

    override fun animateTick(state: BlockState, level: Level, pos: BlockPos, random: Random) {
        super.animateTick(state, level, pos, random)
        val dir = state.getValue(FACING)

        val x = pos.x.toDouble() + (0.5 * (dir.stepX + 1));
        val y = pos.y.toDouble() + (0.5 * (dir.stepY + 1));
        val z = pos.z.toDouble() + (0.5 * (dir.stepZ + 1));
        val speedX = dir.stepX * 0.24
        val speedY = dir.stepY * 0.24
        val speedZ = dir.stepZ * 0.24

        for (i in 0..16) {
            val x2 = x + random.nextDouble() * 0.2 - 0.1
            val y2 = y + random.nextDouble() * 0.2 - 0.1
            val z2 = z + random.nextDouble() * 0.2 - 0.1
            //level.addParticle(ParticleTypes.CAMPFIRE_COSY_SMOKE, x2, y2, z2, speedX, speedY, speedZ)
        }
    }

    override fun neighborChanged(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        neighborBlock: Block,
        fromPos: BlockPos,
        moving: Boolean
    ) {
        super.neighborChanged(state, level, pos, neighborBlock, fromPos, moving)
        if (level.getBestNeighborSignal(pos) > 0) {
            level as ServerLevel
            val ship = level.getShipObjectManagingPos(pos) ?: level.getShipManagingPos(pos) ?: return
            ThrusterShipControl.getOrCreate(ship).addFarter(pos, state.getValue(FACING))
        } else {
            level as ServerLevel
            level.getShipManagingPos(pos)?.getAttachment<ThrusterShipControl>()?.removeFarter(pos, state.getValue(FACING))
        }
    }
}