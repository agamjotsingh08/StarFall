package com.starfall.game;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.*;
import android.view.Choreographer;
import android.view.MotionEvent;
import android.view.View;
import java.util.Locale;
import java.util.Random;

/** Vsync-driven hardware Canvas renderer. Activity, input and simulation share the main thread. */
@SuppressLint("ViewConstructor") // Constructed only by MainActivity with explicit simulation dependencies.
public final class GameView extends View implements Choreographer.FrameCallback {
    public interface Actions { void settings(); void saveSettings(); void campaign(); }
    public final GameWorld world;
    private final Actions actions;
    private final GameArt art;
    private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Typeface regular = Typeface.create("sans-serif-medium", Typeface.NORMAL);
    private final Typeface heavy = Typeface.create("sans-serif-black", Typeface.BOLD);
    private final Typeface mono = Typeface.create("monospace", Typeface.BOLD);
    private final RectF rect = new RectF();
    private final float[] stars = new float[360];
    private final int WHITE = 0xffecf4ff, MUTED = 0xff91a9bf, CYAN = 0xff76f6dd;
    private float scale = 1, left, top, safeTop, safeBottom, safeLeft, safeRight;
    private float touchX, touchY, startX, startY;
    private int pointer = -1;
    private boolean running;
    private long lastFrame, statsStart, frameCount, slowFrames;
    private double accumulator;
    private String scoreText = "000000", bestText = "0", statsText = "Measuring...";
    private int cachedScore = -1, cachedBest = -1;
    private float fps;
    private final float[] frameMs = new float[600];
    private int metricIndex, metricCount;

