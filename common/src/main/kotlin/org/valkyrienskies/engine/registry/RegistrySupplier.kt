package org.valkyrienskies.engine.registry

interface RegistrySupplier<T> {

    val name: String
    fun get(): T

}