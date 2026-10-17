package pt.molduratv;

import android.content.*;
import android.view.*;
import android.widget.*;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.exoplayer.ExoPlayer;
import org.junit.*;
import org.junit.runner.RunWith;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@androidx.media3.common.util.UnstableApi
public final class PlaybackSmokeTest {
    private static Object dreamField(DreamPlayback dream,String name) {
        try { Field field=DreamPlayback.class.getDeclaredField(name); field.setAccessible(true); return field.get(dream); }
        catch(ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    @Test public void infoKeyShowsContentDetailsWithoutChangingThePlaylist() throws Exception {
        seed("root");
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario,a->{ ExoPlayer p=(ExoPlayer)field(a,"player"); return p!=null && p.isPlaying(); },15000);
            java.util.concurrent.CountDownLatch done=new java.util.concurrent.CountDownLatch(1);
            scenario.onActivity(a->{ a.dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_INFO)); ((java.util.concurrent.ExecutorService)field(a,"worker")).execute(done::countDown); });
            assertTrue(done.await(10,java.util.concurrent.TimeUnit.SECONDS));
            androidx.test.espresso.Espresso.onView(androidx.test.espresso.matcher.ViewMatchers.withText("Informação do conteúdo"))
                .check(androidx.test.espresso.assertion.ViewAssertions.matches(androidx.test.espresso.matcher.ViewMatchers.isDisplayed()));
        }
    }
    @Test public void nativeDreamPlaybackRestartsRightWithSoundAndReleasesBothPlayers() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            java.util.concurrent.atomic.AtomicReference<DreamPlayback> holder=new java.util.concurrent.atomic.AtomicReference<>();
            try {
                scenario.onActivity(a->{
                    DreamPlayback dream=new DreamPlayback(a,new SettingsBackup.Data(java.util.Collections.emptyList(),5,0,false,false,false)); holder.set(dream); a.setContentView(dream.root);
                    MediaEntry left=new MediaEntry("content://pt.molduratv.test.fixtures/document/31-short.mp4","left","video/mp4",1);
                    MediaEntry right=new MediaEntry("content://pt.molduratv.test.fixtures/document/32-long.mp4","right","video/mp4",1);
                    dream.collection(java.util.Collections.singletonList(new PhotoSlides.Slide(left,9d/16,right,9d/16)));
                });
                await(scenario,a->{ VideoPairPlayback pair=(VideoPairPlayback)dreamField(holder.get(),"pair"); return pair!=null && pair.phase()==VideoPairPlayback.Phase.RIGHT; },20000);
                scenario.onActivity(a->{ ExoPlayer left=(ExoPlayer)dreamField(holder.get(),"left"),right=(ExoPlayer)dreamField(holder.get(),"right");
                    assertEquals(0,left.getVolume(),.001); assertEquals(1,right.getVolume(),.001); assertTrue(right.getCurrentPosition()<1500);
                });
                await(scenario,a->dreamField(holder.get(),"left")==null,15000);
            } finally {
                scenario.onActivity(a->{ if(holder.get()!=null) { holder.get().close(); assertNull(dreamField(holder.get(),"left")); assertNull(dreamField(holder.get(),"right")); } });
            }
        }
    }
    private Context context;
    private java.util.Map<String, ?> originalSettings;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        originalSettings = new java.util.HashMap<>(context.getSharedPreferences("collection", 0).getAll());
        context.getSharedPreferences("collection", 0).edit().clear().commit();
    }
    @After public void cleanup() {
        android.content.SharedPreferences.Editor editor = context.getSharedPreferences("collection", 0).edit().clear();
        for (java.util.Map.Entry<String, ?> entry : originalSettings.entrySet()) {
            Object value = entry.getValue();
            if (value instanceof String) editor.putString(entry.getKey(), (String) value);
            else if (value instanceof Integer) editor.putInt(entry.getKey(), (Integer) value);
            else if (value instanceof Boolean) editor.putBoolean(entry.getKey(), (Boolean) value);
        }
        editor.commit();
    }
    private void seed(String root) {
        context.getSharedPreferences("collection", 0).edit()
            .putString("roots", "[\"content://pt.molduratv.test.fixtures/tree/" + root + "\"]")
            .putInt("seconds", 5).putBoolean("loop", false).commit();
    }
    private static Object field(MainActivity activity, String name) {
        try { Field field = MainActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(activity); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    private void await(ActivityScenario<MainActivity> scenario, java.util.function.Predicate<MainActivity> condition, long limit) throws Exception {
        long end = System.currentTimeMillis() + limit; AtomicBoolean okay = new AtomicBoolean();
        while (System.currentTimeMillis() < end) {
            scenario.onActivity(activity -> okay.set(condition.test(activity)));
            if (okay.get()) return;
            Thread.sleep(100);
        }
        fail("Condition did not become true before timeout");
    }
    private void start(ActivityScenario<MainActivity> scenario) throws Exception {
        await(scenario, activity -> ((Button) field(activity, "playButton")).isEnabled(), 15000);
        scenario.onActivity(activity -> ((Button) field(activity, "playButton")).performClick());
    }
    @Test public void mixedPlaylistPlaysPhotoThenFullVideoThenPhotoAndEnds() throws Exception {
        seed("root");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer) field(activity,"player"); return p != null && p.isPlaying() && p.getCurrentMediaItemIndex() == 0; }, 20000);
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer) field(activity,"player"); return p != null && p.isPlaying() && p.getCurrentMediaItemIndex() == 1; }, 15000);
            scenario.onActivity(activity -> assertEquals(3000, ((ExoPlayer) field(activity,"player")).getDuration(), 150));
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer) field(activity,"player"); return p != null && p.isPlaying() && p.getCurrentMediaItemIndex() == 2; }, 12000);
            await(scenario, activity -> !(boolean) field(activity,"presenting"), 12000);
        }
    }
    @Test public void invalidPhotoIsSkippedAndValidPhotoStillPlays() throws Exception {
        seed("broken");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer p = (ExoPlayer) field(activity,"player");
                return p != null && p.isPlaying() && p.getMediaItemCount() == 1;
            }, 20000);
        }
    }
    @Test public void leavingActivityReleasesPlayerAndReturningStaysPaused() throws Exception {
        seed("root");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer) field(activity,"player"); return p != null && p.isPlaying(); }, 20000);
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            scenario.onActivity(activity -> assertNull(field(activity,"player")));
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            scenario.onActivity(activity -> assertFalse(((ExoPlayer) field(activity,"player")).getPlayWhenReady()));
        }
    }
    @Test public void smartPairUsesOneIntervalThenOddPhotoThenFullVideo() throws Exception {
        seed("pairs");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity, "player"), peer = (ExoPlayer) field(activity, "companion");
                return main != null && peer != null && main.isPlaying() && peer.isPlaying();
            }, 20000);
            scenario.onActivity(activity -> {
                ExoPlayer main = (ExoPlayer) field(activity, "player");
                assertEquals(3, main.getMediaItemCount()); assertEquals(5000, main.getDuration());
                androidx.media3.ui.PlayerView left = (androidx.media3.ui.PlayerView) field(activity, "playerView");
                androidx.media3.ui.PlayerView right = (androidx.media3.ui.PlayerView) field(activity, "companionView");
                assertEquals(androidx.media3.ui.AspectRatioFrameLayout.RESIZE_MODE_FIT, left.getResizeMode());
                assertEquals(left.getRight(), right.getLeft()); assertEquals(left.getHeight(), right.getHeight());
                assertEquals(View.VISIBLE, right.getVisibility());
            });
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer) field(activity,"player"); return p != null && p.isPlaying() && p.getCurrentMediaItemIndex() == 1; }, 12000);
            scenario.onActivity(activity -> assertNull(field(activity, "companion")));
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer) field(activity,"player"); return p != null && p.isPlaying() && p.getCurrentMediaItemIndex() == 2; }, 12000);
            scenario.onActivity(activity -> assertEquals(3000, ((ExoPlayer) field(activity,"player")).getDuration(), 150));
            await(scenario, activity -> !(boolean) field(activity,"presenting"), 10000);
        }
    }
    @Test public void leavingPairReleasesBothPlayersAndReturningStaysPaused() throws Exception {
        seed("pairs");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer p = (ExoPlayer) field(activity,"player"), peer = (ExoPlayer) field(activity,"companion");
                return p != null && peer != null && p.isPlaying() && peer.isPlaying();
            }, 20000);
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            scenario.onActivity(activity -> { assertNull(field(activity,"player")); assertNull(field(activity,"companion")); });
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            await(scenario, activity -> field(activity, "companion") != null && !(boolean) field(activity, "companionLoading"), 15000);
            scenario.onActivity(activity -> {
                assertFalse(((ExoPlayer) field(activity,"player")).getPlayWhenReady());
                assertFalse(((ExoPlayer) field(activity,"companion")).getPlayWhenReady());
            });
        }
    }
    @Test public void automaticTransitionToAnotherPairKeepsPlayingAndMediaPauseAffectsBoth() throws Exception {
        seed("multiplepairs");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity, "player"), peer = (ExoPlayer) field(activity, "companion");
                return main != null && peer != null && main.isPlaying() && peer.isPlaying() && main.getCurrentMediaItemIndex() == 1;
            }, 20000);
            scenario.onActivity(activity -> ((View) field(activity, "playback"))
                .dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PAUSE)));
            scenario.onActivity(activity -> {
                assertFalse(((ExoPlayer) field(activity, "player")).getPlayWhenReady());
                assertFalse(((ExoPlayer) field(activity, "companion")).getPlayWhenReady());
                ((View) field(activity, "playback")).dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
            });
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity, "player"), peer = (ExoPlayer) field(activity, "companion");
                return main != null && peer != null && main.isPlaying() && peer.isPlaying();
            }, 10000);
        }
    }
    @Test public void rightRestartsWithSoundAndResumeRestoresTheRightTurnAndBothPositions() throws Exception {
        seed("videopairs");
        java.util.concurrent.atomic.AtomicReference<int[]> geometry = new java.util.concurrent.atomic.AtomicReference<>();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> (boolean)field(activity,"geometryLocked"),15000);
            scenario.onActivity(activity -> {
                View left = (View)field(activity,"playerView"), right = (View)field(activity,"companionView");
                geometry.set(new int[]{left.getLeft(),left.getTop(),left.getWidth(),left.getHeight(),right.getLeft(),right.getWidth()});
            });
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player"), peer = (ExoPlayer) field(activity,"companion");
                VideoPairPlayback phase = (VideoPairPlayback)field(activity,"videoPair");
                return main != null && peer != null && phase != null && phase.phase() == VideoPairPlayback.Phase.RIGHT
                    && main.isPlaying() && peer.isPlaying() && peer.getCurrentPosition() < 1000;
            }, 20000);
            scenario.onActivity(activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player"), peer = (ExoPlayer) field(activity,"companion");
                assertEquals(0, main.getCurrentMediaItemIndex()); assertEquals(2, main.getMediaItemCount());
                assertTrue(main.isPlaying()); assertEquals(0f, main.getVolume(), 0); assertEquals(1f, peer.getVolume(), 0);
                assertTrue(peer.getCurrentPosition() < 1500);
                View left = (View)field(activity,"playerView"), right = (View)field(activity,"companionView");
                assertArrayEquals(geometry.get(),new int[]{left.getLeft(),left.getTop(),left.getWidth(),left.getHeight(),right.getLeft(),right.getWidth()});
            });
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            scenario.onActivity(activity -> { assertNull(field(activity,"player")); assertNull(field(activity,"companion")); });
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.RESUMED);
            await(scenario, activity -> field(activity,"companion") != null && !(boolean) field(activity,"companionLoading"), 15000);
            scenario.onActivity(activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player"), peer = (ExoPlayer) field(activity,"companion");
                assertEquals(VideoPairPlayback.Phase.RIGHT, ((VideoPairPlayback)field(activity,"videoPair")).phase());
                assertFalse(main.getPlayWhenReady()); assertFalse(peer.getPlayWhenReady());
                ((View)field(activity,"playback")).dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN, KeyEvent.KEYCODE_MEDIA_PLAY));
            });
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer)field(activity,"player"), peer = (ExoPlayer)field(activity,"companion");
                return main != null && peer != null && peer.getCurrentPosition() > 4500 && main.isPlaying();
            }, 12000);
            scenario.onActivity(activity -> {
                assertEquals(0, ((ExoPlayer)field(activity,"player")).getCurrentMediaItemIndex());
                assertTrue(((ExoPlayer)field(activity,"player")).getCurrentPosition() < 2500);
            });
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player");
                return main != null && main.getCurrentMediaItemIndex() == 1 && main.isPlaying();
            }, 15000);
        }
    }
    @Test public void mutedRightLoopsAndThenRestartsForItsOwnFullAudibleTurn() throws Exception {
        seed("videopairsreverse");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player"), peer = (ExoPlayer) field(activity,"companion");
                return main != null && peer != null && main.isPlaying() && peer.isPlaying()
                    && main.getCurrentPosition() > 5500;
            }, 20000);
            scenario.onActivity(activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player"), peer = (ExoPlayer) field(activity,"companion");
                assertEquals(0, main.getCurrentMediaItemIndex()); assertTrue(peer.isPlaying());
                assertEquals(0f, peer.getVolume(), 0); assertEquals(1f, main.getVolume(), 0);
                assertEquals(androidx.media3.common.Player.REPEAT_MODE_ONE, peer.getRepeatMode());
            });
            await(scenario, activity -> {
                ExoPlayer peer = (ExoPlayer)field(activity,"companion");
                VideoPairPlayback phase = (VideoPairPlayback)field(activity,"videoPair");
                return peer != null && phase != null && phase.phase() == VideoPairPlayback.Phase.RIGHT
                    && peer.isPlaying() && peer.getCurrentPosition() < 1000;
            }, 10000);
            scenario.onActivity(activity -> {
                ExoPlayer main = (ExoPlayer)field(activity,"player"), peer = (ExoPlayer)field(activity,"companion");
                assertEquals(0f, main.getVolume(), 0); assertEquals(1f, peer.getVolume(), 0);
                assertEquals(androidx.media3.common.Player.REPEAT_MODE_OFF, peer.getRepeatMode());
            });
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer) field(activity,"player");
                return main != null && main.getCurrentMediaItemIndex() == 1 && main.isPlaying();
            }, 12000);
        }
    }
    @Test public void pairFailureRetriesBothVideosIndividuallyWithoutLosingTheRightVideo() throws Exception {
        seed("videopairs");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer)field(activity,"player"), peer = (ExoPlayer)field(activity,"companion");
                return main != null && peer != null && main.isPlaying() && peer.isPlaying();
            }, 20000);
            scenario.onActivity(activity -> {
                try {
                    java.lang.reflect.Method fallback = MainActivity.class.getDeclaredMethod("dropCompanion", String.class);
                    fallback.setAccessible(true); fallback.invoke(activity, "Falha simulada do segundo descodificador.");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                ExoPlayer main = (ExoPlayer)field(activity,"player");
                assertNull(field(activity,"companion")); assertEquals(3, main.getMediaItemCount());
                assertTrue(main.getMediaItemAt(0).mediaId.endsWith("31-short.mp4"));
                assertTrue(main.getMediaItemAt(1).mediaId.endsWith("32-long.mp4"));
            });
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer)field(activity,"player");
                return main != null && main.getCurrentMediaItemIndex() == 1 && main.isPlaying();
            }, 12000);
        }
    }
    @Test public void adjacentHorizontalSixteenByNinePhotosStayIndividual() throws Exception {
        seed("widephotos");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer main = (ExoPlayer)field(activity,"player");
                return main != null && main.isPlaying();
            }, 15000);
            scenario.onActivity(activity -> {
                ExoPlayer main = (ExoPlayer)field(activity,"player");
                assertEquals(2, main.getMediaItemCount()); assertNull(field(activity,"companion"));
                assertEquals(View.GONE, ((View)field(activity,"companionView")).getVisibility());
                View image = (View)field(activity,"playerView"), screen = (View)field(activity,"playback");
                assertEquals(screen.getWidth(), image.getWidth()); assertEquals(screen.getHeight(), image.getHeight());
            });
        }
    }
    @Test public void individualModeDoesNotCreateACompanionAndSurvivesSettingsReload() throws Exception {
        seed("pairs");
        context.getSharedPreferences("collection",0).edit().putInt("layoutMode",PhotoSlides.INDIVIDUAL).commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> { ExoPlayer p = (ExoPlayer)field(activity,"player"); return p != null && p.isPlaying(); },15000);
            scenario.onActivity(activity -> {
                assertEquals(4,((ExoPlayer)field(activity,"player")).getMediaItemCount());
                assertNull(field(activity,"companion"));
            });
            assertEquals(PhotoSlides.INDIVIDUAL,new AppSettings(context).layoutMode);
        }
    }
    @Test public void portraitPairTransitionUsesCoverAndReleasesItAfterTheNewImagesAppear() throws Exception {
        seed("multiplepairs");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario, activity -> {
                ExoPlayer p = (ExoPlayer)field(activity,"player"), peer = (ExoPlayer)field(activity,"companion");
                return p != null && peer != null && p.isPlaying() && peer.isPlaying();
            },15000);
            scenario.onActivity(activity -> {
                ((View)field(activity,"playback")).dispatchKeyEvent(new KeyEvent(KeyEvent.ACTION_DOWN,KeyEvent.KEYCODE_MEDIA_NEXT));
                assertTrue(((PresentationCover)field(activity,"cover")).visible());
            });
            await(scenario, activity -> {
                ExoPlayer p = (ExoPlayer)field(activity,"player");
                return p != null && p.getCurrentMediaItemIndex() == 1 && p.isPlaying()
                    && !((PresentationCover)field(activity,"cover")).visible();
            },15000);
            scenario.moveToState(androidx.lifecycle.Lifecycle.State.CREATED);
            scenario.onActivity(activity -> assertFalse(((PresentationCover)field(activity,"cover")).visible()));
        }
    }
    @Test public void embeddedMotionPhotoPlaysStillThenItsFullVideo() throws Exception {
        seed("motion");
        context.getSharedPreferences("collection",0).edit().putInt("motionMode",MotionPhotos.PHOTO_THEN_VIDEO).commit();
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            start(scenario);
            await(scenario,activity -> { ExoPlayer p=(ExoPlayer)field(activity,"player"); return p!=null && p.isPlaying() && p.getMediaItemCount()==2; },20000);
            scenario.onActivity(activity -> assertEquals(5000,((ExoPlayer)field(activity,"player")).getDuration(),50));
            await(scenario,activity -> { ExoPlayer p=(ExoPlayer)field(activity,"player"); return p!=null && p.isPlaying() && p.getCurrentMediaItemIndex()==1; },15000);
            scenario.onActivity(activity -> assertEquals(3000,((ExoPlayer)field(activity,"player")).getDuration(),150));
            await(scenario,activity -> !(boolean)field(activity,"presenting"),10000);
        }
    }
    @Test public void simulatedVolumeRemovalContinuesOtherLocalOriginAndKeepsSelectedFolders() throws Exception {
        java.io.File base=new java.io.File(context.getCacheDir(),"volume-test-"+System.nanoTime());
        java.io.File gone=new java.io.File(base,"gone"), healthy=new java.io.File(base,"healthy");
        assertTrue(gone.mkdirs()); assertTrue(healthy.mkdirs());
        java.io.File a=new java.io.File(gone,"01.jpg"), b=new java.io.File(healthy,"02.jpg");
        try {
            for(java.io.File target:new java.io.File[]{a,b}) try(java.io.InputStream input=InstrumentationRegistry.getInstrumentation().getContext().getAssets().open("photo.jpg");
                    java.io.OutputStream output=new java.io.FileOutputStream(target)) {
                byte[] data=new byte[8192]; int read; while((read=input.read(data))!=-1) output.write(data,0,read);
            }
            context.getSharedPreferences("collection",0).edit()
                .putString("roots",new org.json.JSONArray(java.util.Arrays.asList(gone.toURI().toString(),healthy.toURI().toString())).toString())
                .putInt("seconds",5).putBoolean("loop",false).commit();
            try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
                start(scenario);
                await(scenario,activity -> { ExoPlayer p=(ExoPlayer)field(activity,"player"); return p!=null && p.isPlaying(); },15000);
                scenario.onActivity(activity -> {
                    try {
                        java.lang.reflect.Method removed=MainActivity.class.getDeclaredMethod("storageChanged",boolean.class,String.class);
                        removed.setAccessible(true); removed.invoke(activity,false,gone.getAbsolutePath());
                    } catch(ReflectiveOperationException error) { throw new AssertionError(error); }
                    assertEquals(2,new AppSettings(context).roots.size());
                });
                await(scenario,activity -> {
                    ExoPlayer p=(ExoPlayer)field(activity,"player");
                    return p!=null && p.isPlaying() && p.getMediaItemCount()==1 && p.getCurrentMediaItem().mediaId.contains("02.jpg");
                },15000);
            }
        } finally { a.delete(); b.delete(); gone.delete(); healthy.delete(); base.delete(); }
    }
}
