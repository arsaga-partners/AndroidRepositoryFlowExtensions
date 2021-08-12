plugins {
    id("com.android.library")
    kotlin("android")
    kotlin("kapt")
}
dependencies {
    api(project(mapOf("path" to ":extension:repository")))
}
