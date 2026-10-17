package pt.molduratv;

import java.io.IOException;
import java.util.*;

/** Platform-independent install policy, also applied after returning from Settings. */
public final class UpdateIdentity {
    public final String packageName, versionName;
    public final long versionCode;
    public final int minSdk;
    public final Set<String> signers;
    public UpdateIdentity(String packageName, String versionName, long versionCode, int minSdk, Set<String> signers) {
        this.packageName = packageName; this.versionName = versionName; this.versionCode = versionCode;
        this.minSdk = minSdk; this.signers = Collections.unmodifiableSet(new HashSet<>(signers));
    }
    public void validateAgainst(UpdateIdentity installed, String expectedVersion, int deviceSdk) throws IOException {
        if (!installed.packageName.equals(packageName)) throw new IOException("O ficheiro não pertence à MolduraTV e foi recusado.");
        if (!ReleaseUpdate.normalize(expectedVersion).equals(versionName)) throw new IOException("A versão do APK não corresponde à Release publicada.");
        if (ReleaseUpdate.compareVersions(versionName, installed.versionName) <= 0)
            throw new IOException("Esta versão já não é mais recente do que a aplicação instalada.");
        if (versionCode <= installed.versionCode) throw new IOException("O versionCode do APK não é superior ao instalado.");
        if (minSdk > deviceSdk) throw new IOException("Esta atualização exige uma versão mais recente do Android.");
        if (signers.isEmpty() || !signers.equals(installed.signers))
            throw new IOException("A assinatura é diferente da aplicação instalada. Publica o APK com a mesma chave de assinatura.");
    }
}
