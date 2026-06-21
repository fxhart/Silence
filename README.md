
# About Silence

## TL;DR

Silence makes your messages **unreadable**, not **invisible**. Use it when you need the *content* of an SMS protected and you accept that the carrier still knows you sent it. Don't use it when the *fact* that you communicated is the thing you need to hide.

Silence is a fork of [Silence](https://github.com/SilenceIM/Silence), an encrypted SMS/MMS app, modernized against a current Android SDK.

Before anything else, be clear about what this app does and does not do. If you get this wrong, you can hurt yourself with a false sense of safety. So the limitations come first.

## What Silence is NOT

**It is not private. It is not anonymous.** Silence encrypts the *contents* of a message. It does nothing to hide the *metadata* around it, because that metadata belongs to the carrier's network, not to this app. Your carrier — and anyone with lawful or unlawful access to carrier records — can still see:

- **Who** you messaged and who messaged you (both phone numbers)
- **When** each message was sent and delivered (timestamps)
- **Where** you were, approximately, via the cell tower your phone used
- **Which SIM** you sent it from, tied to your IMSI/IMEI
- **How big** the message was and how many SMS segments it used
- For MMS: subject, recipient lists, attachment types and sizes, and the delivery/read-report flags

**It does not hide that you are using encryption.** Encrypted messages are high-entropy data and are statistically distinguishable from ordinary text. A determined observer such as your carrier can tell that encrypted messaging is in use — though it cannot be cheaply filtered out of normal SMS traffic in bulk.

**It is not a Signal replacement.** If your threat model requires hiding *who* you talk to, *when*, or *from where*, SMS is the wrong transport and Silence is the wrong tool. Use something that routes over the internet through a server you control or trust. Silence protects message *content*, not the *fact* or the *pattern* of your communication.

**It is not magic.** Both parties must be running Silence (or Silence) and must have completed a key exchange. Messages to anyone else are sent as normal, unencrypted SMS/MMS.

## What Silence IS

**Secure, unreadable message content.** When both sides run Silence, the body of your message is end-to-end encrypted on your device before it ever reaches the radio. The carrier carries ciphertext. They route it, log it, and bill it — but they cannot read it, and neither can anyone intercepting it in transit.

**SMS/MMS, not data.** Silence rides the cellular text network, not the
internet. That means:

- **No account.** No sign-up, no email, no phone-number registration with any service.
  
- **No server.** There is no central service to subpoena, breach, go offline, or change its terms on you. Your keys live on your device.
  
- **It works where data doesn't** — weak signal, roaming, dead zones, no Wi-Fi, or a plan with no data at all. If a plain text message can get through, an encrypted one can too.

**A drop-in replacement for your normal messaging app.** Silence handles all of your SMS and MMS — encrypted and plain — so you don't run two apps. Encryption turns on automatically once you've exchanged keys with another Silence/Silence user; everyone else just gets normal texts. There is no separate "secure mode" to remember to switch into.

**Encrypted at rest.** Your message database is encrypted on the device behind your passphrase, so a lost or seized phone doesn't hand over your history.

**Open source.** The full source is here. You can read it, build it, and verify the claims on this page for yourself — which is the only kind of trust that should count.

# Legal
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

## License

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html
