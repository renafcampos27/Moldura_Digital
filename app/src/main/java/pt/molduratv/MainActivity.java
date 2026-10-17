package pt.molduratv;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.net.Uri;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.exoplayer.DefaultRenderersFactory;
import androidx.media3.exoplayer.image.BitmapFactoryImageDecoder;
import androidx.media3.ui.*;
import androidx.activity.ComponentActivity;
import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.core.view.WindowInsetsControllerCompat;
import java.util.*;
import java.util.concurrent.*;

@androidx.media3.common.util.UnstableApi
public final class MainActivity extends ComponentActivity {
    private static final int BG = Color.rgb(16, 25, 31), PANEL = Color.rgb(27, 43, 51);
    private static final int ACCENT = Color.rgb(113, 226, 194), TEXT = Color.rgb(238, 246, 248);
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final ExecutorService scanWorker = Executors.newSingleThreadExecutor();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private final List<MediaEntry> library = new ArrayList<>();
    private AppSettings settings;
    private BackupController backups;
    private StorageController storage;
    private UpdateController updates;
    private CollectionIndex index;
    private Diagnostics diagnostics;
    private StorageWatcher storageWatcher;
    private boolean bootstrapping = true, cachedCollection, pendingRescan, startedOnce;
    private volatile int volumeGeneration;
    private final List<PhotoSlides.Slide> recoveredSlides = new ArrayList<>();
    private final ActivityResultLauncher<Intent> folderPicker = registerForActivityResult(
        new ActivityResultContracts.StartActivityForResult(), result -> acceptFolder(result.getResultCode(), result.getData()));
    private boolean backupBusy;
    private LinearLayout home;
    private TextView summary, status;
    private Button playButton, refreshButton, addButton;
    private FrameLayout playback;
    private PlayerView playerView;
    private ExoPlayer player;
    private PlayerView companionView;
    private PlayerControlView controls;
    private TextView caption;
    private Button infoButton;
    private int infoGeneration;
    private ExoPlayer companion;
    private PhotoMetadata photoMetadata;
    private List<PhotoSlides.Slide> slides = new ArrayList<>();
    private boolean companionLoading, playIntent;
    private boolean firstFinished, secondFinished;
    private boolean companionHasFocus;
    private boolean mainHasFocus = true, pairChanging, leftEndQueued, rightEndQueued;
    private VideoPairPlayback videoPair;
    private VideoPairPlayback.Phase savedVideoPhase = VideoPairPlayback.Phase.LEFT;
    private boolean savedFirstFinished, savedSecondFinished;
    private long savedCompanionPosition;
    private double firstDisplayRatio, secondDisplayRatio;
    private boolean firstSizeConfirmed, secondSizeConfirmed, geometryLocked;
    private PresentationCover cover;
    private boolean transitionBusy, firstFrameReady, secondFrameReady;
    private volatile int presentationGeneration;
    private final Runnable hideControls = () -> { if (controls != null) controls.hide(); if (caption != null) caption.setVisibility(View.GONE); if(infoButton!=null) infoButton.setVisibility(View.GONE); };
    private final Runnable companionTimeout = () -> dropCompanion("O segundo conteúdo demorou demasiado a abrir.");
    private boolean scanning, scanAgain, presenting, stopped, resumePlaying;
    private final Runnable rescanStorage = () -> { if (!stopped) scan(presenting); };
    private int scanGeneration, savedIndex;
    private long savedPosition;
    private final Runnable timeout = () -> skipFailed("O conteúdo demorou demasiado a abrir.");

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        settings = new AppSettings(this);
        index = CollectionIndex.get(this);
        diagnostics = new Diagnostics(this);
        photoMetadata = new PhotoMetadata(getContentResolver(),index);
        storageWatcher = new StorageWatcher(this,this::storageChanged);
        storage = new StorageController(this, this::scan);
        backups = new BackupController(this, settings, worker, new BackupController.Host() {
            @Override public boolean canEdit() { return !scanning && !presenting && !backupBusy; }
            @Override public void busy(boolean value) { backupBusy = value; updateSummary(); }
            @Override public void changed() { updateSummary(); scan(); }
        }, storage, state);
        updates = new UpdateController(this, state);
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() {
                if (presenting) endPresentation(); else finish();
            }
        });
        WindowInsetsControllerCompat insets = WindowCompat.getInsetsController(getWindow(), getWindow().getDecorView());
        insets.hide(WindowInsetsCompat.Type.systemBars());
        insets.setSystemBarsBehavior(WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
        buildHome();
        loadIndex();
    }

    private int dp(int value) { return Math.round(value * getResources().getDisplayMetrics().density); }
    private TextView text(String value, int size, int color) {
        TextView view = new TextView(this); view.setText(value); view.setTextSize(size);
        view.setTextColor(color); view.setPadding(0, dp(5), 0, dp(5)); return view;
    }
    private GradientDrawable background(int color, int stroke) {
        GradientDrawable drawable = new GradientDrawable(); drawable.setColor(color);
        drawable.setCornerRadius(dp(12)); if (stroke != 0) drawable.setStroke(dp(3), stroke); return drawable;
    }
    private Button button(String title, Runnable click) {
        Button view = new Button(this); view.setText(title); view.setTextSize(16);
        view.setTextColor(TEXT); view.setAllCaps(false); view.setPadding(dp(18), dp(8), dp(18), dp(8));
        view.setBackground(background(PANEL, 0));
        view.setOnFocusChangeListener((v, focused) -> {
            view.setBackground(background(focused ? Color.rgb(37, 70, 73) : PANEL, focused ? ACCENT : 0));
            view.setTextColor(focused ? ACCENT : TEXT);
        });
        view.setOnClickListener(v -> click.run());
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(-2, dp(56));
        params.setMargins(0, dp(6), dp(12), dp(6)); view.setLayoutParams(params); return view;
    }

    private void buildHome() {
        ScrollView scroll = new ScrollView(this); scroll.setFillViewport(true); scroll.setBackgroundColor(BG);
        home = new LinearLayout(this); home.setOrientation(LinearLayout.VERTICAL);
        home.setPadding(dp(48), dp(28), dp(48), dp(28)); scroll.addView(home);
        home.addView(text("A TUA COLEÇÃO, NO GRANDE ECRÃ", 12, ACCENT));
        TextView title = text("MolduraTV", 42, TEXT); title.setTypeface(null, Typeface.BOLD); home.addView(title);
        home.addView(text("Fotografias e vídeos. Uma apresentação com as tuas memórias.", 19, TEXT));
        summary = text("", 16, ACCENT); home.addView(summary);
        LinearLayout actions = new LinearLayout(this);
        playButton = button("▶  Iniciar apresentação", this::startPresentation);
        addButton = button("+  Adicionar pasta", this::pickFolder);
        refreshButton = button("Atualizar", this::scan);
        actions.addView(playButton); actions.addView(addButton); actions.addView(refreshButton); home.addView(actions);
        LinearLayout options = new LinearLayout(this);
        options.addView(button("Opções da apresentação", this::options));
        options.addView(button("Gerir pastas", this::manageFolders));
        options.addView(button("Backups", () -> backups.showMenu()));
        options.addView(button("Atualizações", () -> updates.check()));
        home.addView(options);
        LinearLayout storageActions = new LinearLayout(this);
        storageActions.addView(button("Permissões", () -> storage.showPermissions()));
        storageActions.addView(button("Seletor do Android / rede", this::pickSystemFolder));
        storageActions.addView(button("Diagnóstico", () -> diagnostics.show(this,this::rebuildIndex)));
        home.addView(storageActions);
        home.addView(button("Protetor de ecrã Android TV", () -> ScreensaverSettings.show(this)));
        status = text("", 15, TEXT); home.addView(status);
        home.addView(text("USB · Disco externo · Armazenamento interno · Pastas da app", 14, ACCENT));
        home.addView(text("Escolhe as pastas na própria MolduraTV. As subpastas também entram na coleção.\n"
            + "Para locais de rede disponibilizados por outras apps, usa Seletor do Android / rede.", 14, Color.LTGRAY));
        home.addView(text("Durante a apresentação: OK abre os controlos · ← / → anterior / seguinte\n"
            + "INFO / Menu mostra informação · Reproduzir/Pausa controla a apresentação\nVoltar regressa à coleção", 14, Color.LTGRAY));
        setContentView(scroll); updateSummary(); addButton.requestFocus();
    }

    private void updateSummary() {
        if (summary == null) return;
        int photos = (int) library.stream().filter(MediaEntry::isPhoto).count();
        int videos = library.size() - photos, folders = settings.roots.size();
        summary.setText(getString(R.string.collection_summary,
            getResources().getQuantityString(R.plurals.folders, folders, folders),
            getResources().getQuantityString(R.plurals.photos, photos, photos),
            getResources().getQuantityString(R.plurals.videos, videos, videos), settings.seconds));
        playButton.setEnabled((!scanning || cachedCollection) && !presenting && !backupBusy && !library.isEmpty());
        playButton.setAlpha(playButton.isEnabled() ? 1f : 0.4f);
        refreshButton.setEnabled(!scanning && !presenting && !backupBusy); addButton.setEnabled(!scanning && !presenting && !backupBusy);
    }

    private void pickFolder() {
        if (presenting) return;
        storage.choose(StorageController.Mode.FOLDER, null, null, folder -> {
            try { addFolder(FolderSource.fromFile(folder)); }
            catch (java.io.IOException | IllegalArgumentException error) { message("Não foi possível guardar esta pasta."); }
        });
    }
    private void addFolder(String root) {
        FolderSource.validate(root, true);
        if (settings.roots.contains(root)) { message("Esta pasta já faz parte da coleção."); return; }
        if (settings.roots.size() >= SettingsBackup.MAX_FOLDERS) { message("A coleção já atingiu o limite de 512 pastas."); return; }
        settings.roots.add(root); settings.save(); scan();
    }
    private void pickSystemFolder() {
        if (presenting) return;
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);
        try { folderPicker.launch(intent); }
        catch (ActivityNotFoundException e) {
            message("O seletor do Android não está disponível nesta box. Para armazenamento interno e discos, usa Adicionar pasta. Para rede, é necessário um seletor compatível com o fornecedor de rede.");
        }
    }

    private void acceptFolder(int result, Intent data) {
        if (result != RESULT_OK || data == null || data.getData() == null) return;
        Uri uri = data.getData();
        try {
            getContentResolver().takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION);
            addFolder(uri.toString());
        } catch (SecurityException e) { message("Não foi possível guardar o acesso. Escolhe outra pasta ou fornecedor."); }
    }

    private void loadIndex() {
        List<String> roots = new ArrayList<>(settings.roots);
        scanWorker.execute(() -> {
            List<MediaEntry> saved = index.load(roots);
            handler.post(() -> {
                if (isDestroyed()) return;
                bootstrapping=false;
                if (roots.equals(settings.roots) && !saved.isEmpty()) {
                    library.clear(); library.addAll(saved); cachedCollection=true; updateSummary();
                }
                scanAgain=false; scan();
            });
        });
    }

    private void scan() {
        scan(false);
    }
    private void scan(boolean duringPresentation) {
        if (bootstrapping) { scanAgain=true; return; }
        if (scanning) { scanAgain = true; return; }
        if (presenting && !duringPresentation || backupBusy) { pendingRescan=true; return; }
        scanning = true; int generation = ++scanGeneration;
        int volumes=volumeGeneration;
        status.setText(cachedCollection ? R.string.verifying_index : R.string.searching); updateSummary();
        List<String> roots = new ArrayList<>(settings.roots);
        scanWorker.execute(() -> {
            LibraryScanner.Result found = LibraryScanner.scan(getContentResolver(), roots);
            if (Thread.currentThread().isInterrupted()) return;
            if (volumes!=volumeGeneration) {
                handler.post(() -> { if(!isDestroyed() && generation==scanGeneration) { scanning=false; scan(presenting); } });
                return;
            }
            List<MediaEntry> ordered = CollectionOrder.arrange(found.entries,0,new Random());
            boolean stored = index.save(roots,ordered);
            handler.post(() -> {
                if (isDestroyed() || generation != scanGeneration) return;
                library.clear(); library.addAll(ordered);
                scanning = false; cachedCollection=false; updateSummary();
                if (!stored) diagnostics.record("Coleção","Índice","Não foi possível atualizar o índice; a coleção continua disponível nesta sessão.");
                for (int i=0; i<Math.min(20,found.warnings.size()); i++) diagnostics.record("Pastas selecionadas","Acesso à origem",found.warnings.get(i));
                status.setText(!found.warnings.isEmpty() ? found.warnings.get(0)
                    : library.isEmpty() ? "Adiciona uma pasta com fotografias ou vídeos para começar."
                    : "Coleção pronta. Os vídeos mantêm a sua duração e o respetivo som.");
                if (!library.isEmpty()) playButton.requestFocus(); else addButton.requestFocus();
                if (presenting) prepareRecoveredSources();
                if (scanAgain) { scanAgain = false; scan(presenting); }
            });
        });
    }

    private void options() {
        if (presenting) return;
        if (backupBusy) { message("Aguarda que o backup termine."); return; }
        String[] values = {"Fotografias: " + settings.seconds + " segundos",
            "Ordem: " + new String[]{"Nome", "Data de modificação (antigas primeiro)", "Aleatória","Data de captura (antigas primeiro)"}[settings.order],
            "Enquadramento (vídeos e modo clássico): " + (settings.crop ? "Preencher com corte" : "Mostrar inteira"),
            "Som dos vídeos: " + (settings.muted ? "Desligado" : "Ligado"),
            "Repetir coleção: " + (settings.loop ? "Sim" : "Não"),
            "Apresentação: " + PhotoSlides.modeName(settings.layoutMode),
            "Motion Photos: " + MotionPhotos.modeName(settings.motionMode),
            "Filtros de reprodução: " + settings.filter.summary(),
            "Live Photos (fotografia + MOV): " + LivePhotos.modeName(settings.liveMode)};
        new AlertDialog.Builder(this).setTitle("Opções da apresentação").setItems(values, (dialog, which) -> {
            if (which == 7) {
                new FilterDialog(this,settings).show();
            } else if(which==8) {
                new AlertDialog.Builder(this).setTitle("Live Photos: mesmo nome e mesma pasta")
                    .setSingleChoiceItems(LivePhotos.LABELS,settings.liveMode,(d,selected)-> {
                        settings.liveMode=selected; settings.save(); d.dismiss(); options();
                    }).setNegativeButton("Cancelar",null).show();
            } else if (which == 0) {
                int[] durations = {5, 10, 30, 60, 120, 300};
                String[] labels = {"5 segundos", "10 segundos", "30 segundos", "1 minuto", "2 minutos", "5 minutos"};
                new AlertDialog.Builder(this).setTitle("Tempo por fotografia").setItems(labels, (d, selected) -> {
                    settings.seconds = durations[selected]; settings.save(); updateSummary();
                }).show();
            } else if (which == 1) {
                new AlertDialog.Builder(this).setTitle("Ordem da coleção")
                    .setItems(new String[]{"Nome", "Data de modificação (antigas primeiro)", "Aleatória","Data de captura (antigas primeiro)"}, (d, selected) -> {
                        settings.order = selected; settings.save();
                    }).show();
            } else if (which == 5) {
                new AlertDialog.Builder(this).setTitle("Divisão do ecrã")
                    .setSingleChoiceItems(new String[]{"Automático", "Individual", "Dois lado a lado"}, settings.layoutMode, (d, selected) -> {
                        settings.layoutMode = selected; settings.save(); d.dismiss(); options();
                    }).setNegativeButton("Cancelar", null).show();
            } else if (which == 6) {
                new AlertDialog.Builder(this).setTitle("Fotografias com movimento incorporado")
                    .setSingleChoiceItems(new String[]{"Fotografia","Movimento","Fotografia e movimento"},settings.motionMode,(d,selected) -> {
                        settings.motionMode=selected; settings.save(); d.dismiss(); options();
                    }).setNegativeButton("Cancelar",null).show();
            } else {
                if (which == 2) settings.crop = !settings.crop;
                if (which == 3) settings.muted = !settings.muted;
                if (which == 4) settings.loop = !settings.loop;
                settings.save(); options();
            }
        }).setNegativeButton("Fechar", null).show();
    }

    private void manageFolders() {
        if (presenting) return;
        if (scanning || backupBusy) { message("Aguarda que a operação atual termine."); return; }
        if (settings.roots.isEmpty()) { message("Ainda não adicionaste pastas."); return; }
        String[] labels = settings.roots.stream().map(FolderSource::display).toArray(String[]::new);
        new AlertDialog.Builder(this).setTitle("Escolhe uma pasta para retirar da coleção")
            .setItems(labels, (d, which) -> new AlertDialog.Builder(this)
                .setMessage("Retirar esta pasta da coleção? Os ficheiros são preservados.")
                .setPositiveButton("Retirar", (confirm, ok) -> {
                    String root = settings.roots.remove(which); settings.save();
                    try { if (!FolderSource.isLocal(root)) getContentResolver().releasePersistableUriPermission(Uri.parse(root), Intent.FLAG_GRANT_READ_URI_PERMISSION); }
                    catch (SecurityException ignored) { }
                    scan();
                }).setNegativeButton("Cancelar", null).show())
            .setNegativeButton("Fechar", null).show();
    }

    private void startPresentation() {
        if (library.isEmpty() || scanning && !cachedCollection || backupBusy || presenting) return;
        List<MediaEntry> entries = new ArrayList<>(library);
        int order = settings.order;
        recoveredSlides.clear();
        slides = new ArrayList<>(); playback = null;
        presenting = true; savedIndex = 0; savedPosition = 0; resumePlaying = true;
        savedCompanionPosition = 0; savedFirstFinished = false; savedSecondFinished = false;
        savedVideoPhase = VideoPairPlayback.Phase.LEFT;
        int generation = ++presentationGeneration;
        updateSummary(); status.setText(R.string.preparing_photos);
        int layoutMode = settings.layoutMode;
        int motionMode = settings.motionMode;
        int liveMode=settings.liveMode;
        PlaybackFilter filter = settings.filter;
        android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
        double screenRatio = (double) metrics.widthPixels / metrics.heightPixels;
        worker.execute(() -> {
            List<PhotoSlides.Slide> prepared = prepareSlides(entries,order,layoutMode,motionMode,liveMode,screenRatio,generation,filter);
            handler.post(() -> {
                if (isDestroyed() || !presenting || generation != presentationGeneration) return;
                if (prepared.isEmpty()) {
                    presenting=false; updateSummary();
                    message("Nenhum conteúdo corresponde aos filtros. Revê Opções da apresentação → Filtros de reprodução."); return;
                }
                slides = prepared; openPresentation();
            });
        });
    }

    private List<PhotoSlides.Slide> prepareSlides(List<MediaEntry> entries,int order,int layoutMode,int motionMode,int liveMode,double screenRatio,int generation,PlaybackFilter filter) {
        return SessionPreparer.prepare(this,entries,order,layoutMode,motionMode,liveMode,filter,screenRatio,photoMetadata,
            () -> generation!=presentationGeneration || Thread.currentThread().isInterrupted());
    }

    private void prepareRecoveredSources() {
        pendingRescan=false;
        Set<String> existing=new HashSet<>();
        for (PhotoSlides.Slide slide : slides) { existing.add(slide.first.uri); if(slide.paired()) existing.add(slide.second.uri); }
        for (PhotoSlides.Slide slide : recoveredSlides) { existing.add(slide.first.uri); if(slide.paired()) existing.add(slide.second.uri); }
        List<MediaEntry> added=new ArrayList<>(); for(MediaEntry entry:library) if(!existing.contains(entry.uri)) added.add(entry);
        if(added.isEmpty()) return;
        // Association needs the complete collection, including companions already in the session.
        List<MediaEntry> collection=new ArrayList<>(library);
        int generation=presentationGeneration, volumes=volumeGeneration, order=settings.order, layout=settings.layoutMode, motion=settings.motionMode;
        PlaybackFilter filter=settings.filter;
        int liveMode=settings.liveMode;
        android.util.DisplayMetrics display=getResources().getDisplayMetrics(); double ratio=(double)display.widthPixels/display.heightPixels;
        worker.execute(() -> {
            List<PhotoSlides.Slide> prepared=prepareSlides(collection,order,layout,motion,liveMode,ratio,generation,filter);
            handler.post(() -> {
                if(!presenting || isDestroyed() || generation!=presentationGeneration || volumes!=volumeGeneration) return;
                List<PhotoSlides.Slide> known=new ArrayList<>(slides); known.addAll(recoveredSlides);
                recoveredSlides.addAll(SessionRecovery.onlyNew(prepared,known));
            });
        });
    }

    private void openPresentation() {
        playback = new FrameLayout(this) {
            @Override public boolean dispatchKeyEvent(KeyEvent event) {
                return handlePresentationKey(event) || super.dispatchKeyEvent(event);
            }
        };
        playback.setBackgroundColor(Color.BLACK);
        playerView = new PlayerView(this);
        playerView.setUseController(false);
        playerView.setKeepContentOnPlayerReset(true);
        playerView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        playback.addView(playerView, new FrameLayout.LayoutParams(-1, -1));
        companionView = new PlayerView(this); companionView.setUseController(false);
        companionView.setShowBuffering(PlayerView.SHOW_BUFFERING_WHEN_PLAYING);
        companionView.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT);
        companionView.setVisibility(View.GONE);
        playback.addView(companionView, new FrameLayout.LayoutParams(-1, -1));
        cover = new PresentationCover(this, handler);
        playback.addView(cover.layer, new FrameLayout.LayoutParams(-1, -1));
        controls = new PlayerControlView(this); controls.setShowTimeoutMs(0); controls.setAnimationEnabled(false);
        playback.addView(controls, new FrameLayout.LayoutParams(-1, -1));
        playback.addOnLayoutChangeListener((v, l, t, r, b, ol, ot, or, ob) -> {
            if (r - l != or - ol || b - t != ob - ot) layoutPhotos();
        });
        playback.setOnClickListener(v -> showControls());
        caption = text("", 16, Color.WHITE); caption.setPadding(dp(32), dp(10), dp(32), dp(10));
        caption.setBackgroundColor(0xAA10191F);
        FrameLayout.LayoutParams label = new FrameLayout.LayoutParams(-1, -2, Gravity.TOP);
        playback.addView(caption, label);
        infoButton=button("Informação",this::showContentInfo);
        FrameLayout.LayoutParams infoLayout=new FrameLayout.LayoutParams(-2,dp(56),Gravity.BOTTOM|Gravity.END); infoLayout.setMargins(dp(16),dp(16),dp(24),dp(92));
        playback.addView(infoButton,infoLayout);
        setContentView(playback); getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        if (!stopped) createPlayer();
        showControls();
    }

    private void showControls() {
        if (controls == null) return;
        controls.show();
        View playPause = controls.findViewById(androidx.media3.ui.R.id.exo_play_pause);
        if (playPause != null) playPause.requestFocus(); else controls.requestFocus();
        caption.setVisibility(View.VISIBLE);
        if(infoButton!=null) infoButton.setVisibility(View.VISIBLE);
        handler.removeCallbacks(hideControls); handler.postDelayed(hideControls, 4000);
    }

    private MediaItem mediaItem(MediaEntry entry) {
        return MediaPlayers.item(entry,settings.seconds);
    }

    private ExoPlayer newPlayer() {
        return MediaPlayers.create(this,settings.layoutMode);
    }

    private void createPlayer() {
        player = newPlayer();
        mainHasFocus = true;
        player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
            .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), true);
        player.setHandleAudioBecomingNoisy(true);
        playerView.setPlayer(player); player.setVolume(settings.muted ? 0 : 1);
        playIntent = resumePlaying;
        // Keep user intent separate from the temporary pause while the second photo loads.
        // Player callbacks may be deferred inside another listener; a transient boolean guard
        // would incorrectly treat that internal pause as a user pause on a later slide.
        controls.setPlayer(new ForwardingPlayer(player) {
            @Override public void setPlayWhenReady(boolean ready) { requestPlaying(ready); }
            @Override public void play() { requestPlaying(true); }
            @Override public void pause() { requestPlaying(false); }
            @Override public void seekToNext() { next(); }
            @Override public void seekToPrevious() { previous(); }
            @Override public void seekToNextMediaItem() { next(); }
            @Override public void seekToPreviousMediaItem() { previous(); }
            @Override public boolean getPlayWhenReady() { return playIntent; }
            @Override public boolean isPlaying() {
                return super.isPlaying() || companion != null && companion.isPlaying();
            }
            @Override public int getPlaybackState() {
                int state = super.getPlaybackState();
                return state == Player.STATE_ENDED && companion != null && !secondFinished
                    ? companion.getPlaybackState() : state;
            }
        });
        player.setRepeatMode(Player.REPEAT_MODE_OFF);
        player.setPauseAtEndOfMediaItems(true);
        List<MediaItem> items = new ArrayList<>();
        for (PhotoSlides.Slide slide : slides) items.add(mediaItem(slide.first));
        player.setMediaItems(items, Math.min(savedIndex, items.size() - 1), savedPosition);
        ExoPlayer source = player;
        player.addListener(new Player.Listener() {
            @Override public void onMediaItemTransition(MediaItem item, int reason) {
                if (player != source) return;
                updateCompanion(false); updateCaption();
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (player != source) return;
                if (state == Player.STATE_ENDED) {
                    firstFinished = true;
                    if (videoPair != null) videoEnded(true);
                }
                syncPlayback(); scheduleAdvance(); refreshTimeouts();
                revealPresentation();
            }
            @Override public void onRenderedFirstFrame() { if (source == player) { firstFrameReady = true; revealPresentation(); } }
            @Override public void onVideoSizeChanged(VideoSize size) {
                if (player == source) confirmVideoSize(true, size);
            }
            @Override public void onPositionDiscontinuity(Player.PositionInfo oldPosition, Player.PositionInfo newPosition, int reason) {
                if (player != source || reason != Player.DISCONTINUITY_REASON_SEEK
                        || oldPosition.mediaItemIndex != newPosition.mediaItemIndex) return;
                if (videoPair != null) return; // Preview restarts must not rewind the audible side.
                firstFinished = false; secondFinished = false;
                if (companion != null) {
                    long duration = companion.getDuration();
                    secondFinished = duration != C.TIME_UNSET && duration > 0 && newPosition.positionMs >= duration;
                    companion.seekTo(secondFinished ? duration : newPosition.positionMs);
                }
                syncPlayback(); refreshTimeouts();
            }
            @Override public void onPlayWhenReadyChanged(boolean ready, int reason) {
                if (player != source) return;
                if (reason == Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    firstFinished = true;
                    if (videoPair != null) videoEnded(true);
                    scheduleAdvance();
                } else if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST) playIntent = ready;
                syncPlayback(); refreshTimeouts();
            }
            @Override public void onPlayerError(PlaybackException error) {
                handler.post(() -> {
                    if (player == source) {
                        PhotoSlides.Slide slide = currentSlide();
                        diagnostics.playback(slide == null ? "Conteúdo" : slide.first.name,error);
                        skipFailed("Não foi possível reproduzir este ficheiro.");
                    }
                });
            }
        });
        updateCompanion(true); player.prepare(); syncPlayback(); updateCaption();
    }

    private void setMainPlaying(boolean playing) {
        if (player != null) player.setPlayWhenReady(playing);
    }

    private void requestPlaying(boolean ready) {
        playIntent = ready; syncPlayback(); refreshTimeouts(); scheduleAdvance();
    }

    private void syncPlayback() {
        if (player == null) return;
        if (pairChanging || transitionBusy) { setMainPlaying(false); if (companion != null) companion.setPlayWhenReady(false); return; }
        boolean peerBuffering = companion != null && companion.getPlaybackState() == Player.STATE_BUFFERING;
        boolean mainBuffering = player.getPlaybackState() == Player.STATE_BUFFERING;
        boolean rightTurn = videoPair != null && videoPair.phase() == VideoPairPlayback.Phase.RIGHT;
        boolean complete = videoPair != null && videoPair.phase() == VideoPairPlayback.Phase.COMPLETE;
        boolean run = playIntent && !companionLoading && !complete;
        player.setVolume(settings.muted || rightTurn || complete ? 0 : 1);
        boolean mainFocus = !rightTurn && !complete;
        if (mainFocus != mainHasFocus) {
            mainHasFocus = mainFocus;
            player.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), mainFocus);
        }
        setMainPlaying(run && !firstFinished && (videoPair != null || !peerBuffering));
        if (companion != null) {
            boolean audible = !settings.muted && rightTurn && !secondFinished;
            companion.setVolume(audible ? 1 : 0);
            if (audible != companionHasFocus) {
                companionHasFocus = audible;
                companion.setAudioAttributes(new AudioAttributes.Builder().setUsage(C.USAGE_MEDIA)
                    .setContentType(C.AUDIO_CONTENT_TYPE_MOVIE).build(), audible);
            }
            companion.setPlayWhenReady(run && !secondFinished && (videoPair != null || !mainBuffering));
        }
    }

    private void refreshTimeouts() {
        handler.removeCallbacks(timeout); handler.removeCallbacks(companionTimeout);
        if (!playIntent || player == null || stopped) return;
        if (!firstFinished && player.getPlaybackState() == Player.STATE_BUFFERING) handler.postDelayed(timeout, 30_000);
        if (companion != null && (companionLoading || companion.getPlaybackState() == Player.STATE_BUFFERING))
            handler.postDelayed(companionTimeout, 30_000);
    }

    private void scheduleAdvance() {
        PhotoSlides.Slide slide = currentSlide();
        boolean finished = videoPair != null ? videoPair.phase() == VideoPairPlayback.Phase.COMPLETE : firstFinished;
        if (slide == null || !finished || !playIntent) return;
        ExoPlayer source = player;
        handler.post(() -> {
            if (source != player || slide != currentSlide() || !playIntent) return;
            if (videoPair != null ? videoPair.phase() == VideoPairPlayback.Phase.COMPLETE : firstFinished) {
                boolean last = player.getCurrentMediaItemIndex() == slides.size() - 1 && recoveredSlides.isEmpty() && !settings.loop;
                next();
                if (last) message("A apresentação terminou.");
            }
        });
    }

    private void videoEnded(boolean left) {
        if (videoPair == null || (left ? leftEndQueued : rightEndQueued)) return;
        if (left) leftEndQueued = true; else rightEndQueued = true;
        VideoPairPlayback phase = videoPair;
        ExoPlayer mainSource = player, peerSource = companion;
        handler.post(() -> {
            if (phase != videoPair || mainSource != player || peerSource != companion || companion == null) return;
            VideoPairPlayback.Action action = phase.ended(left);
            if (action == VideoPairPlayback.Action.RESTART_LEFT || action == VideoPairPlayback.Action.RESTART_RIGHT) {
                // A muted preview restarting must not pause or rewind the audible original.
                if (left) {
                    firstFinished = false; player.seekTo(player.getCurrentMediaItemIndex(), 0); leftEndQueued = false;
                } else {
                    secondFinished = false; companion.seekTo(0); rightEndQueued = false;
                }
                syncPlayback(); refreshTimeouts(); return;
            }
            pairChanging = true;
            setMainPlaying(false); companion.setPlayWhenReady(false);
            player.setVolume(0); companion.setVolume(0);
            try {
                if (action == VideoPairPlayback.Action.START_RIGHT) {
                    firstFinished = false; secondFinished = false;
                    companion.setRepeatMode(Player.REPEAT_MODE_OFF);
                    player.seekTo(player.getCurrentMediaItemIndex(), 0); companion.seekTo(0);
                }
            } finally {
                pairChanging = false; leftEndQueued = false; rightEndQueued = false;
            }
            syncPlayback(); refreshTimeouts(); scheduleAdvance();
        });
    }

    private PhotoSlides.Slide currentSlide() {
        if (player == null) return null;
        int index = player.getCurrentMediaItemIndex();
        return index >= 0 && index < slides.size() ? slides.get(index) : null;
    }

    private void releaseCompanion() {
        handler.removeCallbacks(companionTimeout); companionLoading = false;
        companionHasFocus = false;
        if (companion != null) {
            companionView.setPlayer(null); companion.release(); companion = null;
        }
        if (companionView != null) companionView.setVisibility(View.GONE);
    }

    private void updateCompanion(boolean restoring) {
        releaseCompanion();
        videoPair = null; leftEndQueued = false; rightEndQueued = false;
        firstFinished = restoring && savedFirstFinished;
        secondFinished = restoring && savedSecondFinished;
        PhotoSlides.Slide slide = currentSlide();
        if (slide == null) return;
        firstDisplayRatio = slide.firstRatio; secondDisplayRatio = slide.secondRatio;
        firstSizeConfirmed = false; secondSizeConfirmed = false; geometryLocked = false;
        firstFrameReady = false; secondFrameReady = false;
        if (!slide.paired()) { layoutPhotos(); syncPlayback(); return; }
        if (!slide.first.isPhoto()) videoPair = new VideoPairPlayback(restoring ? savedVideoPhase : VideoPairPlayback.Phase.LEFT);
        companionLoading = true; setMainPlaying(false);
        companion = newPlayer();
        ExoPlayer source = companion;
        source.setHandleAudioBecomingNoisy(true);
        companionView.setPlayer(source); companionView.setVisibility(View.VISIBLE);
        source.setVolume(0);
        source.setRepeatMode(videoPair != null && videoPair.phase() == VideoPairPlayback.Phase.LEFT ? Player.REPEAT_MODE_ONE : Player.REPEAT_MODE_OFF);
        MediaItem secondItem = mediaItem(slide.second);
        if (slide.second.isPhoto()) secondItem = secondItem.buildUpon().setImageDurationMs(settings.seconds * 1000L + 60_000).build();
        source.setMediaItem(secondItem, restoring ? savedCompanionPosition : 0);
        source.addListener(new Player.Listener() {
            @Override public void onRenderedFirstFrame() {
                if (source == companion && slide == currentSlide()) { secondFrameReady = true; revealPresentation(); }
            }
            @Override public void onVideoSizeChanged(VideoSize size) {
                if (source == companion && slide == currentSlide()) confirmVideoSize(false, size);
            }
            @Override public void onPlaybackStateChanged(int state) {
                if (source != companion || slide != currentSlide()) return;
                if (state == Player.STATE_READY && companionLoading) {
                    handler.removeCallbacks(companionTimeout); companionLoading = false;
                }
                if (state == Player.STATE_ENDED) {
                    secondFinished = true; companionLoading = false;
                    if (videoPair != null) videoEnded(false);
                }
                syncPlayback(); scheduleAdvance(); refreshTimeouts();
                revealPresentation();
            }
            @Override public void onPlayerError(PlaybackException error) {
                handler.post(() -> {
                    if (source == companion && slide == currentSlide()) {
                        diagnostics.playback(slide.second.name,error);
                        dropCompanion("Não foi possível reproduzir o par neste equipamento.");
                    }
                });
            }
            @Override public void onPlayWhenReadyChanged(boolean ready, int reason) {
                if (source != companion) return;
                if (reason != Player.PLAY_WHEN_READY_CHANGE_REASON_USER_REQUEST
                        && reason != Player.PLAY_WHEN_READY_CHANGE_REASON_END_OF_MEDIA_ITEM) {
                    playIntent = ready; syncPlayback(); refreshTimeouts();
                }
            }
        });
        layoutPhotos(); source.prepare(); refreshTimeouts();
    }

    private boolean imageReady(PlayerView view) {
        View image = view.findViewById(androidx.media3.ui.R.id.exo_image);
        return image instanceof ImageView && image.getVisibility() == View.VISIBLE && ((ImageView)image).getDrawable() != null;
    }

    private void revealPresentation() {
        if (cover == null || !cover.visible() || transitionBusy || player == null) return;
        PhotoSlides.Slide slide = currentSlide();
        if (slide == null) return;
        boolean left = slide.first.isPhoto() ? imageReady(playerView) && player.getPlaybackState() == Player.STATE_READY : firstFrameReady;
        boolean right = !slide.paired() || companion != null && (slide.second.isPhoto()
            ? imageReady(companionView) && companion.getPlaybackState() == Player.STATE_READY : secondFrameReady);
        if (left && right) cover.clear();
        else playback.postDelayed(() -> { if (slide == currentSlide()) revealPresentation(); }, 100);
    }

    private boolean hdr(ExoPlayer source) {
        Format format = source == null ? null : source.getVideoFormat();
        ColorInfo color = format == null ? null : format.colorInfo;
        return color != null && (color.colorTransfer == C.COLOR_TRANSFER_ST2084 || color.colorTransfer == C.COLOR_TRANSFER_HLG);
    }

    private void transitionTo(int target) {
        if (transitionBusy || player == null) return;
        ExoPlayer source = player;
        PhotoSlides.Slide oldSlide = currentSlide();
        transitionBusy = true; syncPlayback();
        Runnable change = () -> {
            if (player != source || !presenting || stopped) { transitionBusy = false; return; }
            transitionBusy = false;
            if (target == player.getCurrentMediaItemIndex()) updateCompanion(false);
            player.seekTo(target, 0); syncPlayback(); revealPresentation();
        };
        if (cover != null && oldSlide != null && !hdr(player) && !hdr(companion))
            cover.prepare(playerView, oldSlide.first.isPhoto(), companion == null ? null : companionView,
                oldSlide.second == null || oldSlide.second.isPhoto(), change);
        else change.run();
    }

    private void confirmVideoSize(boolean first, VideoSize size) {
        double ratio = PhotoSlides.displayedRatio(size.width, size.height, size.pixelWidthHeightRatio);
        if (ratio <= 0 || geometryLocked) return;
        PhotoSlides.Slide slide = currentSlide();
        if (slide == null || slide.first.isPhoto()) return;
        if (first) { firstDisplayRatio = ratio; firstSizeConfirmed = true; }
        else { secondDisplayRatio = ratio; secondSizeConfirmed = true; }
        if (slide.paired() && ratio >= 1) {
            handler.post(() -> { if (slide == currentSlide()) dropCompanion("O leitor identificou um vídeo horizontal."); });
            return;
        }
        if (!slide.paired() || firstSizeConfirmed && secondSizeConfirmed) {
            if (slide.paired() && settings.layoutMode == PhotoSlides.AUTOMATIC && playback.getHeight() > 0
                    && !PhotoSlides.comfortable(firstDisplayRatio, secondDisplayRatio, (double)playback.getWidth() / playback.getHeight())) {
                handler.post(() -> { if (slide == currentSlide()) dropCompanion("O formato fica mais legível individualmente."); });
                return;
            }
            layoutPhotos(); geometryLocked = slide.paired();
        }
    }

    private void layoutPhotos() {
        PhotoSlides.Slide slide = currentSlide();
        if (slide == null || playback == null) return;
        boolean pair = slide.paired() && companion != null;
        boolean videoPair = pair && !slide.first.isPhoto();
        // A single-file time bar cannot represent two independent video durations.
        controls.setShowFastForwardButton(!videoPair); controls.setShowRewindButton(!videoPair);
        for (int id : new int[]{androidx.media3.ui.R.id.exo_progress, androidx.media3.ui.R.id.exo_position,
                androidx.media3.ui.R.id.exo_duration}) {
            View timeline = controls.findViewById(id);
            if (timeline != null) timeline.setVisibility(videoPair ? View.GONE : View.VISIBLE);
        }
        playerView.setResizeMode(pair || (settings.smartPhotos && (slide.first.isPhoto() || slide.firstRatio > 0 && slide.firstRatio < 1)) || !settings.crop
            ? AspectRatioFrameLayout.RESIZE_MODE_FIT : AspectRatioFrameLayout.RESIZE_MODE_ZOOM);
        int width = playback.getWidth(), height = playback.getHeight();
        if (pair && width > 0 && height > 0) {
            int[] fit = PhotoSlides.fit(width, height, firstDisplayRatio, secondDisplayRatio);
            place(playerView, fit[2], fit[4], fit[0], fit[1]);
            place(companionView, fit[3], fit[4], fit[0] + fit[2], fit[1]);
        } else place(playerView, -1, -1, 0, 0);
    }

    private void place(View view, int width, int height, int start, int top) {
        FrameLayout.LayoutParams old = (FrameLayout.LayoutParams)view.getLayoutParams();
        if (old.width == width && old.height == height && old.getMarginStart() == start && old.topMargin == top) return;
        FrameLayout.LayoutParams params = new FrameLayout.LayoutParams(width, height, Gravity.TOP | Gravity.START);
        params.setMarginStart(start); params.topMargin = top; view.setLayoutParams(params);
    }

    private void dropCompanion(String reason) {
        PhotoSlides.Slide slide = currentSlide();
        if (slide == null || !slide.paired()) return;
        diagnostics.record(slide.first.name+" / "+slide.second.name,"Apresentação individual",reason);
        slides.set(player.getCurrentMediaItemIndex(), new PhotoSlides.Slide(slide.first, slide.firstRatio));
        videoPair = null;
        int index = player.getCurrentMediaItemIndex();
        if (!slide.first.isPhoto()) {
            // Some boxes cannot allocate two video decoders. Retry each original individually.
            slides.add(index + 1, new PhotoSlides.Slide(slide.second, slide.secondRatio));
            player.addMediaItem(index + 1, mediaItem(slide.second));
        }
        releaseCompanion(); layoutPhotos(); syncPlayback(); updateCaption(); scheduleAdvance(); refreshTimeouts();
        revealPresentation();
        Toast.makeText(this, reason + (slide.first.isPhoto() ? " A manter a outra fotografia." : " A reproduzir os vídeos individualmente."), Toast.LENGTH_SHORT).show();
    }

    private void updateCaption() {
        if (player == null || caption == null || player.getCurrentMediaItem() == null) return;
        PhotoSlides.Slide slide = currentSlide();
        String title = slide == null ? "" : slide.first.name + (slide.paired() ? " · " + slide.second.name : "");
        caption.setText(getString(R.string.item_caption, player.getCurrentMediaItemIndex() + 1,
            player.getMediaItemCount(), title));
    }

    private void skipFailed(String reason) {
        if (player == null || !presenting || stopped) return;
        handler.removeCallbacks(timeout);
        int index = player.getCurrentMediaItemIndex();
        MediaItem failed = player.getCurrentMediaItem();
        if (failed == null) return;
        PhotoSlides.Slide slide = currentSlide();
        diagnostics.record(slide == null ? "Conteúdo" : slide.first.name,"Elemento ignorado",reason);
        if (slide != null && !slide.paired() && slide.first.motionVideo && settings.motionMode == MotionPhotos.VIDEO
                && settings.filter.mayAccept(new MediaEntry(slide.first.uri,slide.first.name,slide.first.mime,slide.first.modified,slide.first.size))
                && !settings.filter.needsRatio(new MediaEntry(slide.first.uri,slide.first.name,slide.first.mime,slide.first.modified,slide.first.size))) {
            MediaEntry still = new MediaEntry(slide.first.uri,slide.first.name,slide.first.mime,slide.first.modified,slide.first.size);
            slides.set(index,new PhotoSlides.Slide(still,slide.firstRatio));
            player.replaceMediaItem(index,mediaItem(still)); player.seekTo(index,0); player.prepare(); updateCompanion(false); updateCaption(); return;
        }
        if (slide != null && slide.paired()) {
            if (!slide.first.isPhoto()) {
                long position = player.getCurrentPosition();
                dropCompanion("Não foi possível reproduzir o par neste equipamento.");
                player.seekTo(index, position); player.prepare(); syncPlayback(); return;
            }
            slides.set(index, new PhotoSlides.Slide(slide.second, slide.secondRatio));
            releaseCompanion(); player.replaceMediaItem(index, mediaItem(slide.second));
            player.seekTo(index, 0); player.prepare(); updateCompanion(false); updateCaption(); return;
        }
        slides.remove(index);
        if (slides.isEmpty()) {
            endPresentation(); message("Não foi possível reproduzir os conteúdos. Verifica os formatos e o acesso às pastas."); return;
        }
        if (!settings.loop && index >= player.getMediaItemCount() - 1) {
            endPresentation(); message("A apresentação terminou. O último ficheiro não pôde ser reproduzido."); return;
        }
        Toast.makeText(this, reason + " A passar ao seguinte.", Toast.LENGTH_SHORT).show();
        boolean playing = playIntent;
        releaseCompanion();
        player.removeMediaItem(index);
        player.seekTo(index < player.getMediaItemCount() ? index : 0, 0);
        player.prepare(); playIntent = playing; updateCompanion(false); updateCaption();
    }

    private void endPresentation() {
        presenting = false; presentationGeneration++; releasePlayer();
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        buildHome(); status.setText(R.string.ready); playButton.requestFocus();
        if (pendingRescan) { pendingRescan=false; scan(); }
    }
    private void storageChanged(boolean mounted,String path) {
        if (isDestroyed()) return;
        volumeGeneration++;
        pendingRescan=true;
        String name = path == null ? "Armazenamento" : new java.io.File(path).getName();
        diagnostics.record(name,mounted ? "Disco ligado" : "Disco indisponível",
            mounted ? "A coleção será atualizada; as pastas guardadas são mantidas." : "Os conteúdos dessa origem podem estar temporariamente indisponíveis.");
        if (!mounted && presenting && player != null && path != null) {
            SessionRecovery.Result waiting=SessionRecovery.removeVolume(recoveredSlides,0,path,false);
            recoveredSlides.clear(); recoveredSlides.addAll(waiting.slides);
            PhotoSlides.Slide current = currentSlide();
            int oldIndex = player.getCurrentMediaItemIndex();
            SessionRecovery.Result recovered = SessionRecovery.removeVolume(slides,oldIndex,path,settings.loop);
            if (!recovered.slides.equals(slides)) {
                boolean playing = playIntent;
                long position = player.getCurrentPosition(), peerPosition = companion == null ? 0 : companion.getCurrentPosition();
                VideoPairPlayback.Phase phase = videoPair == null ? VideoPairPlayback.Phase.LEFT : videoPair.phase();
                boolean leftEnded=firstFinished, rightEnded=secondFinished;
                if (recovered.index < 0) {
                    endPresentation();
                    Toast.makeText(this,"O disco ficou indisponível. As pastas guardadas foram mantidas.",Toast.LENGTH_LONG).show();
                } else {
                    boolean same = recovered.slides.get(recovered.index) == current;
                    releasePlayer(); slides=recovered.slides;
                    savedIndex=recovered.index; savedPosition=same ? position : 0; savedCompanionPosition=same ? peerPosition : 0;
                    savedFirstFinished=same && leftEnded; savedSecondFinished=same && rightEnded;
                    savedVideoPhase=same ? phase : VideoPairPlayback.Phase.LEFT; resumePlaying=playing;
                    createPlayer();
                    Toast.makeText(this,"Disco indisponível. A continuar com as outras origens.",Toast.LENGTH_SHORT).show();
                }
            }
        }
        handler.removeCallbacks(rescanStorage); handler.postDelayed(rescanStorage,1000);
    }
    private void rebuildIndex() {
        if (presenting || backupBusy) return;
        cachedCollection=false; updateSummary();
        worker.execute(() -> {
            photoMetadata.invalidate(); boolean cleared=index.clearIndex();
            handler.post(() -> {
                if (isDestroyed()) return;
                if (!cleared) diagnostics.record("Coleção","Índice","Não foi possível limpar o índice privado.");
                scan(presenting);
            });
        });
    }
    private void releasePlayer() {
        handler.removeCallbacks(timeout);
        handler.removeCallbacks(hideControls); releaseCompanion();
        transitionBusy = false;
        if (cover != null) cover.clear();
        if (player != null) {
            controls.setPlayer(null); playerView.setKeepContentOnPlayerReset(false);
            playerView.setPlayer(null); player.release(); player = null;
            playerView.setKeepContentOnPlayerReset(true);
        }
        controls = presenting ? controls : null;
    }
    private void message(String value) { new AlertDialog.Builder(this).setMessage(value).setPositiveButton("OK", null).show(); }
    private void showContentInfo() {
        PhotoSlides.Slide slide=currentSlide(); if(player==null || slide==null) return;
        int token=++infoGeneration,session=presentationGeneration;
        String first=ContentDetails.tracks(player.getVideoFormat(),player.getAudioFormat(),player.getDuration());
        String second=companion==null?"":ContentDetails.tracks(companion.getVideoFormat(),companion.getAudioFormat(),companion.getDuration());
        worker.execute(()-> {
            String details=ContentDetails.read(getContentResolver(),photoMetadata,slide.first,first,slide.paired()?"Esquerda · ":"");
            if(slide.paired()) details+="\n\n"+ContentDetails.read(getContentResolver(),photoMetadata,slide.second,second,"Direita · ");
            photoMetadata.flush(); final String result=details;
            handler.post(()-> { if(!isDestroyed() && !stopped && presenting && session==presentationGeneration && token==infoGeneration && currentSlide()==slide)
                new AlertDialog.Builder(this).setTitle("Informação do conteúdo").setMessage(result).setPositiveButton("Fechar",null).show(); });
        });
    }

    private boolean handlePresentationKey(KeyEvent event) {
        if (presenting && player != null && event.getAction() == KeyEvent.ACTION_DOWN) {
            int key = event.getKeyCode();
            if(key==KeyEvent.KEYCODE_INFO || key==KeyEvent.KEYCODE_MENU) { if(event.getRepeatCount()==0) showContentInfo(); return true; }
            if (key == KeyEvent.KEYCODE_MEDIA_NEXT) { next(); return true; }
            if (key == KeyEvent.KEYCODE_MEDIA_PREVIOUS) { previous(); return true; }
            boolean visible = controls != null && controls.isFullyVisible();
            if (visible) { handler.removeCallbacks(hideControls); handler.postDelayed(hideControls, 4000); }
            if (!visible) {
                if (key == KeyEvent.KEYCODE_DPAD_RIGHT || key == KeyEvent.KEYCODE_MEDIA_NEXT) { next(); return true; }
                if (key == KeyEvent.KEYCODE_DPAD_LEFT || key == KeyEvent.KEYCODE_MEDIA_PREVIOUS) { previous(); return true; }
                if (key == KeyEvent.KEYCODE_DPAD_CENTER || key == KeyEvent.KEYCODE_ENTER || key == KeyEvent.KEYCODE_DPAD_UP || key == KeyEvent.KEYCODE_DPAD_DOWN) {
                    showControls(); return true;
                }
            }
            if (key == KeyEvent.KEYCODE_MEDIA_PLAY_PAUSE) {
                requestPlaying(!playIntent);
                return true;
            }
            if (key == KeyEvent.KEYCODE_MEDIA_PLAY || key == KeyEvent.KEYCODE_MEDIA_PAUSE) {
                requestPlaying(key == KeyEvent.KEYCODE_MEDIA_PLAY);
                return true;
            }
            if (controls != null && controls.dispatchMediaKeyEvent(event)) return true;
        }
        return false;
    }
    private void next() {
        int index = player.getCurrentMediaItemIndex() + 1;
        if (!recoveredSlides.isEmpty()) {
            transitionBusy=true; syncPlayback();
            List<PhotoSlides.Slide> additions=new ArrayList<>(recoveredSlides); recoveredSlides.clear();
            slides.addAll(additions); List<MediaItem> items=new ArrayList<>();
            for(PhotoSlides.Slide slide:additions) items.add(mediaItem(slide.first));
            player.addMediaItems(items); transitionBusy=false; updateCaption();
        }
        if (index < player.getMediaItemCount()) transitionTo(index);
        else if (settings.loop) transitionTo(0);
        else endPresentation();
    }
    private void previous() {
        int index = player.getCurrentMediaItemIndex() - 1;
        int target = index >= 0 ? index : settings.loop ? player.getMediaItemCount() - 1 : 0;
        transitionTo(target);
    }
    @Override protected void onStart() {
        super.onStart(); stopped = false;
        storageWatcher.start();
        if (startedOnce && !bootstrapping) scan(presenting);
        startedOnce=true;
        if (presenting && player == null && playback != null && !slides.isEmpty()) { createPlayer(); showControls(); }
    }
    @Override protected void onStop() {
        stopped = true;
        storageWatcher.stop(); handler.removeCallbacks(rescanStorage);
        if (presenting) resumePlaying = false;
        if (player != null) {
            savedIndex = player.getCurrentMediaItemIndex(); savedPosition = player.getCurrentPosition();
            savedCompanionPosition = companion == null ? 0 : companion.getCurrentPosition();
            savedFirstFinished = firstFinished; savedSecondFinished = secondFinished;
            savedVideoPhase = videoPair == null ? VideoPairPlayback.Phase.LEFT : videoPair.phase();
            // Returning to the app requires an explicit play action, including during preparation.
            releasePlayer();
        }
        super.onStop();
    }
    @Override protected void onDestroy() {
        scanGeneration++; presentationGeneration++; worker.shutdownNow(); scanWorker.shutdownNow(); storageWatcher.stop(); handler.removeCallbacksAndMessages(null); releasePlayer();
        updates.close(); storage.close(); super.onDestroy();
    }
    @Override protected void onSaveInstanceState(Bundle state) {
        backups.saveState(state); updates.saveState(state); super.onSaveInstanceState(state);
    }
}

