package pt.molduratv;

import android.content.*;
import androidx.core.content.ContextCompat;
import java.util.function.BiConsumer;

final class StorageWatcher {
    private final Context context;
    private boolean registered;
    private final BroadcastReceiver receiver;
    StorageWatcher(Context context,BiConsumer<Boolean,String> changed) {
        this.context=context.getApplicationContext();
        receiver=new BroadcastReceiver() {
            @Override public void onReceive(Context unused,Intent intent) {
                String path = intent.getData() == null ? null : intent.getData().getPath();
                changed.accept(Intent.ACTION_MEDIA_MOUNTED.equals(intent.getAction()),path);
            }
        };
    }
    void start() {
        if (registered) return;
        IntentFilter filter = new IntentFilter(); filter.addDataScheme("file");
        filter.addAction(Intent.ACTION_MEDIA_MOUNTED); filter.addAction(Intent.ACTION_MEDIA_UNMOUNTED);
        filter.addAction(Intent.ACTION_MEDIA_EJECT); filter.addAction(Intent.ACTION_MEDIA_REMOVED); filter.addAction(Intent.ACTION_MEDIA_BAD_REMOVAL);
        ContextCompat.registerReceiver(context,receiver,filter,ContextCompat.RECEIVER_NOT_EXPORTED); registered=true;
    }
    void stop() { if (registered) { context.unregisterReceiver(receiver); registered=false; } }
}
