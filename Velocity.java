package com.bulsy.greenwall;

import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Point;
import android.graphics.Rect;
import android.support.v4.view.VelocityTrackerCompat;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.MotionEvent;
import android.view.VelocityTracker;
import android.view.View;
import java.io.*;
import java.util.*

/**
 * Represents the main screen of play for the game.
 *
 * There are many ugly things here, partly because game programming lends itself
 * to questionable style.  ...and partly because this is my first venture into Android.
 *
 * Messy manual object recycling, violation of normal object oriented privacy
 * principles, and everything is extremely imperative/procedural, even for Java.
 * But apparently keeping android
 * happy is more important than data hiding and hands-free garbage collection.
 *
 * Created by ugliest on 12/29/14.
 */
public class PlayScreen extends Screen {

    static final int INIT_SELECTABLE_SPEED = 150;  // initial speed of selectable dart at bottom of screen
    private volatile Dart selectedDart = null;
    private int maxShownSelectableDart;
    private MainActivity act = null;
    private int selectable_speed;


    VelocityTracker mVelocityTracker = null;
    DisplayMetrics dm = new DisplayMetrics();
    @Override
    public boolean onTouch(MotionEvent e) {
        switch (e.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (gamestate == State.ROUNDSUMMARY
                        || gamestate == State.STARTGAME
                        || gamestate == State.PLAYERDIED) {
                    gamestate = State.STARTROUND; // prep and start round
                    return false; // no followup msgs
                }
                else if (gamestate == State.GAMEOVER) {
                    act.leaveGame(); // user touched after gameover -> back to entry screen
                    return false;  // no followup msgs
                }
                else {
                    synchronized (dartsSelectable) {
                        Iterator<Dart> itf = dartsSelectable.iterator();
                        while (itf.hasNext()) {
                            Dart D = itf.next();
                            if (d.hasCollision(e.getX(), e.getY()))
                                if (d.seed == bullseye) {
                                    // user hit the bullseye
                                    act.playSound(Sound.KSPLAT);
                                    d.burst();
                                    loseLife();
                                    return false; // no followup msgs
                                } else {
                                    //user picked up a dart
                                    selectedDart = f;
                                }
                        }
                    }
                    if (mVelocityTracker == null) {
                        // Retrieve a new VelocityTracker object to watch the velocity of a motion.
                        mVelocityTracker = VelocityTracker.obtain();
                    } else {
                        // Reset the velocity tracker back to its initial state.
                        mVelocityTracker.clear();
                    }
                    // Add a user's movement to the tracker.
                    mVelocityTracker.addMovement(e);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (selectedDart != null) {
                    selectedDart.x = e.getX();
                    selectedDart.y = e.getY();
                }
                mVelocityTracker.addMovement(e);
                break;

            case MotionEvent.ACTION_UP:
                if (selectedDart != null) {
                    Dart f = selectedDart;
                    selectedDart = null;

                    mVelocityTracker.computeCurrentVelocity(1000);
                    int pointerId = e.getPointerId(e.getActionIndex());
                    float tvx = VelocityTrackerCompat.getXVelocity(mVelocityTracker,
                            pointerId);
                    float tvy = VelocityTrackerCompat.getYVelocity(mVelocityTracker,
                            pointerId);

                    if (-tvy > 10) {
                        // there is upward motion at release-- user threw dart

                        // scale throw speed for display size/density
                        tvx = tvx / act.densityscalefactor;
                        tvy = tvy / act.densityscalefactor;

                        // help ease perspective problem when we release the dart away from the horizontal center of the screen
                        tvx += (e.getX()-width/2)*3.5*act.densityscalefactor;

                        d.throwDart(tvx, tvy);
                        synchronized (dartsFlying) {
                            dartsFlying.add(f);
                            dartsSelectable.remove(f);
                        }

                        // attempting to adjust sound for how hard the dart was thrown horizontally.
                        // hardness == 0 --> not thrown with any force
                        // hardness == 1 --> thrown as hard as possible
                        // assume that 5000 represents "really fast"; z vel should sound "harder" than y-vel
                        float hardness = (d.vz - d.vy/2)/5000;  // vy: up is negative.
                        if (hardness >= 1f)
                            hardness = 1.0f;
                        if (hardness < .3f)
                            hardness = .3f;
                        act.playSound(Sound.THROW, hardness * .9f, hardness * 2);
                    }
                }
                mVelocityTracker.recycle();
                break;
        }

        return true;
    }

    }
}
