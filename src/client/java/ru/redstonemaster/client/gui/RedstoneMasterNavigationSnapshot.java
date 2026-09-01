package ru.redstonemaster.client.gui;

import org.jetbrains.annotations.Nullable;
import ru.redstonemaster.client.gui.tutorial.TutorialStudyTarget;

/** Снимок состояния GUI для истории «назад / вперёд» (как в браузере). */
record RedstoneMasterNavigationSnapshot(
		RedstoneMasterTab tab,
		@Nullable TutorialStudyTarget studyTarget,
		int tutorialScrollOffset,
		int tutorialSavedListScrollOffset,
		int settingsScrollOffset,
		String expandedTutorialSections
) {
}
