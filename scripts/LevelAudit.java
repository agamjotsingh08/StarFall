package com.starfall.game;

/** Exhaustive configuration matrix. Invulnerability isolates progression from player skill. */
public final class LevelAudit {
    public static void main(String[] args) {
        long started=System.nanoTime();int total=0;
        System.out.println("stage,name,cases,normal_phase_cases,overdrive_cases,max_seconds,peak_gameplay_objects");
        for(int stage=1;stage<=Campaign.STAGES;stage++) {
            int cases=0,normal=0,overdrive=0,peak=0;float longest=0;
            for(int mode=0;mode<4;mode++) for(int ship=0;ship<Fleet.COUNT;ship++) for(int seed=0;seed<3;seed++) {
                GameWorld w=new GameWorld(seed*997L+stage,null);w.difficulty=mode;w.selectedShip=ship;
                w.quality=seed;w.unlockedStage=w.selectedStage=stage;w.start();
                boolean sawNormal=false,sawOverdrive=false;
                String context="stage="+stage+" mode="+mode+" ship="+ship+" seed="+seed;
                for(int frame=0;frame<120*180 && w.state==GameWorld.PLAYING;frame++) {
                    w.invulnerable=10;
                    float tx=216;
                    for(GameWorld.Entity e:w.entities) if(e.active && e.type>=GameWorld.SCOUT && e.type<=GameWorld.BOSS) {
                        tx=e.x;
                        if(e.type==GameWorld.BOSS) {sawNormal|=e.hp>=e.maxHp*.5f;sawOverdrive|=e.hp<e.maxHp*.5f;}
                    }
                    w.target(tx,700);
                    if(frame%1200==600) w.nova();
                    if(frame%900==450) {w.headingX=1;w.headingY=0;w.dash();}
                    if(frame%800==400) {
                        float before=w.time;int score=w.score;w.pause();w.update(.5f);
                        require(w.time==before && w.score==score,"pause altered combat "+context);w.resume();
                    }
                    w.update(GameWorld.STEP);
                    require(Float.isFinite(w.x)&&Float.isFinite(w.y)&&w.x>=24&&w.x<=408&&w.y>=140&&w.y<=805,"position "+context);
                    require(w.health>0&&w.health<=w.maxHealth&&w.weapon>=1&&w.weapon<=3,"loadout "+context);
                    require(w.bombCharge>=0&&w.bombCharge<=1&&w.dashCharge>=0&&w.dashCharge<=1,"ability charge "+context);
                    if(frame%30==0) {
                        int active=0;
                        for(int i=0;i<w.entities.length;i++) {
                            GameWorld.Entity e=w.entities[i];if(!e.active)continue;
                            require(Float.isFinite(e.x)&&Float.isFinite(e.y)&&Float.isFinite(e.vx)&&Float.isFinite(e.vy),"entity "+context);
                            require(i<320 ? e.type!=GameWorld.SPARK : e.type==GameWorld.SPARK,"pool separation "+context);
                            if(i<320)active++;
                        }
                        peak=Math.max(peak,active);
                    }
                }
                require(w.state==(stage==12 ? GameWorld.WON : GameWorld.DEBRIEF),"stage stalled "+context+" time="+w.time);
                if(Campaign.boss(stage)) require(sawNormal&&sawOverdrive&&w.bossesDefeated==1,"boss phases "+context);
                require(w.best>=w.score,"high score "+context);
                for(GameWorld.Entity e:w.entities) require(!e.active,"uncleared stage objects "+context);
                longest=Math.max(longest,w.time);normal+=sawNormal?1:0;overdrive+=sawOverdrive?1:0;
                if(stage<12) {
                    require(w.unlockedStage==stage+1,"unlock "+context);
                    w.chooseUpgrade(seed);require(w.wave==stage+1&&w.state==GameWorld.PLAYING,"advance "+context);
                }
                w.start();require(w.wave==stage&&w.score==0&&w.health==w.maxHealth,"replay reset "+context);
                cases++;total++;
            }
            System.out.printf(java.util.Locale.US,"%d,%s,%d,%d,%d,%.2f,%d%n",stage,Campaign.name(stage),cases,normal,overdrive,longest,peak);
        }
        System.out.printf(java.util.Locale.US,"PASS: %d independent level configurations in %.2fs%n",total,(System.nanoTime()-started)/1e9);
    }
    private static void require(boolean ok,String detail) {if(!ok)throw new AssertionError(detail);}
}
