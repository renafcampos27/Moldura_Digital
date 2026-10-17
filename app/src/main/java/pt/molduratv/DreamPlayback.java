package pt.molduratv;

import android.content.Context;
import android.graphics.Color;
import android.os.*;
import android.view.*;
import android.widget.*;
import androidx.media3.common.*;
import androidx.media3.exoplayer.ExoPlayer;
import androidx.media3.ui.*;
import java.util.*;

/** Native dream playback: at most two decoders, whole images and complete audible video turns. */
@androidx.media3.common.util.UnstableApi
final class DreamPlayback {
    private final Context context;
    private final SettingsBackup.Data settings;
    private final Diagnostics diagnostics;
    private final Handler handler=new Handler(Looper.getMainLooper());
    final FrameLayout root;
    private final PlayerView leftView,rightView;
    private final TextView message;
    private List<PhotoSlides.Slide> slides=new ArrayList<>(),pending;
    private ExoPlayer left,right;
    private int position,generation;
    private boolean closed,begun,changing,focusAllowed=true;
    private VideoPairPlayback pair;
    private double leftRatio,rightRatio;
    private boolean leftSized,rightSized,geometryLocked;
    private long leftBufferSince,rightBufferSince;
    DreamPlayback(Context context,SettingsBackup.Data settings) {
        this.context=context; this.settings=settings; diagnostics=new Diagnostics(context);
        root=new FrameLayout(context); root.setBackgroundColor(Color.BLACK);
        leftView=view(); rightView=view(); root.addView(leftView,new FrameLayout.LayoutParams(-1,-1)); root.addView(rightView,new FrameLayout.LayoutParams(-1,-1));
        message=new TextView(context); message.setTextColor(Color.WHITE); message.setTextSize(22); message.setGravity(Gravity.CENTER); root.addView(message,new FrameLayout.LayoutParams(-1,-1));
        message.setText(R.string.dream_preparing);
        root.addOnLayoutChangeListener((v,l,t,r,b,ol,ot,or,ob)->layout());
    }
    private PlayerView view() { PlayerView view=new PlayerView(context); view.setUseController(false); view.setResizeMode(AspectRatioFrameLayout.RESIZE_MODE_FIT); return view; }
    void collection(List<PhotoSlides.Slide> prepared) {
        if(closed) return;
        if(left!=null) { pending=new ArrayList<>(prepared); return; }
        slides=new ArrayList<>(prepared); position=0; open();
    }
    void removeVolume(String path) {
        if(closed || path==null) return;
        SessionRecovery.Result result=SessionRecovery.removeVolume(slides,position,path,settings.loop);
        if(pending!=null) pending=SessionRecovery.removeVolume(pending,0,path,false).slides;
        if(result.slides.equals(slides)) return;
        PhotoSlides.Slide current=position<slides.size()?slides.get(position):null;
        if(result.index>=0 && result.slides.get(result.index)==current) { slides=result.slides; position=result.index; return; }
        slides=result.slides; position=Math.max(0,result.index); open();
    }
    private void open() {
        release(); if(closed) return;
        if(slides.isEmpty() || position>=slides.size()) { message.setText(R.string.dream_empty); message.setVisibility(View.VISIBLE); return; }
        message.setVisibility(View.GONE); focusAllowed=true; begun=false;
        PhotoSlides.Slide slide=slides.get(position); leftRatio=slide.firstRatio; rightRatio=slide.secondRatio;
        leftSized=rightSized=geometryLocked=false;
        pair=slide.paired() && !slide.first.isPhoto()?new VideoPairPlayback():null;
        int token=generation;
        left=MediaPlayers.create(context,settings.layoutMode); leftView.setPlayer(left); left.setAudioAttributes(MediaPlayers.audio(),true); left.setHandleAudioBecomingNoisy(true);
        rightView.setVisibility(slide.paired()?View.VISIBLE:View.GONE);
        if(slide.paired()) { right=MediaPlayers.create(context,settings.layoutMode); rightView.setPlayer(right); right.setAudioAttributes(MediaPlayers.audio(),false); right.setVolume(0); }
        listen(left,true,token); left.setMediaItem(MediaPlayers.item(slide.first,settings.seconds));
        if(right!=null) { listen(right,false,token); right.setMediaItem(MediaPlayers.item(slide.second,settings.seconds)); if(pair!=null) right.setRepeatMode(Player.REPEAT_MODE_ONE); right.prepare(); }
        left.prepare(); layout();
        armTimeout(left,true,token); if(right!=null) armTimeout(right,false,token);
    }
    private void listen(ExoPlayer source,boolean first,int token) {
        source.addListener(new Player.Listener() {
            @Override public void onPlaybackStateChanged(int state) {
                if(closed || token!=generation || changing) return;
                if(state!=Player.STATE_BUFFERING) { if(first) leftBufferSince=0; else rightBufferSince=0; }
                if(state==Player.STATE_ENDED) ended(first,token);
                else { sync(); armTimeout(source,first,token); }
            }
            @Override public void onPlayerError(PlaybackException error) {
                handler.post(()-> { if(token==generation && !closed) failed(first,error); });
            }
            @Override public void onPlayWhenReadyChanged(boolean ready,int reason) {
                if(token==generation && !ready && (reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_FOCUS_LOSS || reason==Player.PLAY_WHEN_READY_CHANGE_REASON_AUDIO_BECOMING_NOISY)) { focusAllowed=false; sync(); }
            }
            @Override public void onVideoSizeChanged(VideoSize size) {
                if(token!=generation || geometryLocked) return;
                double ratio=PhotoSlides.displayedRatio(size.width,size.height,size.pixelWidthHeightRatio); if(ratio<=0) return;
                if(first) { leftRatio=ratio; leftSized=true; } else { rightRatio=ratio; rightSized=true; }
                if(pair!=null && ratio>=1) { handler.post(()-> { if(token==generation) singles(); }); return; }
                if(pair==null || leftSized && rightSized) {
                    if(pair!=null && settings.layoutMode==PhotoSlides.AUTOMATIC && root.getHeight()>0 && !PhotoSlides.comfortable(leftRatio,rightRatio,(double)root.getWidth()/root.getHeight())) {
                        handler.post(()-> { if(token==generation) singles(); }); return;
                    }
                    geometryLocked=true; layout();
                }
            }
        });
    }
    private void sync() {
        if(left==null || changing) return;
        if(!begun) begun=left.getPlaybackState()==Player.STATE_READY && (right==null || right.getPlaybackState()==Player.STATE_READY);
        boolean rightTurn=pair!=null && pair.phase()==VideoPairPlayback.Phase.RIGHT;
        left.setVolume(settings.muted || rightTurn?0:1);
        if(right!=null) right.setVolume(settings.muted || !rightTurn?0:1);
        left.setPlayWhenReady(begun && focusAllowed); if(right!=null) right.setPlayWhenReady(begun && focusAllowed);
    }
    private void ended(boolean first,int token) {
        if(token!=generation) return;
        if(pair==null) { if(first) handler.post(()-> { if(token==generation) next(); }); return; }
        VideoPairPlayback.Action action=pair.ended(first);
        if(action==VideoPairPlayback.Action.START_RIGHT) {
            changing=true; left.pause(); right.pause(); left.setVolume(0); right.setVolume(0);
            left.setAudioAttributes(MediaPlayers.audio(),false); right.setAudioAttributes(MediaPlayers.audio(),true);
            left.setRepeatMode(Player.REPEAT_MODE_ONE); right.setRepeatMode(Player.REPEAT_MODE_OFF);
            left.seekTo(0); right.seekTo(0); changing=false; sync();
        } else if(action==VideoPairPlayback.Action.ADVANCE) handler.post(()-> { if(token==generation) next(); });
    }
    private void armTimeout(ExoPlayer source,boolean first,int token) {
        if(source.getPlaybackState()!=Player.STATE_BUFFERING) return;
        long start=SystemClock.elapsedRealtime();
        if(first) leftBufferSince=start; else rightBufferSince=start;
        handler.postDelayed(()-> {
            if(!closed && token==generation && (first?leftBufferSince:rightBufferSince)==start && source.getPlaybackState()==Player.STATE_BUFFERING && SystemClock.elapsedRealtime()-start>=30000) failed(first,null);
        },30000);
    }
    private void failed(boolean first,PlaybackException error) {
        if(position>=slides.size()) return;
        PhotoSlides.Slide slide=slides.get(position); MediaEntry entry=first?slide.first:slide.second;
        if(error!=null) diagnostics.playback(entry==null?"Protetor":entry.name,error);
        else diagnostics.record(entry==null?"Protetor":entry.name,"Protetor de ecrã","Tempo limite de leitura.");
        if(slide.paired()) { singles(); return; }
        slides.remove(position); if(position>=slides.size()) position=0; open();
    }
    private void singles() {
        if(position>=slides.size() || !slides.get(position).paired()) return;
        PhotoSlides.Slide slide=slides.remove(position);
        slides.add(position,new PhotoSlides.Slide(slide.first,slide.firstRatio)); slides.add(position+1,new PhotoSlides.Slide(slide.second,slide.secondRatio)); open();
    }
    private void next() {
        if(pending!=null) { slides=pending; pending=null; position=0; }
        else { position++; if(position>=slides.size() && settings.loop) position=0; }
        open();
    }
    private void layout() {
        if(position>=slides.size()) return;
        PhotoSlides.Slide slide=slides.get(position);
        if(!slide.paired() || !(leftRatio>0) || !(rightRatio>0) || root.getWidth()<2 || root.getHeight()<1) {
            leftView.setLayoutParams(new FrameLayout.LayoutParams(-1,-1)); return;
        }
        int[] fit=PhotoSlides.fit(root.getWidth(),root.getHeight(),leftRatio,rightRatio);
        FrameLayout.LayoutParams a=new FrameLayout.LayoutParams(fit[2],fit[4]); a.leftMargin=fit[0]; a.topMargin=fit[1]; leftView.setLayoutParams(a);
        FrameLayout.LayoutParams b=new FrameLayout.LayoutParams(fit[3],fit[4]); b.leftMargin=fit[0]+fit[2]; b.topMargin=fit[1]; rightView.setLayoutParams(b);
    }
    private void release() {
        generation++; handler.removeCallbacksAndMessages(null); pair=null;
        leftView.setPlayer(null); rightView.setPlayer(null);
        if(left!=null) { left.release(); left=null; } if(right!=null) { right.release(); right=null; }
    }
    void close() { closed=true; release(); }
}
