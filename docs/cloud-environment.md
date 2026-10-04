# Cloud-Environment (claude.ai/code)

Einstellungen des Environments, in dem die Cloud-Sessions laufen.

**Stand 04.10.2026:** Noch nicht eingerichtet. Die Sessions laufen in „Standard-Cloud-Umgebung“ ohne diese Einstellungen: `dl.google.com` ist geblockt (403), `ANDROID_HOME` ist leer. Da `maven.google.com` auf `dl.google.com` umleitet, fehlen dann auch AGP und AndroidX, lokal baut nichts.

**Netzwerk:** Custom, Allowed domains `dl.google.com`, „Also include default list of common package managers“ angehakt.

**Umgebungsvariable:** `ANDROID_HOME=/opt/android-sdk`

**Setup-Skript:**

```bash
#!/bin/bash
set -e
export ANDROID_HOME=/opt/android-sdk
BUILD=13114758   # cmdline-tools; bei 404 aktuelle Nummer von developer.android.com/studio nehmen
mkdir -p $ANDROID_HOME/cmdline-tools
cd /tmp && curl -sSLo clt.zip https://dl.google.com/android/repository/commandlinetools-linux-${BUILD}_latest.zip
unzip -q clt.zip -d $ANDROID_HOME/cmdline-tools && mv $ANDROID_HOME/cmdline-tools/cmdline-tools $ANDROID_HOME/cmdline-tools/latest
yes | $ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager --licenses >/dev/null
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platform-tools"
$ANDROID_HOME/cmdline-tools/latest/bin/sdkmanager "platforms;android-36" "build-tools;36.0.0"
echo "export ANDROID_HOME=$ANDROID_HOME" >> /etc/profile.d/android.sh
```

Platform und Build-Tools lädt Gradle beim ersten Build selbst nach, solange sie hier nicht fest stehen.
