package pt.molduratv;

import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.ParcelFileDescriptor;
import androidx.exifinterface.media.ExifInterface;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import org.junit.*;
import org.junit.runner.RunWith;
import java.io.*;
import java.nio.*;
import java.nio.file.Files;
import java.time.Instant;
import java.util.*;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class ExpandedMediaTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    private File directory;
    @Before public void setup() throws Exception { directory=Files.createTempDirectory(context.getCacheDir().toPath(),"expanded-").toFile(); }
    @After public void cleanup() { File[] files=directory.listFiles(); if(files!=null) for(File file:files) file.delete(); directory.delete(); }
    private byte[] jpeg() throws Exception {
        Bitmap bitmap=Bitmap.createBitmap(80,120,Bitmap.Config.ARGB_8888); bitmap.eraseColor(Color.RED);
        ByteArrayOutputStream out=new ByteArrayOutputStream(); bitmap.compress(Bitmap.CompressFormat.JPEG,95,out); bitmap.recycle(); return out.toByteArray();
    }
    private MediaEntry entry(File file,String mime) { return new MediaEntry(file.toURI().toString(),file.getName(),mime,file.lastModified(),file.length()); }
    @Test public void exifCaptureOrderBeatsFileModificationAndPersistsInIndex() throws Exception {
        File a=new File(directory,"a.jpg"),b=new File(directory,"b.jpg"); Files.write(a.toPath(),jpeg()); Files.write(b.toPath(),jpeg());
        ExifInterface old=new ExifInterface(b); old.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,"2020:01:02 12:00:00"); old.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL,"+01:00"); old.saveAttributes();
        ExifInterface recent=new ExifInterface(a); recent.setAttribute(ExifInterface.TAG_DATETIME_ORIGINAL,"2024:01:02 12:00:00"); recent.setAttribute(ExifInterface.TAG_OFFSET_TIME_ORIGINAL,"+01:00"); recent.saveAttributes();
        assertTrue(a.setLastModified(100000)); assertTrue(b.setLastModified(200000));
        CollectionIndex index=CollectionIndex.get(context); PhotoMetadata metadata=new PhotoMetadata(context.getContentResolver(),index);
        List<PhotoSlides.Slide> slides=SessionPreparer.prepare(context,Arrays.asList(entry(a,"image/jpeg"),entry(b,"image/jpeg")),3,PhotoSlides.INDIVIDUAL,0,LivePhotos.SEPARATE,PlaybackFilter.all(),16d/9,metadata,()->false);
        assertEquals(b.toURI().toString(),slides.get(0).first.uri);
        assertEquals(Instant.parse("2020-01-02T11:00:00Z").toEpochMilli(),index.metadata(entry(b,"image/jpeg")).captured);
    }
    @Test public void separateLivePhotoUsesStillThenMovieAndFiltersEachPart() throws Exception {
        File photo=new File(directory,"IMG_1.jpg"),movie=new File(directory,"IMG_1.mov"); Files.write(photo.toPath(),jpeg()); Files.write(movie.toPath(),new byte[]{0});
        PlaybackFilter filter=PlaybackFilter.all().with(false,new PlaybackFilter.Rule(false,Arrays.asList(PlaybackFilter.ORIENTATIONS),Arrays.asList(PlaybackFilter.RATIOS),Arrays.asList(PlaybackFilter.VIDEO_FORMATS),false));
        List<PhotoSlides.Slide> slides=SessionPreparer.prepare(context,Arrays.asList(entry(photo,"image/jpeg"),entry(movie,"video/quicktime")),0,PhotoSlides.INDIVIDUAL,0,LivePhotos.BOTH,filter,16d/9,new PhotoMetadata(context.getContentResolver()),()->false);
        assertEquals(1,slides.size()); assertTrue(slides.get(0).first.isPhoto());
    }
    private void tag(ByteBuffer buffer,int id,int type,int count,int value) { buffer.putShort((short)id).putShort((short)type).putInt(count).putInt(value); }
    @Test public void syntheticRawThumbnailBecomesReadOnlyJpegWithoutChangingOriginal() throws Exception {
        byte[] jpeg=jpeg(); int primary=8,thumbnail=primary+2+4*12+4,start=thumbnail+2+5*12+4;
        ByteBuffer raw=ByteBuffer.allocate(start+jpeg.length).order(ByteOrder.LITTLE_ENDIAN);
        raw.put((byte)'I').put((byte)'I').putShort((short)42).putInt(primary).putShort((short)4);
        tag(raw,256,4,1,800); tag(raw,257,4,1,1200); tag(raw,274,3,1,1); tag(raw,50706,1,4,0x00000401); raw.putInt(thumbnail);
        raw.putShort((short)5); tag(raw,256,4,1,80); tag(raw,257,4,1,120); tag(raw,259,3,1,6); tag(raw,513,4,1,start); tag(raw,514,4,1,jpeg.length); raw.putInt(0); raw.put(jpeg);
        File source=new File(directory,"synthetic.dng"); Files.write(source.toPath(),raw.array()); byte[] before=Files.readAllBytes(source.toPath());
        try(ParcelFileDescriptor descriptor=context.getContentResolver().openFileDescriptor(RawPreviewProvider.uri(context,source.toURI().toString()),"r")) {
            assertNotNull(descriptor); Bitmap decoded=BitmapFactory.decodeFileDescriptor(descriptor.getFileDescriptor()); assertNotNull(decoded);
            assertEquals(80,decoded.getWidth()); assertEquals(120,decoded.getHeight()); decoded.recycle();
        }
        assertArrayEquals(before,Files.readAllBytes(source.toPath()));
        try { context.getContentResolver().openFileDescriptor(RawPreviewProvider.uri(context,source.toURI().toString()),"w"); fail(); } catch(FileNotFoundException expected) { }
    }
    @Test public void dreamServiceIsEnabledExportedOnlyBehindSystemPermissionAndRawProviderPrivate() throws Exception {
        ServiceInfo service=context.getPackageManager().getServiceInfo(new ComponentName(context,MolduraDreamService.class),PackageManager.GET_META_DATA);
        assertTrue(service.enabled); assertTrue(service.exported); assertEquals("android.permission.BIND_DREAM_SERVICE",service.permission); assertTrue(service.metaData.containsKey("android.service.dream"));
        ProviderInfo provider=context.getPackageManager().resolveContentProvider(context.getPackageName()+".raw-preview",0); assertNotNull(provider); assertFalse(provider.exported);
    }
    @Test public void indexUpgradeInvalidatesOldHeadersSoCaptureDateCanBeRead() {
        try(android.database.sqlite.SQLiteDatabase db=android.database.sqlite.SQLiteDatabase.create(null)) {
            db.execSQL("CREATE TABLE metadata(uri TEXT PRIMARY KEY,modified INTEGER,bytes INTEGER,ratio REAL,motion INTEGER)");
            db.execSQL("INSERT INTO metadata VALUES('file:///old',1,20,0.75,0)");
            CollectionIndex.get(context).onUpgrade(db,1,2);
            try(android.database.Cursor cursor=db.rawQuery("SELECT captured FROM metadata",null)) { assertFalse(cursor.moveToFirst()); }
        }
    }
}
