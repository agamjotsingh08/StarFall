package com.starfall.game;

import org.junit.Test;
import static org.junit.Assert.*;

public class GameWorldTest {
    GameWorld game() { GameWorld w=new GameWorld(42,null); w.difficulty=Campaign.VETERAN; w.start(); return w; }
    @Test public void pauseFreezesCombatAndResumeContinues() {
        GameWorld w=game(); w.update(.01f); float t=w.time; w.pause(); w.update(1);
        assertEquals(t,w.time,0); w.resume(); w.update(.01f); assertTrue(w.time>t);
    }
    @Test public void controlsStayInsideArena() {
        GameWorld w=game(); w.target(-1000,9999);
        assertEquals(24,w.targetX,0); assertEquals(805,w.targetY,0);
        for(int i=0;i<120;i++) w.update(GameWorld.STEP);
        assertTrue(w.x>=24 && w.y<=805);
    }
    @Test public void damageInvulnerabilityAndDefeatAreConsistent() {
        GameWorld w=game(); w.invulnerable=0; w.hurt(); w.hurt(); assertEquals(2,w.health);
        w.invulnerable=0; w.hurt(); w.invulnerable=0; w.hurt();
        assertEquals(GameWorld.LOST,w.state); w.hurt(); assertEquals(0,w.health);
    }
    @Test public void novaConsumesChargeAndClearsHostileBullets() {
        GameWorld w=game(); w.bullet(30,50,0,100,GameWorld.ENEMY_SHOT);
        w.nova(); assertEquals(0,w.bombCharge,0);
        for(GameWorld.Entity e:w.entities) assertFalse(e.active && e.type==GameWorld.ENEMY_SHOT);
        w.bullet(30,50,0,100,GameWorld.ENEMY_SHOT); w.nova();
        boolean found=false; for(GameWorld.Entity e:w.entities) found|=e.active && e.type==GameWorld.ENEMY_SHOT; assertTrue(found);
    }
    @Test public void fastProjectileHitsEnemyAndAwardsScore() {
        GameWorld w=game(); w.spawnTimer=100; w.fireTimer=100;
        GameWorld.Entity e=w.spawn(GameWorld.SCOUT,216,400,20); e.hp=2; e.phase=0;
        w.bullet(216,430,0,-6000,GameWorld.SHOT); w.update(GameWorld.STEP);
        assertEquals(1,w.kills); assertTrue(w.score>0);
    }
    @Test public void restartResetsTheWholeRunButKeepsPreferences() {
        GameWorld w=game(); w.best=12345; w.quality=0; w.score=300; w.weapon=3; w.health=1; w.start();
        assertEquals(0,w.score); assertEquals(3,w.health); assertEquals(1,w.weapon); assertEquals(12345,w.best); assertEquals(0,w.quality);
        for(GameWorld.Entity e:w.entities) assertFalse(e.active);
    }
    @Test public void upgradesAndRepairsHaveCaps() {
        GameWorld w=game(); w.weapon=3; w.pickup(w.x,w.y,GameWorld.UPGRADE); w.update(GameWorld.STEP); assertEquals(3,w.weapon);
        w.pickup(w.x,w.y,GameWorld.REPAIR); w.update(GameWorld.STEP); assertEquals(3,w.health);
    }
    @Test public void bossDefeatFinishesAndRetainsHighScore() {
        GameWorld w=game(); w.wave=12; GameWorld.Entity e=w.spawn(GameWorld.BOSS,216,190,70); w.destroy(e);
        assertEquals(GameWorld.WON,w.state); assertEquals(w.score,w.best); assertTrue(w.score>=5000);
    }
    @Test public void effectsCannotStarveGameplayPool() {
        GameWorld w=game(); w.wave=12; w.sparks(216,300,2000,1);
        GameWorld.Entity boss=w.spawn(GameWorld.BOSS,216,190,70);
        assertNotNull(boss); w.destroy(boss); assertEquals(GameWorld.WON,w.state);
    }
    @Test public void inactivePilotCannotSurviveFirstOperation() {
        GameWorld w=game();
        for(int i=0;i<120*40 && w.state==GameWorld.PLAYING;i++) w.update(GameWorld.STEP);
        assertEquals("Stationary play must not clear this difficulty",GameWorld.LOST,w.state);
    }
    @Test public void stageClearWaitsForChoiceAndUnlocksNextOperation() {
        GameWorld w=game(); w.completeStage();
        assertEquals(GameWorld.DEBRIEF,w.state); assertEquals(2,w.unlockedStage);
        float t=w.time; w.update(10); assertEquals(t,w.time,0);
        w.chooseUpgrade(0); assertEquals(2,w.wave); assertEquals(GameWorld.PLAYING,w.state);
        assertEquals(2,w.weapon); assertEquals(1,w.powerRank);
        w.chooseUpgrade(0); assertEquals("Choice cannot apply twice",2,w.wave);
    }
    @Test public void intermediateBossContinuesCampaignInsteadOfEndingGame() {
        GameWorld w=game();w.wave=3;GameWorld.Entity boss=w.spawn(GameWorld.BOSS,216,190,70);w.destroy(boss);
        assertEquals(GameWorld.DEBRIEF,w.state);assertEquals(4,w.unlockedStage);
        w.chooseUpgrade(2);assertEquals(4,w.wave);assertEquals(1,w.coolingRank);
    }
    @Test public void dashUsesSteeringDirectionAndHasCooldown() {
        GameWorld w=game();w.target(300,w.y);float old=w.x;w.dash();
        assertTrue(w.x>old);assertEquals(0,w.dashCharge,0);assertTrue(w.invulnerable>=.42f);
        float after=w.x;w.dash();assertEquals(after,w.x,0);
    }
    @Test public void hullUpgradeAddsCapacityOnlyWhenFullyRepaired() {
        GameWorld w=game();w.completeStage();w.chooseUpgrade(1);assertEquals(4,w.maxHealth);assertEquals(4,w.health);
        w.health=1;w.completeStage();w.chooseUpgrade(1);assertEquals(4,w.maxHealth);assertEquals(3,w.health);
    }
    @Test public void lockedMissionCannotBeStartedAndNightmareIsHarder() {
        GameWorld w=game();w.selectedStage=12;w.start();assertEquals(1,w.wave);
        assertTrue(Campaign.bulletSpeed(1,Campaign.NIGHTMARE)>Campaign.bulletSpeed(1,Campaign.VETERAN));
        assertTrue(Campaign.formationInterval(1,Campaign.NIGHTMARE)<Campaign.formationInterval(1,Campaign.VETERAN));
        w.difficulty=Campaign.NIGHTMARE;w.start();w.difficulty=0;assertEquals("Difficulty is fixed for the run",Campaign.NIGHTMARE,w.runDifficulty);
    }
    @Test public void casualIsDefaultAndGivesRoomToRecover() {
        GameWorld w=new GameWorld(42,null);w.start();
        assertEquals(Campaign.CASUAL,w.runDifficulty);assertEquals(5,w.health);
        assertTrue(w.spawnTimer>=2);w.invulnerable=0;w.hurt();assertTrue(w.invulnerable>=1.8f);
        w.health=2;w.completeStage();w.chooseUpgrade(0);assertEquals(3,w.health);
        assertTrue(w.dashCooldown()<game().dashCooldown());assertTrue(w.novaCooldown()<game().novaCooldown());
    }
    @Test public void casualSurvivesLongerThanVeteranAcrossSeeds() {
        float casualTime=0,veteranTime=0;
        for(int seed=0;seed<20;seed++) {
            for(int mode:new int[]{Campaign.CASUAL,Campaign.VETERAN}) {
                GameWorld w=new GameWorld(seed,null);w.difficulty=mode;w.start();
                for(int frame=0;frame<120*40 && w.state==GameWorld.PLAYING;frame++) w.update(GameWorld.STEP);
                if(mode==Campaign.CASUAL) casualTime+=w.time;else veteranTime+=w.time;
            }
        }
        assertTrue("Casual should give at least twice the average reaction time",casualTime>veteranTime*2);
    }
    @Test public void shipLoadoutIsFixedForRunAndChangesActualAbilities() {
        GameWorld w=new GameWorld(42,null);w.selectedShip=Fleet.FALCON;w.start();
        float falconDash=w.dashCooldown();assertEquals(4,w.health);
        w.selectedShip=Fleet.BASTION;assertEquals(Fleet.FALCON,w.runShip);
        w.start();assertEquals(Fleet.BASTION,w.runShip);assertEquals(6,w.health);assertTrue(w.dashCooldown()>falconDash);
        float bastionNova=w.novaCooldown();w.selectedShip=Fleet.VANGUARD;w.start();assertTrue(w.novaCooldown()<bastionNova);
    }
    @Test public void falconFiresMoreOftenThanVanguard() {
        int[] counts=new int[Fleet.COUNT];
        for(int ship=0;ship<Fleet.COUNT;ship++) {
            GameWorld w=new GameWorld(42,null);w.selectedShip=ship;w.start();w.spawnTimer=100;
            for(int frame=0;frame<120;frame++) w.update(GameWorld.STEP);
            for(GameWorld.Entity e:w.entities) if(e.active && e.type==GameWorld.SHOT) counts[ship]++;
        }
        assertTrue(counts[Fleet.FALCON]>counts[Fleet.VANGUARD]);
    }
    @Test public void bastionCollectsSuppliesFromFartherAway() {
        GameWorld w=new GameWorld(42,null);w.selectedShip=Fleet.BASTION;w.start();w.health=2;w.spawnTimer=100;
        w.pickup(w.x+140,w.y,GameWorld.REPAIR);
        for(int frame=0;frame<120;frame++) w.update(GameWorld.STEP);
        assertEquals(3,w.health);
        GameWorld v=new GameWorld(42,null);v.start();v.health=2;v.spawnTimer=100;v.pickup(v.x+140,v.y,GameWorld.REPAIR);
        for(int frame=0;frame<120;frame++) v.update(GameWorld.STEP);
        assertEquals(2,v.health);
    }
    @Test public void casualDropsRepairAfterEightKillsAndHullCannotOverfill() {
        GameWorld w=new GameWorld(42,null);w.selectedShip=Fleet.BASTION;w.start();w.health=2;w.kills=7;
        w.destroy(w.spawn(GameWorld.SCOUT,100,100,20));
        boolean repair=false;for(GameWorld.Entity e:w.entities) repair|=e.active && e.type==GameWorld.REPAIR;
        assertTrue(repair);
        w.health=w.maxHealth;w.completeStage();w.chooseUpgrade(1);assertEquals(7,w.maxHealth);assertEquals(7,w.health);
        w.completeStage();w.chooseUpgrade(1);assertEquals(7,w.maxHealth);assertEquals(7,w.health);
    }
    @Test public void fixedSeedProducesDeterministicSimulation() {
        GameWorld a=game(),b=game();
        for(int i=0;i<120*60;i++) { a.invulnerable=b.invulnerable=10; a.update(GameWorld.STEP); b.update(GameWorld.STEP); }
        assertEquals(a.score,b.score); assertEquals(a.wave,b.wave); assertEquals(a.kills,b.kills);
        for(int i=0;i<a.entities.length;i++){assertEquals(a.entities[i].active,b.entities[i].active);assertEquals(a.entities[i].x,b.entities[i].x,0);}
    }
    @Test public void graphicsQualityDoesNotChangeCombatRandomness() {
        GameWorld low=game(),ultra=game();low.quality=0;ultra.quality=2;
        for(int frame=0;frame<120*35;frame++) {
            low.invulnerable=ultra.invulnerable=10;
            if(frame%60==0) {low.sparks(100,100,30,1);ultra.sparks(100,100,30,1);}
            low.update(GameWorld.STEP);ultra.update(GameWorld.STEP);
        }
        assertEquals(low.score,ultra.score);assertEquals(low.kills,ultra.kills);assertEquals(low.stageEscaped,ultra.stageEscaped);
        for(int i=0;i<320;i++) {
            assertEquals(low.entities[i].active,ultra.entities[i].active);
            assertEquals(low.entities[i].x,ultra.entities[i].x,0);
            assertEquals(low.entities[i].y,ultra.entities[i].y,0);
        }
    }
    @Test public void cappedPickupsReportSalvageInsteadOfAFalseUpgrade() {
        GameWorld w=game();w.weapon=3;w.spawnTimer=100;
        w.pickup(w.x,w.y,GameWorld.UPGRADE);w.update(GameWorld.STEP);
        assertEquals(3,w.weapon);assertEquals(100,w.score);assertEquals("SALVAGE RECOVERED",w.bannerTitle);
        w.pickup(w.x,w.y,GameWorld.REPAIR);w.update(GameWorld.STEP);
        assertEquals(w.maxHealth,w.health);assertEquals(200,w.score);assertEquals("SALVAGE RECOVERED",w.bannerTitle);
    }
}
