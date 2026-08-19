# Releasing BaldPhone

Git tags matching `v*` trigger `.github/workflows/release.yml`. The workflow builds the `gPlay` flavor, verifies its package and version, signs it with the permanent release identity, and publishes the APK, SHA-256 checksum, and public certificate to GitHub Releases.

## Release identity

The release signing key must remain unchanged for the lifetime of the app. Android will reject an update signed by a different identity.

Certificate SHA-256 fingerprint:

```text
E5:D4:FC:72:56:C0:93:C7:A1:C0:F1:45:DE:B3:CD:05:C1:25:B1:B4:77:ED:CD:A4:44:66:5C:9D:C2:9C:3B:2A
```

The private keystore is not tracked by Git. The maintainer copy is stored at `~/.android/baldphone-release.jks`, its password is stored in macOS Keychain under `com.bogee.BaldPhone.release-keystore`, and encrypted copies are configured in these GitHub Actions secrets:

* `BALDPHONE_RELEASE_KEYSTORE_BASE64`
* `BALDPHONE_RELEASE_STORE_PASSWORD`
* `BALDPHONE_RELEASE_KEY_PASSWORD`

Back up the local keystore and Keychain password securely. GitHub secrets cannot be downloaded after they are saved.

## Publishing an update

1. Increase both `versionCode` and `versionName` in `app/build.gradle`.
2. Merge the tested change into `master`.
3. Create and push a tag equal to `v` plus `versionName`, for example `v15.0.0`.
4. Wait for the **Release APK** workflow and verify the published checksum and certificate.

Obtainium users should add `https://github.com/bogee/BaldPhone`. Future releases must use a higher `versionCode` and the same signing key.
