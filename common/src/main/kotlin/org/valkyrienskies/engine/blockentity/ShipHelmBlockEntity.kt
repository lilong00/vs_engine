package org.valkyrienskies.engine.blockentity

import net.minecraft.Util
import net.minecraft.commands.arguments.EntityAnchorArgument
import net.minecraft.core.BlockPos
import net.minecraft.core.Direction
import net.minecraft.core.Direction.Axis
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.server.level.ServerLevel
import net.minecraft.world.MenuProvider
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import net.minecraft.world.level.block.HorizontalDirectionalBlock
import net.minecraft.world.level.block.StairBlock
import net.minecraft.world.level.block.entity.BlockEntity
import net.minecraft.world.level.block.state.BlockState
import net.minecraft.world.level.block.state.properties.BlockStateProperties.HORIZONTAL_FACING
import net.minecraft.world.level.block.state.properties.Half
import org.joml.Vector3d
import org.joml.Vector3dc
import org.valkyrienskies.core.api.ships.ServerShip
import org.valkyrienskies.core.impl.api.ServerShipProvider
import org.valkyrienskies.core.impl.api.shipValue
import org.valkyrienskies.core.impl.util.logger
import org.valkyrienskies.engine.EngineBlockEntities
import org.valkyrienskies.engine.EngineConfig
import org.valkyrienskies.engine.block.ShipHelmBlock
import org.valkyrienskies.engine.gui.shiphelm.ShipHelmScreen
import org.valkyrienskies.engine.gui.shiphelm.ShipHelmScreenMenu
import org.valkyrienskies.engine.ship.EngineShipControl
import org.valkyrienskies.engine.util.ShipAssembler
import org.valkyrienskies.mod.common.ValkyrienSkiesMod
import org.valkyrienskies.mod.common.entity.ShipMountingEntity
import org.valkyrienskies.mod.common.getShipObjectManagingPos
import org.valkyrienskies.mod.common.util.toDoubles
import org.valkyrienskies.mod.common.util.toJOMLD

