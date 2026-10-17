package pt.molduratv;

import android.os.*;
import android.service.dreams.DreamService;
import java.util.*;
import java.util.concurrent.*;

/** Registered and enabled by default. Only Android/the user can choose the system's active dream. */
@androidx.media3.common.util.UnstableApi
public final class MolduraDreamService extends DreamService {
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private DreamPlayback playback;
    private StorageWatcher watcher;
    private SettingsBackup.Data settings;
    private volatile int generation;
    private boolean running;
    private Future<?> preparation;
    private final Runnable refresh=this::load;
    @Override public void onAttachedToWindow() {
        super.onAttachedToWindow(); setInteractive(false); setFullscreen(true); setScreenBright(true);
        if(getWindow()!=null) getWindow().setColorMode(android.content.pm.ActivityInfo.COLOR_MODE_HDR);
    }
    @Override public void onDreamingStarted() {
        super.onDreamingStarted(); if(running) stop(); running=true;
        settings=new AppSettings(this).snapshot(); playback=new DreamPlayback(this,settings); setContentView(playback.root);
        watcher=new StorageWatcher(this,(mounted,path)-> {
            if(!running) return;
            if(!mounted) playback.removeVolume(path);
            generation++; handler.removeCallbacks(refresh); handler.postDelayed(refresh,1000);
        }); watcher.start(); load();
    }
    private void load() {
        if(!running) return;
        int token=++generation;
        if(preparation!=null) preparation.cancel(true);
        SettingsBackup.Data configuration=settings;
        double ratio=(double)getResources().getDisplayMetrics().widthPixels/Math.max(1,getResources().getDisplayMetrics().heightPixels);
        preparation=worker.submit(()-> {
            try {
                LibraryScanner.Result result=LibraryScanner.scan(getContentResolver(),configuration.roots);
                if(token!=generation || Thread.currentThread().isInterrupted()) return;
                PhotoMetadata metadata=new PhotoMetadata(getContentResolver(),CollectionIndex.get(this));
                List<PhotoSlides.Slide> prepared=SessionPreparer.prepare(this,result.entries,configuration.order,configuration.layoutMode,configuration.motionMode,configuration.liveMode,configuration.filter,ratio,metadata,
                    ()->token!=generation || Thread.currentThread().isInterrupted());
                handler.post(()-> { if(running && token==generation) playback.collection(prepared); });
            } catch(RuntimeException error) {
                handler.post(()-> { if(running && token==generation) { new Diagnostics(this).record("Protetor de ecrã","Leitura","Não foi possível preparar a coleção."); playback.collection(Collections.emptyList()); } });
            }
        });
    }
    private void stop() {
        running=false; generation++; handler.removeCallbacksAndMessages(null);
        if(preparation!=null) { preparation.cancel(true); preparation=null; }
        if(watcher!=null) { watcher.stop(); watcher=null; } if(playback!=null) { playback.close(); playback=null; }
    }
    @Override public void onDreamingStopped() { stop(); super.onDreamingStopped(); }
    @Override public void onDetachedFromWindow() { stop(); super.onDetachedFromWindow(); }
    @Override public void onDestroy() { stop(); worker.shutdownNow(); super.onDestroy(); }
}
