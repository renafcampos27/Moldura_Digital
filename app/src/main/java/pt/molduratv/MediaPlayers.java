package pt.molduratv;

import android.content.Context;
import androidx.media3.common.*;
import androidx.media3.exoplayer.*;
import androidx.media3.exoplayer.image.*;

@androidx.media3.common.util.UnstableApi
final class MediaPlayers {
    static ExoPlayer create(Context context,int layout) {
        android.graphics.Point display=androidx.media3.common.util.Util.getCurrentDisplayModeSize(context);
        int edge=ImageBudget.maxEdge(display.x,display.y,Runtime.getRuntime().maxMemory(),layout!=PhotoSlides.INDIVIDUAL,android.os.Process.is64Bit());
        DefaultRenderersFactory renderers=new DefaultRenderersFactory(context) {
            @Override protected ImageDecoder.Factory getImageDecoderFactory(Context ctx) { return new BitmapFactoryImageDecoder.Factory(ctx).setMaxOutputSize(edge); }
        };
        renderers.setEnableDecoderFallback(true); return new ExoPlayer.Builder(context,renderers).build();
    }
    static MediaItem item(MediaEntry entry,int seconds) {
        MediaItem.Builder item=new MediaItem.Builder().setMediaId(entry.uri).setUri(entry.playbackUri()).setMimeType(entry.playbackMime())
            .setMediaMetadata(new MediaMetadata.Builder().setTitle(entry.name).build());
        if(entry.isPhoto()) item.setImageDurationMs(seconds*1000L); return item.build();
    }
    static AudioAttributes audio() { return new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA).setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(); }
}
