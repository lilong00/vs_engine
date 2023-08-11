package org.valkyrienskies.engine.gui.shiphelm

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.network.chat.TranslatableComponent
import net.minecraft.resources.ResourceLocation
import net.minecraft.world.entity.player.Inventory
import net.minecraft.world.phys.BlockHitResult
import org.valkyrienskies.engine.EngineConfig
import org.valkyrienskies.engine.EngineMod
import org.valkyrienskies.mod.common.getShipManagingPos

class ShipHelmScreen(handler: ShipHelmScreenMenu, playerInventory: Inventory, text: Component) :
    AbstractContainerScreen<ShipHelmScreenMenu>(handler, playerInventory, text) {

    // 定义船舶组装按钮
    private lateinit var assembleButton: ShipHelmButton
    // 定义船舶对齐按钮
    private lateinit var alignButton: ShipHelmButton
    // 定义船舶拆解按钮
    private lateinit var disassembleButton: ShipHelmButton

    // 定义增加和减少按钮
    private lateinit var NorthincreaseButton: ShipHelmButton
    private lateinit var NorthdecreaseButton: ShipHelmButton
    private lateinit var EastincreaseButton: ShipHelmButton
    private lateinit var EastdecreaseButton: ShipHelmButton
    private lateinit var SouthincreaseButton: ShipHelmButton
    private lateinit var SouthdecreaseButton: ShipHelmButton
    private lateinit var WestincreaseButton: ShipHelmButton
    private lateinit var WestdecreaseButton: ShipHelmButton
    private lateinit var UpincreaseButton: ShipHelmButton
    private lateinit var UpeddecreaseButton: ShipHelmButton
    private lateinit var DownincreaseButton: ShipHelmButton
    private lateinit var DowndecreaseButton: ShipHelmButton

    // 获取玩家当前看向的方块位置
    private val pos = (Minecraft.getInstance().hitResult as? BlockHitResult)?.blockPos

    init {
        titleLabelX = 120
    }

    var GUINorth = North()
    var GUIEast = East()
    var GUISouth = South()
    var GUIWest = West()
    var GUIUp = Up()
    var GUIDown = Down()

    // 初始化界面
    override fun init() {
        super.init()
        val x = (width - imageWidth) / 2
        val y = (height - imageHeight) / 2

        // 添加组装按钮
        assembleButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_1_X, y + BUTTON_1_Y, 56, 23, ASSEMBLE_TEXT, font) {
                minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 0)
            }
        )

        // 添加对齐按钮
        alignButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_2_X, y + BUTTON_2_Y, 56, 23, ALIGN_TEXT, font) {
                minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 1)
            }
        )

        // 添加拆解按钮
        disassembleButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_3_X, y + BUTTON_3_Y, 56, 23,TODO_TEXT, font) {
                minecraft?.gameMode?.handleInventoryButtonClick(menu.containerId, 3)
            }
        )

        // 根据服务器配置设置拆解按钮是否可用
        disassembleButton.active = EngineConfig.SERVER.allowDisassembly
        updateButtons()

        NorthincreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_4_X, y + BUTTON_4_Y, 23,13, INCREASE_TEXT, font) {
                if (North < 96) {
                    North++
                    GUINorth = North()
                }
            }
        )

        NorthdecreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_5_X, y + BUTTON_5_Y, 23, 13,DECREASE_TEXT, font) {
                if (North > 0) {
                    North--
                    GUINorth = North()
                }
            }
        )

        EastincreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_6_X, y + BUTTON_6_Y, 23, 13, INCREASE_TEXT, font) {
                if (East < 96) {
                    East++
                    GUIEast = East()
                }
            }
        )

        EastdecreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_7_X, y + BUTTON_7_Y, 23, 13, DECREASE_TEXT, font) {
                if (East > 0) {
                    East--
                    GUIEast = East()
                }
            }
        )

        SouthincreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_8_X, y + BUTTON_8_Y, 23, 13, INCREASE_TEXT, font) {
                if (South < 96) {
                    South++
                    GUISouth = South()
                }
            }
        )

        SouthdecreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_9_X, y + BUTTON_9_Y, 23, 13, DECREASE_TEXT, font) {
                if (South > 0) {
                    South--
                    GUISouth = South()
                }
            }
        )

        WestincreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_10_X, y + BUTTON_10_Y, 23, 13, INCREASE_TEXT, font) {
                if (West < 96) {
                    West++
                    GUIWest = West()
                }
            }
        )

        WestdecreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_11_X, y + BUTTON_11_Y, 23, 13, DECREASE_TEXT, font) {
                if (West > 0) {
                    West--
                    GUIWest = West()
                }
            }
        )

        UpincreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_12_X, y + BUTTON_12_Y, 23, 13, INCREASE_TEXT, font) {
                if (Up < 96) {
                    Up++
                    GUIUp = Up()
                }
            }
        )

        UpeddecreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_13_X, y + BUTTON_13_Y, 23, 13, DECREASE_TEXT, font) {
                if (Up > 0) {
                    Up--
                    GUIUp = Up()
                }
            }
        )

        DownincreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_14_X, y + BUTTON_14_Y, 23, 13, INCREASE_TEXT, font) {
                if (Down < 96) {
                    Down++
                    GUIDown = Down()
                }
            }
        )

        DowndecreaseButton = addRenderableWidget(
            ShipHelmButton(x + BUTTON_15_X, y + BUTTON_15_Y, 23, 13, DECREASE_TEXT, font) {
                if (Down > 0) {
                    Down--
                    GUIDown = Down()
                }
            }
        )
    }

    // 更新按钮状态
    private fun updateButtons() {
        val level = Minecraft.getInstance().level ?: return
        val isLookingAtShip = level.getShipManagingPos(pos ?: return) != null
        assembleButton.active = !isLookingAtShip
        disassembleButton.active = EngineConfig.SERVER.allowDisassembly && isLookingAtShip
    }

    // 渲染背景
    override fun renderBg(matrixStack: PoseStack, partialTicks: Float, mouseX: Int, mouseY: Int) {
        updateButtons()

        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        RenderSystem.setShaderTexture(0, TEXTURE)
        val x = (width - NEW_IMAGE_WIDTH) / 2
        val y = (height - NEW_IMAGE_HEIGHT) / 2
        blit(matrixStack, x, y, 0, 0, NEW_IMAGE_WIDTH, NEW_IMAGE_HEIGHT)
    }

    // 渲染标签
    override fun renderLabels(matrixStack: PoseStack, i: Int, j: Int) {
        font.draw(matrixStack, title, titleLabelX.toFloat(), titleLabelY.toFloat(), 0x404040)

        if (this.menu.aligning) {
            alignButton.message = ALIGNING_TEXT
            alignButton.active = false
        } else {
            alignButton.message = ALIGN_TEXT
            alignButton.active = true
        }

        font.draw(matrixStack, "NORTH: $North", NORTH_X.toFloat(), NORTH_Y.toFloat(), 0x404040)
        font.draw(matrixStack, "EAST: $East", EAST_X.toFloat(), EAST_Y.toFloat(), 0x404040)
        font.draw(matrixStack, "South: $South", SOUTH_X.toFloat(), SOUTH_Y.toFloat(), 0x404040)
        font.draw(matrixStack, "WEST: $West", WEST_X.toFloat(), WEST_Y.toFloat(), 0x404040)
        font.draw(matrixStack, "Up: $Up", UP_X.toFloat(), UP_Y.toFloat(), 0x404040)
        font.draw(matrixStack, "Down: $Down", DOWN_X.toFloat(), DOWN_Y.toFloat(), 0x404040)

        // TODO render stats
    }

    // 处理鼠标释放事件
    override fun mouseReleased(mouseX: Double, mouseY: Double, button: Int): Boolean {
        isDragging = false
        if (getChildAt(mouseX, mouseY).filter { it.mouseReleased(mouseX, mouseY, button) }.isPresent) return true

        return super.mouseReleased(mouseX, mouseY, button)
    }

    companion object { // TEXTURE DATA

        var North = 0
        fun North(){
            North
        }

        var East = 0
        fun East(){
            East
        }
        var South = 0
        fun South(){
            South
        }
        var West = 0
        fun West(){
            West
        }
        var Up = 0
        fun Up(){
            Up
        }
        var Down = 0
        fun Down(){
            Down
        }

        internal val TEXTURE = ResourceLocation(EngineMod.MOD_ID, "textures/gui/ship_helm.png")

        private const val BUTTON_4_X = -143
        private const val BUTTON_4_Y = 3
        private const val BUTTON_5_X = -143
        private const val BUTTON_5_Y = 33
        private const val BUTTON_6_X = -93
        private const val BUTTON_6_Y = 53
        private const val BUTTON_7_X = -93
        private const val BUTTON_7_Y = 83
        private const val BUTTON_8_X = -143
        private const val BUTTON_8_Y = 53
        private const val BUTTON_9_X = -143
        private const val BUTTON_9_Y = 83
        private const val BUTTON_10_X = -93
        private const val BUTTON_10_Y = 3
        private const val BUTTON_11_X = -93
        private const val BUTTON_11_Y = 33
        private const val BUTTON_12_X = -43
        private const val BUTTON_12_Y = 53
        private const val BUTTON_13_X = -43
        private const val BUTTON_13_Y = 83
        private const val BUTTON_14_X = -43
        private const val BUTTON_14_Y = 3
        private const val BUTTON_15_X = -43
        private const val BUTTON_15_Y = 33

        private const val NORTH_X = 183
        private const val NORTH_Y = 3
        private const val EAST_X = 183
        private const val EAST_Y = 23
        private const val SOUTH_X = 183
        private const val SOUTH_Y = 43
        private const val WEST_X = 183
        private const val WEST_Y = 63
        private const val UP_X = 183
        private const val UP_Y = 83
        private const val DOWN_X = 183
        private const val DOWN_Y = 103

        private const val BUTTON_1_X = 10
        private const val BUTTON_1_Y = 73
        private const val BUTTON_2_X = 10
        private const val BUTTON_2_Y = 103
        private const val BUTTON_3_X = 10
        private const val BUTTON_3_Y = 133

        private const val NEW_IMAGE_WIDTH = 400
        private const val NEW_IMAGE_HEIGHT = 300

        private val INCREASE_TEXT = TranslatableComponent("gui.vs_engine.increase")
        private val DECREASE_TEXT = TranslatableComponent("gui.vs_engine.decrease")

        private val ASSEMBLE_TEXT = TranslatableComponent("gui.vs_engine.assemble")
        private val DISSEMBLE_TEXT = TranslatableComponent("gui.vs_engine.disassemble")
        private val ALIGN_TEXT = TranslatableComponent("gui.vs_engine.align")
        private val ALIGNING_TEXT = TranslatableComponent("gui.vs_engine.aligning")
        private val TODO_TEXT = TranslatableComponent("gui.vs_engine.todo")
    }
}
