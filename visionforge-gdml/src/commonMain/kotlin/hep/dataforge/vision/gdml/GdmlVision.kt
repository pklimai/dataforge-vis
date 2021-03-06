package hep.dataforge.vision.gdml


import hep.dataforge.names.Name
import hep.dataforge.names.asName
import hep.dataforge.names.plus
import hep.dataforge.vision.get
import hep.dataforge.vision.set
import hep.dataforge.vision.solid.*
import hep.dataforge.vision.solid.World.ONE
import hep.dataforge.vision.solid.World.ZERO
import scientifik.gdml.*
import kotlin.math.cos
import kotlin.math.sin


private fun Solid.withPosition(
    lUnit: LUnit,
    newPos: GDMLPosition? = null,
    newRotation: GDMLRotation? = null,
    newScale: GDMLScale? = null
): Solid = apply {
    newPos?.let {
        val point = Point3D(it.x(lUnit), it.y(lUnit), it.z(lUnit))
        if (position != null || point != ZERO) {
            position = point
        }
    }
    newRotation?.let {
        val point = Point3D(it.x(), it.y(), it.z())
        if (rotation != null || point != ZERO) {
            rotation = point
        }
        //this@withPosition.rotationOrder = RotationOrder.ZXY
    }
    newScale?.let {
        val point = Point3D(it.x, it.y, it.z)
        if (scale != null || point != ONE) {
            scale = point
        }
    }
    //TODO convert units if needed
}

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Number.times(d: Double) = toDouble() * d

@Suppress("NOTHING_TO_INLINE")
private inline operator fun Number.times(f: Float) = toFloat() * f

private fun SolidGroup.addSolid(
    context: GDMLTransformer,
    solid: GDMLSolid,
    name: String = "",
    block: Solid.() -> Unit = {}
): Solid {
    //context.solidAdded(solid)
    val lScale = solid.lscale(context.lUnit)
    val aScale = solid.ascale()
    return when (solid) {
        is GDMLBox -> box(solid.x * lScale, solid.y * lScale, solid.z * lScale, name)
        is GDMLTube -> tube(
            solid.rmax * lScale,
            solid.z * lScale,
            solid.rmin * lScale,
            solid.startphi * aScale,
            solid.deltaphi * aScale,
            name
        )
        is GDMLCone -> cone(solid.rmax1, solid.z, solid.rmax2, name = name) {
            require(solid.rmin1 == 0.0) { "Empty cones are not supported" }
            require(solid.rmin2 == 0.0) { "Empty cones are not supported" }
            startAngle = solid.startphi.toFloat()
            angle = solid.deltaphi.toFloat()
        }
        is GDMLXtru -> extrude(name) {
            shape {
                solid.vertices.forEach {
                    point(it.x * lScale, it.y * lScale)
                }
            }
            solid.sections.sortedBy { it.zOrder }.forEach { section ->
                layer(
                    section.zPosition * lScale,
                    section.xOffset * lScale,
                    section.yOffset * lScale,
                    section.scalingFactor
                )
            }
        }
        is GDMLScaledSolid -> {
            //Add solid with modified scale
            val innerSolid = solid.solidref.resolve(context.root)
                ?: error("Solid with tag ${solid.solidref.ref} for scaled solid ${solid.name} not defined")

            addSolid(context, innerSolid, name) {
                block()
                scaleX *= solid.scale.x.toFloat()
                scaleY *= solid.scale.y.toFloat()
                scaleZ = solid.scale.z.toFloat()
            }
        }
        is GDMLSphere -> sphere(solid.rmax * lScale, solid.deltaphi * aScale, solid.deltatheta * aScale, name) {
            phiStart = solid.startphi * aScale
            thetaStart = solid.starttheta * aScale
        }
        is GDMLOrb -> sphere(solid.r * lScale, name = name)
        is GDMLPolyhedra -> extrude(name) {
            //getting the radius of first
            require(solid.planes.size > 1) { "The polyhedron geometry requires at least two planes" }
            val baseRadius = solid.planes.first().rmax * lScale
            shape {
                (0..solid.numsides).forEach {
                    val phi = solid.deltaphi * aScale / solid.numsides * it + solid.startphi * aScale
                    (baseRadius * cos(phi) to baseRadius * sin(phi))
                }
            }
            solid.planes.forEach { plane ->
                //scaling all radii relative to first layer radius
                layer(plane.z * lScale, scale = plane.rmax * lScale / baseRadius)
            }
        }
        is GDMLBoolSolid -> {
            val first = solid.first.resolve(context.root) ?: error("")
            val second = solid.second.resolve(context.root) ?: error("")
            val type: CompositeType = when (solid) {
                is GDMLUnion -> CompositeType.UNION
                is GDMLSubtraction -> CompositeType.SUBTRACT
                is GDMLIntersection -> CompositeType.INTERSECT
            }

            return composite(type, name) {
                addSolid(context, first) {
                    withPosition(
                        context.lUnit,
                        solid.resolveFirstPosition(context.root),
                        solid.resolveFirstRotation(context.root),
                        null
                    )
                }
                addSolid(context, second) {
                    withPosition(
                        context.lUnit,
                        solid.resolvePosition(context.root),
                        solid.resolveRotation(context.root),
                        null
                    )
                }
            }
        }
        else -> error("Renderer for $solid not supported yet")
    }.apply(block)
}

