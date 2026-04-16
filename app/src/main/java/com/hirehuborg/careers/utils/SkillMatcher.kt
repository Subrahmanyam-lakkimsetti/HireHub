package com.hirehuborg.careers.utils

import com.hirehuborg.careers.R
import com.hirehuborg.careers.data.model.SkillCategory

/**
 * On-device skill detection engine.
 *
 * Strategy:
 *  - Normalize resume text to lowercase
 *  - Match against curated keyword maps per category
 *  - Use word-boundary aware matching to avoid false positives
 *    (e.g. "react" inside "reactivity" should NOT match "React")
 *  - Return grouped SkillCategory list for UI rendering
 *  - Return flat list for RTDB storage + Gemini prompt
 */
object SkillMatcher {

    // ── Skill taxonomy ───────────────────────────────────────────────────────
    // Each entry: display name → list of text patterns that match it

    private val PROGRAMMING_LANGUAGES = mapOf(
        "Kotlin"        to listOf("kotlin"),
        "Java"          to listOf("java"),
        "Python"        to listOf("python"),
        "JavaScript"    to listOf("javascript", "js"),
        "TypeScript"    to listOf("typescript", "ts"),
        "C++"           to listOf("c++", "cpp"),
        "C#"            to listOf("c#", "csharp", "c sharp"),
        "Swift"         to listOf("swift"),
        "Dart"          to listOf("dart"),
        "Go"            to listOf(" go ", "golang"),
        "Rust"          to listOf("rust"),
        "PHP"           to listOf("php"),
        "Ruby"          to listOf("ruby"),
        "Scala"         to listOf("scala"),
        "R"             to listOf(" r programming", "r language"),
        "MATLAB"        to listOf("matlab"),
        "Bash"          to listOf("bash", "shell scripting")
    )

    private val FRAMEWORKS_LIBRARIES = mapOf(
        "React"         to listOf("react", "reactjs", "react.js"),
        "Angular"       to listOf("angular", "angularjs"),
        "Vue.js"        to listOf("vue", "vuejs", "vue.js"),
        "Node.js"       to listOf("node.js", "nodejs", "node js"),
        "Express.js"    to listOf("express", "expressjs"),
        "Spring Boot"   to listOf("spring boot", "springboot"),
        "Spring"        to listOf("spring framework"),
        "Django"        to listOf("django"),
        "Flask"         to listOf("flask"),
        "FastAPI"       to listOf("fastapi"),
        "Flutter"       to listOf("flutter"),
        "React Native"  to listOf("react native"),
        "Next.js"       to listOf("next.js", "nextjs"),
        "Laravel"       to listOf("laravel"),
        "Ruby on Rails" to listOf("ruby on rails", "rails"),
        ".NET"          to listOf(".net", "dotnet", "asp.net"),
        "TensorFlow"    to listOf("tensorflow"),
        "PyTorch"       to listOf("pytorch"),
        "Keras"         to listOf("keras"),
        "Hibernate"     to listOf("hibernate"),
        "Redux"         to listOf("redux")
    )

    private val DATABASES = mapOf(
        "MySQL"         to listOf("mysql"),
        "PostgreSQL"    to listOf("postgresql", "postgres"),
        "MongoDB"       to listOf("mongodb", "mongo"),
        "Firebase"      to listOf("firebase"),
        "Redis"         to listOf("redis"),
        "SQLite"        to listOf("sqlite"),
        "Oracle DB"     to listOf("oracle"),
        "Cassandra"     to listOf("cassandra"),
        "DynamoDB"      to listOf("dynamodb"),
        "Elasticsearch" to listOf("elasticsearch"),
        "MS SQL Server" to listOf("sql server", "mssql")
    )

    private val CLOUD_DEVOPS = mapOf(
        "AWS"           to listOf("aws", "amazon web services"),
        "Azure"         to listOf("azure", "microsoft azure"),
        "GCP"           to listOf("gcp", "google cloud"),
        "Docker"        to listOf("docker"),
        "Kubernetes"    to listOf("kubernetes", "k8s"),
        "Terraform"     to listOf("terraform"),
        "Jenkins"       to listOf("jenkins"),
        "GitHub Actions" to listOf("github actions"),
        "CI/CD"         to listOf("ci/cd", "ci cd", "continuous integration"),
        "Linux"         to listOf("linux", "ubuntu", "centos"),
        "Ansible"       to listOf("ansible"),
        "Nginx"         to listOf("nginx"),
        "Apache"        to listOf("apache server", "apache http")
    )

