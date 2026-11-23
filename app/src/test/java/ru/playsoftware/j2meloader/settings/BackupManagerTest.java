package ru.playsoftware.j2meloader.settings;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class BackupManagerTest {

	@Test
	public void safeEntryName_acceptsNormalPaths() {
		assertTrue(BackupManager.isSafeEntryName("converted/Game_1a2b/converted.dex"));
		assertTrue(BackupManager.isSafeEntryName("data/Tam Quốc Phân Tranh_d9c7b275/x.rsh"));
		assertTrue(BackupManager.isSafeEntryName("file.txt"));
	}

	@Test
	public void safeEntryName_rejectsTraversalAndAbsolute() {
		assertFalse(BackupManager.isSafeEntryName(""));
		assertFalse(BackupManager.isSafeEntryName("/etc/passwd"));
		assertFalse(BackupManager.isSafeEntryName("../evil"));
		assertFalse(BackupManager.isSafeEntryName("a/../../evil"));
		assertFalse(BackupManager.isSafeEntryName("a//b"));
		assertFalse(BackupManager.isSafeEntryName("a/./b"));
		assertFalse(BackupManager.isSafeEntryName(".."));
	}

	@Test
	public void normalizeEntryName_convertsSeparatorsAndStripsDotSlash() {
		assertEquals("a/b/c", BackupManager.normalizeEntryName("a\\b\\c"));
		assertEquals("a/b", BackupManager.normalizeEntryName("./a/b"));
		assertEquals("a", BackupManager.normalizeEntryName("././a"));
	}

	@Test
	public void isSkipped_skipsDatabaseAndTempOnlyAtRoot() {
		assertTrue(BackupManager.isSkipped("J2ME-apps.db"));
		assertTrue(BackupManager.isSkipped("J2ME-apps.db-wal"));
		assertTrue(BackupManager.isSkipped(".nomedia"));
		assertTrue(BackupManager.isSkipped("converted/.tmp"));
		assertTrue(BackupManager.isSkipped("converted/.tmp/x.dex"));
		// same names inside an app folder must be kept
		assertFalse(BackupManager.isSkipped("data/Game/J2ME-apps.db"));
		assertFalse(BackupManager.isSkipped("converted/Game/converted.dex"));
	}

	@Test
	public void isLegacySkipped_skipsLogAndVersion() {
		assertTrue(BackupManager.isLegacySkipped("log.txt"));
		assertTrue(BackupManager.isLegacySkipped("DATA_VERSION"));
		assertFalse(BackupManager.isLegacySkipped("configs"));
	}
}
