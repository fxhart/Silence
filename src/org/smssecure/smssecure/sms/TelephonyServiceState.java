package org.smssecure.smssecure.sms;

import android.content.Context;
import android.os.Build;
import android.os.Looper;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

public class TelephonyServiceState {

  public boolean isConnected(Context context) {
    ListenThread listenThread = new ListenThread(context);
    listenThread.start();

    return listenThread.get();
  }

  private static class ListenThread extends Thread {

    private final Context context;

    private boolean complete;
    private boolean result;

    public ListenThread(Context context) {
      this.context = context.getApplicationContext();
    }

    @Override
    public void run() {
      TelephonyManager telephonyManager = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);

      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        ServiceStateTelephonyCallback callback = new ServiceStateTelephonyCallback();
        telephonyManager.registerTelephonyCallback(Executors.newSingleThreadExecutor(), callback);
        set(callback.awaitAndIsConnected());
        telephonyManager.unregisterTelephonyCallback(callback);
      } else {
        Looper         looper   = initializeLooper();
        ListenCallback callback = new ListenCallback(looper);
        //noinspection deprecation
        telephonyManager.listen(callback, PhoneStateListener.LISTEN_SERVICE_STATE);
        Looper.loop();
        //noinspection deprecation
        telephonyManager.listen(callback, PhoneStateListener.LISTEN_NONE);
        set(callback.isConnected());
      }
    }

    private Looper initializeLooper() {
      Looper looper = Looper.myLooper();

      if (looper == null) {
        Looper.prepare();
      }

      return Looper.myLooper();
    }

    public synchronized boolean get() {
      while (!complete) {
        try {
          wait();
        } catch (InterruptedException e) {
          throw new AssertionError(e);
        }
      }

      return result;
    }

    private synchronized void set(boolean result) {
      this.result   = result;
      this.complete = true;
      notifyAll();
    }
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private static class ServiceStateTelephonyCallback extends TelephonyCallback
      implements TelephonyCallback.ServiceStateListener {

    private final CountDownLatch latch = new CountDownLatch(1);
    private volatile boolean connected;

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
      this.connected = serviceState.getState() == ServiceState.STATE_IN_SERVICE;
      latch.countDown();
    }

    public boolean awaitAndIsConnected() {
      try {
        latch.await(10, TimeUnit.SECONDS);
      } catch (InterruptedException e) {
        Thread.currentThread().interrupt();
      }
      return connected;
    }
  }

  @SuppressWarnings("deprecation")
  private static class ListenCallback extends PhoneStateListener {

    private final    Looper  looper;
    private volatile boolean connected;

    public ListenCallback(Looper looper) {
      this.looper = looper;
    }

    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
      this.connected = (serviceState.getState() == ServiceState.STATE_IN_SERVICE);
      looper.quit();
    }

    public boolean isConnected() {
      return connected;
    }
  }
}
