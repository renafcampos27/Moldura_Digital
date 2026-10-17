package pt.molduratv;

import android.content.Context;
import java.util.*;
import java.util.function.BooleanSupplier;

/** Shared preparation for the activity and native screensaver; runs outside the UI thread. */
final class SessionPreparer {
    static List<PhotoSlides.Slide> prepare(Context context,List<MediaEntry> input,int order,int layout,int motionMode,int liveMode,
                                         PlaybackFilter filter,double screenRatio,PhotoMetadata metadata,BooleanSupplier cancelled) {
        try {
            List<MediaEntry> originals=new ArrayList<>();
            for(MediaEntry entry:CollectionOrder.arrange(input,order,new Random())) originals.add(entry.isRaw()?entry.withPreview(RawPreviewProvider.uri(context,entry.uri).toString()):entry);
            if(order==3) {
                Map<String,Long> dates=new HashMap<>();
                for(MediaEntry entry:originals) { if(cancelled.getAsBoolean()) return Collections.emptyList(); dates.put(entry.uri,CaptureDates.orderKey(metadata.inspect(entry).captured,entry.modified)); }
                originals.sort(Comparator.comparingLong((MediaEntry entry)->dates.get(entry.uri)).thenComparing(entry->entry.name,String.CASE_INSENSITIVE_ORDER));
            }
            List<MediaEntry> expanded=new ArrayList<>();
            for(MediaEntry entry:LivePhotos.expand(originals,liveMode)) {
                if(cancelled.getAsBoolean()) return Collections.emptyList();
                boolean motion=motionMode!=MotionPhotos.PHOTO && entry.isPhoto() && !entry.isRaw() && metadata.inspect(entry).motion;
                if(!motion || motionMode==MotionPhotos.PHOTO_THEN_VIDEO) expanded.add(entry);
                if(motion) expanded.add(entry.asMotionVideo());
            }
            List<MediaEntry> filtered=new ArrayList<>();
            for(MediaEntry entry:expanded) {
                if(cancelled.getAsBoolean()) return Collections.emptyList();
                if(filter.mayAccept(entry) && filter.accepts(entry,filter.needsRatio(entry)?metadata.ratio(entry):0)) filtered.add(entry);
            }
            return PhotoSlides.arrange(filtered,layout,screenRatio,order==2,entry->cancelled.getAsBoolean()?0:metadata.ratio(entry));
        } finally { metadata.flush(); }
    }
}
