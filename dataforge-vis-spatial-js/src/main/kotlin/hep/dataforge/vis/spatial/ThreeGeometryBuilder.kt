package hep.dataforge.vis.spatial

import hep.dataforge.meta.Meta
import hep.dataforge.meta.get
import hep.dataforge.meta.int
import hep.dataforge.vis.spatial.three.toBufferGeometry
import info.laht.threekt.core.BufferGeometry
import info.laht.threekt.core.Face3
import info.laht.threekt.core.Geometry
import info.laht.threekt.math.Color
import info.laht.threekt.math.Vector3

// TODO use unsafe cast instead
fun Point3D.asVector(): Vector3 = Vector3(this.x, this.y, this.z)

class ThreeGeometryBuilder : GeometryBuilder<BufferGeometry> {

    private val vertices = ArrayList<Point3D>()
    private val faces = ArrayList<Face3>()

    private val vertexCache = HashMap<Point3D, Int>()

    private fun append(vertex: Point3D): Int {
        val index = vertexCache[vertex] ?: -1//vertices.indexOf(vertex)
        return if (index > 0) {
            index
        } else {
            vertices.add(vertex)
            vertexCache[vertex] = vertices.size - 1
            vertices.size - 1
        }
    }

    override fun face(vertex1: Point3D, vertex2: Point3D, vertex3: Point3D, normal: Point3D?, meta: Meta) {
        val materialIndex = meta["materialIndex"].int ?: 0
        val color = meta["color"]?.color() ?: Color()
        faces.add(
            Face3(
                append(vertex1),
                append(vertex2),
                append(vertex3),
                normal?.asVector() ?: Vector3(0, 0, 0),
                color,
                materialIndex
            )
        )
    }

    override fun build(): BufferGeometry {
        return Geometry().apply {
            vertices = this@ThreeGeometryBuilder.vertices.map { it.asVector() }.toTypedArray()
            faces = this@ThreeGeometryBuilder.faces.toTypedArray()
        }.toBufferGeometry()
    }
}