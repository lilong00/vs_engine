package org.valkyrienskies.engine

import net.minecraft.core.Registry
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties
import org.valkyrienskies.core.apigame.world.chunks.BlockType
import org.valkyrienskies.mod.common.BlockStateInfo
import org.valkyrienskies.mod.common.BlockStateInfoProvider
import org.valkyrienskies.physics_api.Lod1BlockStateId
import org.valkyrienskies.physics_api.Lod1LiquidBlockStateId
import org.valkyrienskies.physics_api.Lod1SolidBlockStateId
import org.valkyrienskies.physics_api.voxel.Lod1LiquidBlockState
import org.valkyrienskies.physics_api.voxel.Lod1SolidBlockState

object EngineWeights : BlockStateInfoProvider {
    override val blockStateData: List<Triple<Lod1SolidBlockStateId, Lod1LiquidBlockStateId, Lod1BlockStateId>>
        get() = emptyList()
    override val liquidBlockStates: List<Lod1LiquidBlockState>
        get() = emptyList()
    override val priority: Int
        get() = 200

    override val solidBlockStates: List<Lod1SolidBlockState>
        get() = emptyList()

    override fun getBlockStateMass(blockState: BlockState): Double? {
        if (blockState.block == EngineBlocks.BALLAST.get()) {
            return EngineConfig.SERVER.ballastWeight + (EngineConfig.SERVER.ballastNoWeight - EngineConfig.SERVER.ballastWeight) * (
                    (
                            blockState.getValue(
                                BlockStateProperties.POWER
                            ) + 1
                            ) / 16.0
                    )
        }

        return null
    }

    override fun getBlockStateType(blockState: BlockState): BlockType? {
        return null
    }

    fun register() {
        Registry.register(BlockStateInfo.REGISTRY, ResourceLocation(EngineMod.MOD_ID, "ballast"), this)
    }
}
