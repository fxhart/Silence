package org.smssecure.smssecure.jobs.requirements;

import android.content.Context;
import android.os.Build;
import android.telephony.PhoneStateListener;
import android.telephony.ServiceState;
import android.telephony.TelephonyCallback;
import android.telephony.TelephonyManager;

import androidx.annotation.RequiresApi;

import org.whispersystems.jobqueue.requirements.RequirementListener;
import org.whispersystems.jobqueue.requirements.RequirementProvider;

import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;

public class ServiceRequirementProvider implements RequirementProvider {

  private final TelephonyManager     telephonyManager;
  private final ServiceStateListener serviceStateListener;
  private final ServiceStateCallback serviceStateCallback;
  private final AtomicBoolean        listeningForServiceState;

  private RequirementListener requirementListener;

  public ServiceRequirementProvider(Context context) {
    this.telephonyManager         = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    this.serviceStateListener     = new ServiceStateListener();
    this.serviceStateCallback     = Build.VERSION.SDK_INT >= Build.VERSION_CODES.S ? new ServiceStateCallback() : null;
    this.listeningForServiceState = new AtomicBoolean(false);
  }

  @Override
  public void setListener(RequirementListener requirementListener) {
    this.requirementListener = requirementListener;
  }

  public void start() {
    if (listeningForServiceState.compareAndSet(false, true)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        telephonyManager.registerTelephonyCallback(Executors.newSingleThreadExecutor(), serviceStateCallback);
      } else {
        //noinspection deprecation
        telephonyManager.listen(serviceStateListener, PhoneStateListener.LISTEN_SERVICE_STATE);
      }
    }
  }

  private void handleInService() {
    if (listeningForServiceState.compareAndSet(true, false)) {
      if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        telephonyManager.unregisterTelephonyCallback(serviceStateCallback);
      } else {
        //noinspection deprecation
        telephonyManager.listen(serviceStateListener, PhoneStateListener.LISTEN_NONE);
      }
    }

    if (requirementListener != null) {
      requirementListener.onRequirementStatusChanged();
    }
  }

  @RequiresApi(Build.VERSION_CODES.S)
  private class ServiceStateCallback extends TelephonyCallback
      implements TelephonyCallback.ServiceStateListener {
    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
      if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
        handleInService();
      }
    }
  }

  @SuppressWarnings("deprecation")
  private class ServiceStateListener extends PhoneStateListener {
    @Override
    public void onServiceStateChanged(ServiceState serviceState) {
      if (serviceState.getState() == ServiceState.STATE_IN_SERVICE) {
        handleInService();
      }
    }
  }
}
