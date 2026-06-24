package org.smssecure.smssecure.crypto.storage;

import android.content.Context;

import org.smssecure.smssecure.crypto.MasterSecret;
import org.signal.libsignal.protocol.SignalProtocolAddress;
import org.signal.libsignal.protocol.IdentityKey;
import org.signal.libsignal.protocol.IdentityKeyPair;
import org.signal.libsignal.protocol.InvalidKeyIdException;
import org.signal.libsignal.protocol.ReusedBaseKeyException;
import org.signal.libsignal.protocol.ecc.ECPublicKey;
import org.signal.libsignal.protocol.groups.state.SenderKeyRecord;
import org.signal.libsignal.protocol.groups.state.SenderKeyStore;
import org.signal.libsignal.protocol.state.IdentityKeyStore;
import org.signal.libsignal.protocol.state.KyberPreKeyRecord;
import org.signal.libsignal.protocol.state.KyberPreKeyStore;
import org.signal.libsignal.protocol.state.PreKeyRecord;
import org.signal.libsignal.protocol.state.PreKeyStore;
import org.signal.libsignal.protocol.state.SessionRecord;
import org.signal.libsignal.protocol.state.SessionStore;
import org.signal.libsignal.protocol.state.SignalProtocolStore;
import org.signal.libsignal.protocol.state.SignedPreKeyRecord;
import org.signal.libsignal.protocol.state.SignedPreKeyStore;

import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.signal.libsignal.protocol.NoSessionException;

public class SilenceSignalProtocolStore implements SignalProtocolStore {

  private final PreKeyStore       preKeyStore;
  private final SignedPreKeyStore signedPreKeyStore;
  private final IdentityKeyStore  identityKeyStore;
  private final SessionStore      sessionStore;
  private final int               subscriptionId;

  public SilenceSignalProtocolStore(Context context, MasterSecret masterSecret, int subscriptionId) {
    this.preKeyStore       = new SilencePreKeyStore(context, masterSecret, subscriptionId);
    this.signedPreKeyStore = new SilencePreKeyStore(context, masterSecret, subscriptionId);
    this.identityKeyStore  = new SilenceIdentityKeyStore(context, masterSecret, subscriptionId);
    this.sessionStore      = new SilenceSessionStore(context, masterSecret, subscriptionId);
    this.subscriptionId    = subscriptionId;
  }

  @Override
  public IdentityKeyPair getIdentityKeyPair() {
    return identityKeyStore.getIdentityKeyPair();
  }

  @Override
  public int getLocalRegistrationId() {
    return identityKeyStore.getLocalRegistrationId();
  }

  @Override
  public PreKeyRecord loadPreKey(int preKeyId) throws InvalidKeyIdException {
    return preKeyStore.loadPreKey(preKeyId);
  }

  @Override
  public void storePreKey(int preKeyId, PreKeyRecord record) {
    preKeyStore.storePreKey(preKeyId, record);
  }

  @Override
  public boolean containsPreKey(int preKeyId) {
    return preKeyStore.containsPreKey(preKeyId);
  }

  @Override
  public void removePreKey(int preKeyId) {
    preKeyStore.removePreKey(preKeyId);
  }

  @Override
  public SessionRecord loadSession(SignalProtocolAddress axolotlAddress) {
    return sessionStore.loadSession(axolotlAddress);
  }

  @Override
  public List<Integer> getSubDeviceSessions(String number) {
    return sessionStore.getSubDeviceSessions(number);
  }

  @Override
  public List<SessionRecord> loadExistingSessions(List<SignalProtocolAddress> addresses) throws NoSessionException {
    return sessionStore.loadExistingSessions(addresses);
  }

  @Override
  public void storeSession(SignalProtocolAddress axolotlAddress, SessionRecord record) {
    sessionStore.storeSession(axolotlAddress, record);
  }

  @Override
  public boolean containsSession(SignalProtocolAddress axolotlAddress) {
    return sessionStore.containsSession(axolotlAddress);
  }

  @Override
  public void deleteSession(SignalProtocolAddress axolotlAddress) {
    sessionStore.deleteSession(axolotlAddress);
  }

  @Override
  public void deleteAllSessions(String number) {
    sessionStore.deleteAllSessions(number);
  }

  @Override
  public SignedPreKeyRecord loadSignedPreKey(int signedPreKeyId) throws InvalidKeyIdException {
    return signedPreKeyStore.loadSignedPreKey(signedPreKeyId);
  }

  @Override
  public List<SignedPreKeyRecord> loadSignedPreKeys() {
    return signedPreKeyStore.loadSignedPreKeys();
  }

  @Override
  public void storeSignedPreKey(int signedPreKeyId, SignedPreKeyRecord record) {
    signedPreKeyStore.storeSignedPreKey(signedPreKeyId, record);
  }

  @Override
  public boolean containsSignedPreKey(int signedPreKeyId) {
    return signedPreKeyStore.containsSignedPreKey(signedPreKeyId);
  }

  @Override
  public void removeSignedPreKey(int signedPreKeyId) {
    signedPreKeyStore.removeSignedPreKey(signedPreKeyId);
  }

  @Override
  public IdentityKey getIdentity(SignalProtocolAddress address) {
    return null;
  }

  @Override
  public boolean isTrustedIdentity(SignalProtocolAddress address, IdentityKey identityKey, Direction direction) {
    return identityKeyStore.isTrustedIdentity(address, identityKey, direction);
  }

  @Override
  public IdentityKeyStore.IdentityChange saveIdentity(SignalProtocolAddress address, IdentityKey identityKey) {
    return identityKeyStore.saveIdentity(address, identityKey);
  }

  // SenderKeyStore — not used for SMS, stub implementation
  @Override
  public void storeSenderKey(SignalProtocolAddress sender, UUID distributionId, SenderKeyRecord record) {}

  @Override
  public SenderKeyRecord loadSenderKey(SignalProtocolAddress sender, UUID distributionId) {
    return null;
  }

  // KyberPreKeyStore — not used (no post-quantum keys over SMS), stub implementation
  @Override
  public KyberPreKeyRecord loadKyberPreKey(int kyberPreKeyId) throws InvalidKeyIdException {
    throw new InvalidKeyIdException("No Kyber prekeys");
  }

  @Override
  public List<KyberPreKeyRecord> loadKyberPreKeys() {
    return Collections.emptyList();
  }

  @Override
  public void storeKyberPreKey(int kyberPreKeyId, KyberPreKeyRecord record) {}

  @Override
  public boolean containsKyberPreKey(int kyberPreKeyId) {
    return false;
  }

  @Override
  public void markKyberPreKeyUsed(int kyberPreKeyId, int signedPreKeyId, ECPublicKey senderRatchetKey)
      throws ReusedBaseKeyException {}
}
