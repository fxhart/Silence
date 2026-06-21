package org.smssecure.smssecure.util.task;

import android.app.AlertDialog;
import android.content.Context;
import android.os.AsyncTask;
import android.widget.ProgressBar;

import java.lang.ref.WeakReference;

public abstract class ProgressDialogAsyncTask<Params, Progress, Result> extends AsyncTask<Params, Progress, Result> {

  private final WeakReference<Context> contextReference;
  private       AlertDialog            progress;
  private final String                 title;
  private final String                 message;

  public ProgressDialogAsyncTask(Context context, String title, String message) {
    super();
    this.contextReference = new WeakReference<>(context);
    this.title            = title;
    this.message          = message;
  }

  public ProgressDialogAsyncTask(Context context, int title, int message) {
    this(context, context.getString(title), context.getString(message));
  }

  @Override
  protected void onPreExecute() {
    final Context context = contextReference.get();
    if (context == null) return;

    ProgressBar progressBar = new ProgressBar(context);
    progressBar.setIndeterminate(true);
    int padding = Math.round(16 * context.getResources().getDisplayMetrics().density);
    progressBar.setPadding(padding, padding, padding, padding);

    AlertDialog.Builder builder = new AlertDialog.Builder(context);
    builder.setTitle(title);
    builder.setMessage(message);
    builder.setCancelable(false);
    builder.setView(progressBar);
    progress = builder.create();
    progress.show();
  }

  @Override
  protected void onPostExecute(Result result) {
    if (progress != null) progress.dismiss();
  }

  protected Context getContext() {
    return contextReference.get();
  }
}
