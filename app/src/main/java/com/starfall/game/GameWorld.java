package com.starfall.game;

import java.util.Random;

/** Pure simulation. All coordinates use a 432 x 900 arena, independent of device pixels. */
public final class GameWorld {
    public static final float WIDTH = 432, HEIGHT = 900, STEP = 1f / 120f;
    public static final int MENU = 0, PLAYING = 1, PAUSED = 2, LOST = 3, WON = 4, DEBRIEF = 5;
    public static final int SHOT = 1, ENEMY_SHOT = 2, SCOUT = 3, FIGHTER = 4,
            HEAVY = 5, BOSS = 6, UPGRADE = 7, REPAIR = 8, SPARK = 9;
    public interface Events {
        void sound(int kind); void runEnded(int score, boolean won);
        default void stageUnlocked(int stage) {}
    }
    public static final class Entity {
        public boolean active;
        public boolean grazed;
        public int type, hp, maxHp, variant;
        public float x, y, vx, vy, radius, age, life, cooldown, originX, phase, hit;
    }
    public final Entity[] entities = new Entity[768];
    public final Random random;
    private final Random effectRandom;
    public final Events events;
    public int state = MENU, score, best, health = 3, wave = 1, weapon = 1, kills, combo, maxCombo;
    public int quality = 2, difficulty, runDifficulty, selectedStage = 1, unlockedStage = 1;
    public int selectedShip = Fleet.VANGUARD, runShip = Fleet.VANGUARD;
    public int maxHealth = 3, powerRank, coolingRank, stageKills, stageEscaped, stageDamage, grazes, bossesDefeated;
    public String stageGrade = "A";
    public float dashCharge = 1, dashFlash, dashFromX, dashFromY, headingX, headingY = -1;
    public boolean soundEnabled = true, showStats;
    public float x = WIDTH / 2, y = 740, targetX = x, targetY = y, bank;
    public float time, ambientTime, waveTime, fireTimer, spawnTimer, invulnerable, shake;
    public float banner = 3, comboTime, bombCharge = 1, blast, damageFlash;
    public boolean bossSpawned;
    public String bannerTitle = "SECTOR 01", bannerSubtitle = "THE OUTER RIM";
    public static final int S_FIRE = 0, S_HIT = 1, S_BOOM = 2, S_PICKUP = 3, S_HURT = 4, S_NOVA = 5;

