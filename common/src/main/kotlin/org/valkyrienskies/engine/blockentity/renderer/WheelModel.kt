package org.valkyrienskies.engine.blockentity.renderer

import com.google.common.collect.ImmutableMap
import com.mojang.blaze3d.vertex.PoseStack
import net.minecraft.client.Minecraft
import net.minecraft.client.resources.model.BakedModel
import net.minecraft.world.level.block.state.StateHolder
import net.minecraft.world.level.block.state.properties.EnumProperty
import org.valkyrienskies.engine.block.WoodType
import java.util.function.Function

// OK so what dis does im making mc happy about states
// WheelModels has many states (wood type)
// WheelModel has 1 woodtype and represents 1 state
// In the mixin it gets queued and abused
object WheelModels {
    private val mc get() = Minecraft.getInstance()
    private val property = EnumProperty.create("wood", WoodType::class.java)

    private val models by lazy { property.possibleValues.associateWith { WheelModel(it) } }

    fun render(
        matrixStack: PoseStack
    ) {
        matrixStack.pushPose()
        matrixStack.translate(-0.5, -0.625, -0.25)

        matrixStack.popPose()
    }

    fun setModelGetter(getter: Function<WoodType, BakedModel>) {
        models.values.forEach { it.getter = getter::apply }
    }

    class WheelModel(type: WoodType) :
        StateHolder<WheelModels, WheelModel>(WheelModels, ImmutableMap.of(property, type), null) {

        var getter: (WoodType) -> BakedModel = { throw IllegalStateException("Getter not set") }

        val model by lazy { getter(type) }
    }
}