    public GameView(Context context, GameWorld world, Actions actions) {
        super(context); this.world = world; this.actions = actions; art = new GameArt(getResources());
        setFocusable(true); setClickable(true);
        setContentDescription("Starfall space shooter. Drag to steer; auto-fire. Dash at bottom left. Nova at bottom right. Pause at top right.");
        Random r = new Random(42);
        for (int i = 0; i < stars.length; i += 3) { stars[i] = r.nextFloat()*432; stars[i+1] = r.nextFloat()*900; stars[i+2] = .2f + r.nextFloat(); }
        setOnApplyWindowInsetsListener((view, insets) -> {
            safeTop = insets.getSystemWindowInsetTop(); safeBottom = insets.getSystemWindowInsetBottom();
            safeLeft = insets.getSystemWindowInsetLeft(); safeRight = insets.getSystemWindowInsetRight();
            if (android.os.Build.VERSION.SDK_INT >= 28 && insets.getDisplayCutout() != null) {
                safeTop = Math.max(safeTop, insets.getDisplayCutout().getSafeInsetTop());
                safeBottom = Math.max(safeBottom, insets.getDisplayCutout().getSafeInsetBottom());
            }
            layoutArena(); return insets;
        });
    }
    private void layoutArena() {
        scale = Math.min((getWidth()-safeLeft-safeRight)/432f, (getHeight()-safeTop-safeBottom)/900f);
        if (scale <= 0) scale = 1;
        left = safeLeft + (getWidth()-safeLeft-safeRight-432*scale)/2;
        top = safeTop + (getHeight()-safeTop-safeBottom-900*scale)/2;
    }
    @Override protected void onSizeChanged(int w, int h, int oldw, int oldh) { layoutArena(); }
    public void startFrames() {
        if (running) return; running = true; lastFrame = 0; accumulator = 0;
        statsStart = System.nanoTime(); frameCount = slowFrames = 0;
        Choreographer.getInstance().postFrameCallback(this);
    }
    public void stopFrames() {
        running = false; pointer = -1; Choreographer.getInstance().removeFrameCallback(this);
        lastFrame = 0; accumulator = 0;
    }
    @Override public void doFrame(long nanos) {
        if (!running) return;
        if (lastFrame != 0) {
            float elapsed = (nanos-lastFrame)/1_000_000_000f;
            if (world.state == GameWorld.PLAYING) {
                frameMs[metricIndex++ % frameMs.length] = elapsed*1000;
                metricCount = Math.min(frameMs.length, metricCount+1);
            }
            accumulator += Math.min(.067, elapsed);
            int steps = 0;
            while (accumulator >= GameWorld.STEP && steps++ < 8) { world.update(GameWorld.STEP); accumulator -= GameWorld.STEP; }
            if (steps >= 8) accumulator = 0; // Avoid a catch-up spiral after OS stalls.
            if (elapsed > .025) slowFrames++;
        }
        lastFrame = nanos; frameCount++;
        if (nanos-statsStart > 1_000_000_000L) {
            fps = frameCount*1_000_000_000f/(nanos-statsStart);
            statsText = String.format(Locale.US, "%.0f FPS  /  %d slow frames", fps, slowFrames);
            statsStart = nanos; frameCount = slowFrames = 0;
        }
        invalidate(); Choreographer.getInstance().postFrameCallback(this);
    }
    public String performanceSummary() {
        float[] sorted = java.util.Arrays.copyOf(frameMs, metricCount); java.util.Arrays.sort(sorted);
        float p95 = metricCount == 0 ? 0 : sorted[Math.min(metricCount-1, (int)(metricCount*.95))];
        return String.format(Locale.US, "fps=%.1f p95_frame_ms=%.2f samples=%d", fps, p95, metricCount);
    }
    @Override protected void onDraw(Canvas c) {
        super.onDraw(c); c.drawColor(0xff030712);
        // Full-bleed scenery frames a consistent arena on phones, tablets and resizable windows.
        art.texture.setAlpha(100); rect.set(0, 0, getWidth(), getHeight()); c.drawBitmap(art.nebula, null, rect, art.texture);
        c.save(); c.translate(left, top); c.scale(scale, scale); c.clipRect(0, 0, 432, 900);
        float drift = (float)Math.sin(world.ambientTime*.025)*15;
        rect.set(-25+drift, -38, 457+drift, 938); art.texture.setAlpha(255);
        c.drawBitmap(art.nebula, null, rect, art.texture);
        fill(c, 0, 0, 432, 900, 0x3d030714);
        int operation=world.state==GameWorld.MENU ? world.selectedStage : world.wave;
        int act=(operation-1)/3;
        if(act==1) fill(c,0,0,432,900,0x202a104b);
        else if(act==2) fill(c,0,0,432,900,0x223b210a);
        else if(act==3) fill(c,0,0,432,900,0x283e0718);
        drawStars(c);
        if (world.state == GameWorld.MENU) menu(c);
        else {
            c.save();
            if (world.shake > 0 && world.quality > 0) c.translate((float)Math.sin(world.time*101)*world.shake*.35f, (float)Math.cos(world.time*87)*world.shake*.25f);
            battlefield(c); c.restore(); hud(c);
            if (world.state == GameWorld.PAUSED) paused(c);
            if (world.state == GameWorld.DEBRIEF) debrief(c);
            if (world.state == GameWorld.LOST || world.state == GameWorld.WON) result(c);
        }
        if (world.showStats) text(c, statsText, 16, 891, 9, MUTED, mono, Paint.Align.LEFT);
        c.restore();
    }
    private void drawStars(Canvas c) {
        int end = world.quality == 0 ? 120 : stars.length;
        for (int i = 0; i < end; i += 3) {
            float z = stars[i+2], sy = (stars[i+1]+world.ambientTime*(8+z*24))%900;
            p.setColor(Color.argb((int)(80+z*110), 170, 211, 255));
            c.drawCircle(stars[i], sy, z*.85f, p);
            if (world.quality == 2 && z > 1) { p.setAlpha(40); p.setStrokeWidth(.7f); c.drawLine(stars[i], sy-4, stars[i], sy+2, p); }
        }
    }
    private void menu(Canvas c) {
        tag(c, "STARFALL  /  COMBAT OPERATIONS", 216, 54);
        text(c, "NO SAFE", 32, 160, 51, WHITE, heavy, Paint.Align.LEFT);
        text(c, "ORBIT.", 32, 220, 68, CYAN, heavy, Paint.Align.LEFT);
        text(c, "Twelve operations. Four command ships.", 35, 255, 14, MUTED, regular, Paint.Align.LEFT);
        float sy = 410+(float)Math.sin(world.ambientTime*1.8)*6;
        art.glow(c, 216, sy+35, 310, 330, 0, 110);
        p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1); p.setColor(0x3851aaca);
        c.drawCircle(216, sy, 125, p); c.drawCircle(216, sy, 132, p); p.setStyle(Paint.Style.FILL);
        engine(c, 216, sy+84, .75f);
        art.player(c, world.selectedShip, 216, sy, 179, (float)Math.sin(world.ambientTime*.7)*3);
        text(c, "‹", 51, 424, 36, CYAN, heavy, Paint.Align.CENTER);
        text(c, "›", 381, 424, 36, CYAN, heavy, Paint.Align.CENTER);
        text(c, Fleet.NAMES[world.selectedShip]+"  /  "+(world.selectedShip+1)+" OF 3", 216, 547, 17, WHITE, heavy, Paint.Align.CENTER);
        text(c, Fleet.ROLES[world.selectedShip], 216, 570, 10, CYAN, mono, Paint.Align.CENTER);
        int hull=Campaign.hull(world.difficulty)+Fleet.hullBonus(world.selectedShip);
        text(c,"HULL "+hull+"  /  "+(world.selectedShip==Fleet.FALCON ? "RAPID FIRE + FAST DASH" : world.selectedShip==Fleet.BASTION ? "EXTENDED SUPPLY MAGNET" : "FASTER NOVA RECHARGE"),216,286,11,CYAN,mono,Paint.Align.CENTER);
        for(int i=1;i<=Campaign.STAGES;i++) {
            float bx=34+(i-1)*31;
            p.setColor(i==world.selectedStage ? CYAN : i<=world.unlockedStage ? 0xff315b68 : 0xff172735);
            rect.set(bx,595,bx+23,619); c.drawRoundRect(rect,3,3,p);
            text(c,Integer.toString(i),bx+11.5f,611,10,i==world.selectedStage ? 0xff081e29 : MUTED,mono,Paint.Align.CENTER);
        }
        text(c,"MISSION SELECT  /  "+world.unlockedStage+" UNLOCKED  ›",216,647,11,MUTED,mono,Paint.Align.CENTER);
        button(c, "DEPLOY", "OP "+world.selectedStage+"  /  "+Campaign.name(world.selectedStage), 32, 674, 368, 76, true);
        text(c, world.difficultyName()+"  /  TAP TO CHANGE", 216, 784, 12, 0xffffbd83, mono, Paint.Align.CENTER);
        line(c, 32, 812, 400, 812, 0x35447185, 1);
        text(c, "BEST  " + bestLabel(), 32, 850, 12, WHITE, mono, Paint.Align.LEFT);
        text(c, "SETTINGS  +", 400, 850, 11, CYAN, mono, Paint.Align.RIGHT);
        tag(c, "DRAG TO STEER  /  DASH TO SURVIVE", 216, 882);
    }
    private void battlefield(Canvas c) {
        if(world.dashFlash>0) {
            line(c,world.dashFromX,world.dashFromY,world.x,world.y,Color.argb((int)(world.dashFlash*180),118,246,221),8*world.dashFlash);
            art.glow(c,world.x,world.y,135,135,0,(int)(world.dashFlash*220));
        }
        for (GameWorld.Entity e : world.entities) {
            if (!e.active || e.type == GameWorld.SHOT || e.type == GameWorld.ENEMY_SHOT || e.type == GameWorld.SPARK) continue;
            if (e.type >= GameWorld.SCOUT && e.type <= GameWorld.BOSS) {
                int index = e.type == GameWorld.HEAVY ? 2 : e.type == GameWorld.BOSS ? 3 : 1;
                float size = e.type == GameWorld.BOSS ? 157 : e.radius*2.65f;
                if (world.quality > 0) art.glow(c, e.x, e.y, size*1.4f, size*1.2f, 1, 85);
                art.ship(c, index, e.x, e.y, size, e.type == GameWorld.SCOUT ? (float)Math.sin(e.age*1.9)*9 : 0, e.hit);
                if(e.cooldown<.25f && e.y>110 && e.y<610) {
                    // A bright reactor telegraphs the next enemy burst.
                    art.glow(c,e.x,e.y+14,40,40,2,210);
                }
                if (e.type == GameWorld.HEAVY && e.hp < e.maxHp) {
                    fill(c, e.x-23, e.y-40, e.x+23, e.y-37, 0xff3a2936);
                    fill(c, e.x-23, e.y-40, e.x-23+46f*e.hp/e.maxHp, e.y-37, 0xffffaa69);
                }
            } else {
                boolean repair = e.type == GameWorld.REPAIR;
                art.glow(c, e.x, e.y, 80, 80, repair ? 0 : 2, 190);
                c.save(); c.rotate(e.age*55, e.x, e.y);
                p.setColor(0xd9122635); rect.set(e.x-16,e.y-16,e.x+16,e.y+16); c.drawRoundRect(rect,4,4,p);
                p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1.3f); p.setColor(repair ? CYAN : 0xffffca7d); c.drawRoundRect(rect,4,4,p); p.setStyle(Paint.Style.FILL); c.restore();
                text(c, repair ? "+" : "W", e.x, e.y+6, 18, WHITE, heavy, Paint.Align.CENTER);
            }
        }
        for (GameWorld.Entity e : world.entities) {
            if (!e.active) continue;
            if (e.type == GameWorld.SHOT) {
                if (world.quality > 0) art.glow(c,e.x,e.y,18,45,0,180);
                p.setColor(0xff96fbe8); rect.set(e.x-2,e.y-14,e.x+2,e.y+9); c.drawRoundRect(rect,2,2,p);
                p.setColor(0xffffffff); rect.set(e.x-.7f,e.y-12,e.x+.7f,e.y+4); c.drawRoundRect(rect,1,1,p);
            } else if (e.type == GameWorld.ENEMY_SHOT) {
                if (world.quality > 0) art.glow(c,e.x,e.y,33,33,1,210);
                p.setColor(0xffff516f); c.drawCircle(e.x,e.y,5.5f,p); p.setColor(0xffffd3c6); c.drawCircle(e.x,e.y,2.5f,p);
            } else if (e.type == GameWorld.SPARK) {
                int alpha = (int)(Math.min(1,e.life*2)*255);
                p.setColor(e.variant == 0 ? 0xff9dfbe9 : 0xffffb365); p.setAlpha(alpha);
                p.setStrokeWidth(e.radius); c.drawLine(e.x,e.y,e.x-e.vx*.025f,e.y-e.vy*.025f,p);
            }
        }
        if (world.state != GameWorld.LOST) {
            if (world.invulnerable <= 0 || (int)(world.time*12)%2 == 0) {
                if (world.quality > 0) art.glow(c,world.x,world.y,115,115,0,90);
                engine(c,world.x-9,world.y+23,.65f); engine(c,world.x+9,world.y+23,.65f);
                art.player(c,world.runShip,world.x,world.y,world.runShip==Fleet.BASTION ? 69 : 62,world.bank);
                if (world.invulnerable > 0) {
                    p.setColor(0x8876f6dd); p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(1);
                    c.drawCircle(world.x,world.y,39,p); p.setStyle(Paint.Style.FILL);
                }
                // Tiny cockpit pip shows the actual forgiving hit area during intense combat.
                p.setColor(0xffe9ffff); c.drawCircle(world.x,world.y,2,p);
            }
        }
        if (world.blast > 0) {
            p.setColor(Color.argb((int)(world.blast*200),118,246,221));
            p.setStyle(Paint.Style.STROKE); p.setStrokeWidth(4+world.blast*10);
            c.drawCircle(world.x,world.y,(1-world.blast)*1000+40,p); p.setStyle(Paint.Style.FILL);
        }
        if (world.damageFlash > 0) fill(c,0,0,432,900,Color.argb((int)(world.damageFlash*70),255,45,75));
    }
    private void engine(Canvas c,float x,float y,float size) {
        float pulse = 1+(float)Math.sin(world.ambientTime*45)*.15f;
        if (world.quality > 0) art.glow(c,x,y+18*size,40*size,100*size,0,220);
        p.setColor(0xaa328bff); rect.set(x-6*size,y,x+6*size,y+43*size*pulse); c.drawOval(rect,p);
        p.setColor(0xff91fff0); rect.set(x-3*size,y,x+3*size,y+24*size*pulse); c.drawOval(rect,p);
        p.setColor(0xfff3fffb); rect.set(x-1.5f*size,y,x+1.5f*size,y+13*size); c.drawOval(rect,p);
    }
    private void hud(Canvas c) {
        fill(c,0,0,432,99,0xd0091424);
        tag(c,"SCORE",32,29,Paint.Align.LEFT); text(c,scoreLabel(),30,60,30,WHITE,heavy,Paint.Align.LEFT);
        tag(c,"OPERATION",257,29); text(c,world.stageNumber()+"/12",257,55,20,CYAN,mono,Paint.Align.CENTER);
        p.setColor(0xff142b3b); rect.set(360,20,410,70); c.drawRoundRect(rect,15,15,p);
        fill(c,379,36,383,53,WHITE); fill(c,388,36,392,53,WHITE);
        for (int i=0;i<world.maxHealth;i++) { p.setColor(i<world.health ? CYAN : 0xff29404c); rect.set(32+i*17,76,45+i*17,81); c.drawRoundRect(rect,2,2,p); }
        text(c,world.difficultyName(),162,83,10,MUTED,mono,Paint.Align.LEFT);
        if (world.combo>=3) text(c,world.combo+" KILLS ×"+Math.min(4,1+world.combo/10),401,83,10,0xffffcd82,mono,Paint.Align.RIGHT);
        if(!Campaign.boss(world.wave)) fill(c,0,97,432*Math.min(1,world.waveTime/Campaign.duration(world.wave)),99,CYAN);
        for (GameWorld.Entity e:world.entities) if(e.active && e.type==GameWorld.BOSS) {
            tag(c,Campaign.name(world.wave)+(e.hp<e.maxHp*.5f ? " / OVERDRIVE" : " / COMMAND SHIP"),216,120);
            fill(c,32,131,400,137,0xff382238); fill(c,32,131,32+368f*e.hp/e.maxHp,137,0xffff4d90);
        }
        if(world.banner>0 && world.state==GameWorld.PLAYING) {
            float alpha=Math.min(1,world.banner)*Math.min(1,(2.8f-world.banner)*3);
            text(c,world.bannerTitle,216,322,27,Color.argb((int)(alpha*255),236,244,255),heavy,Paint.Align.CENTER);
            text(c,world.bannerSubtitle,216,346,11,Color.argb((int)(alpha*255),118,246,221),mono,Paint.Align.CENTER);
        }
        fill(c,0,815,432,900,0xd0091424);
        boolean dashReady=world.dashCharge>=.999f;
        p.setColor(dashReady ? 0xff244856 : 0xff132b39); rect.set(30,830,162,881); c.drawRoundRect(rect,8,8,p);
        text(c,"DASH",96,853,14,dashReady ? WHITE : MUTED,heavy,Paint.Align.CENTER);
        text(c,dashReady ? "READY" : "RECHARGING",96,870,9,MUTED,mono,Paint.Align.CENTER);
        fill(c,42,877,42+108*world.dashCharge,880,CYAN);
        text(c,"MK."+world.weapon,216,851,16,WHITE,heavy,Paint.Align.CENTER);
        text(c,Fleet.NAMES[world.runShip],216,871,9,MUTED,mono,Paint.Align.CENTER);
        boolean ready=world.bombCharge>=.999f;
        p.setColor(ready ? CYAN : 0xff193443); rect.set(270,830,402,881); c.drawRoundRect(rect,8,8,p);
        text(c,"NOVA",336,853,14,ready ? 0xff08242b : MUTED,heavy,Paint.Align.CENTER);
        text(c,ready ? "ARMED" : "RECHARGING",336,870,9,ready ? 0xff28645e : MUTED,mono,Paint.Align.CENTER);
        if(!ready) fill(c,282,877,282+108*world.bombCharge,880,CYAN);
    }
    private void paused(Canvas c) {
        fill(c,0,0,432,900,0xdf050b17);
        tag(c,"OP "+world.stageNumber()+"  /  "+Campaign.name(world.wave),216,278);
        text(c,"TACTICAL",216,340,43,WHITE,heavy,Paint.Align.CENTER);
        text(c,"PAUSE.",216,391,49,CYAN,heavy,Paint.Align.CENTER);
        button(c,"RESUME OPERATION","RETURN TO ENGAGEMENT",40,445,352,76,true);
        button(c,"SETTINGS","SOUND / GRAPHICS",40,541,352,65,false);
        text(c,"RETURN TO HANGAR",216,661,12,MUTED,mono,Paint.Align.CENTER);
    }
    private void debrief(Canvas c) {
        fill(c,0,0,432,900,0xf2050b17);
        tag(c,"OPERATION "+world.stageNumber()+" / COMPLETE",216,135);
        text(c,"SECTOR",32,203,43,WHITE,heavy,Paint.Align.LEFT);
        text(c,"CLEARED.",32,256,49,CYAN,heavy,Paint.Align.LEFT);
        text(c,world.stageGrade,370,250,73,0xffffbd83,heavy,Paint.Align.CENTER);
        text(c,world.stageKills+" KILLS  /  "+world.stageDamage+" HITS  /  "+world.stageEscaped+" ESCAPED",32,297,11,MUTED,mono,Paint.Align.LEFT);
        line(c,32,322,400,322,0xff2b4352,1);
        text(c,"CHOOSE YOUR NEXT ADVANTAGE",32,361,15,WHITE,heavy,Paint.Align.LEFT);
        upgradeCard(c,"01","WEAPONS ARRAY","Upgrade cannons. Increase damage and fire rate.",398,0);
        upgradeCard(c,"02","HULL REINFORCEMENT",world.health==world.maxHealth && world.maxHealth<world.hullLimit() ? "Add one hull segment. Repair the ship." : "Repair two hull segments before deployment.",506,1);
        upgradeCard(c,"03","COMBAT REACTOR","Faster dash + nova recharge. Restore nova 40%.",614,2);
        if(world.runDifficulty==Campaign.CASUAL) text(c,"CASUAL BONUS: +1 HULL WITH ANY UPGRADE",216,747,11,CYAN,mono,Paint.Align.CENTER);
        tag(c,"NEXT: OP "+(world.wave+1)+" / "+Campaign.name(world.wave+1),216,781);
        text(c,Campaign.BRIEFS[world.wave],216,811,12,MUTED,regular,Paint.Align.CENTER);
    }
    private void upgradeCard(Canvas c,String number,String title,String body,float y,int choice) {
        p.setColor(0xff112534);rect.set(32,y,400,y+88);c.drawRoundRect(rect,9,9,p);
        fill(c,32,y+13,35,y+75,choice==1 ? 0xffffbd83 : CYAN);
        text(c,number,49,y+29,12,CYAN,mono,Paint.Align.LEFT);
        text(c,title,82,y+30,16,WHITE,heavy,Paint.Align.LEFT);
        text(c,body,49,y+60,11,MUTED,regular,Paint.Align.LEFT);
    }
    private void result(Canvas c) {
        fill(c,0,0,432,900,0xde050b17); boolean won=world.state==GameWorld.WON;
        tag(c,won ? "ALL OPERATIONS COMPLETE" : "OP "+world.stageNumber()+" / "+Campaign.name(world.wave),216,236);
        text(c,won ? "CAMPAIGN" : "VESSEL",216,299,43,WHITE,heavy,Paint.Align.CENTER);
        text(c,won ? "SECURED." : "LOST.",216,350,48,CYAN,heavy,Paint.Align.CENTER);
        text(c,scoreLabel(),216,430,54,WHITE,heavy,Paint.Align.CENTER); tag(c,"FINAL SCORE",216,454);
        line(c,55,483,377,483,0xff254052,1);
        tag(c,"ELIMINATED",112,515); tag(c,"BEST CHAIN",320,515);
        text(c,Integer.toString(world.kills),112,552,26,WHITE,mono,Paint.Align.CENTER);
        text(c,Integer.toString(world.maxCombo),320,552,26,WHITE,mono,Paint.Align.CENTER);
        button(c,"REDEPLOY","REASSESS. ADAPT. ENGAGE.",40,605,352,76,true);
        text(c,"RETURN TO HANGAR",216,731,12,MUTED,mono,Paint.Align.CENTER);
    }
    private String scoreLabel() {
        if(cachedScore!=world.score) { cachedScore=world.score; scoreText=String.format(Locale.US,"%06d",world.score); }
        return scoreText;
    }
    private String bestLabel() { if(cachedBest!=world.best) { cachedBest=world.best; bestText=Integer.toString(world.best); } return bestText; }
    private void button(Canvas c,String label,String subtitle,float x,float y,float w,float h,boolean primary) {
        p.setColor(primary ? CYAN : 0xff132638); rect.set(x,y,x+w,y+h); c.drawRoundRect(rect,9,9,p);
        text(c,label,x+22,y+32,19,primary ? 0xff07212c : WHITE,heavy,Paint.Align.LEFT);
        text(c,subtitle,x+22,y+52,10,primary ? 0xff28645e : MUTED,mono,Paint.Align.LEFT);
        text(c,"›",x+w-24,y+43,31,primary ? 0xff07212c : CYAN,regular,Paint.Align.CENTER);
    }
    private void tag(Canvas c,String s,float x,float y) { tag(c,s,x,y,Paint.Align.CENTER); }
    private void tag(Canvas c,String s,float x,float y,Paint.Align a) { text(c,s,x,y,10,MUTED,mono,a); }
    private void text(Canvas c,String s,float x,float y,float size,int color,Typeface font,Paint.Align align) {
        textPaint.setTypeface(font); textPaint.setFakeBoldText(true); textPaint.setTextSize(size); textPaint.setColor(color); textPaint.setTextAlign(align); c.drawText(s,x,y,textPaint);
    }
    private void fill(Canvas c,float l,float t,float r,float b,int color) { p.setColor(color); p.setStyle(Paint.Style.FILL); c.drawRect(l,t,r,b,p); }
    private void line(Canvas c,float x,float y,float ex,float ey,int color,float width) { p.setColor(color); p.setStrokeWidth(width); c.drawLine(x,y,ex,ey,p); }
    @SuppressLint("ClickableViewAccessibility")
    @Override public boolean onTouchEvent(MotionEvent event) {
        int action=event.getActionMasked(), idx=event.getActionIndex();
        float tx=(event.getX(idx)-left)/scale, ty=(event.getY(idx)-top)/scale;
        if(action==MotionEvent.ACTION_DOWN) pointer=-1;
        // Ignore initial taps on the scenery outside the fixed-aspect arena.
        // An existing drag may leave the arena; target() still clamps that movement.
        if((action==MotionEvent.ACTION_DOWN || action==MotionEvent.ACTION_POINTER_DOWN)
                && (tx<0 || tx>GameWorld.WIDTH || ty<0 || ty>GameWorld.HEIGHT)) return true;
        if(action==MotionEvent.ACTION_DOWN) {
            performClick();
            if(world.state==GameWorld.MENU) {
                if(ty>=674 && ty<=750 && tx>=32 && tx<=400) { world.start(); metricCount=metricIndex=0; }
                else if(ty>=315 && ty<=580 && tx>=24 && tx<=408) {
                    world.selectedShip=(world.selectedShip+(tx<110 ? Fleet.COUNT-1 : 1))%Fleet.COUNT;actions.saveSettings();
                }
                else if(ty>=588 && ty<=660) actions.campaign();
                else if(ty>=760 && ty<=805) {world.difficulty=(world.difficulty+1)%Campaign.DIFFICULTIES.length;actions.saveSettings();}
                else if(ty>=817 && ty<=875 && tx>240) actions.settings();
                return true;
            }
            if(world.state==GameWorld.PAUSED) {
                if(ty>=445 && ty<=521) world.resume();
                else if(ty>=541 && ty<=606) actions.settings();
                else if(ty>=627 && ty<=690) world.state=GameWorld.MENU;
                return true;
            }
            if(world.state==GameWorld.DEBRIEF) {
                for(int i=0;i<3;i++) if(tx>=32 && tx<=400 && ty>=398+i*108 && ty<=486+i*108) {
                    pointer=-1;world.chooseUpgrade(i);break;
                }
                return true;
            }
            if(world.state==GameWorld.LOST || world.state==GameWorld.WON) {
                if(ty>=605 && ty<=681) { world.start(); metricCount=metricIndex=0; }
                else if(ty>=695 && ty<=760) world.state=GameWorld.MENU;
                return true;
            }
            if(tx>=350 && ty<=90) { world.pause(); return true; }
            if(tx>=270 && ty>=815) { world.nova(); return true; }
            if(tx<=175 && ty>=815) { dash(); return true; }
            pointer=event.getPointerId(idx); touchX=tx; touchY=ty; startX=world.x; startY=world.y;
        } else if(action==MotionEvent.ACTION_POINTER_DOWN && world.state==GameWorld.PLAYING) {
            if(tx>=270 && ty>=815) world.nova();
            else if(tx<=175 && ty>=815) dash();
        } else if(action==MotionEvent.ACTION_MOVE && pointer!=-1 && world.state==GameWorld.PLAYING) {
            int i=event.findPointerIndex(pointer);
            if(i>=0) world.target(startX+(event.getX(i)-left)/scale-touchX,startY+(event.getY(i)-top)/scale-touchY);
        } else if(action==MotionEvent.ACTION_UP || action==MotionEvent.ACTION_CANCEL || (action==MotionEvent.ACTION_POINTER_UP && event.getPointerId(idx)==pointer)) pointer=-1;
        return true;
    }
    private void dash() {
        float oldX=world.x, oldY=world.y;world.dash();
        // Keep relative drag continuous after a second-finger dash.
        if(pointer!=-1) {startX+=world.x-oldX;startY+=world.y-oldY;}
    }
    @Override public boolean performClick() { super.performClick(); return true; }
}