    public GameWorld(long seed, Events events) {
        random = new Random(seed); effectRandom = new Random(seed ^ 0x5DEECE66DL); this.events = events;
        for (int i = 0; i < entities.length; i++) entities[i] = new Entity();
    }
    public void start() {
        for (Entity e : entities) e.active = false;
        state = PLAYING; score = 0;
        runDifficulty = Math.max(Campaign.CASUAL, Math.min(Campaign.NIGHTMARE, difficulty));
        runShip = Fleet.valid(selectedShip);
        maxHealth = health = Campaign.hull(runDifficulty)+Fleet.hullBonus(runShip);
        wave = Math.max(1, Math.min(unlockedStage, selectedStage));
        weapon = Math.min(3,1+(wave-1)/3); powerRank = (wave-1)/2; coolingRank = 0; kills = 0;
        stageKills = stageEscaped = stageDamage = grazes = bossesDefeated = 0;
        combo = maxCombo = 0; comboTime = 0; x = targetX = WIDTH / 2; y = targetY = 740;
        time = waveTime = fireTimer = 0; spawnTimer = runDifficulty == Campaign.CASUAL ? 2.5f : 1; bank = 0;
        invulnerable = 1.2f; shake = blast = damageFlash = dashFlash = 0;
        bombCharge = dashCharge = 1; headingX = 0; headingY = -1; bossSpawned = false;
        announce("OPERATION " + stageNumber(), Campaign.name(wave));
    }
    public void pause() { if (state == PLAYING) state = PAUSED; }
    public void resume() { if (state == PAUSED) state = PLAYING; }
    public void target(float tx, float ty) {
        float dx = tx-targetX, dy = ty-targetY, distance = (float)Math.hypot(dx,dy);
        if (distance > 2) { headingX = dx/distance; headingY = dy/distance; }
        targetX = clamp(tx, 24, WIDTH - 24); targetY = clamp(ty, 140, HEIGHT - 95);
    }
    public void update(float dt) {
        ambientTime += dt;
        if (state != PLAYING) return;
        time += dt; waveTime += dt;
        invulnerable = Math.max(0, invulnerable - dt);
        shake = Math.max(0, shake - dt * 16);
        blast = Math.max(0, blast - dt * 1.8f);
        damageFlash = Math.max(0, damageFlash - dt * 2.5f);
        dashFlash = Math.max(0, dashFlash - dt * 3);
        banner = Math.max(0, banner - dt);
        comboTime -= dt; if (comboTime <= 0) combo = 0;
        bombCharge = Math.min(1, bombCharge + dt / novaCooldown());
        dashCharge = Math.min(1, dashCharge + dt / dashCooldown());
        float oldX = x;
        x += (targetX - x) * Math.min(1, dt * 22); y += (targetY - y) * Math.min(1, dt * 22);
        bank += (clamp((x - oldX) * 8, -18, 18) - bank) * dt * 12;
        fireTimer -= dt;
        if (fireTimer <= 0) {
            fireTimer += (weapon == 3 ? .115f : .15f) / ((1+powerRank*.04f)*Fleet.fireRate(runShip));
            bullet(x - 10, y - 27, -8, -850, SHOT);
            bullet(x + 10, y - 27, 8, -850, SHOT);
            if (weapon >= 2) { bullet(x - 23, y - 12, -140, -790, SHOT); bullet(x + 23, y - 12, 140, -790, SHOT); }
            if (weapon >= 3) bullet(x, y - 36, 0, -1000, SHOT);
            sound(S_FIRE);
        }
        if (!Campaign.boss(wave)) {
            spawnTimer -= dt;
            if (spawnTimer <= 0 && waveTime < Campaign.duration(wave)) {
                formation(); spawnTimer = Campaign.formationInterval(wave,runDifficulty);
            }
            if (waveTime >= Campaign.duration(wave)+2 && enemiesRemaining() == 0) {
                completeStage(); return;
            }
        } else if (!bossSpawned && waveTime > 2) {
            Entity boss = spawn(BOSS, WIDTH / 2, -100, 70);
            if (boss != null) {
                boss.hp = boss.maxHp = (int)((1050+wave*70)*Campaign.durability(runDifficulty));
                boss.cooldown = 1; boss.vy = 100; boss.variant = wave/3-1; bossSpawned = true;
            }
        }
        for (Entity e : entities) {
            if (!e.active) continue;
            e.age += dt; e.hit = Math.max(0, e.hit - dt * 6);
            if (e.type == SPARK) {
                e.x += e.vx * dt; e.y += e.vy * dt; e.vx *= 1 - dt * 2; e.vy *= 1 - dt * 2;
                e.life -= dt; if (e.life <= 0) e.active = false;
                continue;
            }
            if (e.type >= SCOUT && e.type <= BOSS) {
                if (e.type == BOSS) {
                    e.y = Math.min(190, e.y + e.vy * dt);
                    e.x = WIDTH / 2 + (float)Math.sin(e.age * (.65f+wave*.018f)) * 112;
                } else if (e.type == FIGHTER && e.age > 1.8f) {
                    if (e.variant == 0) {
                        e.variant = 1; e.vx = clamp((x-e.x)*.6f,-130,130); e.vy *= 1.7f;
                    }
                    e.x += e.vx*dt; e.y += e.vy*dt;
                } else {
                    if (e.type != HEAVY || e.age < 2.3f || e.age > 6.5f) e.y += e.vy * dt;
                    e.x = clamp(e.originX + (float)Math.sin(e.age * 1.9 + e.phase) * (e.type == SCOUT ? 34 : 18), 24, WIDTH - 24);
                }
                e.cooldown -= dt;
                if (e.cooldown <= 0 && e.y > 110 && e.y < 610) {
                    enemyFire(e);
                    e.cooldown = (e.type == BOSS ? (e.hp < e.maxHp*.5f ? .5f : .75f) :
                        Math.max(.65f,1.65f-wave*.045f)+random.nextFloat()*.45f)*Campaign.firingDelay(runDifficulty);
                }
                if (circle(e.x, e.y, e.radius * .72f, x, y, 11)) hurt();
                if (e.y > HEIGHT + 80 || e.x < -70 || e.x > WIDTH+70) { e.active = false; stageEscaped++; combo = 0; }
            } else if (e.type == SHOT || e.type == ENEMY_SHOT) {
                float oldY = e.y;
                e.x += e.vx * dt; e.y += e.vy * dt;
                if (e.type == SHOT) {
                    for (Entity enemy : entities) {
                        if (!enemy.active || enemy.type < SCOUT || enemy.type > BOSS) continue;
                        // Swept vertical interval prevents fast projectiles tunneling through enemies.
                        float nearY = clamp(enemy.y, Math.min(oldY, e.y), Math.max(oldY, e.y));
                        if (circle(e.x, nearY, 4, enemy.x, enemy.y, enemy.radius * .8f)) {
                            e.active = false; enemy.hp -= 2+powerRank/2; enemy.hit = 1;
                            if (enemy.hp <= 0) destroy(enemy); else if (random.nextFloat() < .18f) sparks(e.x, e.y, 2, 0);
                            break;
                        }
                    }
                } else if (circle(e.x, e.y, e.radius, x, y, 10)) { e.active = false; hurt(); }
                else if (!e.grazed && circle(e.x,e.y,e.radius,x,y,27)) {
                    e.grazed = true; grazes++; score += 8; bombCharge = Math.min(1,bombCharge+.006f);
                }
                if (e.y < -80 || e.y > HEIGHT + 50 || e.x < -60 || e.x > WIDTH + 60) e.active = false;
            } else {
                e.y += 105 * dt;
                float dx=x-e.x, dy=y-e.y, distance=(float)Math.hypot(dx,dy);
                if(distance < Fleet.magnetRadius(runShip) && distance > 1) {
                    float pull=Math.min(distance,280*dt); e.x+=dx/distance*pull; e.y+=dy/distance*pull;
                }
                if (circle(e.x, e.y, 26, x, y, 26)) {
                    if (e.type == UPGRADE) {
                        if(weapon<3) {weapon++;announce("WEAPONS UP",weapon==3 ? "PLASMA OVERDRIVE" : "QUAD CANNONS ONLINE");}
                        else announce("SALVAGE RECOVERED","+100 SCORE / WEAPONS MAXED");
                    } else {
                        if(health<maxHealth) {health++;announce("HULL RESTORED","STRUCTURAL INTEGRITY STABILIZED");}
                        else announce("SALVAGE RECOVERED","+100 SCORE / HULL INTACT");
                    }
                    score += 100; e.active = false; sound(S_PICKUP); sparks(e.x, e.y, 20, 0);
                }
                if (e.y > HEIGHT + 40) e.active = false;
            }
            if (state != PLAYING) break;
        }
    }
    void formation() {
        int count = (wave >= 7 ? 5 : 4)-(runDifficulty <= Campaign.STANDARD ? 1 : 0);
        float spacing = 340f/(count-1), start = 46;
        for (int i = 0; i < count; i++) {
            int type = i == 1 && (wave >= 2 || (runDifficulty >= Campaign.VETERAN && random.nextFloat()<.3f)) ? HEAVY : (random.nextFloat() < .4f ? FIGHTER : SCOUT);
            Entity e = spawn(type, start + i * spacing, -40 - i * 28, type == HEAVY ? 27 : 20);
            if (e == null) break;
            e.hp = e.maxHp = Math.max(2,(int)((type == HEAVY ? 30+wave*4 : type == FIGHTER ? 12+wave*2 : 7+wave*2)*Campaign.durability(runDifficulty)));
            e.vy = (95 + wave * 4 + random.nextFloat() * 28)*(runDifficulty == Campaign.CASUAL ? .8f : 1);
            e.cooldown = (1 + random.nextFloat()*.8f)*Campaign.firingDelay(runDifficulty);
        }
    }
    void enemyFire(Entity e) {
        float speed = Campaign.bulletSpeed(wave,runDifficulty);
        if (e.type == BOSS) {
            float angle = (float)Math.atan2(y - e.y, x - e.x);
            boolean enraged = e.hp < e.maxHp*.5f;
            int count = (enraged ? 9 : 7)-(runDifficulty == Campaign.CASUAL ? 2 : 0);
            for (int i = 0; i < count; i++) {
                float a = angle + (i - (count - 1) / 2f) * .16f;
                bullet(e.x, e.y + 45, (float)Math.cos(a) * speed, (float)Math.sin(a) * speed, ENEMY_SHOT);
            }
            if (e.variant >= 1) {
                for (int side=-1;side<=1;side+=2) {
                    float a = (float)Math.PI/2+(float)Math.sin(e.age*1.4f)*.8f*side;
                    bullet(e.x+side*52,e.y+25,(float)Math.cos(a)*speed*.8f,(float)Math.sin(a)*speed*.8f,ENEMY_SHOT);
                }
            }
            if ((enraged || e.variant >= 2) && (int)(e.age*2)%3 == 0) for (int i=0;i<16;i++) {
                float a = i*(float)Math.PI/8+e.age*.24f;
                bullet(e.x,e.y,(float)Math.cos(a)*speed*.65f,(float)Math.sin(a)*speed*.65f,ENEMY_SHOT);
            }
            if (e.variant == 3 && enraged) {
                for(int i=0;i<7;i++) bullet(42+i*58,150,0,speed*.72f,ENEMY_SHOT);
            }
        } else {
            float a = (float)Math.atan2(y - e.y, x - e.x);
            bullet(e.x, e.y + 16, (float)Math.cos(a) * speed, (float)Math.sin(a) * speed, ENEMY_SHOT);
            if (e.type == HEAVY || (e.type == FIGHTER && wave >= 5)) {
                for(int side=-1;side<=1;side+=2) bullet(e.x,e.y+16,(float)Math.cos(a+side*.22f)*speed,(float)Math.sin(a+side*.22f)*speed,ENEMY_SHOT);
            }
        }
    }
    Entity spawn(int type, float sx, float sy, float radius) {
        // Separate effect slots prevent explosions from starving gameplay objects or
        // recycling an enemy reference while collision handling still uses it.
        int first = type == SPARK ? 320 : 0, end = type == SPARK ? entities.length : 320;
        for (int i = first; i < end; i++) {
            Entity e = entities[i];
            if (e.active) continue;
            e.active = true; e.type = type; e.x = e.originX = sx; e.y = sy; e.radius = radius;
            e.age = e.vx = e.vy = e.hit = 0; e.life = 1; e.hp = e.maxHp = 1; e.cooldown = 1;
            e.phase = type == SPARK ? 0 : random.nextFloat() * 6.28f; e.variant = 0; e.grazed = false; return e;
        }
        return null;
    }
    void bullet(float bx, float by, float vx, float vy, int type) {
        Entity e = spawn(type, bx, by, type == SHOT ? 4 : 5);
        if (e != null) { e.vx = vx; e.vy = vy; }
    }
    void pickup(float px, float py, int type) { spawn(type, px, py, 18); }
    void destroy(Entity enemy) {
        int type = enemy.type; float ex = enemy.x, ey = enemy.y;
        enemy.active = false; kills++; stageKills++; combo++; maxCombo = Math.max(maxCombo, combo); comboTime = 3.5f;
        score += (type == BOSS ? 5000 : type == HEAVY ? 120 : 60) * Math.min(4, 1 + combo / 10);
        shake = Math.max(shake, type == BOSS ? 10 : 3);
        sparks(ex, ey, type == BOSS ? 110 : 28, 1); sound(S_BOOM);
        if (type == BOSS) { bossesDefeated++; completeStage(); return; }
        int repairEvery=runDifficulty == Campaign.CASUAL ? 8 : runDifficulty == Campaign.STANDARD ? 14 : 40;
        int upgradeEvery=runDifficulty <= Campaign.STANDARD ? 10 : 20;
        if (health < maxHealth && kills%repairEvery==0) pickup(ex,ey,REPAIR);
        if (kills%upgradeEvery==0) pickup(ex+12,ey,UPGRADE);
    }
    void hurt() {
        if (state != PLAYING || invulnerable > 0) return;
        health--; stageDamage++; invulnerable = Campaign.damageGrace(runDifficulty); shake = 8; damageFlash = .7f; combo = 0;
        sparks(x, y, 30, 1); sound(S_HURT);
        if (health <= 0) finish(false);
    }
    public void nova() {
        if (state != PLAYING || bombCharge < .999f) return;
        bombCharge = 0; blast = 1; shake = 8; invulnerable = Math.max(invulnerable, .7f);
        sound(S_NOVA);
        for (Entity e : entities) if (e.active) {
            if (e.type == ENEMY_SHOT) { e.active = false; sparks(e.x, e.y, 2, 0); }
            else if (e.type >= SCOUT && e.type <= BOSS) {
                e.hp -= 90+powerRank*8; e.hit = 1; if (e.hp <= 0) destroy(e);
            }
        }
    }
    public void dash() {
        if(state!=PLAYING || dashCharge<.999f) return;
        dashCharge=0; dashFlash=1; dashFromX=x; dashFromY=y;
        float nx=clamp(x+headingX*105,24,WIDTH-24), ny=clamp(y+headingY*105,140,HEIGHT-95);
        targetX=clamp(targetX+nx-x,24,WIDTH-24); targetY=clamp(targetY+ny-y,140,HEIGHT-95);
        x=nx; y=ny; invulnerable=Math.max(invulnerable,.42f); sound(S_PICKUP);
    }
    void completeStage() {
        if(state!=PLAYING) return;
        stageGrade=stageDamage==0 && stageEscaped<5 ? "S" : stageDamage<2 && stageEscaped<15 ? "A" : stageDamage<3 ? "B" : "C";
        score+=(stageGrade.equals("S") ? 1500 : stageGrade.equals("A") ? 900 : 400)*wave;
        best=Math.max(best,score);
        for(Entity e:entities) e.active=false;
        if(wave==Campaign.STAGES) {finish(true);return;}
        unlockedStage=Math.max(unlockedStage,wave+1);
        if(events!=null) events.stageUnlocked(unlockedStage);
        state=DEBRIEF;
    }
    public void chooseUpgrade(int choice) {
        if(state!=DEBRIEF || choice<0 || choice>2) return;
        if(choice==0) { weapon=Math.min(3,weapon+1); powerRank++; }
        else if(choice==1) {
            if(health==maxHealth && maxHealth<hullLimit()) maxHealth++;
            health=Math.min(maxHealth,health+2);
        } else { coolingRank++; bombCharge=Math.min(1,bombCharge+.4f); }
        if(runDifficulty==Campaign.CASUAL) health=Math.min(maxHealth,health+1);
        wave++; waveTime=0; spawnTimer=1; bossSpawned=false; stageKills=stageEscaped=stageDamage=0;
        state=PLAYING; invulnerable=1.2f; dashCharge=1; x=targetX=WIDTH/2; y=targetY=740;
        announce("OPERATION "+stageNumber(),Campaign.name(wave)); sound(S_PICKUP);
    }
    public String stageNumber() { return wave<10 ? "0"+wave : Integer.toString(wave); }
    public String difficultyName() { return Campaign.DIFFICULTIES[Math.max(0,Math.min(3,state==MENU ? difficulty : runDifficulty))]; }
    public int hullLimit() { return Math.max(5,Campaign.hull(runDifficulty)+Fleet.hullBonus(runShip)+1); }
    // Apply ship perks after the reactor floor so upgrades never erase a ship's specialty.
    public float dashCooldown() { return Math.max(2.2f,Campaign.dashSeconds(runDifficulty)-coolingRank*.5f)*Fleet.dashMultiplier(runShip); }
    public float novaCooldown() { return Math.max(14,Campaign.novaSeconds(runDifficulty)-coolingRank*3)*Fleet.novaMultiplier(runShip); }
    void sparks(float px, float py, int count, int color) {
        int budget = quality == 0 ? count / 3 : quality == 1 ? count * 2 / 3 : count;
        for (int i = 0; i < budget; i++) {
            Entity e = spawn(SPARK, px, py, 1 + effectRandom.nextFloat() * 3);
            if (e == null) return;
            float a = effectRandom.nextFloat() * 6.28f, v = 35 + effectRandom.nextFloat() * 210;
            e.vx = (float)Math.cos(a) * v; e.vy = (float)Math.sin(a) * v;
            e.life = .25f + effectRandom.nextFloat() * .6f; e.variant = color;
        }
    }
    void finish(boolean won) {
        state = won ? WON : LOST; best = Math.max(best, score);
        if (events != null) events.runEnded(score, won);
    }
    void announce(String title, String subtitle) { bannerTitle = title; bannerSubtitle = subtitle; banner = 2.8f; }
    void sound(int kind) { if (soundEnabled && events != null) events.sound(kind); }
    public int enemiesRemaining() {
        int count = 0; for (Entity e : entities) if (e.active && e.type >= SCOUT && e.type <= BOSS) count++;
        return count;
    }
    public static float clamp(float v, float min, float max) { return Math.max(min, Math.min(max, v)); }
    static boolean circle(float ax, float ay, float ar, float bx, float by, float br) {
        float dx = ax - bx, dy = ay - by, r = ar + br; return dx * dx + dy * dy <= r * r;
    }
}
