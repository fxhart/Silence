
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

---

## A note on how Silence is built (AI disclosure)

In the same spirit as the limitations above: you deserve to know how this
software is made before you trust it.

**Most of the code in Silence was written by an AI coding assistant (Claude),
directed by a maintainer who is not a professional programmer.** I understand
networks, systems, and the problem I'm trying to solve; I do not hand-write most
of the Android code myself. The AI does the heavy lifting and I review,
test, and decide.

You should know what that does and does not mean.

**What the AI did *not* write: the cryptography.** Silence does not invent its own
encryption. It inherits the cryptographic core from Silence, which inherits it
from TextSecure / the Signal protocol (libsignal) — a well-known, widely
reviewed implementation. The AI-assisted work is the *modernization and plumbing*
around that core: bringing the app up to a current Android SDK, fixing build and
API issues, and the glue that holds the app together. The rule "don't roll your
own crypto" still applies here, and Silence doesn't.

**What this does mean: it has not been professionally audited.** AI-generated
code can contain subtle bugs, and in a security application subtle bugs matter
more than usual — a mistake in the plumbing *around* good crypto can still
weaken it. Neither the original Silence code nor this fork has had a formal
independent security audit. I cannot personally vouch for the correctness of
every line at the depth a professional cryptographer or Android security
engineer could.

**Why I'm telling you this instead of hiding it.** Because the alternative is
worse. A privacy tool that quietly oversells itself is dangerous. I would rather
you trust this app *less* and verify it *more* than rely on a confidence I
haven't earned.

**What that means for you:**

- Treat Silence as **community-grade software, not audited software.** It is
  useful for keeping the content of everyday messages away from your carrier and
  casual interception. It is not a guarantee against a sophisticated, targeted
  adversary.
- The source is fully open for exactly this reason. **If you have the skills to
  review it, please do** — that scrutiny is the only thing that makes
  open-source security real, and it matters more here, not less, because of how
  this was built.
- If you find a problem, report it. Good-faith security reports are welcome and
  will be taken seriously.

This is a personal project shared in the hope it's useful, with no warranty of
any kind. You use it at your own risk — which is true of all software, but worth
saying plainly here.

---

# Legal
## Cryptography Notice

This distribution includes cryptographic software. The country in which you currently reside may have restrictions on the import, possession, use, and/or re-export to another country, of encryption software.
BEFORE using any encryption software, please check your country's laws, regulations and policies concerning the import, possession, or use, and re-export of encryption software, to see if this is permitted.
See <http://www.wassenaar.org/> for more information.

## License

Licensed under the GPLv3: http://www.gnu.org/licenses/gpl-3.0.html
