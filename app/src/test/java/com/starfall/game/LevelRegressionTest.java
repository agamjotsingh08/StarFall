package com.starfall.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class LevelRegressionTest {
    @Test public void shipCooldownPerksSurviveEveryReactorUpgrade() {
        for(int mode=0;mode<4;mode++) for(int rank=0;rank<=11;rank++) {
            GameWorld v=new GameWorld(1,null),f=new GameWorld(1,null),b=new GameWorld(1,null);
            v.difficulty=f.difficulty=b.difficulty=mode;
            f.selectedShip=Fleet.FALCON;b.selectedShip=Fleet.BASTION;
            v.start();f.start();b.start();v.coolingRank=f.coolingRank=b.coolingRank=rank;
            assertEquals("Falcon dash perk: mode="+mode+" rank="+rank,v.dashCooldown()*.75f,f.dashCooldown(),.001f);
            assertEquals("Vanguard Nova perk: mode="+mode+" rank="+rank,b.novaCooldown()*.8f,v.novaCooldown(),.001f);
        }
    }
    @Test public void everyBossHasBothAttackPhasesAndDistinctEscalation() {
        for(int mode=0;mode<4;mode++) for(int stage=3;stage<=12;stage+=3) {
            GameWorld w=new GameWorld(5,null);w.difficulty=mode;w.unlockedStage=w.selectedStage=stage;w.start();
            GameWorld.Entity boss=w.spawn(GameWorld.BOSS,216,190,70);boss.variant=stage/3-1;
            boss.maxHp=1000;boss.hp=1000;boss.age=3.1f;
            w.enemyFire(boss);int normal=hostileCount(w);
            for(GameWorld.Entity e:w.entities) if(e.type==GameWorld.ENEMY_SHOT) e.active=false;
            boss.hp=499;w.enemyFire(boss);int enraged=hostileCount(w);
            assertTrue("Boss must fire in its first phase: "+stage,normal>=5);
            assertTrue("Overdrive must add pressure: "+stage,enraged>normal);
            for(GameWorld.Entity e:w.entities) if(e.active && e.type==GameWorld.ENEMY_SHOT) {
                assertTrue(Float.isFinite(e.vx));assertTrue(Float.isFinite(e.vy));assertTrue(Math.hypot(e.vx,e.vy)>0);
            }
        }
    }
    private int hostileCount(GameWorld w) {int n=0;for(GameWorld.Entity e:w.entities) if(e.active && e.type==GameWorld.ENEMY_SHOT)n++;return n;}
    @Test public void novaBossKillCompletesEachBossExactlyOnce() {
        for(int stage=3;stage<=12;stage+=3) {
            final int[] ended={0},unlocked={0};
            GameWorld w=new GameWorld(1,new GameWorld.Events() {
                public void sound(int kind) {}
                public void runEnded(int score,boolean won) {ended[0]++;assertTrue(won);}
                public void stageUnlocked(int stage) {unlocked[0]++;}
            });
            w.unlockedStage=w.selectedStage=stage;w.start();w.bossSpawned=true;
            GameWorld.Entity boss=w.spawn(GameWorld.BOSS,216,190,70);boss.hp=1;
            for(int i=0;i<80;i++) w.bullet(50+i*4,500,0,150,GameWorld.ENEMY_SHOT);
            w.nova();int score=w.score;w.nova();w.update(GameWorld.STEP);
            assertEquals(1,w.bossesDefeated);assertEquals(score,w.score);
            assertEquals(stage==12 ? GameWorld.WON : GameWorld.DEBRIEF,w.state);
            assertEquals(stage==12 ? 1 : 0,ended[0]);assertEquals(stage==12 ? 0 : 1,unlocked[0]);
            for(GameWorld.Entity e:w.entities) assertFalse(e.active);
        }
    }
    @Test public void allStagesIgnoreAbilitiesAndUpgradesWhilePausedOrDefeated() {
        for(int stage=1;stage<=12;stage++) {
            GameWorld w=new GameWorld(stage,null);w.unlockedStage=w.selectedStage=stage;w.start();
            w.pause();float x=w.x,y=w.y;w.nova();w.dash();w.chooseUpgrade(0);w.update(1);
            assertEquals(0,w.time,0);assertEquals(stage,w.wave);assertEquals(x,w.x,0);assertEquals(y,w.y,0);
            assertEquals(1,w.bombCharge,0);assertEquals(1,w.dashCharge,0);
            w.resume();while(w.state==GameWorld.PLAYING) {w.invulnerable=0;w.hurt();}
            int score=w.score;w.nova();w.dash();w.chooseUpgrade(1);w.update(1);
            assertEquals(GameWorld.LOST,w.state);assertEquals(0,w.health);assertEquals(stage,w.wave);assertEquals(score,w.score);
            w.start();assertEquals(GameWorld.PLAYING,w.state);assertEquals(stage,w.wave);assertEquals(w.maxHealth,w.health);
        }
    }
    @Test public void bossSpawnRetriesWhenPoolWasFull() {
        GameWorld w=new GameWorld(3,null);w.selectedStage=w.unlockedStage=3;w.start();w.waveTime=3;w.fireTimer=100;
        for(int i=0;i<320;i++) assertNotNull(w.spawn(GameWorld.SHOT,100,500,4));
        w.update(GameWorld.STEP);assertFalse(w.bossSpawned);
        w.entities[100].active=false;w.update(GameWorld.STEP);assertTrue(w.bossSpawned);
        assertEquals(1,w.enemiesRemaining());
    }
}
