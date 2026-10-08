# SMS Syncer

A native Android app that forwards new SMS from each provider to that provider's receivers. Built with Kotlin and Jetpack Compose, for private installation on Android 8.0 or later.

The app is displayed in Korean, including its launcher name **문자 전달**, permission guidance and editing controls. App-owned text is defined in `app/src/main/res/values/strings.xml`.

## How it works

The app opens on one **제공자** (Providers) page. Each provider card shows its name, phone number, and receivers, with a **수정** (Edit) button. The **제공자 추가** (Add provider) button stays at the bottom of the screen.

The add/edit page contains the provider name and phone number at the top and its **수신자** (Receivers) list below. You can add, edit, or remove receivers, with optional receiver labels. Press **제공자 저장** to save all changes together. Leaving with unsaved changes prompts you to discard them. Existing providers can be deleted from the edit page.

Each provider's incoming SMS goes only to **its own receivers**, up to 20 per provider. The same receiver can belong to several providers. Provider phone numbers must be unique, and provider names are required. Numeric provider short codes of 3–6 digits are supported.

Providers and their receivers persist across app restarts. A provider can be saved without receivers, but it will not forward messages until receivers are added. Grant SMS access and turn on the forwarding switch after configuring at least one provider with receivers. Removing all configured routes switches forwarding off.

On upgrade from the earlier two-tab app, existing watched numbers become providers, using their labels as names (or their local phone number if unnamed). The previous shared recipient list is copied into each provider, retaining IDs, receiver labels, forwarding enablement, and the prior routing behavior. Each provider's receivers can then be changed independently. Migration preserves duplicate receipt hashes.

Enter Korean local numbers such as `010-1234-5678` or `02-312-3456`; no country code is required. Number parsing always uses Korea, regardless of the SIM or device language. Screens, editing, and SMS submission use domestic numbers. Matching keeps a canonical identity internally so incoming `+82` numbers and previously saved Korean international entries still match local numbers. New entries must be Korean numbers (or numeric short codes for providers). Matching uses the complete normalized number, never just its trailing digits. Alphanumeric sender IDs are not supported.

The forwarded SMS uses the matched provider's saved name in brackets, followed by the original message text:

```text
[ 회사 ] The original message text
```

It is sent from the installed phone's **default SMS SIM**, with normal carrier SMS charges. Set a default SMS SIM in Android Settings before enabling forwarding. Long messages are split into SMS segments, each of which can incur a charge.

The app has no send-history screen and does not store send-history records or SMS content. Updating from an earlier version removes its old app history. Duplicate receipt hashes are retained to prevent repeated forwarding. Failed or uncertain sends are not automatically retried.

## Build

Open this directory in Android Studio and let Gradle sync. The project uses Android Gradle Plugin 9.4.1, Gradle 9.6.0, Android SDK 36, and the bundled Kotlin support in AGP. Use a compatible JDK (17–25), including Android Studio's bundled runtime. Install SDK Platform 36 and accept its SDK licenses.

From the terminal, with `JAVA_HOME` pointing to your JDK:

```sh
./gradlew assembleDebug testDebugUnitTest lintDebug
```

The debug APK is generated at `app/build/outputs/apk/debug/app-debug.apk`. Install it on a phone using Android Studio or:

```sh
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

`local.properties` contains your local Android SDK path and is intentionally ignored. Android Studio can generate it on another computer. Release builds are unsigned until you configure your own signing key.

## Permissions and device behavior

- Requests only `RECEIVE_SMS` and `SEND_SMS`. It does not request access to the historical SMS inbox, contacts, or the internet.
- Uses Android's manifest SMS receiver, so it does not need a continuously running service or an open app window. Launch the app once after installation. Android force-stop blocks reception until you open the app again. Manufacturer background restrictions can also affect reception; verify on the intended phone.
- These are hard-restricted permissions. The installation method must allowlist them in addition to the user granting runtime access. A permission dialog alone does not guarantee access on every device. For development, use Android Studio/ADB installation and verify SMS permissions in Android Settings.
- This is a private-install forwarding app, not a complete default SMS client. Google Play distribution requires a separate review of the SMS permissions policy and may require a different application design.
- For Android versions with additional sensitive-SMS restrictions, OTP/verification messages may not be exposed to an ordinary forwarding app. This project does not request privileged access or bypass those restrictions.
- Supports carrier SMS. MMS, RCS, WhatsApp, and other app messages are outside its scope.

Provider details and receivers stay in app-private storage, and backups are disabled. Message bodies are held only for processing and submission. Receipt hashes are kept locally for duplicate detection. A provider number cannot also be a receiver number, and the app will never forward directly back to the matched sender. These checks reduce simple loops, but cannot detect forwarding configured on other devices.

Incoming messages are submitted during the SMS broadcast. There is no durable outgoing queue or automatic retry. A process interruption can leave a send incomplete or its status unknown; the app deliberately avoids replaying an uncertain submission.

## Verification

The JVM tests cover Korean number normalization, short codes, exact provider selection, receiver isolation, shared receivers, editing/removal behavior, validation, old-settings migration, schema round trips, and duplicate receipt fingerprints.

For device verification, see [the manual test checklist](docs/DEVICE_TESTING.md). Real carrier sending and manufacturer background behavior require a physical phone.
