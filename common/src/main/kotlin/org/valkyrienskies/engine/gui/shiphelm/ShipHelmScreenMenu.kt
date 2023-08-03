package org.valkyrienskies.engine.gui.shiphelm

import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.entity.player.Player
import net.minecraft.world.inventory.AbstractContainerMenu
import org.valkyrienskies.engine.EngineConfig
import org.valkyrienskies.engine.EngineScreens
import org.valkyrienskies.engine.blockentity.ShipHelmBlockEntity

// 这是一个船舵屏幕菜单类，用于处理船舵的交互
class ShipHelmScreenMenu(syncId: Int, playerInv: Inventory, val blockEntity: ShipHelmBlockEntity?) :
    AbstractContainerMenu(EngineScreens.SHIP_HELM.get(), syncId) {

    // 构造函数，如果没有提供船舵实体，将其设置为null
    constructor(syncId: Int, playerInv: Inventory) : this(syncId, playerInv, null)

    // 获取船舵是否正在对齐和是否已经组装的状态
    val aligning = blockEntity?.aligning ?: false
    val assembled = blockEntity?.assembled ?: false
    // 检查玩家是否可以与船舵交互
    override fun stillValid(player: Player): Boolean = true

    // 处理玩家点击菜单按钮的事件
    override fun clickMenuButton(player: Player, id: Int): Boolean {
        // 如果船舵实体不存在，返回false
        if (blockEntity == null) return false

        // 如果玩家点击的是id为0的按钮，船舵未组装，且不在客户端，执行组装操作
        if (id == 0 && !assembled && !player.level.isClientSide) {
            blockEntity.assemble(player)
            return true
        }

        // 如果玩家点击的是id为1的按钮，船舵已组装，且不在客户端，执行对齐操作
        if (id == 1 && assembled && !player.level.isClientSide) {
            blockEntity.align()
            return true
        }

        // 如果玩家点击的是id为3的按钮，船舵已组装，且不在客户端，且服务器允许拆解，执行拆解操作
        if (id == 3 && assembled && !player.level.isClientSide && EngineConfig.SERVER.allowDisassembly) {
            blockEntity.disassemble()
            return true
        }

        // 其他情况，调用父类的clickMenuButton方法
        return super.clickMenuButton(player, id)
    }

    // 伴生对象，包含一个工厂方法，用于创建ShipHelmScreenMenu实例
    companion object {
        val factory: (syncId: Int, playerInv: Inventory) -> ShipHelmScreenMenu = ::ShipHelmScreenMenu
    }
}

