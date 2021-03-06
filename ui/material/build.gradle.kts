plugins {
    id("scientifik.js")
}

val dataforgeVersion: String by rootProject.extra

kotlin {
    target {
        useCommonJs()
    }
}

dependencies{
    api(project(":ui:react"))

    api("subroh0508.net.kotlinmaterialui:core:0.4.0")
    api("subroh0508.net.kotlinmaterialui:lab:0.4.0")
    api(npm("@material-ui/core","4.9.14"))
    api(npm("@material-ui/lab","4.0.0-alpha.51"))
    //api(npm("@material-ui/icons","4.9.1"))
}