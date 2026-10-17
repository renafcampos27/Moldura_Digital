package pt.molduratv;

import android.content.Context;
import android.content.pm.*;
import android.os.Build;
import androidx.core.content.pm.PackageInfoCompat;
import java.io.*;
import java.security.MessageDigest;
import java.util.*;

/** Never send an APK to the installer before checking its identity and signer. */
public final class UpdatePackageVerifier {
    private UpdatePackageVerifier() {}
    public static PackageInfo installed(Context context) throws PackageManager.NameNotFoundException {
        return context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
    }
    public static void verify(Context context, File file, String expectedVersion) throws Exception {
        PackageManager manager = context.getPackageManager();
        PackageInfo archive = manager.getPackageArchiveInfo(file.getAbsolutePath(), signatureFlags());
        PackageInfo current = manager.getPackageInfo(context.getPackageName(), signatureFlags());
        if (archive == null || archive.applicationInfo == null)
            throw new IOException("O ficheiro não pertence à MolduraTV e foi recusado.");
        UpdateIdentity candidate = new UpdateIdentity(archive.packageName, archive.versionName,
            PackageInfoCompat.getLongVersionCode(archive), archive.applicationInfo.minSdkVersion, signers(archive));
        UpdateIdentity existing = new UpdateIdentity(current.packageName, current.versionName,
            PackageInfoCompat.getLongVersionCode(current), 26, signers(current));
        candidate.validateAgainst(existing, expectedVersion, Build.VERSION.SDK_INT);
    }
    @SuppressWarnings("deprecation") // GET_SIGNATURES is required only on Android 8.0/8.1.
    private static int signatureFlags() {
        return Build.VERSION.SDK_INT >= 28 ? PackageManager.GET_SIGNING_CERTIFICATES : PackageManager.GET_SIGNATURES;
    }
    private static Set<String> signers(PackageInfo info) throws Exception {
        Signature[] signatures;
        if (Build.VERSION.SDK_INT >= 28) signatures = info.signingInfo == null ? null : info.signingInfo.getApkContentsSigners();
        else signatures = legacySignatures(info);
        Set<String> fingerprints = new HashSet<>();
        if (signatures != null) for (Signature signature : signatures) {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(signature.toByteArray());
            fingerprints.add(android.util.Base64.encodeToString(hash, android.util.Base64.NO_WRAP));
        }
        return fingerprints;
    }
    @SuppressWarnings("deprecation") // PackageInfo.signatures is the platform API before Android 9.
    private static Signature[] legacySignatures(PackageInfo info) { return info.signatures; }
}
