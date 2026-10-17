package pt.molduratv;

import android.content.ContentResolver;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.media3.common.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

@androidx.media3.common.util.UnstableApi
final class ContentDetails {
    static String tracks(Format video,Format audio,long duration) {
        StringBuilder text=new StringBuilder();
        if(duration!=C.TIME_UNSET && duration>=0) text.append("Duração: ").append(String.format(Locale.ROOT,"%d:%02d",duration/60000,(duration/1000)%60)).append('\n');
        if(video!=null) {
            text.append("Vídeo: ").append(video.sampleMimeType==null?"desconhecido":video.sampleMimeType);
            if(video.codecs!=null) text.append(" · ").append(video.codecs);
            if(video.width>0 && video.height>0) text.append("\nResolução do vídeo: ").append(video.width).append('×').append(video.height);
            if(video.frameRate>0) text.append(String.format(Locale.ROOT," · %.2f fps",video.frameRate));
            if(video.colorInfo!=null) {
                int transfer=video.colorInfo.colorTransfer;
                text.append("\nSinal: ").append(transfer==C.COLOR_TRANSFER_ST2084?"HDR (PQ)":transfer==C.COLOR_TRANSFER_HLG?"HDR (HLG)":transfer==C.COLOR_TRANSFER_SDR?"SDR":"transferência de cor não identificada");
            }
            text.append('\n');
        }
        if(audio!=null) {
            text.append("Áudio: ").append(audio.sampleMimeType==null?"desconhecido":audio.sampleMimeType);
            if(audio.channelCount>0) text.append(" · ").append(audio.channelCount).append(" canal(is)");
            if(audio.sampleRate>0) text.append(" · ").append(audio.sampleRate).append(" Hz"); text.append('\n');
        }
        return text.toString();
    }
    static String read(ContentResolver resolver,PhotoMetadata metadata,MediaEntry entry,String tracks,String side) {
        PhotoMetadata.Info info=metadata.inspect(entry);
        StringBuilder text=new StringBuilder(side).append(entry.name).append("\nFormato: ").append(entry.mime).append('\n');
        if(entry.isRaw()) text.append("RAW: pré-visualização JPEG incorporada, sem revelação do sensor.\n");
        if(entry.motionVideo) text.append("Vídeo incorporado em Motion Photo\n");
        if(entry.size>=0) text.append(String.format(Locale.ROOT,"Tamanho original: %.2f MiB\n",entry.size/(1024d*1024)));
        if(entry.isPhoto()) try(ParcelFileDescriptor descriptor=resolver.openFileDescriptor(Uri.parse(entry.playbackUri()),"r")) {
            if(descriptor!=null) { BitmapFactory.Options bounds=new BitmapFactory.Options(); bounds.inJustDecodeBounds=true; BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor(),null,bounds);
                if(bounds.outWidth>0 && bounds.outHeight>0) text.append(entry.isRaw()?"Pré-visualização: ":"Dimensões do ficheiro: ").append(bounds.outWidth).append('×').append(bounds.outHeight).append('\n');
            }
        } catch(java.io.IOException | RuntimeException ignored) { }
        String proportion=PlaybackFilter.ratioKey(info.ratio); if(proportion.equals("other")) proportion="outra";
        text.append("Proporção de apresentação: ").append(info.ratio>0?proportion+String.format(Locale.ROOT," (%.3f)",info.ratio):"desconhecida").append('\n');
        text.append("Data de captura: ").append(date(info.captured)).append('\n');
        text.append("Modificação do ficheiro: ").append(date(entry.modified)).append('\n').append(tracks);
        return text.toString();
    }
    private static String date(long value) { return value>0?DateTimeFormatter.ofPattern("dd/MM/uuuu HH:mm:ss").withZone(ZoneId.systemDefault()).format(Instant.ofEpochMilli(value)):"não disponível"; }
}
