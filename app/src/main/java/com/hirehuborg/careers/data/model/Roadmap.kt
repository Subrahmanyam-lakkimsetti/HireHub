package com.hirehuborg.careers.data.model

data class Roadmap(
    var id: String = "",
    var skillName: String = "",
    var roadmapContent: String = "",
    var steps: List<RoadmapStep> = emptyList(),
    var resources: List<Resource> = emptyList(),
    var estimatedDuration: String = "",
    var generatedAt: Long = System.currentTimeMillis()
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "", emptyList(), emptyList(), "", System.currentTimeMillis())
}

data class RoadmapStep(
    var stepNumber: Int = 0,
    var title: String = "",
    var description: String = "",
    var duration: String = "",
    var resources: List<String> = emptyList()
) {
    // No-argument constructor for Firebase
    constructor() : this(0, "", "", "", emptyList())
}

data class Resource(
    var title: String = "",
    var url: String = "",
    var type: String = ""
) {
    // No-argument constructor for Firebase
    constructor() : this("", "", "")
}