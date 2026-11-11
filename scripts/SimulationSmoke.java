package com.starfall.game;

/** Dependency-free long-run simulation soak. Also useful before the SDK is available. */
public final class SimulationSmoke {
    public static void main(String[] args) {
        long started=System.nanoTime(); int wins=0, attempts=36;
        for(int seed=0;seed<attempts;seed++) {
            GameWorld w=new GameWorld(seed,null); w.difficulty=(seed/3)%4;w.selectedShip=seed%3;w.start();
            for(int frame=0;frame<120*1200 && w.state!=GameWorld.WON;frame++) {
                if(w.state==GameWorld.DEBRIEF) w.chooseUpgrade(seed/12);
                w.invulnerable=10; // Test driver survives to exercise all wave and boss paths.
                float target=216;
                for(GameWorld.Entity e:w.entities) if(e.active && e.type>=GameWorld.SCOUT && e.type<=GameWorld.BOSS) target=e.x;
                w.target(target,700); w.update(GameWorld.STEP);
                if(!Float.isFinite(w.x) || !Float.isFinite(w.y)) throw new AssertionError("Invalid position");
                if(w.wave<1 || w.wave>Campaign.STAGES || w.health<0 || w.weapon>3) throw new AssertionError("Invalid game state");
            }
            if(w.state!=GameWorld.WON) throw new AssertionError("Run did not reach victory: seed="+seed+" wave="+w.wave);
            wins++;
            w.start(); if(w.score!=0 || w.state!=GameWorld.PLAYING) throw new AssertionError("Restart failed");
        }
        System.out.printf("PASS: %d/%d seeded full missions, boss victories and restarts in %.2f seconds%n",wins,attempts,(System.nanoTime()-started)/1e9);
    }
}
