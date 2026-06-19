package org.smssecure.smssecure;

import android.app.ActivityManager;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.core.content.FileProvider;

import org.smssecure.smssecure.util.DynamicTheme;
import org.whispersystems.libpastelog.util.Scrubber;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.lang.ref.WeakReference;
import java.util.Locale;

public class LogSubmitActivity extends BaseActionBarActivity {

  private static final String SUPPORT_EMAIL  = "fxhart@gmail.com";
  private static final String EMAIL_SUBJECT  = "Silence Debug Log";
  private static final String LOG_FILE_NAME  = "silence_debug.txt";

  private DynamicTheme dynamicTheme = new DynamicTheme();
  private EditText     logPreview;
  private Button       sendButton;
  private Button       cancelButton;

  @Override
  protected void onCreate(Bundle icicle) {
    dynamicTheme.onCreate(this);
    super.onCreate(icicle);
    setContentView(R.layout.log_submit_activity_simple);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);

    logPreview   = findViewById(R.id.log_preview);
    sendButton   = findViewById(R.id.send_button);
    cancelButton = findViewById(R.id.cancel_button);

    sendButton.setEnabled(false);
    sendButton.setOnClickListener(v -> sendLogEmail());
    cancelButton.setOnClickListener(v -> finish());

    new CollectLogTask(this).execute();
  }

  @Override
  protected void onResume() {
    dynamicTheme.onResume(this);
    super.onResume();
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    if (item.getItemId() == android.R.id.home) {
      finish();
      return true;
    }
    return super.onOptionsItemSelected(item);
  }

  private void sendLogEmail() {
    String logText = logPreview.getText().toString();

    try {
      File logFile = new File(getCacheDir(), LOG_FILE_NAME);
      FileWriter writer = new FileWriter(logFile);
      writer.write(logText);
      writer.close();

      Uri fileUri = FileProvider.getUriForFile(
          this,
          getPackageName() + ".fileprovider",
          logFile
      );

      Intent intent = new Intent(Intent.ACTION_SEND);
      intent.setType("message/rfc822");
      intent.putExtra(Intent.EXTRA_EMAIL,   new String[]{ SUPPORT_EMAIL });
      intent.putExtra(Intent.EXTRA_SUBJECT, EMAIL_SUBJECT);
      intent.putExtra(Intent.EXTRA_TEXT,    "Debug log attached.");
      intent.putExtra(Intent.EXTRA_STREAM,  fileUri);
      intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

      startActivity(Intent.createChooser(intent, "Send log via…"));
      finish();
    } catch (IOException e) {
      Toast.makeText(this, R.string.log_submit_activity__log_fetch_failed, Toast.LENGTH_LONG).show();
    }
  }

  private static String collectLog() {
    try {
      Process       process = Runtime.getRuntime().exec("logcat -d");
      BufferedReader reader  = new BufferedReader(new InputStreamReader(process.getInputStream()));
      StringBuilder  sb      = new StringBuilder();
      String         sep     = System.getProperty("line.separator");
      String line;
      while ((line = reader.readLine()) != null) {
        sb.append(line).append(sep);
      }
      return new Scrubber().scrub(sb.toString());
    } catch (IOException e) {
      return "";
    }
  }

  private static String buildHeader(LogSubmitActivity activity) {
    android.content.pm.PackageManager pm = activity.getPackageManager();
    StringBuilder sb = new StringBuilder();

    sb.append("Device  : ")
      .append(Build.MANUFACTURER).append(" ")
      .append(Build.MODEL).append(" (").append(Build.PRODUCT).append(")\n");
    sb.append("Android : ").append(Build.VERSION.RELEASE)
      .append(" (").append(Build.VERSION.INCREMENTAL)
      .append(", ").append(Build.DISPLAY).append(")\n");

    Runtime rt = Runtime.getRuntime();
    sb.append("Memory  : ").append(String.format(Locale.ENGLISH,
        "%dM (%.2f%% free, %dM max)",
        rt.totalMemory() / 1048576L,
        (float) rt.freeMemory() / rt.totalMemory() * 100f,
        rt.maxMemory() / 1048576L)).append("\n");

    ActivityManager am = (ActivityManager) activity.getSystemService(ACTIVITY_SERVICE);
    sb.append("Memclass: ").append(am.getMemoryClass()).append("\n");
    sb.append("OS Host : ").append(Build.HOST).append("\n");
    sb.append("App     : ");
    try {
      sb.append(pm.getApplicationLabel(pm.getApplicationInfo(activity.getPackageName(), 0)))
        .append(" ")
        .append(pm.getPackageInfo(activity.getPackageName(), 0).versionName)
        .append("\n");
    } catch (android.content.pm.PackageManager.NameNotFoundException e) {
      sb.append("Unknown\n");
    }
    sb.append("\n");
    return sb.toString();
  }

  private static class CollectLogTask extends AsyncTask<Void, Void, String> {
    private final WeakReference<LogSubmitActivity> ref;

    CollectLogTask(LogSubmitActivity activity) {
      this.ref = new WeakReference<>(activity);
    }

    @Override
    protected void onPreExecute() {
      LogSubmitActivity activity = ref.get();
      if (activity != null) {
        activity.logPreview.setText("Loading logs…");
        activity.sendButton.setEnabled(false);
      }
    }

    @Override
    protected String doInBackground(Void... voids) {
      LogSubmitActivity activity = ref.get();
      if (activity == null) return null;
      return buildHeader(activity) + collectLog();
    }

    @Override
    protected void onPostExecute(String result) {
      LogSubmitActivity activity = ref.get();
      if (activity == null) return;
      if (result == null || result.trim().isEmpty()) {
        Toast.makeText(activity, R.string.log_submit_activity__log_fetch_failed, Toast.LENGTH_LONG).show();
        activity.finish();
        return;
      }
      activity.logPreview.setText(result);
      activity.sendButton.setEnabled(true);
    }
  }
}
