package hep.dataforge.vision.solid.fx

import hep.dataforge.meta.MetaItem
import hep.dataforge.meta.double
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.values.ValueType
import hep.dataforge.vision.Colors
import hep.dataforge.vision.solid.SolidMaterial
import javafx.scene.paint.Color
import javafx.scene.paint.Material
import javafx.scene.paint.PhongMaterial

object FXMaterials {
    val RED = PhongMaterial().apply {
        diffuseColor = Color.DARKRED
        specularColor = Color.WHITE
    }

    val WHITE = PhongMaterial().apply {
        diffuseColor = Color.WHITE
        specularColor = Color.LIGHTBLUE
    }

    val GREY = PhongMaterial().apply {
        diffuseColor = Color.DARKGREY
        specularColor = Color.WHITE
    }

    val BLUE = PhongMaterial(Color.BLUE)
}

/**
 * Infer color based on meta item
 * @param opacity default opacity
 */
fun MetaItem<*>.color(opacity: Double = 1.0): Color {
    return when (this) {
        is MetaItem.ValueItem -> if (this.value.type == ValueType.NUMBER) {
            val int = value.number.toInt()
            val red = int and 0x00ff0000 shr 16
            val green = int and 0x0000ff00 shr 8
            val blue = int and 0x000000ff
            Color.rgb(red, green, blue, opacity)
        } else {
            Color.web(this.value.string)
        }
        is MetaItem.NodeItem -> {
            Color.rgb(
                node[Colors.RED_KEY]?.int ?: 0,
                node[Colors.GREEN_KEY]?.int ?: 0,
                node[Colors.BLUE_KEY]?.int ?: 0,
                node[SolidMaterial.OPACITY_KEY]?.double ?: opacity
            )
        }
    }
}

/**
 * Infer FX material based on meta item
 */
fun MetaItem<*>?.material(): Material {
    return when (this) {
        null -> FXMaterials.GREY
        is MetaItem.ValueItem -> PhongMaterial(color())
        is MetaItem.NodeItem -> PhongMaterial().apply {
            val opacity = node[SolidMaterial.OPACITY_KEY].double ?: 1.0
            diffuseColor = node[SolidMaterial.COLOR_KEY]?.color(opacity) ?: Color.DARKGREY
            specularColor = node[SolidMaterial.SPECULAR_COLOR_KEY]?.color(opacity) ?: Color.WHITE
        }
    }
}

