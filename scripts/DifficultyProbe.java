package com.starfall.game;
import java.util.Arrays;
public final class DifficultyProbe {
    public static void main(String[] args) {
        for(int difficulty=0;difficulty<Campaign.DIFFICULTIES.length;difficulty++) {
            float[] survival=new float[20];
            int clears=0;
            for(int seed=0;seed<survival.length;seed++) {
                GameWorld w=new GameWorld(seed,null);w.difficulty=difficulty;w.start();
                for(int i=0;i<120*60 && w.state==GameWorld.PLAYING;i++) w.update(GameWorld.STEP);
                if(w.state==GameWorld.DEBRIEF) clears++;
                survival[seed]=w.time;
            }
            Arrays.sort(survival);
            System.out.printf("%s stationary-pilot survival: median %.1fs, range %.1f-%.1fs, %d clears (20 seeds)%n",Campaign.DIFFICULTIES[difficulty],survival[10],survival[0],survival[19],clears);
        }
    }
}
