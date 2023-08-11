package org.valkyrienskies.engine.block

import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.InteractionHand
import net.minecraft.world.InteractionResult
import net.minecraft.world.entity.player.Player
import net.minecraft.world.item.context.BlockPlaceContext
import net.minecraft.world.level.BlockGetter
import net.minecraft.world.level.Level
import net.minecraft.world.level.LevelAccessor
import net.minecraft.world.level.block.BaseEntityBlock
import net.minecraft.world.level.block.Block
import net.minecraft.world.level.block.RenderShape
import net.minecraft.world.level.block.Rotation
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.entity.BlockEntityTicker
import net.minecraft.world.level.block.entity.BlockEntityType
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.StateDefinition
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.pathfinder.PathComputationType
import net.minecraft.world.phys.BlockHitResult
import net.minecraft.world.phys.shapes.CollisionContext
import net.minecraft.world.phys.shapes.VoxelShape
import org.valkyrienskies.core.api.ships.getAttachment
import org.valkyrienskies.engine.blockentity.ShipHelmBlockEntity
import org.valkyrienskies.engine.ship.EngineShipControl
import org.valkyrienskies.engine.util.DirectionalShape
import org.valkyrienskies.engine.util.RotShapes
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.getShipManagingPos
import org.valkyrienskies.mod.common.getShipObjectManagingPos

   class AssemblyCoreBlock(properties: Properties) : BaseEntityBlock(properties) {
        // 定义船舵的基座形状
    val HELM_BASE = RotShapes.box(0.0, 0.0, 0.0, 16.0, 16.0, 16.0)

    // 定义船舵的整体形状，由基座和杆组成
    val HELM_SHAPE = DirectionalShape(HELM_BASE)

       // 初始化函数，注册默认状态
   init {
        registerDefaultState(this.stateDefinition.any().setValue(HORIZONTAL_FACING, Direction.NORTH))
    }

       // 当块被放置时的操作
  override fun onPlace(state: BlockState, level: Level, pos: BlockPos, oldState: BlockState, isMoving: Boolean) {
        super.onPlace(state, level, pos, oldState, isMoving)

        if (level.isClientSide) return
        level as ServerLevel

        val ship = level.getShipObjectManagingPos(pos) ?: level.getShipManagingPos(pos) ?: return
        EngineShipControl.getOrCreate(ship).helms += 1
    }

       // 当块被破坏时的操作
  override fun destroy(level: LevelAccessor, pos: BlockPos, state: BlockState) {
        super.destroy(level, pos, state)

        if (level.isClientSide) return
        level as ServerLevel

        level.getShipManagingPos(pos)?.getAttachment<EngineShipControl>()?.let { control ->

            if (control.helms <= 1 && control.seatedPlayer?.vehicle?.type == ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE) {
                control.seatedPlayer!!.unRide()
                control.seatedPlayer = null
            }

            control.helms -= 1
        }
    }
       

  // 当块被使用时的操作
    override fun use(
        state: BlockState,
        level: Level,
        pos: BlockPos,
        player: Player,
        hand: InteractionHand,
        blockHitResult: BlockHitResult
    ): InteractionResult {
        if (level.isClientSide) return InteractionResult.SUCCESS
        val blockEntity = level.getBlockEntity(pos) as ShipHelmBlockEntity

        // 打开GUI
        player.openMenu(blockEntity)

        return InteractionResult.CONSUME
    }

       // 获取渲染形状
  override fun getRenderShape(blockState: BlockState): RenderShape {
        return RenderShape.MODEL
    }

       // 获取放置状态
  override fun getStateForPlacement(ctx: BlockPlaceContext): BlockState? {
        return defaultBlockState()
            .setValue(HORIZONTAL_FACING, ctx.horizontalDirection.opposite)
    }

       // 创建块状态定义
  override fun createBlockStateDefinition(builder: StateDefinition.Builder<Block, BlockState>) {
        builder.add(HORIZONTAL_FACING)
    }

      // 创建新的块实体
  override fun newBlockEntity(blockPos: BlockPos, state: BlockState): BlockEntity {
        return ShipHelmBlockEntity(blockPos, state)
    }

       // 获取形状
  override fun getShape(
        blockState: BlockState,
        blockGetter: BlockGetter,
        blockPos: BlockPos,
        collisionContext: CollisionContext
    ): VoxelShape {
        return HELM_SHAPE[blockState.getValue(HORIZONTAL_FACING)]
    }

      // 使用形状进行光照遮挡
  override fun useShapeForLightOcclusion(blockState: BlockState): Boolean {
        return true
    }

       // 判断是否可以寻路
  override fun isPathfindable(
            blockState: BlockState,
            blockGetter: BlockGetter,
            blockPos: BlockPos,
            pathComputationType: PathComputationType
    ): Boolean {
        return false
    }

       // 旋转块状态
  override fun rotate(state: BlockState, rotation: Rotation): BlockState? {
        return state.setValue(HORIZONTAL_FACING, rotation.rotate(state.getValue(HORIZONTAL_FACING) as Direction)) as BlockState
    }

       // 获取实体块的计时器
  override fun <T : BlockEntity?> getTicker(
        level: Level,
        state: BlockState,
        type: BlockEntityType<T>
    ): BlockEntityTicker<T> = BlockEntityTicker { level, pos, state, blockEntity ->
        if (level.isClientSide) return@BlockEntityTicker
        if (blockEntity is ShipHelmBlockEntity) {
            blockEntity.tick()
        }
    }
}


