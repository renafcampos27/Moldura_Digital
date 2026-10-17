package pt.molduratv;

import android.app.Activity;
import android.content.*;
import android.net.Uri;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import static org.junit.Assert.*;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import static androidx.test.espresso.action.ViewActions.click;

@RunWith(AndroidJUnit4.class)
@androidx.media3.common.util.UnstableApi
public final class BackupFlowTest {
    private static final Uri FILE = Uri.parse("content://pt.molduratv.test.fixtures/document/backup.json");
    private Context context;
    private Map<String, ?> original;
    @Before public void setup() {
        context = InstrumentationRegistry.getInstrumentation().getTargetContext();
        original = new HashMap<>(context.getSharedPreferences("collection", 0).getAll());
        context.getSharedPreferences("collection", 0).edit().clear()
            .putString("roots", "[\"content://pt.molduratv.test.fixtures/tree/root\"]").commit();
    }
    @After public void cleanup() {
        SharedPreferences.Editor edit = context.getSharedPreferences("collection", 0).edit().clear();
        for (Map.Entry<String, ?> entry : original.entrySet()) {
            if (entry.getValue() instanceof String) edit.putString(entry.getKey(), (String) entry.getValue());
            else if (entry.getValue() instanceof Integer) edit.putInt(entry.getKey(), (Integer) entry.getValue());
            else if (entry.getValue() instanceof Boolean) edit.putBoolean(entry.getKey(), (Boolean) entry.getValue());
        }
        edit.commit();
    }
    private static Object field(MainActivity activity, String name) {
        try { Field field = MainActivity.class.getDeclaredField(name); field.setAccessible(true); return field.get(activity); }
        catch (ReflectiveOperationException error) { throw new AssertionError(error); }
    }
    @Test public void playbackFilterDialogCancelSaveAndPersistence() throws Exception {
        try(ActivityScenario<MainActivity> scenario=ActivityScenario.launch(MainActivity.class)) {
            idle(scenario);
            scenario.onActivity(activity->new FilterDialog(activity,(AppSettings)field(activity,"settings")).show());
            onView(withText("Fotografias")).inRoot(isDialog()).perform(click());
            onView(withText("Orientações")).inRoot(isDialog()).perform(click());
            onView(withText("Horizontal")).inRoot(isDialog()).perform(click());
            onView(withText("Cancelar")).inRoot(isDialog()).perform(click());
            assertTrue(new AppSettings(context).filter.photos.orientations.contains("horizontal"));
            onView(withText("Orientações")).inRoot(isDialog()).perform(click());
            onView(withText("Horizontal")).inRoot(isDialog()).perform(click());
            onView(withText("Guardar")).inRoot(isDialog()).perform(click());
            assertFalse(new AppSettings(context).filter.photos.orientations.contains("horizontal"));
            assertTrue(new AppSettings(context).filter.videos.orientations.contains("horizontal"));
        }
    }
    private void idle(ActivityScenario<MainActivity> scenario) throws Exception {
        long end = System.currentTimeMillis() + 30000; AtomicBoolean idle = new AtomicBoolean();
        while (System.currentTimeMillis() < end) {
            scenario.onActivity(activity -> idle.set(!(boolean)field(activity,"bootstrapping") && !(boolean)field(activity,"scanning") && !(boolean)field(activity,"backupBusy")));
            if (idle.get()) return;
            Thread.sleep(100);
        }
        fail("Operation did not finish");
    }
    private void result(ActivityScenario<MainActivity> scenario, int request) throws Exception {
        idle(scenario);
        scenario.onActivity(activity -> ((BackupController)field(activity,"backups"))
            .onResult(request, Activity.RESULT_OK, new Intent().setData(FILE)));
        idle(scenario);
    }
    private void write(String text) throws Exception {
        try (OutputStream out = context.getContentResolver().openOutputStream(FILE, "wt")) {
            assertNotNull(out); out.write(text.getBytes(StandardCharsets.UTF_8));
        }
    }
    @Test public void exportWritesReadableBackupWithCurrentSettings() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            result(scenario, 40);
            try (InputStream input = context.getContentResolver().openInputStream(FILE)) {
                SettingsBackup.Data saved = SettingsBackup.read(input);
                assertEquals(Collections.singletonList("content://pt.molduratv.test.fixtures/tree/root"), saved.roots);
                assertEquals(60, saved.seconds); assertTrue(saved.loop); assertFalse(saved.muted);
            }
        }
    }
    @Test public void restoreNeedsConfirmationAndReplacesAllOptionsPersistently() throws Exception {
        SettingsBackup.Data restored = new SettingsBackup.Data(Collections.singletonList("content://pt.molduratv.test.fixtures/tree/broken"),
            120, 2, true, true, false);
        write(SettingsBackup.encode(restored));
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            result(scenario, 41);
            assertEquals(60, new AppSettings(context).seconds);
            onView(withText("Cancelar")).inRoot(isDialog()).perform(click());
            assertEquals(60, new AppSettings(context).seconds);
            result(scenario, 41);
            onView(withText("Restaurar")).inRoot(isDialog()).perform(click());
            idle(scenario);
            AppSettings saved = new AppSettings(context);
            assertEquals(restored.roots, saved.roots); assertEquals(120, saved.seconds); assertEquals(2, saved.order);
            assertTrue(saved.crop); assertTrue(saved.muted); assertFalse(saved.loop);
        }
    }
    @Test public void invalidBackupDoesNotChangeCurrentConfiguration() throws Exception {
        write("{\"format\":\"other-app\"}");
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            result(scenario, 41);
            AppSettings current = new AppSettings(context);
            assertEquals(Collections.singletonList("content://pt.molduratv.test.fixtures/tree/root"), current.roots);
            assertEquals(60, current.seconds); assertTrue(current.loop);
        }
    }
    @Test public void cancelledOrDeniedFolderAuthorizationPreservesSelection() throws Exception {
        try (ActivityScenario<MainActivity> scenario = ActivityScenario.launch(MainActivity.class)) {
            idle(scenario);
            scenario.onActivity(activity -> {
                BackupController controller = (BackupController)field(activity,"backups");
                controller.onResult(42, Activity.RESULT_CANCELED, null);
                try {
                    Field pending = BackupController.class.getDeclaredField("reconnectRoot"); pending.setAccessible(true);
                    pending.set(controller, "content://pt.molduratv.test.fixtures/tree/root");
                } catch (ReflectiveOperationException error) { throw new AssertionError(error); }
                controller.onResult(42, Activity.RESULT_OK,
                    new Intent().setData(Uri.parse("content://pt.molduratv.test.fixtures/tree/broken")));
            });
            assertEquals(Collections.singletonList("content://pt.molduratv.test.fixtures/tree/root"), new AppSettings(context).roots);
        }
    }
}
