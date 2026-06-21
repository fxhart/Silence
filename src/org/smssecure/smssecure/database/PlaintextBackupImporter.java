package org.smssecure.smssecure.database;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;

import org.smssecure.smssecure.crypto.MasterCipher;
import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.recipients.RecipientFactory;
import org.smssecure.smssecure.recipients.RecipientFormattingException;
import org.smssecure.smssecure.recipients.Recipients;
import org.xmlpull.v1.XmlPullParserException;

import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;


public class PlaintextBackupImporter {

  private static final String TAG = PlaintextBackupImporter.class.getSimpleName();
  private static String backupPath;

  private static final String[] BACKUP_FILENAMES = {
      "SilencePlaintextBackup.xml", "TextSecurePlaintextBackup.xml",
      "SMSSecurePlaintextBackup.xml", "SignalPlaintextBackup.xml"
  };

  public static void importPlaintextFromSd(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    Log.w("PlaintextBackupImporter", "Importing plaintext...");
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      importPlaintextFromDownloads(context, masterSecret);
    } else {
      backupPath = getPlaintextExportDirectoryPathLegacy();
      verifyExternalStorageForPlaintextImport();
      importPlaintext(context, masterSecret, backupPath);
    }
  }

  private static void importPlaintextFromDownloads(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    for (String filename : BACKUP_FILENAMES) {
      try (Cursor cursor = context.getContentResolver().query(
          MediaStore.Downloads.EXTERNAL_CONTENT_URI,
          new String[]{MediaStore.Downloads._ID},
          MediaStore.Downloads.DISPLAY_NAME + "=?",
          new String[]{filename}, null))
      {
        if (cursor != null && cursor.moveToFirst()) {
          long id = cursor.getLong(0);
          Uri uri = android.content.ContentUris.withAppendedId(
              MediaStore.Downloads.EXTERNAL_CONTENT_URI, id);
          Log.i(TAG, "Importing backup from Downloads: " + filename);
          importPlaintextFromUri(context, masterSecret, uri);
          return;
        }
      }
    }
    throw new NoExternalStorageException();
  }

  private static void verifyExternalStorageForPlaintextImport() throws NoExternalStorageException {
    if (!Environment.getExternalStorageDirectory().canRead())
      throw new NoExternalStorageException();
  }

  private static String getPlaintextExportDirectoryPathLegacy() throws NoExternalStorageException {
    File sdDirectory = Environment.getExternalStorageDirectory();

    for (String s : BACKUP_FILENAMES) {
      String path = sdDirectory.getAbsolutePath() + File.separator + s;
      if (new File(path).exists()) {
        Log.i(TAG, "Importing backup from file '" + path + "'");
        return path;
      }
    }

    throw new NoExternalStorageException();
  }

  private static void importPlaintextFromUri(Context context, MasterSecret masterSecret, Uri uri)
      throws IOException
  {
    Log.w("PlaintextBackupImporter", "importPlaintextFromUri()");
    SmsDatabase    db          = DatabaseFactory.getSmsDatabase(context);
    SQLiteDatabase transaction = db.beginTransaction();

    try {
      ThreadDatabase threads         = DatabaseFactory.getThreadDatabase(context);
      XmlBackup      backup          = new XmlBackup(context.getContentResolver().openInputStream(uri));
      MasterCipher   masterCipher    = new MasterCipher(masterSecret);
      java.util.Set<Long> modifiedThreads = new java.util.HashSet<>();
      XmlBackup.XmlBackupItem item;

      while ((item = backup.getNext()) != null) {
        if (item.getAddress() == null || item.getAddress().equals("null")) continue;
        if (!isAppropriateTypeForImport(item.getType())) continue;

        Recipients      recipients = RecipientFactory.getRecipientsFromString(context, item.getAddress(), false);
        long            threadId   = threads.getThreadIdFor(recipients);
        SQLiteStatement statement  = db.createInsertStatement(transaction);

        addStringToStatement(statement, 1, item.getAddress());
        addNullToStatement(statement, 2);
        addLongToStatement(statement, 3, item.getDate());
        addLongToStatement(statement, 4, item.getDate());
        addLongToStatement(statement, 5, item.getProtocol());
        addLongToStatement(statement, 6, item.getRead());
        addLongToStatement(statement, 7, item.getStatus());
        addTranslatedTypeToStatement(statement, 8, item.getType());
        addNullToStatement(statement, 9);
        addStringToStatement(statement, 10, item.getSubject());
        addEncryptedStingToStatement(masterCipher, statement, 11, item.getBody());
        addStringToStatement(statement, 12, item.getServiceCenter());
        addLongToStatement(statement, 13, threadId);
        modifiedThreads.add(threadId);
        statement.execute();
      }

      for (long threadId : modifiedThreads) {
        threads.update(threadId, true);
      }

      Log.w("PlaintextBackupImporter", "Exited loop");
    } catch (org.xmlpull.v1.XmlPullParserException e) {
      Log.w("PlaintextBackupImporter", e);
      throw new IOException("XML Parsing error!");
    } finally {
      db.endTransaction(transaction);
    }
  }

  private static void importPlaintext(Context context, MasterSecret masterSecret, String path)
      throws IOException
  {
    Log.w("PlaintextBackupImporter", "importPlaintext()");
    SmsDatabase    db          = DatabaseFactory.getSmsDatabase(context);
    SQLiteDatabase transaction = db.beginTransaction();

    try {
      ThreadDatabase threads         = DatabaseFactory.getThreadDatabase(context);
      XmlBackup      backup          = new XmlBackup(path);
      MasterCipher   masterCipher    = new MasterCipher(masterSecret);
      Set<Long>      modifiedThreads = new HashSet<Long>();
      XmlBackup.XmlBackupItem item;

      while ((item = backup.getNext()) != null) {
        Recipients      recipients = RecipientFactory.getRecipientsFromString(context, item.getAddress(), false);
        long            threadId   = threads.getThreadIdFor(recipients);
        SQLiteStatement statement  = db.createInsertStatement(transaction);

        if (item.getAddress() == null || item.getAddress().equals("null"))
          continue;

        if (!isAppropriateTypeForImport(item.getType()))
          continue;

        addStringToStatement(statement, 1, item.getAddress());
        addNullToStatement(statement, 2);
        addLongToStatement(statement, 3, item.getDate());
        addLongToStatement(statement, 4, item.getDate());
        addLongToStatement(statement, 5, item.getProtocol());
        addLongToStatement(statement, 6, item.getRead());
        addLongToStatement(statement, 7, item.getStatus());
        addTranslatedTypeToStatement(statement, 8, item.getType());
        addNullToStatement(statement, 9);
        addStringToStatement(statement, 10, item.getSubject());
        addEncryptedStingToStatement(masterCipher, statement, 11, item.getBody());
        addStringToStatement(statement, 12, item.getServiceCenter());
        addLongToStatement(statement, 13, threadId);
        modifiedThreads.add(threadId);
        statement.execute();
      }

      for (long threadId : modifiedThreads) {
        threads.update(threadId, true);
      }

      Log.w("PlaintextBackupImporter", "Exited loop");
    } catch (XmlPullParserException e) {
      Log.w("PlaintextBackupImporter", e);
      throw new IOException("XML Parsing error!");
    } finally {
      db.endTransaction(transaction);
    }
  }

  private static void addEncryptedStingToStatement(MasterCipher masterCipher, SQLiteStatement statement, int index, String value) {
    if (value == null || value.equals("null")) {
      statement.bindNull(index);
    } else {
      statement.bindString(index, masterCipher.encryptBody(value));
    }
  }

  private static void addTranslatedTypeToStatement(SQLiteStatement statement, int index, int type) {
    statement.bindLong(index, SmsDatabase.Types.translateFromSystemBaseType(type) | SmsDatabase.Types.ENCRYPTION_SYMMETRIC_BIT);
  }

  private static void addStringToStatement(SQLiteStatement statement, int index, String value) {
    if (value == null || value.equals("null")) statement.bindNull(index);
    else                                       statement.bindString(index, value);
  }

  private static void addNullToStatement(SQLiteStatement statement, int index) {
    statement.bindNull(index);
  }

  private static void addLongToStatement(SQLiteStatement statement, int index, long value) {
    statement.bindLong(index, value);
  }

  private static boolean isAppropriateTypeForImport(long theirType) {
    long ourType = SmsDatabase.Types.translateFromSystemBaseType(theirType);

    return ourType == MmsSmsColumns.Types.BASE_INBOX_TYPE ||
           ourType == MmsSmsColumns.Types.BASE_SENT_TYPE ||
           ourType == MmsSmsColumns.Types.BASE_SENT_FAILED_TYPE;
  }


}
