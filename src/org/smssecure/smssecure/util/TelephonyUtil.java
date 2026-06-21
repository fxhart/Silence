package org.smssecure.smssecure.util;

import android.content.Context;
import android.content.res.Configuration;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.telephony.TelephonyManager;
import android.telephony.PhoneNumberUtils;
import android.util.Log;

import org.smssecure.smssecure.util.ServiceUtil;

public class TelephonyUtil {
  private static final String TAG = TelephonyUtil.class.getSimpleName();

  public static TelephonyManager getManager(final Context context) {
    return (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
  }

  public static String getMccMnc(final Context context) {
    final TelephonyManager tm = (TelephonyManager) context.getSystemService(Context.TELEPHONY_SERVICE);
    final int configMcc = context.getResources().getConfiguration().mcc;
    final int configMnc = context.getResources().getConfiguration().mnc;
    if (tm.getSimState() == TelephonyManager.SIM_STATE_READY) {
      Log.w(TAG, "Choosing MCC+MNC info from TelephonyManager.getSimOperator()");
      return tm.getSimOperator();
    } else if (tm.getPhoneType() != TelephonyManager.PHONE_TYPE_CDMA) {
      Log.w(TAG, "Choosing MCC+MNC info from TelephonyManager.getNetworkOperator()");
      return tm.getNetworkOperator();
    } else if (configMcc != 0 && configMnc != 0) {
      Log.w(TAG, "Choosing MCC+MNC info from current context's Configuration");
      return String.format("%03d%d",
          configMcc,
          configMnc == Configuration.MNC_ZERO ? 0 : configMnc);
    } else {
      return null;
    }
  }

  public static String getApn(final Context context) {
    final ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
    NetworkInfo info = cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE_MMS);
    return info != null ? info.getExtraInfo() : null;
  }

  public static boolean isMyPhoneNumber(final Context context, String number){
    return number != null && PhoneNumberUtils.compare(context, getPhoneNumber(context), number);
  }

  public static String getPhoneNumber(final Context context){
    final TelephonyManager tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
    try {
      return tm.getLine1Number();
    } catch (SecurityException e) {
      Log.w(TAG, "No permission to read line1 number", e);
      return null;
    }
  }

  @SuppressWarnings("deprecation")
  public static NetworkInfo getNetworkInfo(final Context context) {
    return ServiceUtil.getConnectivityManager(context).getActiveNetworkInfo();
  }

  public static boolean isConnectedRoaming(final Context context) {
    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.M) {
      android.net.Network active = ServiceUtil.getConnectivityManager(context).getActiveNetwork();
      if (active == null) return false;
      android.net.NetworkCapabilities caps =
          ServiceUtil.getConnectivityManager(context).getNetworkCapabilities(active);
      android.net.NetworkInfo info = ServiceUtil.getConnectivityManager(context).getNetworkInfo(active);
      return caps != null
          && caps.hasTransport(android.net.NetworkCapabilities.TRANSPORT_CELLULAR)
          && info != null && info.isRoaming();
    } else {
      NetworkInfo info = getNetworkInfo(context);
      return info != null && info.isConnected() && info.isRoaming()
          && info.getType() == ConnectivityManager.TYPE_MOBILE;
    }
  }
}
