package org.smssecure.smssecure.protocol;

import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.InvalidKeyException;
import org.signal.libsignal.protocol.InvalidMessageException;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.state.PreKeyBundle;

import java.nio.ByteBuffer;

/**
 * SMS key exchange message — carries a PreKeyBundle over SMS so the recipient
 * can call SessionBuilder.process(preKeyBundle) and establish an outgoing session.
 *
 * Wire format (binary, then base64-encoded in SmsTransportDetails):
 *   [1]  version byte (0x01)
 *   [4]  registrationId (big-endian int)
 *   [4]  signedPreKeyId (big-endian int)
 *   [33] signedPreKey (EC public key bytes)
 *   [64] signedPreKeySignature
 *   [33] identityKey (EC public key bytes)
 *   [4]  oneTimePreKeyId (big-endian int, -1 if absent)
 *   [33] oneTimePreKey (EC public key bytes, all zeros if absent)
 *   ---- total = 172 bytes -> base64 ~= 232 chars (fits in 2 SMS segments)
 */
public class KeyExchangeMessage {

  public static final int CURRENT_VERSION    = 1;
  private static final int SIGNED_PK_SIZE    = 33;
  private static final int SIGNATURE_SIZE    = 64;
  private static final int IDENTITY_KEY_SIZE = 33;

  private final int         registrationId;
  private final int         signedPreKeyId;
  private final ECPublicKey signedPreKey;
  private final byte[]      signedPreKeySignature;
  private final IdentityKey identityKey;
  private final int         oneTimePreKeyId;
  private final ECPublicKey oneTimePreKey;
  private final byte[]      serialized;

  /** Construct a KeyExchangeMessage to send. */
  public KeyExchangeMessage(int registrationId,
                            int signedPreKeyId,
                            ECPublicKey signedPreKey,
                            byte[] signedPreKeySignature,
                            IdentityKey identityKey,
                            int oneTimePreKeyId,
                            ECPublicKey oneTimePreKey)
  {
    this.registrationId        = registrationId;
    this.signedPreKeyId        = signedPreKeyId;
    this.signedPreKey          = signedPreKey;
    this.signedPreKeySignature = signedPreKeySignature;
    this.identityKey           = identityKey;
    this.oneTimePreKeyId       = oneTimePreKey != null ? oneTimePreKeyId : -1;
    this.oneTimePreKey         = oneTimePreKey;
    this.serialized            = doSerialize(registrationId, signedPreKeyId, signedPreKey,
                                             signedPreKeySignature, identityKey,
                                             this.oneTimePreKeyId, oneTimePreKey);
  }

  /** Deserialize a received KeyExchangeMessage. */
  public KeyExchangeMessage(byte[] serialized) throws InvalidMessageException {
    try {
      this.serialized = serialized;
      ByteBuffer buf  = ByteBuffer.wrap(serialized);

      byte version = buf.get();
      if (version != CURRENT_VERSION) {
        throw new InvalidMessageException("Unknown KeyExchangeMessage version: " + version);
      }

      this.registrationId        = buf.getInt();
      this.signedPreKeyId        = buf.getInt();

      byte[] signedPkBytes       = new byte[SIGNED_PK_SIZE];
      buf.get(signedPkBytes);
      this.signedPreKey          = new ECPublicKey(signedPkBytes, 0);

      this.signedPreKeySignature = new byte[SIGNATURE_SIZE];
      buf.get(this.signedPreKeySignature);

      byte[] identityKeyBytes    = new byte[IDENTITY_KEY_SIZE];
      buf.get(identityKeyBytes);
      this.identityKey           = new IdentityKey(identityKeyBytes, 0);

      this.oneTimePreKeyId       = buf.getInt();

      byte[] otpkBytes           = new byte[SIGNED_PK_SIZE];
      buf.get(otpkBytes);
      this.oneTimePreKey         = (this.oneTimePreKeyId == -1) ? null
                                                                 : new ECPublicKey(otpkBytes, 0);
    } catch (InvalidKeyException e) {
      throw new InvalidMessageException(e);
    }
  }

  /** Convert to a PreKeyBundle for use with SessionBuilder.process(). */
  public PreKeyBundle toPreKeyBundle() {
    return new PreKeyBundle(
        registrationId,
        1,
        oneTimePreKey != null ? oneTimePreKeyId : PreKeyBundle.NULL_PRE_KEY_ID,
        oneTimePreKey,
        signedPreKeyId,
        signedPreKey,
        signedPreKeySignature,
        identityKey,
        0, null, null
    );
  }

  public int getRegistrationId()              { return registrationId; }
  public int getSignedPreKeyId()              { return signedPreKeyId; }
  public ECPublicKey getSignedPreKey()        { return signedPreKey; }
  public byte[] getSignedPreKeySignature()    { return signedPreKeySignature; }
  public IdentityKey getIdentityKey()         { return identityKey; }
  public int getOneTimePreKeyId()             { return oneTimePreKeyId; }
  public ECPublicKey getOneTimePreKey()       { return oneTimePreKey; }
  public boolean hasOneTimePreKey()           { return oneTimePreKey != null; }
  public byte[] serialize()                   { return serialized; }

  private static byte[] doSerialize(int registrationId,
                                    int signedPreKeyId,
                                    ECPublicKey signedPreKey,
                                    byte[] signature,
                                    IdentityKey identityKey,
                                    int oneTimePreKeyId,
                                    ECPublicKey oneTimePreKey)
  {
    byte[] signedPkBytes    = signedPreKey.serialize();
    byte[] identityKeyBytes = identityKey.serialize();
    byte[] otpkBytes        = oneTimePreKey != null
                              ? oneTimePreKey.serialize()
                              : new byte[SIGNED_PK_SIZE];

    ByteBuffer buf = ByteBuffer.allocate(1 + 4 + 4 + SIGNED_PK_SIZE + SIGNATURE_SIZE
                                           + IDENTITY_KEY_SIZE + 4 + SIGNED_PK_SIZE);
    buf.put((byte) CURRENT_VERSION);
    buf.putInt(registrationId);
    buf.putInt(signedPreKeyId);
    buf.put(signedPkBytes);
    buf.put(signature);
    buf.put(identityKeyBytes);
    buf.putInt(oneTimePreKeyId);
    buf.put(otpkBytes);
    return buf.array();
  }
}
