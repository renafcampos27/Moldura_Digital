package pt.molduratv;

import android.content.*;
import android.database.Cursor;
import android.graphics.*;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import androidx.exifinterface.media.ExifInterface;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.security.*;
import java.util.*;

/** Private, on-demand JPEG previews. Never develops RAW sensor data or writes to the source. */
public final class RawPreviewProvider extends ContentProvider {
    private static final long MAX_THUMB=16L*1024*1024,MAX_CACHE=96L*1024*1024;
    public static Uri uri(Context context,String source) { return new Uri.Builder().scheme("content").authority(context.getPackageName()+".raw-preview").appendPath("preview").appendQueryParameter("source",source).build(); }
    @Override public boolean onCreate() { return true; }
    @Override public String getType(Uri uri) { return "image/jpeg"; }
    @Override public ParcelFileDescriptor openFile(Uri uri,String mode) throws FileNotFoundException {
        if(!"r".equals(mode)) throw new FileNotFoundException("Pré-visualização apenas de leitura.");
        String source=uri.getQueryParameter("source");
        try { return ParcelFileDescriptor.open(preview(Objects.requireNonNull(getContext()),source),ParcelFileDescriptor.MODE_READ_ONLY); }
        catch(IOException | RuntimeException error) { throw new FileNotFoundException("RAW sem pré-visualização JPEG compatível."); }
    }
    static synchronized File preview(Context context,String source) throws IOException {
        if(source==null) throw new IOException("RAW ausente.");
        Uri uri=Uri.parse(source);
        if(!("file".equals(uri.getScheme()) || "content".equals(uri.getScheme())) || (context.getPackageName()+".raw-preview").equals(uri.getAuthority())) throw new IOException("Origem RAW inválida.");
        long modified=0,size=-1;
        if("file".equals(uri.getScheme())) { File file=new File(Objects.requireNonNull(uri.getPath())); modified=file.lastModified(); size=file.length(); }
        else try(Cursor c=context.getContentResolver().query(uri,new String[]{"last_modified","_size"},null,null,null)) {
            if(c!=null && c.moveToFirst()) { int m=c.getColumnIndex("last_modified"),s=c.getColumnIndex("_size"); if(m>=0 && !c.isNull(m)) modified=c.getLong(m); if(s>=0 && !c.isNull(s)) size=c.getLong(s); }
        } catch(RuntimeException ignored) { }
        File directory=new File(context.getCacheDir(),"raw-previews"); if(!directory.isDirectory() && !directory.mkdirs()) throw new IOException("Cache indisponível.");
        String key;
        try { byte[] hash=MessageDigest.getInstance("SHA-256").digest((source+"\n"+modified+"\n"+size).getBytes(StandardCharsets.UTF_8)); StringBuilder hex=new StringBuilder(); for(byte b:hash) hex.append(String.format(Locale.ROOT,"%02x",b&255)); key=hex.toString(); }
        catch(NoSuchAlgorithmException impossible) { throw new IOException(impossible); }
        File target=new File(directory,key+".jpg");
        if(modified>0 && size>0 && target.isFile() && target.length()>0) { target.setLastModified(System.currentTimeMillis()); return target; }
        try(ParcelFileDescriptor descriptor=context.getContentResolver().openFileDescriptor(uri,"r")) {
            if(descriptor==null) throw new IOException("RAW inacessível.");
            ExifInterface exif=new ExifInterface(descriptor.getFileDescriptor()); long[] range=exif.getThumbnailRange();
            if(range==null || range[1]<=0 || range[1]>MAX_THUMB) throw new IOException("RAW sem pré-visualização limitada.");
            byte[] jpeg=exif.getThumbnailBytes();
            if(jpeg==null || jpeg.length<3 || jpeg.length>MAX_THUMB || (jpeg[0]&255)!=255 || (jpeg[1]&255)!=216) throw new IOException("Pré-visualização RAW sem JPEG.");
            BitmapFactory.Options options=new BitmapFactory.Options(); options.inJustDecodeBounds=true;
            BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,options);
            if(options.outWidth<=0 || options.outHeight<=0) throw new IOException("Pré-visualização inválida.");
            int edge=ImageBudget.maxEdge(1920,1080,Runtime.getRuntime().maxMemory(),true,android.os.Process.is64Bit());
            options.inSampleSize=1; while(Math.max(options.outWidth,options.outHeight)/options.inSampleSize>edge) options.inSampleSize*=2;
            options.inJustDecodeBounds=false; options.inPreferredConfig=Bitmap.Config.ARGB_8888;
            Bitmap bitmap=BitmapFactory.decodeByteArray(jpeg,0,jpeg.length,options); if(bitmap==null) throw new IOException("Falhou a pré-visualização.");
            Bitmap rotated=null; File temp=File.createTempFile("raw-",".tmp",directory);
            try {
                Matrix matrix=new Matrix(); if(exif.isFlipped()) matrix.postScale(-1,1); matrix.postRotate(exif.getRotationDegrees());
                rotated=Bitmap.createBitmap(bitmap,0,0,bitmap.getWidth(),bitmap.getHeight(),matrix,true);
                try(OutputStream out=new FileOutputStream(temp)) { if(!rotated.compress(Bitmap.CompressFormat.JPEG,95,out)) throw new IOException("Falhou a cache RAW."); }
                java.nio.file.Files.move(temp.toPath(),target.toPath(),java.nio.file.StandardCopyOption.REPLACE_EXISTING);
            } finally { if(rotated!=null && rotated!=bitmap) rotated.recycle(); bitmap.recycle(); if(temp.exists()) temp.delete(); }
        }
        File[] files=directory.listFiles((dir,name)->name.endsWith(".jpg"));
        if(files!=null) { Arrays.sort(files,Comparator.comparingLong(File::lastModified)); long bytes=0; for(File file:files) bytes+=file.length(); for(File file:files) if(bytes>MAX_CACHE && !file.equals(target)) { long length=file.length(); if(file.delete()) bytes-=length; } }
        return target;
    }
    @Override public Cursor query(Uri uri,String[] projection,String selection,String[] args,String sort) { return null; }
    @Override public Uri insert(Uri uri,ContentValues values) { throw new UnsupportedOperationException(); }
    @Override public int delete(Uri uri,String selection,String[] args) { throw new UnsupportedOperationException(); }
    @Override public int update(Uri uri,ContentValues values,String selection,String[] args) { throw new UnsupportedOperationException(); }
}
