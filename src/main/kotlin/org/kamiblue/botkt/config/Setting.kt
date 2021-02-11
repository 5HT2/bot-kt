package org.kamiblue.botkt.config

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import org.kamiblue.commons.interfaces.Nameable
import java.util.*
import kotlin.reflect.KProperty

class Setting<T : Any>(
    override val name: String,
    value: T,
    val description: String,
    val consumer: (T, T) -> T
) : Nameable {

    var value = value
        set(value) {
            field = consumer(field, value)
        }
    val defaultValue = value
    val valueClass = value.javaClass
    val jsonName = name.toLowerCase(Locale.ROOT).replace(' ', '_')

    operator fun getValue(thisRef: Any?, property: KProperty<*>) = value

    operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
        this.value = value
    }

    fun reset() {
        value = defaultValue
    }

    fun read(jsonElement: JsonElement) {
        value = gson.fromJson(jsonElement, valueClass)
    }

    fun write() = gson.toJsonTree(value)

    override fun toString(): String {
        return "$name: $value"
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is Setting<*>) return false

        if (defaultValue != other.defaultValue) return false
        if (valueClass != other.valueClass) return false
        if (jsonName != other.jsonName) return false

        return true
    }

    override fun hashCode(): Int {
        var result = defaultValue.hashCode()
        result = 31 * result + valueClass.hashCode()
        result = 31 * result + jsonName.hashCode()
        return result
    }

    private companion object {
        val gson: Gson = GsonBuilder().setPrettyPrinting().create()
    }
}
