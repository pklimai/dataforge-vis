package hep.dataforge.vision.solid.fx

import hep.dataforge.meta.*
import hep.dataforge.names.Name
import hep.dataforge.names.startsWith
import hep.dataforge.names.toName
import hep.dataforge.vision.Vision
import javafx.application.Platform
import javafx.beans.binding.ObjectBinding
import tornadofx.*

/**
 * A caching binding collection for [Vision] properties
 */
class VisualObjectFXBinding(val obj: Vision) {
    private val bindings = HashMap<Name, ObjectBinding<MetaItem<*>?>>()

    init {
        obj.onPropertyChange(this) { name ->
            bindings.filter { it.key.startsWith(name) }.forEach { entry ->
                Platform.runLater {
                    entry.value.invalidate()
                }
            }
//            var currentName = name
//            while (!currentName.isEmpty()) {
//                //recursively update all upper level bindings
//                bindings[currentName]?.invalidate()
//                currentName = currentName.cutLast()
//            }
        }
    }

    operator fun get(key: Name): ObjectBinding<MetaItem<*>?> {
        return bindings.getOrPut(key) {
            object : ObjectBinding<MetaItem<*>?>() {
                override fun computeValue(): MetaItem<*>? = obj.getItem(key)
            }
        }
    }

    operator fun get(key: String) = get(key.toName())
}

fun ObjectBinding<MetaItem<*>?>.value() = objectBinding { it.value }
fun ObjectBinding<MetaItem<*>?>.string() = stringBinding { it.string }
fun ObjectBinding<MetaItem<*>?>.number() = objectBinding { it.number }
fun ObjectBinding<MetaItem<*>?>.double() = objectBinding { it.double }
fun ObjectBinding<MetaItem<*>?>.float() = objectBinding { it.float }
fun ObjectBinding<MetaItem<*>?>.int() = objectBinding { it.int }
fun ObjectBinding<MetaItem<*>?>.long() = objectBinding { it.long }
fun ObjectBinding<MetaItem<*>?>.node() = objectBinding { it.node }

fun ObjectBinding<MetaItem<*>?>.string(default: String) = stringBinding { it.string ?: default }
fun ObjectBinding<MetaItem<*>?>.double(default: Double) = doubleBinding { it.double ?: default }
fun ObjectBinding<MetaItem<*>?>.float(default: Float) = floatBinding { it.float ?: default }
fun ObjectBinding<MetaItem<*>?>.int(default: Int) = integerBinding { it.int ?: default }
fun ObjectBinding<MetaItem<*>?>.long(default: Long) = longBinding { it.long ?: default }

fun <T> ObjectBinding<MetaItem<*>?>.transform(transform: (MetaItem<*>) -> T) = objectBinding { it?.let(transform) }
