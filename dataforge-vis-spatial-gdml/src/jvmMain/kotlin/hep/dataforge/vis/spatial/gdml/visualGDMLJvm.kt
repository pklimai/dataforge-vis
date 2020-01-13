package hep.dataforge.vis.spatial.gdml

import hep.dataforge.vis.spatial.VisualGroup3D
import nl.adaptivity.xmlutil.StAXReader
import scientifik.gdml.GDML
import java.nio.file.Files
import java.nio.file.Path

fun GDML.Companion.readFile(file: Path): GDML {
    val xmlReader = StAXReader(Files.newInputStream(file), "UTF-8")
    return GDML.format.parse(GDML.serializer(), xmlReader)
}

fun VisualGroup3D.gdml(file: Path, key: String = "", transformer: GDMLTransformer.() -> Unit = {}) {
    val gdml = GDML.readFile(file)
    gdml(gdml, key, transformer)
}

