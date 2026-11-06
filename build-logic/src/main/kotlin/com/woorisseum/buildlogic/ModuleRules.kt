package com.woorisseum.buildlogic

internal data class ModuleSnapshot(
    val path: String,
    val dependencies: Set<String> = emptySet(),
    val libraries: Set<String> = emptySet(),
    val testLibraries: Set<String> = emptySet(),
    val plugins: Set<String> = emptySet(),
)

internal data class RuleResult(val violations: Set<String>, val structuralErrors: Set<String>)

internal object ModuleRules {
    private val features = setOf("auth", "household", "transaction", "budget", "shopping")
        .map { ":feature:$it" }.toSet()

    private val allowed = mapOf(
        ":androidApp" to setOf(":shared"),
        ":shared" to features + setOf(":core:domain", ":core:data", ":core:designsystem"),
        ":core:data" to setOf(":core:domain"),
        ":core:domain" to emptySet(),
        ":core:designsystem" to emptySet(),
    ) + features.associateWith { setOf(":core:domain", ":core:designsystem") }

    private val domainLibraries = setOf(
        "org.jetbrains.kotlinx:kotlinx-coroutines-core",
        "org.jetbrains.kotlinx:kotlinx-datetime",
        "org.jetbrains.kotlin:kotlin-stdlib",
        "org.jetbrains.kotlin:kotlin-stdlib-common",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk7",
        "org.jetbrains.kotlin:kotlin-stdlib-jdk8",
    )
    private val domainTestLibraries = domainLibraries + setOf(
        "org.jetbrains.kotlin:kotlin-test",
        "org.jetbrains.kotlin:kotlin-test-junit",
        "org.jetbrains.kotlin:kotlin-test-junit5",
        "org.jetbrains.kotlin:kotlin-test-annotations-common",
        "org.jetbrains.kotlin:kotlin-test-common",
        "org.jetbrains.kotlinx:kotlinx-coroutines-test",
    )
    val forbiddenDomainPlugins = setOf(
        "org.jetbrains.compose",
        "org.jetbrains.kotlin.plugin.compose",
        "com.android.application",
    )

    fun evaluate(modules: List<ModuleSnapshot>): RuleResult {
        val violations = sortedSetOf<String>()
        val errors = sortedSetOf<String>()
        val graph = modules.associate { it.path to it.dependencies }
        modules.forEach { module ->
            val targets = allowed[module.path]
            if (targets == null) errors += "unclassified: ${module.path}"
            module.dependencies.forEach { target ->
                if (target !in graph) errors += "unclassified: $target"
                if (targets != null && target !in targets) {
                    violations += "dependency: ${module.path} -> $target"
                }
            }
            if (module.path == ":core:domain") {
                (module.libraries - domainLibraries).forEach {
                    violations += "domain-library: ${module.path} -> $it"
                }
                (module.testLibraries - domainTestLibraries).forEach {
                    violations += "domain-test-library: ${module.path} -> $it"
                }
                (module.plugins intersect forbiddenDomainPlugins).forEach {
                    violations += "domain-plugin: ${module.path} -> $it"
                }
            }
        }
        val visited = mutableSetOf<String>()
        val stack = mutableListOf<String>()
        fun visit(path: String) {
            if (path in stack) {
                errors += "cycle: ${(stack.drop(stack.indexOf(path)) + path).joinToString(" -> ")}"
                return
            }
            if (!visited.add(path)) return
            stack += path
            graph[path].orEmpty().sorted().forEach(::visit)
            stack.removeAt(stack.lastIndex)
        }
        graph.keys.sorted().forEach(::visit)
        return RuleResult(violations, errors)
    }

    fun baselineEntries(text: String): Set<String> = text.lineSequence()
        .map(String::trim).filter { it.isNotEmpty() && !it.startsWith("#") }.toSortedSet()

    fun diff(baseline: Set<String>, current: Set<String>): List<String> =
        (current - baseline).sorted().map { "+ $it" } +
            (baseline - current).sorted().map { "- $it" }

    fun updateBaseline(text: String, violations: Set<String>): String {
        val comments = text.lineSequence().filter { it.trimStart().startsWith("#") }.toList()
        return (comments + violations.sorted()).joinToString("\n", postfix = "\n")
    }
}
