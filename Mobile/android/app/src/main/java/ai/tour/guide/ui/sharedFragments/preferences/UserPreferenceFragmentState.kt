package ai.tour.guide.ui.sharedFragments.preferences

data class UserPreferenceFragmentState(
    val selectedSingleOptions: Map<String, String> = emptyMap(),
    val selectedMultipleOptions: Map<String, Set<String>> = emptyMap()
)
