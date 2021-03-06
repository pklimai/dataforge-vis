package hep.dataforge.vision.solid.three

import hep.dataforge.names.Name
import hep.dataforge.names.startsWith
import hep.dataforge.provider.Type
import hep.dataforge.vision.Vision
import hep.dataforge.vision.solid.*
import hep.dataforge.vision.solid.SolidMaterial.Companion.MATERIAL_KEY
import hep.dataforge.vision.solid.three.ThreeFactory.Companion.TYPE
import hep.dataforge.vision.solid.three.ThreeMaterials.getMaterial
import info.laht.threekt.core.BufferGeometry
import info.laht.threekt.core.Object3D
import info.laht.threekt.objects.Mesh
import kotlin.reflect.KClass

/**
 * Builder and updater for three.js object
 */
@Type(TYPE)
interface ThreeFactory<in T : Vision> {

    val type: KClass<in T>

    operator fun invoke(obj: T): Object3D

    companion object {
        const val TYPE = "threeFactory"
    }
}

/**
 * Update position, rotation and visibility
 */
fun Object3D.updatePosition(obj: Vision) {
    visible = obj.visible ?: true
    if(obj is Solid) {
        position.set(obj.x, obj.y, obj.z)
        setRotationFromEuler(obj.euler)
        scale.set(obj.scaleX, obj.scaleY, obj.scaleZ)
        updateMatrix()
    }
}

///**
// * Unsafe invocation of a factory
// */
//operator fun <T : VisualObject3D> ThreeFactory<T>.invoke(obj: Any): Object3D {
//    if (type.isInstance(obj)) {
//        @Suppress("UNCHECKED_CAST")
//        return invoke(obj as T)
//    } else {
//        error("The object of type ${obj::class} could not be rendered by this factory")
//    }
//}

/**
 * Update non-position non-geometry property
 */
fun Object3D.updateProperty(source: Vision, propertyName: Name) {
    if (this is Mesh && propertyName.startsWith(MATERIAL_KEY)) {
        this.material = getMaterial(source)
    } else if (
        propertyName.startsWith(Solid.POSITION_KEY)
        || propertyName.startsWith(Solid.ROTATION)
        || propertyName.startsWith(Solid.SCALE_KEY)
    ) {
        //update position of mesh using this object
        updatePosition(source)
    } else if (propertyName == Solid.VISIBLE_KEY) {
        visible = source.visible ?: true
    }
}

/**
 * Generic factory for elements which provide inside geometry builder
 */
object ThreeShapeFactory : MeshThreeFactory<GeometrySolid>(GeometrySolid::class) {
    override fun buildGeometry(obj: GeometrySolid): BufferGeometry {
        return obj.run {
            ThreeGeometryBuilder().apply { toGeometry(this) }.build()
        }
    }
}