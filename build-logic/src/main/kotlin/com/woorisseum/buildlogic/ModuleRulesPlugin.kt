package com.woorisseum.buildlogic

import org.gradle.api.DefaultTask
import org.gradle.api.GradleException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.ExternalModuleDependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import org.gradle.work.DisableCachingByDefault

class ModuleRulesPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        require(project == project.rootProject) { "Apply woorisseum.module.rules only to the root project." }
        // Root check intentionally aggregates architecture checks only; platform builds remain explicit.
        project.pluginManager.apply("base")
        val reports = project.objects.fileCollection()
        project.subprojects.forEach { module ->
            val report = module.tasks.register("moduleRulesSnapshot", ModuleRulesSnapshotTask::class.java) {
                group = "verification"
                description = "Collects declared module dependencies without resolving libraries."
                lines.convention(emptyList())
                outputFile.set(module.layout.buildDirectory.file("module-rules/snapshot.txt"))
            }
            reports.from(report.flatMap { it.outputFile })
            module.afterEvaluate {
                // Namespace-only projects such as :core and :feature have no build script.
                if (module.buildFile.isFile) {
                    val snapshot = sortedSetOf("module|${module.path}")
                    module.configurations.forEach { configuration ->
                        configuration.dependencies.withType(ProjectDependency::class.java).forEach {
                            snapshot += "dependency|${it.path}"
                        }
                        val name = configuration.name.lowercase()
                        val isSourceDependency = listOf("api", "implementation", "compileonly", "runtimeonly")
                            .any(name::endsWith)
                        if (module.path == ":core:domain" && isSourceDependency) {
                            val kind = if ("test" in name) "test-library" else "library"
                            configuration.dependencies.withType(ExternalModuleDependency::class.java).forEach {
                                snapshot += "$kind|${it.group}:${it.name}"
                            }
                        }
                    }
                    ModuleRules.forbiddenDomainPlugins.filter(module.pluginManager::hasPlugin).forEach {
                        snapshot += "plugin|$it"
                    }
                    report.configure { lines.set(snapshot.toList()) }
                }
            }
        }
        val baseline = project.layout.projectDirectory.file("module-rules.txt")
        val checkRules = project.tasks.register("moduleRules", ModuleRulesTask::class.java) {
            group = "verification"
            description = "Checks architecture violations against module-rules.txt."
            snapshotFiles.from(reports)
            baselineFile.set(baseline)
            baselineInputs.from(baseline)
            update.set(false)
        }
        project.tasks.register("moduleRulesBaseline", ModuleRulesTask::class.java) {
            group = "verification"
            description = "Explicitly records current architecture violations in module-rules.txt."
            snapshotFiles.from(reports)
            baselineFile.set(baseline)
            baselineInputs.from(baseline)
            update.set(true)
            mustRunAfter(checkRules)
        }
        project.tasks.named("check") { dependsOn(checkRules) }
    }
}

@DisableCachingByDefault(because = "Small local declaration report")
abstract class ModuleRulesSnapshotTask : DefaultTask() {
    @get:Input abstract val lines: ListProperty<String>
    @get:OutputFile abstract val outputFile: RegularFileProperty

    @TaskAction
    fun collect() {
        val file = outputFile.get().asFile
        file.parentFile.mkdirs()
        file.writeText(lines.get().joinToString("\n", postfix = "\n"))
    }
}

@DisableCachingByDefault(because = "Checks or explicitly updates a version-controlled baseline")
abstract class ModuleRulesTask : DefaultTask() {
    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val snapshotFiles: ConfigurableFileCollection

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.NONE)
    abstract val baselineInputs: ConfigurableFileCollection

    @get:Internal abstract val baselineFile: RegularFileProperty
    @get:Input abstract val update: Property<Boolean>

    @TaskAction
    fun inspect() {
        val modules = snapshotFiles.files.mapNotNull { file ->
            val records = file.readLines().filter(String::isNotBlank).map { it.split('|', limit = 2) }
            val path = records.singleOrNull { it[0] == "module" }?.get(1) ?: return@mapNotNull null
            fun values(kind: String) = records.filter { it[0] == kind }.map { it[1] }.toSet()
            ModuleSnapshot(path, values("dependency"), values("library"), values("test-library"), values("plugin"))
        }
        val result = ModuleRules.evaluate(modules)
        if (result.structuralErrors.isNotEmpty()) {
            throw GradleException("Module structure errors (cannot be baselined):\n" + result.structuralErrors.joinToString("\n"))
        }
        val file = baselineFile.get().asFile
        if (!update.get() && !file.isFile) {
            throw GradleException("Missing module-rules.txt. Create it with ./gradlew moduleRulesBaseline")
        }
        val previous = if (file.isFile) file.readText() else "# Woorisseum module rules baseline\n"
        if (update.get()) {
            file.writeText(ModuleRules.updateBaseline(previous, result.violations))
            logger.lifecycle("Updated module-rules.txt: ${result.violations.size} accepted violation(s). Review the diff before committing.")
        } else {
            val differences = ModuleRules.diff(ModuleRules.baselineEntries(previous), result.violations)
            if (differences.isNotEmpty()) {
                throw GradleException(
                    "Module rules changed in module-rules.txt:\n${differences.joinToString("\n")}\n" +
                        "Fix the violations, or after review run ./gradlew moduleRulesBaseline",
                )
            }
            logger.lifecycle("Module rules match baseline: ${result.violations.size} accepted violation(s).")
        }
    }
}
