package org.smssecure.smssecure.database;

import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.provider.MediaStore;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.smssecure.smssecure.database.model.SmsMessageRecord;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;

public class PlaintextBackupExporter {

  private static final String FILENAME = "SilencePlaintextBackup.xml";

  public static void exportPlaintextToSd(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    exportPlaintext(context, masterSecret);
  }

  private static void exportPlaintext(Context context, MasterSecret masterSecret)
      throws NoExternalStorageException, IOException
  {
    int count = DatabaseFactory.getSmsDatabase(context).getMessageCount();

    OutputStream outputStream;
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
      // Delete any previous backup with the same name so we don't accumulate copies
      context.getContentResolver().delete(
          MediaStore.Downloads.EXTERNAL_CONTENT_URI,
          MediaStore.Downloads.DISPLAY_NAME + "=?",
          new String[]{FILENAME});

      ContentValues values = new ContentValues();
      values.put(MediaStore.Downloads.DISPLAY_NAME, FILENAME);
      values.put(MediaStore.Downloads.MIME_TYPE, "text/xml");
      Uri uri = context.getContentResolver().insert(MediaStore.Downloads.EXTERNAL_CONTENT_URI, values);
      if (uri == null) throw new NoExternalStorageException();
      outputStream = context.getContentResolver().openOutputStream(uri);
      if (outputStream == null) throw new IOException("Could not open output stream for " + uri);
    } else {
      File dir = context.getExternalFilesDir(null);
      if (dir == null || !dir.canWrite()) throw new NoExternalStorageException();
      outputStream = new FileOutputStream(new File(dir, FILENAME));
    }

    XmlBackup.Writer writer = new XmlBackup.Writer(outputStream, count);

    SmsMessageRecord record;
    EncryptingSmsDatabase.Reader reader = null;
    int skip      = 0;
    int ROW_LIMIT = 500;

    do {
      if (reader != null) reader.close();

      reader = DatabaseFactory.getEncryptingSmsDatabase(context).getMessages(masterSecret, skip, ROW_LIMIT);

      while ((record = reader.getNext()) != null) {
        XmlBackup.XmlBackupItem item =
            new XmlBackup.XmlBackupItem(0, record.getIndividualRecipient().getNumber(),
                                        record.getDateReceived(),
                                        MmsSmsColumns.Types.translateToSystemBaseType(record.getType()),
                                        null, record.getDisplayBody().toString(), null,
                                        1, record.getDeliveryStatus());
        writer.writeItem(item);
      }

      skip += ROW_LIMIT;
    } while (reader.getCount() > 0);

    writer.close();
  }
}
