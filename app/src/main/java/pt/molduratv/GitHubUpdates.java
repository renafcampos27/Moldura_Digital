package pt.molduratv;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.security.MessageDigest;
import java.util.concurrent.CancellationException;
import java.util.concurrent.atomic.AtomicBoolean;

/** Bounded HTTPS IO, explicit redirects, streaming hash and cancellation. */
public final class GitHubUpdates {
    private volatile HttpURLConnection connection;
    public interface Progress { void bytes(long bytes); }
    public void cancel() { HttpURLConnection open = connection; if (open != null) open.disconnect(); }
    private HttpURLConnection open(String address, boolean api, AtomicBoolean cancelled) throws IOException {
        for (int redirects = 0; redirects < 6; redirects++) {
            checkCancelled(cancelled);
            if (api ? !ReleaseUpdate.API_URL.equals(address) : !ReleaseUpdate.allowedDownloadUrl(address))
                throw new IOException("O servidor indicou um endereço de atualização não permitido.");
            HttpURLConnection current = (HttpURLConnection)new URL(address).openConnection();
            connection = current;
            current.setConnectTimeout(15_000); current.setReadTimeout(20_000);
            current.setInstanceFollowRedirects(false);
            current.setRequestProperty("User-Agent", "MolduraTV-Updater");
            current.setRequestProperty("Accept", api ? "application/vnd.github+json" : "application/octet-stream");
            if (api) current.setRequestProperty("X-GitHub-Api-Version", "2022-11-28");
            int code = current.getResponseCode();
            if (code == 200) return current;
            if (!api && (code == 301 || code == 302 || code == 303 || code == 307 || code == 308)) {
                String location = current.getHeaderField("Location"); current.disconnect();
                if (location == null) throw new IOException("Redirecionamento sem destino.");
                address = new URL(new URL(address), location).toString(); continue;
            }
            current.disconnect();
            if (api && code == 404) return null;
            if (code == 403 || code == 429) throw new IOException("O GitHub limitou o acesso. Tenta novamente mais tarde.");
            throw new IOException("Não foi possível obter a atualização (HTTP " + code + ").");
        }
        throw new IOException("Demasiados redirecionamentos ao descarregar a atualização.");
    }
    public ReleaseUpdate check(String installed, AtomicBoolean cancelled) throws Exception {
        try {
            HttpURLConnection response = open(ReleaseUpdate.API_URL, true, cancelled);
            if (response == null) throw new IOException("Ainda não existe uma Release estável publicada neste repositório.");
            try (InputStream input = response.getInputStream(); ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                byte[] buffer = new byte[8192]; int count;
                while ((count = input.read(buffer)) != -1) {
                    checkCancelled(cancelled);
                    if (output.size() + count > 1_048_576) throw new IOException("A resposta do GitHub é demasiado grande.");
                    output.write(buffer, 0, count);
                }
                return ReleaseUpdate.parse(new String(output.toByteArray(), StandardCharsets.UTF_8), installed);
            }
        } finally { cancel(); }
    }
    public File download(ReleaseUpdate release, File directory, AtomicBoolean cancelled, Progress progress) throws Exception {
        if (!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Não foi possível preparar a pasta temporária.");
        if (directory.getUsableSpace() < release.size + 8L * 1024 * 1024) throw new IOException("Não há espaço suficiente para a atualização.");
        File partial = new File(directory, "update.part"), ready = new File(directory, "update.apk");
        try {
            HttpURLConnection response = open(release.url, false, cancelled);
            long length = response.getContentLengthLong();
            if (length > 0 && length != release.size) throw new IOException("O tamanho do ficheiro não corresponde ao publicado.");
            try (InputStream input = response.getInputStream(); OutputStream output = new FileOutputStream(partial)) {
                copyVerified(input, output, release, cancelled, progress);
            }
            checkCancelled(cancelled);
            Files.move(partial.toPath(), ready.toPath(), StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            return ready;
        } finally { cancel(); Files.deleteIfExists(partial.toPath()); }
    }
    static void copyVerified(InputStream input, OutputStream output, ReleaseUpdate release, AtomicBoolean cancelled, Progress progress) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        long received = 0, deadline = System.nanoTime() + 600_000_000_000L;
        byte[] buffer = new byte[64 * 1024]; int count;
        checkCancelled(cancelled);
        while ((count = input.read(buffer)) != -1) {
            checkCancelled(cancelled);
            if (System.nanoTime() > deadline) throw new IOException("A transferência demorou demasiado. Tenta novamente.");
            received += count;
            if (received > release.size) throw new IOException("O ficheiro excede o tamanho publicado.");
            output.write(buffer, 0, count); digest.update(buffer, 0, count); progress.bytes(received);
        }
        checkCancelled(cancelled);
        if (received != release.size) throw new IOException("A transferência ficou incompleta.");
        StringBuilder hash = new StringBuilder();
        for (byte value : digest.digest()) hash.append(String.format(java.util.Locale.ROOT, "%02x", value & 255));
        if (!release.sha256.contentEquals(hash)) throw new IOException("O SHA-256 não corresponde ao publicado. O ficheiro foi recusado.");
    }
    private static void checkCancelled(AtomicBoolean cancelled) {
        if (cancelled.get() || Thread.currentThread().isInterrupted()) throw new CancellationException();
    }
}
