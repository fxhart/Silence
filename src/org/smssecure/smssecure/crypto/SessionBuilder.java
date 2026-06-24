package org.smssecure.smssecure.crypto;

import android.content.Context;
import android.content.SharedPreferences;

import org.smssecure.smssecure.protocol.KeyExchangeMessage;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.UntrustedIdentityException;
import org.signal.libsignal.protocol.ecc.ECKeyPair;
import org.signal.libsignal.protocol.state.IdentityKeyStore;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.PreKeyStore;
import org.signal.libsignal.protocol.state.SessionStore;
import org.signal.libsignal.protocol.state.SignalProtocolStore;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyStore;

/**
 * Builds Signal Protocol sessions over SMS using a PreKeyBundle exchange.
 *
 * New protocol:
 *  1. Initiator calls process() to generate a KeyExchangeMessage containing
 *     their PreKeyBundle and sends it over SMS.
 *  2. Responder calls process(KeyExchangeMessage) which calls the library's
 *     SessionBuilder.process(PreKeyBundle), establishing an outgoing session.
 *  3. Responder's first encrypted message is a PreKeySignalMessage, which
 *     the initiator's SessionCipher.decrypt() uses to complete the exchange.
 */
public class SessionBuilder {

  private static final String TAG                   = SessionBuilder.class.getSimpleName();
  private static final String PENDING_KEYX_PREF     = "pending_key_exchange_";
  private static final int    SIGNED_PREKEY_ID      = 1;
  private static final int    ONE_TIME_PREKEY_ID    = 1;

  private final SessionStore       sessionStore;
  private final PreKeyStore        preKeyStore;
  private final SignedPreKeyStore  signedPreKeyStore;
  private final IdentityKeyStore   identityKeyStore;
  private final SignalProtocolAddress remoteAddress;

  public SessionBuilder(SessionStore sessionStore,
                        PreKeyStore preKeyStore,
                        SignedPreKeyStore signedPreKeyStore,
                        IdentityKeyStore identityKeyStore,
                        SignalProtocolAddress remoteAddress)
  {
    this.sessionStore      = sessionStore;
    this.preKeyStore       = preKeyStore;
    this.signedPreKeyStore = signedPreKeyStore;
    this.identityKeyStore  = identityKeyStore;
    this.remoteAddress     = remoteAddress;
  }

  public SessionBuilder(SignalProtocolStore store, SignalProtocolAddress remoteAddress) {
    this(store, store, store, store, remoteAddress);
  }

  /**
   * Process a received KeyExchangeMessage (PreKeyBundle from the remote party).
   * Establishes an outgoing session so we can send encrypted messages.
   * Returns a response KeyExchangeMessage (our PreKeyBundle), or null if we
   * already have an active session.
   */
  public KeyExchangeMessage process(KeyExchangeMessage message)
      throws InvalidKeyException, UntrustedIdentityException
  {
    if (sessionStore.containsSession(remoteAddress)) {
      return null;
    }

    org.signal.libsignal.protocol.SessionBuilder libBuilder =
        new org.signal.libsignal.protocol.SessionBuilder(
            sessionStore, preKeyStore, signedPreKeyStore, identityKeyStore, remoteAddress);
    libBuilder.process(message.toPreKeyBundle());

    return buildOurBundle();
  }

  /**
   * Initiate a new session by generating our PreKeyBundle for the remote party.
   */
  public KeyExchangeMessage process() {
    return buildOurBundle();
  }

  private KeyExchangeMessage buildOurBundle() {
    IdentityKeyPair identityKeyPair = identityKeyStore.getIdentityKeyPair();
    int             registrationId  = identityKeyStore.getLocalRegistrationId();

    ECKeyPair signedPreKeyPair = ECKeyPair.generate();
    byte[]    signature        = identityKeyPair.getPrivateKey()
                                                .calculateSignature(
                                                    signedPreKeyPair.getPublicKey().serialize());
    SignedPreKeyRecord signedPreKey = new SignedPreKeyRecord(
        SIGNED_PREKEY_ID, System.currentTimeMillis(), signedPreKeyPair, signature);
    signedPreKeyStore.storeSignedPreKey(SIGNED_PREKEY_ID, signedPreKey);

    ECKeyPair    oneTimePreKeyPair = ECKeyPair.generate();
    PreKeyRecord oneTimePreKey     = new PreKeyRecord(ONE_TIME_PREKEY_ID, oneTimePreKeyPair);
    preKeyStore.storePreKey(ONE_TIME_PREKEY_ID, oneTimePreKey);

    return new KeyExchangeMessage(
        registrationId,
        SIGNED_PREKEY_ID,
        signedPreKeyPair.getPublicKey(),
        signature,
        identityKeyPair.getPublicKey(),
        ONE_TIME_PREKEY_ID,
        oneTimePreKeyPair.getPublicKey()
    );
  }

  public static boolean hasPendingKeyExchange(Context context, String address) {
    return context.getSharedPreferences("silence_crypto", Context.MODE_PRIVATE)
                  .getBoolean(PENDING_KEYX_PREF + address, false);
  }

  public static void setPendingKeyExchange(Context context, String address, boolean pending) {
    context.getSharedPreferences("silence_crypto", Context.MODE_PRIVATE)
           .edit()
           .putBoolean(PENDING_KEYX_PREF + address, pending)
           .apply();
  }
}
