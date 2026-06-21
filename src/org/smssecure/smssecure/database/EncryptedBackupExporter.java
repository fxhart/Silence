/**
 * Copyright (C) 2011 Whisper Systems
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */
package org.smssecure.smssecure.database;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.channels.FileChannel;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

public class EncryptedBackupExporter {

  private static final String TAG      = EncryptedBackupExporter.class.getSimpleName();
  private static final String ZIP_NAME = "SilenceEncryptedBackup.zip";

  public static void exportToStorage(Context context) throws NoExternalStorageException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      exportToDownloads(context);
    } else {
      verifyExternalStorageForExport(context);
      exportDirectory(context, "");
    }
  }

  public static void importFromStorage(Context context) throws NoExternalStorageException, IOException {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      importFromDownloads(context);
    } else {
      verifyExternalStorageForImport(context);
      importDirectory(context, "");
    }
  }

  // ── Android 10+ MediaStore paths ────────────────────────────────────────────

  private static void exportToDownloads(Context context) throws NoExternalStorageException, IOException {
    // Remove any previous backup with the same name
    context.getContentResolver().delete(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        MediaStore.Downloads.DISPLAY_NAME + "=?",
        new String[]{ZIP_NAME});

    ContentValues values = new ContentValues();
    values.put(MediaStore.Downloads.DISPLAY_NAME, ZIP_NAME);
    values.put(MediaStore.Downloads.MIME_TYPE, "application/zip");
    Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
    if (uri == null) throw new NoExternalStorageException();

    try (OutputStream out = context.getContentResolver().openOutputStream(uri)) {
      if (out == null) throw new IOException("Could not open output stream for " + uri);
      try (ZipOutputStream zos = new ZipOutputStream(out)) {
        zipDirectory(context, zos, new File(context.getFilesDir().getParent()), "");
      }
    }
  }

  private static void zipDirectory(Context context, ZipOutputStream zos, File baseDir, String entryPath)
      throws IOException
  {
    File dir = entryPath.isEmpty() ? baseDir : new File(baseDir, entryPath);
    if (!dir.exists() || !dir.isDirectory()) return;
    if (entryPath.equals("/lib")) return;

    File[] files = dir.listFiles();
    if (files == null) return;

    for (File file : files) {
      String childPath = entryPath.isEmpty() ? file.getName() : entryPath + "/" + file.getName();
      if (file.isFile() && !file.getAbsolutePath().contains("libcurve25519.so")) {
        zos.putNextEntry(new ZipEntry(childPath));
        try (InputStream in = new FileInputStream(file)) {
          byte[] buf = new byte[8192];
          int len;
          while ((len = in.read(buf)) > 0) zos.write(buf, 0, len);
        }
        zos.closeEntry();
      } else if (file.isDirectory()) {
        zipDirectory(context, zos, baseDir, childPath);
      }
    }
  }

  private static void importFromDownloads(Context context) throws NoExternalStorageException, IOException {
    Uri[] found = new Uri[1];
    try (android.database.Cursor cursor = context.getContentResolver().query(
        MediaStore.Downloads.EXTERNAL_CONTENT_URI,
        new String[]{MediaStore.Downloads._ID},
        MediaStore.Downloads.DISPLAY_NAME + "=?",
        new String[]{ZIP_NAME}, null))
    {
      if (cursor == null || !cursor.moveToFirst()) {
        Log.w(TAG, "No encrypted backup found in Downloads");
        throw new NoExternalStorageException();
      }
      long id = cursor.getLong(0);
      found[0] = android.content.ContentUris.withAppendedId(MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
    }

    try (InputStream in = context.getContentResolver().openInputStream(found[0]);
         ZipInputStream zis = new ZipInputStream(in))
    {
      File baseDir = new File(context.getFilesDir().getParent());
      ZipEntry entry;
      while ((entry = zis.getNextEntry()) != null) {
        File outFile = new File(baseDir, entry.getName());
        outFile.getParentFile().mkdirs();
        try (OutputStream out = new FileOutputStream(outFile)) {
          byte[] buf = new byte[8192];
          int len;
          while ((len = zis.read(buf)) > 0) out.write(buf, 0, len);
        }
        zis.closeEntry();
      }
    }
  }

  // ── Legacy pre-Android-10 file paths ────────────────────────────────────────

  private static String getExportDirectoryPath(Context context) {
    File dir = context.getExternalFilesDir(null);
    return dir.getAbsolutePath() + File.separator + "SilenceExport";
  }

  private static void verifyExternalStorageForExport(Context context) throws NoExternalStorageException {
    File dir = context.getExternalFilesDir(null);
    if (dir == null || !dir.canWrite())
      throw new NoExternalStorageException();

    File exportDirectory = new File(getExportDirectoryPath(context));
    if (!exportDirectory.exists())
      exportDirectory.mkdir();
  }

  private static void verifyExternalStorageForImport(Context context) throws NoExternalStorageException {
    File dir = context.getExternalFilesDir(null);
    if (dir == null || !dir.canRead()) {
      Log.w(TAG, "Cannot get external storage directory!");
      throw new NoExternalStorageException();
    }
    if (!new File(getExportDirectoryPath(context)).exists()) {
      Log.w(TAG, "Cannot get export directory path \"" + getExportDirectoryPath(context) + "\"!");
      throw new NoExternalStorageException();
    }
  }

  private static void migrateFile(File from, File to) {
    try {
      if (from.exists()) {
        FileChannel source      = new FileInputStream(from).getChannel();
        FileChannel destination = new FileOutputStream(to).getChannel();
        destination.transferFrom(source, 0, source.size());
        source.close();
        destination.close();
      }
    } catch (IOException ioe) {
      Log.w(TAG, ioe);
    }
  }

  private static void exportDirectory(Context context, String directoryName) throws IOException {
    if (!directoryName.equals("/lib")) {
      File directory       = new File(context.getFilesDir().getParent() + File.separatorChar + directoryName);
      File exportDirectory = new File(getExportDirectoryPath(context) + File.separatorChar + directoryName);

      if (directory.exists() && directory.isDirectory()) {
        exportDirectory.mkdirs();

        File[] contents = directory.listFiles();
        if (contents == null)
          throw new IOException("directory.listFiles() is null for " + directory.getAbsolutePath());

        for (File localFile : contents) {
          if (localFile.isFile() && !localFile.getAbsolutePath().contains("libcurve25519.so")) {
            File exportedFile = new File(exportDirectory.getAbsolutePath() + File.separator + localFile.getName());
            migrateFile(localFile, exportedFile);
          } else {
            exportDirectory(context, directoryName + File.separator + localFile.getName());
          }
        }
      } else {
        Log.w(TAG, "Could not find directory: " + directory.getAbsolutePath());
      }
    }
  }

  private static void importDirectory(Context context, String directoryName) throws IOException {
    File directory       = new File(getExportDirectoryPath(context) + File.separator + directoryName);
    File importDirectory = new File(context.getFilesDir().getParent() + File.separator + directoryName);

    if (directory.exists() && directory.isDirectory()) {
      importDirectory.mkdirs();

      File[] contents = directory.listFiles();
      for (File exportedFile : contents) {
        if (exportedFile.isFile()) {
          File localFile = new File(importDirectory.getAbsolutePath() + File.separator + exportedFile.getName());
          migrateFile(exportedFile, localFile);
        } else if (exportedFile.isDirectory()) {
          importDirectory(context, directoryName + File.separator + exportedFile.getName());
        }
      }
    }
  }
}
