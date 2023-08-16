package org.valkyrienskies.engine.block

import net.minecraft.util.StringRepresentable

// TODO mod compat

enum class WoodType(val resourceName: String) : StringRepresentable {
    ;

    override fun getSerializedName(): String = resourceName
}
