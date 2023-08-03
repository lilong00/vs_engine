package org.valkyrienskies.engine.gui.shiphelm

import com.mojang.blaze3d.systems.RenderSystem
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.gui.Font
import net.minecraft.client.gui.components.Button
import net.minecraft.client.renderer.GameRenderer
import net.minecraft.network.chat.Component
import net.minecraft.util.FormattedCharSequence

// 这是一个船舵按钮类，用于在游戏中创建一个按钮
class ShipHelmButton(x: Int, y: Int, width: Int, height: Int, text: Component, private val font: Font, onPress: OnPress) :
    Button(x, y, width, height, text, onPress) {

    // 按钮是否被按下的状态
    var isPressed = false

    // 初始化按钮，设置为活动状态
    init {
        active = true
    }

    // 渲染按钮的方法，包括按钮的颜色，纹理，深度测试等
    override fun renderButton(poseStack: PoseStack, mouseX: Int, mouseY: Int, partialTicks: Float) {
        // 如果按钮没有被悬停，则设置为未按下状态
        if (!isHovered) isPressed = false

        // 设置渲染系统的着色器和颜色
        RenderSystem.setShader { GameRenderer.getPositionTexShader() }
        RenderSystem.setShaderColor(1.0f, 1.0f, 1.0f, 1.0f)
        // 设置渲染系统的纹理
        RenderSystem.setShaderTexture(0, ShipHelmScreen.TEXTURE)

        // 启用混合和深度测试
        RenderSystem.enableBlend()
        RenderSystem.defaultBlendFunc()
        RenderSystem.enableDepthTest()

        // 如果按钮被按下或者不活动，则绘制按下的按钮
        // 如果按钮被悬停，则绘制悬停的按钮
        if (this.isPressed || !this.active) {
            this.blit(poseStack, x, y, BUTTON_P_X, BUTTON_P_Y, width, height)
        } else if (this.isHovered) {
            this.blit(poseStack, x, y, BUTTON_H_X, BUTTON_H_Y, width, height)
        }

        // 设置按钮的颜色和文本
        val color = 0x404040
        val formattedCharSequence: FormattedCharSequence = message.visualOrderText
        font.draw(
            poseStack,
            formattedCharSequence,
            ((x + width / 2) - font.width(formattedCharSequence) / 2).toFloat(),
            (y + (height - 8) / 2).toFloat(),
            color
        )
    }

    // 当按钮被点击时，设置为按下状态
    override fun onClick(mouseX: Double, mouseY: Double) {
        isPressed = true
        super.onClick(mouseX, mouseY)
    }

    // 当按钮被释放时，设置为未按下状态
    override fun onRelease(mouseX: Double, mouseY: Double) {
        isPressed = false
    }

    // 定义按钮的一些常量
    companion object {
        private const val BUTTON_H_X = 0
        private const val BUTTON_H_Y = 166
        private const val BUTTON_P_X = 0
        private const val BUTTON_P_Y = 189
    }
}
