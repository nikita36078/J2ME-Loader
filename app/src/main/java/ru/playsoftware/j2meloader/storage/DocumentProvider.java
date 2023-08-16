/*
 * Copyright 2023 Nikita Shakarun
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package ru.playsoftware.j2meloader.storage;

import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.graphics.Point;
import android.os.Build;
import android.os.CancellationSignal;
import android.os.ParcelFileDescriptor;
import android.provider.DocumentsContract.Document;
import android.provider.DocumentsContract.Root;
import android.provider.DocumentsProvider;
import android.webkit.MimeTypeMap;

import androidx.annotation.RequiresApi;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import ru.playsoftware.j2meloader.R;
import ru.playsoftware.j2meloader.config.Config;
import ru.playsoftware.j2meloader.util.FileUtils;

@RequiresApi(api = Build.VERSION_CODES.KITKAT)
public class DocumentProvider extends DocumentsProvider {
	private static final String ALL_MIME_TYPES = "*/*";

	private static final String[] DEFAULT_ROOT_PROJECTION = new String[]{
			Root.COLUMN_ROOT_ID,
			Root.COLUMN_MIME_TYPES,
			Root.COLUMN_FLAGS,
			Root.COLUMN_ICON,
			Root.COLUMN_TITLE,
			Root.COLUMN_SUMMARY,
			Root.COLUMN_DOCUMENT_ID,
			Root.COLUMN_AVAILABLE_BYTES
	};
	private static final String[] DEFAULT_DOCUMENT_PROJECTION = new String[]{
			Document.COLUMN_DOCUMENT_ID,
			Document.COLUMN_MIME_TYPE,
			Document.COLUMN_DISPLAY_NAME,
			Document.COLUMN_LAST_MODIFIED,
			Document.COLUMN_FLAGS,
			Document.COLUMN_SIZE
	};

	private File baseDir;

	@Override
	public boolean onCreate() {
		baseDir = new File(Config.getEmulatorDir());
		return true;
	}

	@Override
	public Cursor queryRoots(String[] projection) throws FileNotFoundException {
		final MatrixCursor result = new MatrixCursor(resolveRootProjection(projection));
		final MatrixCursor.RowBuilder row = result.newRow();

		row.add(Root.COLUMN_ROOT_ID, getDocIdForFile(baseDir));
		row.add(Root.COLUMN_SUMMARY, null);

		if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
			row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE);
		} else {
			row.add(Root.COLUMN_FLAGS, Root.FLAG_SUPPORTS_CREATE |
					Root.FLAG_SUPPORTS_IS_CHILD);
		}

		row.add(Root.COLUMN_TITLE, getContext().getString(R.string.app_name));
		row.add(Root.COLUMN_DOCUMENT_ID, getDocIdForFile(baseDir));
		row.add(Root.COLUMN_MIME_TYPES, ALL_MIME_TYPES);
		row.add(Root.COLUMN_AVAILABLE_BYTES, baseDir.getFreeSpace());
		row.add(Root.COLUMN_ICON, R.mipmap.ic_launcher);
		return result;
	}

	@Override
	public Cursor queryDocument(String documentId, String[] projection)
			throws FileNotFoundException {
		final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
		includeFile(result, documentId, null);
		return result;
	}

	@Override
	public Cursor queryChildDocuments(String parentDocumentId, String[] projection,
									  String sortOrder) throws FileNotFoundException {
		final MatrixCursor result = new MatrixCursor(resolveDocumentProjection(projection));
		final File parent = getFileForDocId(parentDocumentId);
		for (File file : parent.listFiles()) {
			includeFile(result, null, file);
		}
		return result;
	}

	@Override
	public ParcelFileDescriptor openDocument(final String documentId, final String mode,
											 CancellationSignal signal) throws FileNotFoundException {
		final File file = getFileForDocId(documentId);
		final int accessMode = ParcelFileDescriptor.parseMode(mode);
		return ParcelFileDescriptor.open(file, accessMode);
	}

	@Override
	public AssetFileDescriptor openDocumentThumbnail(String documentId, Point sizeHint,
													 CancellationSignal signal) throws FileNotFoundException {
		final File file = getFileForDocId(documentId);
		final ParcelFileDescriptor pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY);
		return new AssetFileDescriptor(pfd, 0, file.length());
	}

	private boolean isChildFile(File parentFile, File childFile) {
		File realFileParent = childFile.getParentFile();
		return realFileParent == null || realFileParent.equals(parentFile);
	}

	@Override
	public boolean isChildDocument(String parentDocumentId, String documentId) {
		try {
			File parentFile = getFileForDocId(parentDocumentId);
			File childFile = getFileForDocId(documentId);
			return isChildFile(parentFile, childFile);
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		}
		return false;
	}

	@Override
	public String createDocument(String documentId, String mimeType, String displayName)
			throws FileNotFoundException {
		File parent = getFileForDocId(documentId);
		File file = new File(parent.getPath(), displayName);
		try {
			boolean wasNewFileCreated;
			if (mimeType.equals(Document.MIME_TYPE_DIR)) {
				wasNewFileCreated = file.mkdir();
			} else {
				wasNewFileCreated = file.createNewFile();
			}

			if (!wasNewFileCreated) {
				throw new FileNotFoundException("Failed to create document with name " +
						displayName + " and documentId " + documentId);
			}
		} catch (IOException e) {
			throw new FileNotFoundException("Failed to create document with name " +
					displayName + " and documentId " + documentId);
		}
		return getDocIdForFile(file);
	}

	@Override
	public String renameDocument(String documentId, String displayName)
			throws FileNotFoundException {
		if (displayName == null) {
			throw new FileNotFoundException("Failed to rename document, new name is null");
		}

		File sourceFile = getFileForDocId(documentId);
		File sourceParentFile = sourceFile.getParentFile();
		if (sourceParentFile == null) {
			throw new FileNotFoundException("Failed to rename document. File has no parent.");
		}
		File destFile = new File(sourceParentFile.getPath(), displayName);

		try {
			boolean renameSucceeded = sourceFile.renameTo(destFile);
			if (!renameSucceeded) {
				throw new FileNotFoundException("Failed to rename document. Renamed failed.");
			}
		} catch (Exception e) {
			throw new FileNotFoundException("Failed to rename document. Error: " + e.getMessage());
		}

		return getDocIdForFile(destFile);
	}

	@Override
	public void deleteDocument(String documentId) throws FileNotFoundException {
		File file = getFileForDocId(documentId);
		boolean deleteSucceeded;
		if (file.isDirectory()) {
			deleteSucceeded = FileUtils.deleteDirectory(file);
		} else {
			deleteSucceeded = file.delete();
		}
		if (!deleteSucceeded) {
			throw new FileNotFoundException("Failed to delete document with id " + documentId);
		}
	}

	private String copyDocument(String sourceDocumentId, String sourceParentDocumentId,
							   String targetParentDocumentId) throws FileNotFoundException {
		if (!isChildDocument(sourceParentDocumentId, sourceDocumentId)) {
			throw new FileNotFoundException("Failed to copy document with id " +
					sourceDocumentId + ". Parent is not: " + sourceParentDocumentId);
		}
		return copyDocument(sourceDocumentId, targetParentDocumentId);
	}

	@Override
	public String copyDocument(String sourceDocumentId, String targetParentDocumentId)
			throws FileNotFoundException {
		File parent = getFileForDocId(targetParentDocumentId);
		File oldFile = getFileForDocId(sourceDocumentId);
		File newFile = new File(parent.getPath(), oldFile.getName());
		try {
			FileUtils.copyFileUsingChannel(oldFile, newFile);
		} catch (IOException e) {
			throw new FileNotFoundException("Failed to copy document: " + sourceDocumentId +
					". " + e.getMessage());
		}
		return getDocIdForFile(newFile);
	}

	@RequiresApi(api = Build.VERSION_CODES.N)
	@Override
	public String moveDocument(String sourceDocumentId, String sourceParentDocumentId,
							   String targetParentDocumentId) throws FileNotFoundException {
		try {
			String newDocumentId = copyDocument(sourceDocumentId, sourceParentDocumentId,
					targetParentDocumentId);

			removeDocument(sourceDocumentId, sourceParentDocumentId);
			return newDocumentId;
		} catch (FileNotFoundException e) {
			throw new FileNotFoundException("Failed to move document " + sourceDocumentId);
		}
	}

	@Override
	public String getDocumentType(String documentId) throws FileNotFoundException {
		File file = getFileForDocId(documentId);
		return getTypeForFile(file);
	}

	private static String[] resolveRootProjection(String[] projection) {
		return projection != null ? projection : DEFAULT_ROOT_PROJECTION;
	}

	private static String[] resolveDocumentProjection(String[] projection) {
		return projection != null ? projection : DEFAULT_DOCUMENT_PROJECTION;
	}

	private static String getTypeForFile(File file) {
		if (file.isDirectory()) {
			return Document.MIME_TYPE_DIR;
		} else {
			return getTypeForName(file.getName());
		}
	}

	private static String getTypeForName(String name) {
		final int lastDot = name.lastIndexOf('.');
		if (lastDot >= 0) {
			final String extension = name.substring(lastDot + 1);
			final String mime = MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension);
			if (mime != null) {
				return mime;
			}
		}
		return "application/octet-stream";
	}

	private void includeFile(MatrixCursor result, String docId, File file)
			throws FileNotFoundException {
		if (docId == null) {
			docId = getDocIdForFile(file);
		} else {
			file = getFileForDocId(docId);
		}

		int flags = 0;
		if (file.canWrite()) {
			if (file.canWrite()) {
				flags |= Document.FLAG_DIR_SUPPORTS_CREATE;
			} else {
				flags |= Document.FLAG_SUPPORTS_WRITE;
			}
			flags |= Document.FLAG_SUPPORTS_DELETE;

			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
				flags |= Document.FLAG_SUPPORTS_RENAME;
			}
			if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
				flags |= Document.FLAG_SUPPORTS_MOVE;
				flags |= Document.FLAG_SUPPORTS_COPY;
			}
		}

		final String displayName = file.getName();
		final String mimeType = getTypeForFile(file);

		if (mimeType.startsWith("image/")) {
			flags |= Document.FLAG_SUPPORTS_THUMBNAIL;
		}

		final MatrixCursor.RowBuilder row = result.newRow();
		row.add(Document.COLUMN_DOCUMENT_ID, docId);
		row.add(Document.COLUMN_DISPLAY_NAME, displayName);
		row.add(Document.COLUMN_SIZE, file.length());
		row.add(Document.COLUMN_MIME_TYPE, mimeType);
		row.add(Document.COLUMN_LAST_MODIFIED, file.lastModified());
		row.add(Document.COLUMN_FLAGS, flags);
		row.add(Document.COLUMN_ICON, R.mipmap.ic_launcher);
	}

	private String getDocIdForFile(File file) {
		return file.getAbsolutePath();
	}

	private File getFileForDocId(String docId) throws FileNotFoundException {
		final File file = new File(docId);
		if (!file.exists()) {
			throw new FileNotFoundException("Missing file for " + docId);
		}
		return file;
	}
}
