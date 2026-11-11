package com.starfall.game;

import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.view.ViewGroup;
import android.view.MotionEvent;
import android.os.SystemClock;
import androidx.test.core.app.ActivityScenario;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.filters.SdkSuppress;
import androidx.lifecycle.Lifecycle;
import androidx.test.platform.app.InstrumentationRegistry;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Map;
import android.content.SharedPreferences;
import org.junit.Before;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
@SdkSuppress(minSdkVersion = 29)
public class GameDeviceTest {
    private SharedPreferences preferences;
    private Map<String,?> savedPreferences;
    @Before public void preservePlayerProgress() {
        preferences=InstrumentationRegistry.getInstrumentation().getTargetContext().getSharedPreferences("starfall",0);
        savedPreferences=preferences.getAll();
        assertTrue(preferences.edit().clear().commit());
    }
    @After public void restorePlayerProgress() {
        SharedPreferences.Editor editor=preferences.edit().clear();
        for(Map.Entry<String,?> entry:savedPreferences.entrySet()) {
            Object value=entry.getValue();
            if(value instanceof Integer) editor.putInt(entry.getKey(),(Integer)value);
            else if(value instanceof Boolean) editor.putBoolean(entry.getKey(),(Boolean)value);
            else if(value instanceof String) editor.putString(entry.getKey(),(String)value);
        }
        assertTrue(editor.commit());
    }
    private GameView view(MainActivity a) {return (GameView)((ViewGroup)a.findViewById(android.R.id.content)).getChildAt(0);}
    // ActivityScenario.onActivity waits for an idle queue. Continuous vsync rendering
    // can starve that wait on emulators, so dispatch bounded checks directly instead.
    private void onActivity(java.util.function.Consumer<MainActivity> action) {
        java.util.concurrent.FutureTask<Void> task=new java.util.concurrent.FutureTask<>(()->{
            for(android.app.Activity activity:androidx.test.runner.lifecycle.ActivityLifecycleMonitorRegistry
                    .getInstance().getActivitiesInStage(androidx.test.runner.lifecycle.Stage.RESUMED)) {
                if(activity instanceof MainActivity) {action.accept((MainActivity)activity);return null;}
            }
            throw new AssertionError("No resumed game activity");
        });
        new android.os.Handler(android.os.Looper.getMainLooper()).post(task);
        try {task.get(10,java.util.concurrent.TimeUnit.SECONDS);}
        catch(Exception error) {throw new AssertionError("Game UI check failed or timed out",error);}
    }
    private final class GameScenario implements AutoCloseable {
        private final ActivityScenario<MainActivity> delegate=ActivityScenario.launch(MainActivity.class);
        void recreate() {onActivity(a->view(a).stopFrames());delegate.recreate();}
        void moveToState(Lifecycle.State state) {
            if(state==Lifecycle.State.CREATED) onActivity(a->view(a).stopFrames());
            delegate.moveToState(state);
        }
        @Override public void close() {
            // Stop scheduling frames before ActivityScenario's idle-based teardown.
            try {onActivity(a->view(a).stopFrames());} finally {delegate.close();}
        }
    }
    @Test public void launchRendersAndTouchStartsMission() {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->{
                GameView v=view(a); assertEquals(GameWorld.MENU,v.world.state);
                // Use a known no-inset offscreen viewport for repeatable touch-coordinate coverage.
                v.dispatchApplyWindowInsets(new android.view.WindowInsets.Builder().build());
                v.layout(0,0,432,900);
                long now=SystemClock.uptimeMillis();
                MotionEvent down=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,216,710,0);
                MotionEvent up=MotionEvent.obtain(now,now+10,MotionEvent.ACTION_UP,216,710,0);
                v.dispatchTouchEvent(down);v.dispatchTouchEvent(up);down.recycle();up.recycle();
                assertEquals(GameWorld.PLAYING,v.world.state);
                assertEquals(5,v.world.health);
            });
        }
    }
    @Test public void backgroundingPausesAndDoesNotAutoResumeCombat() {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->view(a).world.start());
            scenario.moveToState(Lifecycle.State.CREATED);
            scenario.moveToState(Lifecycle.State.RESUMED);
            onActivity(a->assertEquals(GameWorld.PAUSED,view(a).world.state));
        }
    }
    @Test public void nativeRendererHandlesCompactTallAndTabletViewports() {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->{
                GameView v=view(a);
                int[][] sizes={{360,640},{432,960},{800,1280},{1280,800}};
                for(int[] size:sizes) {
                    v.layout(0,0,size[0],size[1]);
                    Bitmap b=Bitmap.createBitmap(size[0],size[1],Bitmap.Config.ARGB_8888);
                    v.draw(new Canvas(b));
                    int distinct=0,base=b.getPixel(0,0);
                    for(int y=20;y<size[1];y+=20) for(int x=20;x<size[0];x+=20) if(b.getPixel(x,y)!=base) distinct++;
                    assertTrue("Viewport is not blank: "+size[0]+"x"+size[1],distinct>50);
                    b.recycle();
                }
            });
        }
    }
    @Test public void bossEncounterRendersAndCompletes() throws Exception {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            SystemClock.sleep(500);
            screenshot(scenario,"hangar");
            onActivity(a->{
                GameWorld w=view(a).world; w.start(); w.wave=12; w.weapon=3; w.powerRank=8; w.invulnerable=100;
                w.announce("WARNING","SINGULARITY APPROACHING");
            });
            boolean[] won={false};
            String[] progress={"not started"};
            for(int i=0;i<100 && !won[0];i++) {
                SystemClock.sleep(500);
                onActivity(a->{
                    GameView v=view(a); GameWorld w=v.world;
                    int hp=0;
                    for(GameWorld.Entity e:w.entities) if(e.active && e.type==GameWorld.BOSS) {w.target(e.x,700);hp=e.hp;}
                    progress[0]="state="+w.state+" time="+w.time+" bossHp="+hp+" focus="+v.hasWindowFocus();
                    won[0]=w.state==GameWorld.WON;
                    if(won[0]) android.util.Log.i("StarfallPerf","BOSS_TEST "+v.performanceSummary());
                });
                if(i==12) {
                    screenshot(scenario,"boss-combat");
                    onActivity(a->view(a).world.pause());
                    SystemClock.sleep(200);screenshot(scenario,"pause");
                    onActivity(a->view(a).world.resume());
                }
            }
            if(!won[0]) screenshot(scenario,"boss-failure");
            assertTrue("Live rendered boss encounter must be winnable: "+progress[0],won[0]);
            screenshot(scenario,"victory");
        }
    }
    @Test public void debriefUpgradeTouchAdvancesCampaign() throws Exception {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->{GameWorld w=view(a).world;w.start();w.completeStage();});
            SystemClock.sleep(500);screenshot(scenario,"debrief");
            onActivity(a->{
                GameView v=view(a);v.dispatchApplyWindowInsets(new android.view.WindowInsets.Builder().build());v.layout(0,0,432,900);
                long now=SystemClock.uptimeMillis();MotionEvent tap=MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,216,440,0);
                v.dispatchTouchEvent(tap);tap.recycle();
                assertEquals(GameWorld.PLAYING,v.world.state);assertEquals(2,v.world.wave);assertEquals(2,v.world.weapon);
            });
        }
    }
    @Test public void secondFingerDashKeepsSteeringContinuous() {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->{
                GameView v=view(a);v.dispatchApplyWindowInsets(new android.view.WindowInsets.Builder().build());v.layout(0,0,432,900);v.world.start();
                long now=SystemClock.uptimeMillis();
                send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,100,700,0));
                send(v,MotionEvent.obtain(now,now+5,MotionEvent.ACTION_MOVE,150,650,0));
                MotionEvent.PointerProperties p0=new MotionEvent.PointerProperties(),p1=new MotionEvent.PointerProperties();
                p0.id=0;p1.id=1;p0.toolType=p1.toolType=MotionEvent.TOOL_TYPE_FINGER;
                MotionEvent.PointerCoords c0=new MotionEvent.PointerCoords(),c1=new MotionEvent.PointerCoords();
                c0.x=150;c0.y=650;c1.x=100;c1.y=850;c0.pressure=c1.pressure=1;c0.size=c1.size=1;
                send(v,MotionEvent.obtain(now,now+10,MotionEvent.ACTION_POINTER_DOWN|(1<<MotionEvent.ACTION_POINTER_INDEX_SHIFT),2,
                        new MotionEvent.PointerProperties[]{p0,p1},new MotionEvent.PointerCoords[]{c0,c1},0,0,1,1,0,0,android.view.InputDevice.SOURCE_TOUCHSCREEN,0));
                assertEquals(0,v.world.dashCharge,0);float targetX=v.world.targetX,targetY=v.world.targetY;
                send(v,MotionEvent.obtain(now,now+15,MotionEvent.ACTION_MOVE,160,640,0));
                assertEquals(targetX+10,v.world.targetX,.01f);assertEquals(targetY-10,v.world.targetY,.01f);
                send(v,MotionEvent.obtain(now,now+20,MotionEvent.ACTION_CANCEL,160,640,0));
            });
        }
    }
    private void send(GameView view,MotionEvent event) {view.dispatchTouchEvent(event);event.recycle();}
    @Test public void hangarShipAndDifficultyPersistAfterRecreation() throws Exception {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            for(int ship=0;ship<Fleet.COUNT;ship++) {
                final int selected=ship;
                onActivity(a->{view(a).world.selectedShip=selected;a.saveSettings();});
                SystemClock.sleep(300);screenshot(scenario,"ship-"+Fleet.NAMES[ship].toLowerCase(java.util.Locale.US));
            }
            onActivity(a->{
                GameView v=view(a);v.dispatchApplyWindowInsets(new android.view.WindowInsets.Builder().build());v.layout(0,0,432,900);
                long now=SystemClock.uptimeMillis();
                // Bastion wraps to Vanguard, then Falcon via the right-hand hangar control.
                for(int i=0;i<2;i++) send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,380,420,0));
                send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,216,783,0));
                assertEquals(Fleet.FALCON,v.world.selectedShip);assertEquals(Campaign.STANDARD,v.world.difficulty);
            });
            scenario.recreate();awaitFocus(scenario);
            onActivity(a->{
                GameWorld w=view(a).world;assertEquals(Fleet.FALCON,w.selectedShip);assertEquals(Campaign.STANDARD,w.difficulty);
                w.start();assertEquals(Fleet.FALCON,w.runShip);assertEquals(3,w.health);
            });
        }
    }
    private void awaitFocus(GameScenario scenario) {
        boolean[] ready={false};
        for(int i=0;i<100;i++) {
            onActivity(a->ready[0]=view(a).hasWindowFocus() && view(a).getWidth()>0);
            if(ready[0]) return;
            SystemClock.sleep(50);
        }
        fail("Activity did not gain window focus within five seconds");
    }
    private void screenshot(GameScenario scenario,String name) throws Exception {
        java.util.concurrent.CountDownLatch finished=new java.util.concurrent.CountDownLatch(1);
        Bitmap[] captured={null};int[] status={-1};
        onActivity(a->{
            android.view.View decor=a.getWindow().getDecorView();
            captured[0]=Bitmap.createBitmap(decor.getWidth(),decor.getHeight(),Bitmap.Config.ARGB_8888);
            android.view.PixelCopy.request(a.getWindow(),captured[0],result->{
                status[0]=result;finished.countDown();
            },new android.os.Handler(android.os.Looper.getMainLooper()));
        });
        assertTrue("Window capture timed out: "+name,finished.await(10,java.util.concurrent.TimeUnit.SECONDS));
        assertEquals("Window capture failed: "+name,android.view.PixelCopy.SUCCESS,status[0]);
        Bitmap bitmap=captured[0];
        File parent=InstrumentationRegistry.getInstrumentation().getTargetContext().getExternalFilesDir("screenshots");
        assertNotNull(parent);
        File folder=new File(parent,"v0.3.0");
        assertTrue(folder.isDirectory() || folder.mkdirs());
        try(FileOutputStream stream=new FileOutputStream(new File(folder,name+".png"))) {bitmap.compress(Bitmap.CompressFormat.PNG,100,stream);}
        bitmap.recycle();
    }
    @Test public void sceneryOutsideArenaDoesNotTriggerControls() {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->{
                GameView v=view(a);v.dispatchApplyWindowInsets(new android.view.WindowInsets.Builder().build());
                v.layout(0,0,800,900);v.world.start();long now=SystemClock.uptimeMillis();
                send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,790,850,0));
                assertEquals(1,v.world.bombCharge,0);
                send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,10,850,0));
                assertEquals(1,v.world.dashCharge,0);
                v.layout(0,0,432,1000);
                send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,390,10,0));
                assertEquals(GameWorld.PLAYING,v.world.state);
                // A tap inside the actual pause button still works.
                send(v,MotionEvent.obtain(now,now,MotionEvent.ACTION_DOWN,390,95,0));
                assertEquals(GameWorld.PAUSED,v.world.state);
            });
        }
    }
    @Test public void normalCasualCombatRendersAndSavesScreenshot() throws Exception {
        try(GameScenario scenario=new GameScenario()) {
            awaitFocus(scenario);
            onActivity(a->{GameWorld w=view(a).world;w.selectedShip=Fleet.BASTION;w.start();});
            SystemClock.sleep(6500);
            onActivity(a->{assertEquals(GameWorld.PLAYING,view(a).world.state);assertTrue(view(a).world.enemiesRemaining()>0);});
            screenshot(scenario,"casual-combat");
        }
    }
}
