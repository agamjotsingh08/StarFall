package com.starfall.game;

import android.content.res.Resources;
import android.graphics.*;

/** Textures are decoded once. No bitmap or shader creation occurs in the draw loop. */
final class GameArt {
    final Bitmap nebula, atlas, glow;
    final Bitmap[] fleet = new Bitmap[Fleet.COUNT];
    final Rect[] ships = new Rect[4];
    final Paint texture = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    final RectF dst = new RectF();
    final Paint glowPaint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
    final LightingColorFilter cyan = new LightingColorFilter(0xff55eaff, 0);
    final LightingColorFilter red = new LightingColorFilter(0xffff587c, 0);
    final LightingColorFilter gold = new LightingColorFilter(0xffffbe66, 0);
    final LightingColorFilter flash = new LightingColorFilter(0xffffffff, 0xff999999);

    GameArt(Resources resources) {
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inScaled = false;
        nebula = BitmapFactory.decodeResource(resources, R.drawable.nebula, options);
        atlas = BitmapFactory.decodeResource(resources, R.drawable.ships, options);
        float sx = atlas.getWidth() / 1280f, sy = atlas.getHeight() / 1280f;
        ships[0] = region(55, 35, 575, 575, sx, sy);
        ships[1] = region(690, 35, 1215, 555, sx, sy);
        ships[2] = region(0, 635, 640, 1200, sx, sy);
        ships[3] = region(650, 555, 1280, 1250, sx, sy);
        glow = Bitmap.createBitmap(128, 128, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(glow);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setShader(new RadialGradient(64, 64, 64,
            new int[]{0xdfffffff, 0x70ffffff, 0x20ffffff, 0x00ffffff}, new float[]{0, .15f, .42f, 1}, Shader.TileMode.CLAMP));
        c.drawCircle(64, 64, 64, p);
        fleet[Fleet.FALCON] = buildChassis(false);
        fleet[Fleet.BASTION] = buildChassis(true);
    }
    /** Original vector-built hulls cached as textures; no per-frame path allocation. */
    private Bitmap buildChassis(boolean armored) {
        Bitmap bitmap=Bitmap.createBitmap(512,512,Bitmap.Config.ARGB_8888);
        Canvas c=new Canvas(bitmap); Paint p=new Paint(Paint.ANTI_ALIAS_FLAG);
        int accent=armored ? 0xffffc36b : 0xffb9a2ff;
        Path hull=armored
            ? polygon(256,26,308,90,308,155,422,127,470,213,470,374,400,432,330,382,292,466,220,466,182,382,112,432,42,374,42,213,90,127,204,155,204,90)
            : polygon(256,12,294,184,342,223,455,158,408,298,302,370,278,464,234,464,210,370,104,298,57,158,170,223,218,184);
        p.setShader(new LinearGradient(20,0,480,420,new int[]{0xffc8dfef,0xff35475d,0xff101d31,0xff6a8195},null,Shader.TileMode.CLAMP));
        c.drawPath(hull,p); p.setShader(null);p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(5);p.setColor(0xff94acbf);c.drawPath(hull,p);p.setStyle(Paint.Style.FILL);
        for(int side=-1;side<=1;side+=2) {
            c.save();c.translate(256,0);c.scale(side,1);
            p.setColor(0xff152439);
            c.drawPath(armored ? polygon(65,196,160,167,187,226,187,350,145,387,78,339)
                : polygon(44,240,173,193,139,281,49,344),p);
            p.setColor(accent);p.setStrokeWidth(6);
            c.drawLine(armored ? 100 : 64,armored ? 215 : 269,armored ? 159 : 139,armored ? 215 : 225,p);
            for(int i=0;i<4;i++) {
                p.setColor(0xff60798d);p.setStrokeWidth(4);
                c.drawLine(armored ? 105 : 70,292+i*13,armored ? 159 : 105,280+i*13,p);
            }
            p.setColor(0xff172538);c.drawRect(armored ? 159 : 109,armored ? 115 : 220,armored ? 176 : 121,armored ? 260 : 295,p);
            p.setColor(accent);c.drawRect(armored ? 163 : 112,armored ? 113 : 215,armored ? 172 : 118,armored ? 155 : 243,p);
            p.setColor(0xff93efff);c.drawRect(armored ? 113 : 30,armored ? 392 : 421,armored ? 150 : 44,armored ? 422 : 456,p);
            c.restore();
        }
        Path spine=polygon(256,43,287,188,283,350,256,422,229,350,225,188);
        p.setShader(new LinearGradient(224,0,291,0,0xff263b50,0xffb3c9d9,Shader.TileMode.CLAMP));c.drawPath(spine,p);p.setShader(null);
        p.setColor(0xff071322);c.drawPath(polygon(256,139,276,199,270,262,242,262,236,199),p);
        p.setColor(accent);c.drawPath(polygon(256,155,267,202,263,244,249,244,245,202),p);
        p.setColor(0xffeffbff);c.drawCircle(256,209,5,p);
        return bitmap;
    }
    private static Path polygon(float... points) {
        Path path=new Path();path.moveTo(points[0],points[1]);
        for(int i=2;i<points.length;i+=2) path.lineTo(points[i],points[i+1]);path.close();return path;
    }
    void player(Canvas c,int selected,float x,float y,float size,float rotation) {
        int ship=Fleet.valid(selected);
        if(ship==Fleet.VANGUARD) {ship(c,0,x,y,size,rotation,0);return;}
        c.save();c.rotate(rotation,x,y);texture.setAlpha(255);texture.setColorFilter(null);
        dst.set(x-size/2,y-size/2,x+size/2,y+size/2);c.drawBitmap(fleet[ship],null,dst,texture);c.restore();
    }
    private Rect region(int l, int t, int r, int b, float sx, float sy) {
        return new Rect((int)(l*sx), (int)(t*sy), (int)(r*sx), (int)(b*sy));
    }
    void ship(Canvas c, int index, float x, float y, float size, float rotation, float hit) {
        c.save(); c.rotate(rotation, x, y);
        float h = size * ships[index].height() / ships[index].width();
        dst.set(x-size/2, y-h/2, x+size/2, y+h/2);
        texture.setAlpha(255); c.drawBitmap(atlas, ships[index], dst, texture);
        if (hit > 0) {
            texture.setColorFilter(flash);
            texture.setAlpha((int)(hit*85)); c.drawBitmap(atlas, ships[index], dst, texture);
            texture.setColorFilter(null); texture.setAlpha(255);
        }
        c.restore();
    }
    void glow(Canvas c, float x, float y, float width, float height, int color, int alpha) {
        glowPaint.setColorFilter(color == 0 ? cyan : color == 1 ? red : gold);
        glowPaint.setAlpha(alpha); dst.set(x-width/2, y-height/2, x+width/2, y+height/2);
        c.drawBitmap(glow, null, dst, glowPaint);
    }
}
