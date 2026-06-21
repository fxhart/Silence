package org.smssecure.smssecure.util;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;
import android.widget.Toast;

import org.smssecure.smssecure.R;
import org.smssecure.smssecure.database.DatabaseFactory;
import org.smssecure.smssecure.database.ThreadDatabase;

public class Trimmer {

  public static void trimAllThreads(Context context, int threadLengthLimit) {
    new TrimmingProgressTask(context).execute(threadLengthLimit);
  }

  private static class TrimmingProgressTask extends AsyncTask<Integer, Integer, Void> implements ThreadDatabase.ProgressListener {
    private AlertDialog  progressDialog;
    private ProgressBar  progressBar;
    private Context      context;

    public TrimmingProgressTask(Context context) {
      this.context = context;
    }

    @Override
    protected void onPreExecute() {
      progressBar = new ProgressBar(context, null, android.R.attr.progressBarStyleHorizontal);
      progressBar.setMax(100);
      progressBar.setIndeterminate(false);
      int padding = Math.round(16 * context.getResources().getDisplayMetrics().density);
      progressBar.setPadding(padding, padding, padding, padding);

      progressDialog = new AlertDialog.Builder(context)
          .setTitle(R.string.trimmer__deleting)
          .setMessage(R.string.trimmer__deleting_old_messages)
          .setCancelable(false)
          .setView(progressBar)
          .create();
      progressDialog.show();
    }

    @Override
    protected Void doInBackground(Integer... params) {
      DatabaseFactory.getThreadDatabase(context).trimAllThreads(params[0], this);
      return null;
    }

    @Override
    protected void onProgressUpdate(Integer... progress) {
      double count = progress[1];
      double index = progress[0];
      progressBar.setProgress((int) Math.round((index / count) * 100.0));
    }

    @Override
    protected void onPostExecute(Void result) {
      progressDialog.dismiss();
      Toast.makeText(context,
                     R.string.trimmer__old_messages_successfully_deleted,
                     Toast.LENGTH_LONG).show();
    }

    @Override
    public void onProgress(int complete, int total) {
      this.publishProgress(complete, total);
    }
  }
}