    private val TOOLS_PRACTICES = mapOf(
        "Git"           to listOf("git"),
        "GitHub"        to listOf("github"),
        "REST API"      to listOf("rest api", "restful", "rest services"),
        "GraphQL"       to listOf("graphql"),
        "Microservices" to listOf("microservices", "micro services"),
        "Agile"         to listOf("agile", "scrum", "kanban"),
        "Unit Testing"  to listOf("unit test", "unit testing", "junit", "jest", "pytest"),
        "JIRA"          to listOf("jira"),
        "Postman"       to listOf("postman"),
        "Figma"         to listOf("figma"),
        "Android Dev"   to listOf("android development", "android sdk", "android studio"),
        "iOS Dev"       to listOf("ios development", "xcode"),
        "Machine Learning" to listOf("machine learning", "ml "),
        "Deep Learning" to listOf("deep learning"),
        "Data Science"  to listOf("data science", "data analysis"),
        "HTML/CSS"      to listOf("html", "css"),
        "Webpack"       to listOf("webpack"),
        "MVVM"          to listOf("mvvm"),
        "MVC"           to listOf("mvc pattern", "model view controller")
    )

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Main detection function.
     * @param resumeText Raw or cleaned extracted PDF text
     * @return Pair of:
     *   - List<SkillCategory> for UI (grouped, with colors)
     *   - List<String> flat skill names for RTDB + Gemini
     */
    fun detect(resumeText: String): Pair<List<SkillCategory>, List<String>> {
        val normalized = resumeText.lowercase()

        val languages  = matchCategory(normalized, PROGRAMMING_LANGUAGES)
        val frameworks = matchCategory(normalized, FRAMEWORKS_LIBRARIES)
        val databases  = matchCategory(normalized, DATABASES)
        val cloud      = matchCategory(normalized, CLOUD_DEVOPS)
        val tools      = matchCategory(normalized, TOOLS_PRACTICES)

        val categories = mutableListOf<SkillCategory>()

        if (languages.isNotEmpty()) {
            categories.add(SkillCategory("Programming Languages", "💻", languages, R.color.skill_language))
        }
        if (frameworks.isNotEmpty()) {
            categories.add(SkillCategory("Frameworks & Libraries", "📦", frameworks, R.color.skill_framework))
        }
        if (databases.isNotEmpty()) {
            categories.add(SkillCategory("Databases", "🗄️", databases, R.color.skill_database))
        }
        if (cloud.isNotEmpty()) {
            categories.add(SkillCategory("Cloud & DevOps", "☁️", cloud, R.color.skill_cloud))
        }
        if (tools.isNotEmpty()) {
            categories.add(SkillCategory("Tools & Practices", "🔧", tools, R.color.skill_tools))
        }

        val flatList = (languages + frameworks + databases + cloud + tools).distinct()

        return Pair(categories, flatList)
    }

    /**
     * Matches a single category map against normalized text.
     * Returns display names of matched skills.
     */
    private fun matchCategory(
        normalizedText: String,
        categoryMap: Map<String, List<String>>
    ): List<String> {
        val found = mutableListOf<String>()
        for ((displayName, patterns) in categoryMap) {
            for (pattern in patterns) {
                if (containsSkill(normalizedText, pattern)) {
                    found.add(displayName)
                    break // avoid adding same skill twice from multiple patterns
                }
            }
        }
        return found
    }

    /**
     * Smart pattern matching:
     * - Short patterns (≤ 2 chars) use exact word boundary check
     * - All others use simple contains, which is safe for multi-word skills
     */
    private fun containsSkill(text: String, pattern: String): Boolean {
        val regex = Regex("\\b${Regex.escape(pattern)}\\b", RegexOption.IGNORE_CASE)
        return regex.containsMatchIn(text)
    }
}