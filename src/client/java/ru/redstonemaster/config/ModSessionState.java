package ru.redstonemaster.config;

import ru.redstonemaster.client.gui.RedstoneMasterTab;

/**
 * Состояние вкладки и прокрутки мода только на время запущенного Minecraft (не пишется на диск).
 * Сбрасывается при перезапуске игры.
 */
public final class ModSessionState {
	private static final ModSessionState INSTANCE = new ModSessionState();

	private boolean saved;
	private String lastTab = RedstoneMasterTab.MAIN_MENU.name();
	private int settingsScrollOffset;
	private int tutorialScrollOffset;
	private int tutorialStudyScrollOffset;
	private String tutorialStudyTarget = "";
	private String expandedTutorialSections = "";

	private ModSessionState() {
	}

	public static ModSessionState get() {
		return INSTANCE;
	}

	public boolean hasSaved() {
		return this.saved;
	}

	public String getLastTab() {
		return this.lastTab;
	}

	public int getSettingsScrollOffset() {
		return this.settingsScrollOffset;
	}

	public int getTutorialScrollOffset() {
		return this.tutorialScrollOffset;
	}

	public int getTutorialStudyScrollOffset() {
		return this.tutorialStudyScrollOffset;
	}

	public String getTutorialStudyTarget() {
		return this.tutorialStudyTarget;
	}

	public String getExpandedTutorialSections() {
		return this.expandedTutorialSections;
	}

	public void clear() {
		this.saved = false;
		this.lastTab = RedstoneMasterTab.MAIN_MENU.name();
		this.settingsScrollOffset = 0;
		this.tutorialScrollOffset = 0;
		this.tutorialStudyScrollOffset = 0;
		this.tutorialStudyTarget = "";
		this.expandedTutorialSections = "";
	}

	public void save(
			String lastTab,
			int settingsScrollOffset,
			int tutorialListScrollOffset,
			String expandedTutorialSections,
			String tutorialStudyTarget,
			int tutorialStudyScrollOffset
	) {
		this.saved = true;
		this.lastTab = lastTab;
		this.settingsScrollOffset = Math.max(0, settingsScrollOffset);
		this.tutorialScrollOffset = Math.max(0, tutorialListScrollOffset);
		this.expandedTutorialSections = expandedTutorialSections != null ? expandedTutorialSections : "";
		this.tutorialStudyTarget = tutorialStudyTarget != null ? tutorialStudyTarget : "";
		this.tutorialStudyScrollOffset = Math.max(0, tutorialStudyScrollOffset);
	}
}
