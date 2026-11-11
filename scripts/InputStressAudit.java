package com.starfall.game;

import java.util.Random;

/** Damage is enabled: random steering and ability use exercise defeat and reset paths. */
public final class InputStressAudit {
    public static void main(String[] args) {
        int lost=0,cleared=0,timed=0,total=0;
        for(int stage=1;stage<=12;stage++) {
            int stageLost=0,stageCleared=0;
            for(int mode=0;mode<4;mode++) for(int ship=0;ship<3;ship++) for(int seed=0;seed<2;seed++) {
                Random input=new Random(stage*10000L+mode*100+ship*10+seed);
                GameWorld w=new GameWorld(input.nextLong(),null);w.difficulty=mode;w.selectedShip=ship;
                w.unlockedStage=w.selectedStage=stage;w.start();
                for(int frame=0;frame<120*90 && w.state==GameWorld.PLAYING;frame++) {
                    if(frame%45==0) w.target(input.nextFloat()*600-80,400+input.nextFloat()*500);
                    if(input.nextInt(120)==0) w.dash();
                    if(input.nextInt(360)==0) w.nova();
                    if(frame%600==300) {
                        float t=w.time;w.pause();w.nova();w.dash();w.update(.5f);
                        check(w.state==GameWorld.PAUSED && w.time==t,"pause",stage,mode,ship);w.resume();
                    }
                    w.update(GameWorld.STEP);
                    check(w.health>=0&&w.health<=w.maxHealth,"hull",stage,mode,ship);
                    check(Float.isFinite(w.x)&&Float.isFinite(w.y)&&w.x>=24&&w.x<=408&&w.y>=140&&w.y<=805,"bounds",stage,mode,ship);
                    check(w.bombCharge>=0&&w.bombCharge<=1&&w.dashCharge>=0&&w.dashCharge<=1,"charges",stage,mode,ship);
                }
                if(w.state==GameWorld.LOST) {
                    lost++;stageLost++;check(w.health==0,"defeat hull",stage,mode,ship);
                    int score=w.score;w.nova();w.dash();w.update(1);check(w.score==score&&w.state==GameWorld.LOST,"defeat freeze",stage,mode,ship);
                } else if(w.state==GameWorld.DEBRIEF || w.state==GameWorld.WON) {cleared++;stageCleared++;}
                else timed++;
                w.start();check(w.health==w.maxHealth&&w.score==0&&w.wave==stage,"restart",stage,mode,ship);
                total++;
            }
            System.out.printf("Stage %02d: 24 input-stress runs, %d defeats, %d clears%n",stage,stageLost,stageCleared);
        }
        if(lost==0 || cleared==0) throw new AssertionError("Driver must exercise both loss and clear paths");
        System.out.printf("PASS: %d runs, %d defeats, %d clears, %d still playing at 90s; no invalid state%n",total,lost,cleared,timed);
    }
    private static void check(boolean ok,String what,int stage,int mode,int ship) {
        if(!ok) throw new AssertionError(what+" stage="+stage+" mode="+mode+" ship="+ship);
    }
}
