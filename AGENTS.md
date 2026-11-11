# Starfall project instructions

- Do not commit or push unless the user explicitly asks for that action. The local
  `origin` points to `https://github.com/agamjotsingh08/StarFall.git`.
- Keep the Android implementation native Java/Canvas; the user does not want Unity.
- Prefer forgiving Casual defaults while keeping harder modes selectable.
- Use JDK 21, SDK 35 and the Gradle wrapper. See README.md for portable setup.
- Keep installed tools, local SDK paths, signing material and build output out of Git.
- Do not describe an old APK as the result of new source changes. Check the version
  and successful build result before handing over an APK.
- Record actual checks and limitations in docs/TESTING.md. The user subsequently
  requested native bug checks and screenshots; device testing is authorized again.
