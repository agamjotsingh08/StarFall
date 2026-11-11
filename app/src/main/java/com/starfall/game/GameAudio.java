package com.starfall.game;

import android.content.Context;
import android.media.AudioAttributes;
import android.media.SoundPool;

/** Short preloaded effects; playback never performs disk I/O in the frame loop. */
final class GameAudio {
    private final SoundPool pool;
    private final int[] sounds = new int[6];
    private final boolean[] loaded = new boolean[6];
    private long lastExplosion;
    GameAudio(Context context) {
        pool = new SoundPool.Builder().setMaxStreams(8).setAudioAttributes(new AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build()).build();
        pool.setOnLoadCompleteListener((p,id,status)-> { for(int i=0;i<sounds.length;i++) if(sounds[i]==id) loaded[i]=status==0; });
        int[] resources={R.raw.fire,R.raw.hit,R.raw.explosion,R.raw.pickup,R.raw.hurt,R.raw.nova};
        for(int i=0;i<resources.length;i++) sounds[i]=pool.load(context,resources[i],1);
    }
    void play(int kind) {
        if(kind<0 || kind>=sounds.length || !loaded[kind]) return;
        if(kind==GameWorld.S_BOOM) {
            long now=System.nanoTime(); if(now-lastExplosion<55_000_000L) return; lastExplosion=now;
        }
        float volume=kind==GameWorld.S_FIRE ? .13f : kind==GameWorld.S_BOOM ? .4f : .55f;
        pool.play(sounds[kind],volume,volume,kind==GameWorld.S_FIRE ? 0 : 1,0,1);
    }
    void pause() {pool.autoPause();}
    void release() {pool.release();}
}
