"""Ajusta o projeto android/ gerado pelo `npx cap add android`:
código nativo, permissões, a janela da nota e os ícones do BuyWell."""
import re
import shutil
from pathlib import Path

RAIZ = Path(__file__).resolve().parent.parent
APP = RAIZ / "android" / "app" / "src" / "main"
PKG = APP / "java" / "br" / "tec" / "feu" / "buywell"
PKG.mkdir(parents=True, exist_ok=True)
for nome in ("MainActivity.java", "NotaSefazPlugin.java", "NotaSefazActivity.java"):
    shutil.copy(RAIZ / "nativo" / nome, PKG / nome)

man = APP / "AndroidManifest.xml"
x = man.read_text(encoding="utf-8")
perms = ["android.permission.CAMERA", "android.permission.ACCESS_FINE_LOCATION",
         "android.permission.ACCESS_COARSE_LOCATION", "android.permission.INTERNET"]
novas = "".join(f'    <uses-permission android:name="{p}" />\n' for p in perms if p not in x)
x = x.replace("<application", novas + '    <uses-feature android:name="android.hardware.camera" android:required="false" />\n    <application', 1)
if "NotaSefazActivity" not in x:
    x = x.replace("</application>",
                  '        <activity android:name=".NotaSefazActivity" android:exported="false"\n'
                  '            android:theme="@style/AppTheme.NoActionBar"\n'
                  '            android:configChanges="orientation|screenSize|keyboardHidden" />\n    </application>', 1)
man.write_text(x, encoding="utf-8")

res = APP / "res"
for d in res.glob("mipmap-anydpi*"):
    shutil.rmtree(d)  # sem ícone adaptativo: usa os PNG do BuyWell
for pasta in (RAIZ / "icones").iterdir():
    destino = res / pasta.name
    destino.mkdir(exist_ok=True)
    for png in pasta.glob("*.png"):
        shutil.copy(png, destino / png.name)

gradle = RAIZ / "android" / "app" / "build.gradle"
g = gradle.read_text(encoding="utf-8")
g = re.sub(r'versionName\s+"[^"]*"', 'versionName "0.3.0"', g)
gradle.write_text(g, encoding="utf-8")
print("android/ preparado:", PKG)
