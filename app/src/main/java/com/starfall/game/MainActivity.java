package com.starfall.game;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.view.WindowManager;

public final class MainActivity extends Activity implements GameWorld.Events, GameView.Actions {
    private GameView game;
    private GameWorld world;
    private GameAudio audio;
    private SharedPreferences preferences;
    @Override public void onCreate(Bundle saved) {
        super.onCreate(saved);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        // Target 60 Hz consistently; gameplay simulation itself ticks at 120 Hz.
        WindowManager.LayoutParams attributes=getWindow().getAttributes(); attributes.preferredRefreshRate=60; getWindow().setAttributes(attributes);
        immersive(); preferences=getSharedPreferences("starfall",MODE_PRIVATE);
        world=new GameWorld(System.nanoTime(),this);
        world.best=preferences.getInt("best",0); world.quality=preferences.getInt("quality",2);
        // A new preference defaults existing installations to the requested gentler balance.
        world.difficulty=Math.max(0,Math.min(3,preferences.getInt("difficultyV3",Campaign.CASUAL)));
        world.selectedShip=Fleet.valid(preferences.getInt("selectedShip",Fleet.VANGUARD));
        world.unlockedStage=Math.min(Campaign.STAGES,Math.max(1,preferences.getInt("unlockedStage",1)));
        world.selectedStage=Math.min(world.unlockedStage,Math.max(1,preferences.getInt("selectedStage",1)));
        world.soundEnabled=preferences.getBoolean("sound",true); world.showStats=preferences.getBoolean("stats",false);
        audio=new GameAudio(this); game=new GameView(this,world,this); setContentView(game);
        if(android.os.Build.VERSION.SDK_INT>=33) getOnBackInvokedDispatcher().registerOnBackInvokedCallback(0,this::navigateBack);
    }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    @Override protected void onResume() { super.onResume(); immersive(); if(game!=null) game.startFrames(); }
    @Override protected void onPause() {
        if(world!=null) world.pause(); if(game!=null) { game.stopFrames(); android.util.Log.i("StarfallPerf",game.performanceSummary()); }
        if(audio!=null) audio.pause(); saveSettings(); super.onPause();
    }
    @Override protected void onDestroy() { if(audio!=null) audio.release(); super.onDestroy(); }
    @Override public void onWindowFocusChanged(boolean focus) {
        super.onWindowFocusChanged(focus); if(focus) immersive(); else if(world!=null) world.pause();
    }
    @Override public void onBackPressed() { navigateBack(); }
    private void navigateBack() {
        if(world.state==GameWorld.PLAYING) world.pause();
        else if(world.state!=GameWorld.MENU) world.state=GameWorld.MENU;
        else super.onBackPressed();
    }
    @Override public void sound(int kind) { if(audio!=null) audio.play(kind); }
    @Override public void runEnded(int score,boolean won) { preferences.edit().putInt("best",world.best).apply(); }
    @Override public void stageUnlocked(int stage) {preferences.edit().putInt("unlockedStage",stage).putInt("best",world.best).apply();}
    @Override public void saveSettings() {
        if(preferences==null || world==null) return;
        preferences.edit().putInt("best",world.best).putInt("quality",world.quality).putBoolean("sound",world.soundEnabled).putBoolean("stats",world.showStats)
            .putInt("difficultyV3",world.difficulty).putInt("selectedShip",world.selectedShip).putInt("unlockedStage",world.unlockedStage).putInt("selectedStage",world.selectedStage).apply();
    }
    @Override public void campaign() {
        String[] missions=new String[Campaign.STAGES];
        for(int i=0;i<missions.length;i++) missions[i]=String.format(java.util.Locale.US,"%02d  %s%s",i+1,Campaign.NAMES[i],i+1>world.unlockedStage ? "  [LOCKED]" : Campaign.boss(i+1) ? "  [BOSS]" : "");
        new AlertDialog.Builder(this).setTitle("Campaign / Mission select").setItems(missions,(dialog,index)-> {
            if(index+1<=world.unlockedStage) {world.selectedStage=index+1;saveSettings();}
            else android.widget.Toast.makeText(this,"Complete the previous operation to unlock.",android.widget.Toast.LENGTH_SHORT).show();
        }).setNegativeButton("Back",null).show();
    }
    @Override public void settings() {
        String[] entries={"Sound effects: "+(world.soundEnabled ? "on" : "off"),
            "Graphics: "+new String[]{"Performance","Balanced","Ultra"}[world.quality],
            "Frame counter: "+(world.showStats ? "on" : "off"),"How to play"};
        new AlertDialog.Builder(this).setTitle("Flight settings").setItems(entries,(dialog,which)-> {
            if(which==0) { world.soundEnabled=!world.soundEnabled; saveSettings(); settings(); }
            else if(which==1) new AlertDialog.Builder(this).setTitle("Graphics quality").setSingleChoiceItems(new String[]{"Performance — fewer particles, no bloom","Balanced — bloom and medium particles","Ultra — full effects and starfield"},world.quality,(d,q)-> {world.quality=q;saveSettings();d.dismiss();}).setNegativeButton("Cancel",null).show();
            else if(which==2) {world.showStats=!world.showStats;saveSettings();settings();}
            else new AlertDialog.Builder(this).setTitle("Combat briefing").setMessage("Drag to steer. Your ship follows relative to your finger. Weapons fire automatically.\n\nDASH moves in your last steering direction and gives 0.42 seconds of immunity. Recharge depends on your ship, difficulty and reactor upgrades. Use a second finger while steering.\n\nNOVA clears hostile bullets and damages enemies. Vanguard recharges Nova 20% faster. Near misses recharge it faster.\n\nCollect W for weapons, + for repairs. Nearby supplies are pulled toward your ship. Casual mode repairs one hull after each operation, in addition to your upgrade. Escaping enemies break your kill chain.\n\n12 operations. Bosses at 3, 6, 9 and 12. Cleared operations unlock the next mission for replay from the hangar.\n\nTap the hangar arrows to choose a ship. Vanguard: faster Nova. Falcon: faster cannons and dash, one less hull. Bastion: extra hull and a stronger supply magnet.\n\nTap the difficulty label for Casual (recommended), Standard, Veteran or Nightmare. Ship and difficulty changes apply to the next run. Pause at top right.").setPositiveButton("Understood",null).show();
        }).setPositiveButton("Done",null).show();
    }
}
