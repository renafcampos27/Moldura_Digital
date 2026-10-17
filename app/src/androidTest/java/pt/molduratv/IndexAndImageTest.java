package pt.molduratv;

import android.content.Context;
import android.graphics.Bitmap;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.media3.common.*;
import androidx.media3.exoplayer.image.*;
import androidx.media3.decoder.DecoderInputBuffer;
import java.io.*;
import java.util.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@androidx.media3.common.util.UnstableApi
public final class IndexAndImageTest {
    private final Context context=InstrumentationRegistry.getInstrumentation().getTargetContext();
    @Test public void sqliteSnapshotAndHeaderCacheValidateSelectionSizeAndModification() {
        CollectionIndex index=CollectionIndex.get(context);
        List<String> roots=Collections.singletonList("file:///index-test");
        MediaEntry entry=new MediaEntry("file:///index-test/a.jpg","a.jpg","image/jpeg",100,900);
        assertTrue(index.save(roots,Collections.singletonList(entry)));
        assertEquals(entry.uri,index.load(roots).get(0).uri);
        assertTrue(index.load(Collections.singletonList("file:///other")).isEmpty());
        Map<MediaEntry,PhotoMetadata.Info> metadata=new HashMap<>(); metadata.put(entry,new PhotoMetadata.Info(.75,true));
        index.saveMetadata(metadata); assertNotNull(index.metadata(entry)); assertTrue(index.metadata(entry).motion);
        assertNull(index.metadata(new MediaEntry(entry.uri,entry.name,entry.mime,101,900)));
        assertNull(index.metadata(new MediaEntry(entry.uri,entry.name,entry.mime,100,901)));
        assertNull(index.metadata(new MediaEntry(entry.uri,entry.name,entry.mime,0,0)));
    }
    @Test public void overwrittenLocalPhotoInvalidatesEvenAnOldLibraryEntry() throws Exception {
        File file=File.createTempFile("index-photo-",".jpg",context.getCacheDir());
        try {
            writePhoto(file,800,600); file.setLastModified(100000);
            MediaEntry stale=new MediaEntry(file.toURI().toString(),file.getName(),"image/jpeg",file.lastModified(),file.length());
            PhotoMetadata metadata=new PhotoMetadata(context.getContentResolver(),CollectionIndex.get(context));
            assertEquals(4d/3,metadata.ratio(stale),.001); metadata.flush();
            writePhoto(file,300,600); file.setLastModified(200000);
            assertEquals(.5,metadata.ratio(stale),.001); metadata.flush();
        } finally { assertTrue(file.delete()); }
    }
    private void writePhoto(File file,int width,int height) throws IOException {
        Bitmap bitmap=Bitmap.createBitmap(width,height,Bitmap.Config.ARGB_8888);
        try (OutputStream out=new FileOutputStream(file)) { assertTrue(bitmap.compress(Bitmap.CompressFormat.JPEG,90,out)); }
        finally { bitmap.recycle(); }
    }
    @Test public void media3DecoderEnforcesConfiguredImageBound() throws Exception {
        Bitmap image=Bitmap.createBitmap(1600,1200,Bitmap.Config.ARGB_8888);
        ByteArrayOutputStream bytes=new ByteArrayOutputStream(); image.compress(Bitmap.CompressFormat.JPEG,90,bytes); image.recycle();
        byte[] encoded=bytes.toByteArray();
        ImageDecoder decoder=new BitmapFactoryImageDecoder.Factory(context).setMaxOutputSize(512).createImageDecoder();
        ImageOutputBuffer output=null;
        try {
            DecoderInputBuffer input=decoder.dequeueInputBuffer(); assertNotNull(input);
            input.format=new Format.Builder().setSampleMimeType("image/jpeg").build();
            input.ensureSpaceForWrite(encoded.length); input.data.put(encoded); input.flip(); decoder.queueInputBuffer(input);
            long until=System.currentTimeMillis()+5000;
            while(output==null && System.currentTimeMillis()<until) { output=decoder.dequeueOutputBuffer(); if(output==null) Thread.sleep(20); }
            assertNotNull(output); assertNotNull(output.bitmap);
            assertTrue(output.bitmap.getWidth()<=512); assertTrue(output.bitmap.getHeight()<=512);
        } finally { if(output!=null) output.release(); decoder.release(); }
    }
}
