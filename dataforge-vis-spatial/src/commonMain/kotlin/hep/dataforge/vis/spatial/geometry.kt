package hep.dataforge.vis.spatial

import hep.dataforge.meta.Meta
import hep.dataforge.meta.buildMeta
import hep.dataforge.meta.get
import hep.dataforge.meta.number

expect class Point2D(x: Number, y: Number) {
    var x: Double
    var y: Double
}

operator fun Point2D.component1() = x
operator fun Point2D.component2() = y

fun Point2D.toMeta() = buildMeta {
    VisualObject3D.x put x
    VisualObject3D.y put y
}

fun Meta.point2D() = Point2D(this["x"].number ?: 0, this["y"].number ?: 0)

expect class Point3D(x: Number, y: Number, z: Number) {
    var x: Double
    var y: Double
    var z: Double
}

expect operator fun Point3D.plus(other: Point3D): Point3D

operator fun Point3D.component1() = x
operator fun Point3D.component2() = y
operator fun Point3D.component3() = z

fun Meta.point3D() = Point3D(this["x"].number ?: 0, this["y"].number ?: 0, this["y"].number ?: 0)

fun Point3D.toMeta() = buildMeta {
    VisualObject3D.x put x
    VisualObject3D.y put y
    VisualObject3D.z put z
}