private val volumesName = "volumes".asName()

private fun SolidGroup.addPhysicalVolume(
    context: GDMLTransformer,
    physVolume: GDMLPhysVolume
) {
    val volume: GDMLGroup = physVolume.volumeref.resolve(context.root)
        ?: error("Volume with ref ${physVolume.volumeref.ref} could not be resolved")

    when (context.volumeAction(volume)) {
        GDMLTransformer.Action.ACCEPT -> {
            val group = volume(context, volume)
            this[physVolume.name ?: ""] = group.apply {
                withPosition(
                    context.lUnit,
                    physVolume.resolvePosition(context.root),
                    physVolume.resolveRotation(context.root),
                    physVolume.resolveScale(context.root)
                )
            }
        }
        GDMLTransformer.Action.CACHE -> {
            val fullName = volumesName + volume.name.asName()
            if (context.proto[fullName] == null) {
                context.proto[fullName] = volume(context, volume)
            }

            this[physVolume.name ?: ""] = Proxy(this, fullName).apply {
                withPosition(
                    context.lUnit,
                    physVolume.resolvePosition(context.root),
                    physVolume.resolveRotation(context.root),
                    physVolume.resolveScale(context.root)
                )
            }
        }
        GDMLTransformer.Action.REJECT -> {
            //ignore
        }
    }
}

private fun SolidGroup.addDivisionVolume(
    context: GDMLTransformer,
    divisionVolume: GDMLDivisionVolume
) {
    val volume: GDMLGroup = divisionVolume.volumeref.resolve(context.root)
        ?: error("Volume with ref ${divisionVolume.volumeref.ref} could not be resolved")

    //TODO add divisions
    set(
        Name.EMPTY,
        volume(
            context,
            volume
        )
    )
}

private val solidsName = "solids".asName()

private fun volume(
    context: GDMLTransformer,
    group: GDMLGroup
): SolidGroup {
    return SolidGroup().apply {
        if (group is GDMLVolume) {
            val solid = group.solidref.resolve(context.root)
                ?: error("Solid with tag ${group.solidref.ref} for volume ${group.name} not defined")

            when (context.solidAction(solid)) {
                GDMLTransformer.Action.ACCEPT -> {
                    addSolid(context, solid, solid.name) {
                        context.configureSolid(this, group, solid)
                    }
                }
                GDMLTransformer.Action.CACHE -> {
                    if (context.proto[solid.name] == null) {
                        context.proto.addSolid(context, solid, solid.name) {
                            context.configureSolid(this, group, solid)
                        }
                    }
                    ref(solid.name.asName(), solid.name)
                }
                GDMLTransformer.Action.REJECT -> {
                    //ignore
                }
            }

            when (val vol = group.placement) {
                is GDMLPhysVolume -> addPhysicalVolume(context, vol)
                is GDMLDivisionVolume -> addDivisionVolume(context, vol)
            }
        }

        group.physVolumes.forEach { physVolume ->
            addPhysicalVolume(context, physVolume)
        }
    }
}

fun GDML.toVision(block: GDMLTransformer.() -> Unit = {}): SolidGroup {
    val context = GDMLTransformer(this).apply(block)
    return context.finalize(volume(context, world))
}

/**
 * Append gdml node to the group
 */
fun SolidGroup.gdml(gdml: GDML, key: String = "", transformer: GDMLTransformer.() -> Unit = {}) {
    val visual = gdml.toVision(transformer)
    //println(Visual3DPlugin.json.stringify(VisualGroup3D.serializer(), visual))
    set(key, visual)
}