class ShipHelmBlockEntity(pos: BlockPos, state: BlockState) :
    BlockEntity(EngineBlockEntities.SHIP_HELM.get(), pos, state), MenuProvider, ServerShipProvider {

    // 船只对象，如果当前没有船只，则从服务器获取
    override var ship: ServerShip? = null // TODO ship is not being set in vs2?
        get() = field ?: (level as ServerLevel).getShipObjectManagingPos(this.blockPos)
    // 船只控制器
    val control by shipValue<EngineShipControl>()
    // 船上的座位
    val seats = mutableListOf<ShipMountingEntity>()
    // 判断船只是否已经组装
    val assembled get() = ship != null
    // 判断船只是否正在对齐
    val aligning get() = control?.aligning ?: false
    // 当可能时，是否应该拆解船只
    var shouldDisassembleWhenPossible = false

    // 创建船舵屏幕菜单
    override fun createMenu(id: Int, playerInventory: Inventory, player: Player): AbstractContainerMenu {
        return ShipHelmScreenMenu(id, playerInventory, this,)
    }

    // 获取显示名称
    override fun getDisplayName(): Component {
        return TranslatableComponent("gui.vs_engine.ship_helm")
    }

    // 需要在服务器端调用
    fun spawnSeat(blockPos: BlockPos, state: BlockState, level: ServerLevel): ShipMountingEntity {
        // 获取新的位置
        val newPos = blockPos.relative(state.getValue(HorizontalDirectionalBlock.FACING))
        // 获取新位置的状态
        val newState = level.getBlockState(newPos)
        // 获取新位置的形状
        val newShape = newState.getShape(level, newPos)
        // 获取新位置的块
        val newBlock = newState.block
        // 设置高度
        var height = 0.5
        if (!newState.isAir) {
            height = if (
                newBlock is StairBlock &&
                (!newState.hasProperty(StairBlock.HALF) || newState.getValue(StairBlock.HALF) == Half.BOTTOM)
            )
                0.5 // 有效的StairBlock
            else
                newShape.max(Axis.Y)
        }
        // 创建实体
        val entity = ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE.create(level)!!.apply {
            // 设置实体位置
            val seatEntityPos: Vector3dc = Vector3d(newPos.x + .5, (newPos.y - .5) + height, newPos.z + .5)
            moveTo(seatEntityPos.x(), seatEntityPos.y(), seatEntityPos.z())

            // 设置实体朝向
            lookAt(
                EntityAnchorArgument.Anchor.EYES,
                state.getValue(HORIZONTAL_FACING).normal.toDoubles().add(position())
            )

            // 设置为控制器
            isController = true
        }

        // 添加实体
        level.addFreshEntityWithPassengers(entity)
        return entity
    }

    // 开始骑行
    fun startRiding(player: Player, force: Boolean, blockPos: BlockPos, state: BlockState, level: ServerLevel): Boolean {

        // 清理无效的座位
        for (i in seats.size-1 downTo 0) {
            if (!seats[i].isVehicle) {
                seats[i].kill()
                seats.removeAt(i)
            } else if (!seats[i].isAlive) {
                seats.removeAt(i)
            }
        }

        // 生成座位
        val seat = spawnSeat(blockPos, blockState, level)
        // 开始骑行
        val ride = player.startRiding(seat, force)

        // 如果骑行成功，添加座位
        if (ride) {
            control?.seatedPlayer = player
            seats.add(seat)
        }

        return ride;
    }
    //组装船只
    fun assemble(player: Player) {
        val level = level as ServerLevel

        // 检查组装之前的方块状态，以避免创建空船
        val blockState = level.getBlockState(blockPos)
        if (blockState.block !is ShipHelmBlock) return

        val direction = blockState.getValue(HORIZONTAL_FACING)
        val axis = when (direction) {
            Direction.NORTH -> Axis.Z
            Direction.SOUTH -> Axis.Z
            Direction.EAST -> Axis.X
            Direction.WEST -> Axis.X
            else -> Axis.Y
        }

        val shipSteeringDirection = direction.opposite // 船舵的朝向与方块朝向相反

        val builtShip = ShipAssembler.collectBlocksInRange(
            level,
            blockPos,
            ShipHelmScreen.Up,
            ShipHelmScreen.Down,
            ShipHelmScreen.East,
            ShipHelmScreen.West,
            ShipHelmScreen.South,
            ShipHelmScreen.North,
            axis,  // 添加新的参数：轴方向
            { _ -> true },
            shipSteeringDirection // 添加新的参数：船舵的朝向
        )

        if (builtShip == null) {
            player.sendMessage(
                net.minecraft.network.chat.TextComponent("Ship is too big! Max size is ${EngineConfig.SERVER.maxShipBlocks} blocks (changable in the config)"),
                Util.NIL_UUID
            )
            logger.warn("Failed to assemble ship for ${player.name.string}")
        }
    }

    // 拆解船只
    fun disassemble() {
        val ship = ship ?: return
        val level = level ?: return
        val control = control ?: return

        if (!control.canDisassemble) {
            shouldDisassembleWhenPossible = true
            control.disassembling = true
            control.aligning = true
            return
        }

        val inWorld = ship.shipToWorld.transformPosition(this.blockPos.toJOMLD())

        ShipAssembler.unfillShip(
            level as ServerLevel,
            ship,
            control.aligningTo,
            this.blockPos,
            BlockPos(inWorld.x, inWorld.y, inWorld.z)
        )
        // ship.die() TODO 我们需要这个吗？或者在所有空气中自动检测

        shouldDisassembleWhenPossible = false
    }

    // 对齐船只
    fun align() {
        val control = control ?: return
        control.aligning = !control.aligning
    }

    // 移除设置
    override fun setRemoved() {

        if (level?.isClientSide == false) {
            for (i in seats.indices) {
                seats[i].kill()
            }
            seats.clear()
        }

        super.setRemoved()
    }

    // 坐下
    fun sit(player: Player, force: Boolean = false): Boolean {
        // 如果玩家已经在控制船只，打开舵盘菜单
        if (!force && player.vehicle?.type == ValkyrienSkiesMod.SHIP_MOUNTING_ENTITY_TYPE && seats.contains(player.vehicle as ShipMountingEntity))
        {
            player.openMenu(this);
            return true;
        }

        //val seat = spawnSeat(blockPos, blockState, level as ServerLevel)
        //control?.seatedPlayer = player
        //return player.startRiding(seat, force)
        return startRiding(player, force, blockPos, blockState, level as ServerLevel)

    }

    fun tick() {
        // 在这里添加你想要在每个游戏刻执行的代码
    }
    private val logger by logger()
}