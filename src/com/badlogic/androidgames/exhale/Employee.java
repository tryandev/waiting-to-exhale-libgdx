package com.badlogic.androidgames.exhale;

import android.util.FloatMath;

import com.badlogic.androidgames.framework.DynamicGameObject3D;
import com.badlogic.androidgames.framework.math.Rectangle;

public class Employee extends DynamicGameObject3D {

	public final static int STATE_SIT = 0;
	public final static int STATE_SCARE = 1;
	public final static int STATE_REACT = 2;
	public final static int STATE_LIFT = 3;
	public final static int STATE_FLY = 4;
	
	public final static float RADIUS = 1f;
	public Rectangle hitbox;
	public boolean selected = false;
	public int state = STATE_SIT;
	public float velocityX = 0;
	public float velocityY = 0;
	public float viewOffsetX = 0;
	public float viewOffsetY = 0;
	public boolean dead = false;
	public int hairModel;
	public int hairColor;
	public int body = 0;
	public boolean noob;

	private float xPickup;
	private float reactTime;
	
	public Employee(float x, float y, float z) {
		super(x, y, z, RADIUS);
		float expSize = 1.05f;
		float expPos = 1.05f;
		float rw = (float) (35f * Math.pow(expSize, -x));
		float rh = (float) (45f * Math.pow(expSize, -x));
		float rx = (float) (480 / 2 + position.y * 45.2f * Math.pow(expPos, -x));
		float ry = (float) (360 / 2 + position.x * 31.0f * Math.pow(expPos, -x));
		rx -= rw / 2f;
		ry -= rh / 2f;
		hitbox = new Rectangle(rx, ry, rw, rh);
		noob = (Math.random() > 0.5);
		hairModel = 1 + (int) Math.floor(Math.random() * 2f);
		hairColor = (int) Math.floor(Math.random() * 3f);
		body = 0;
	}

	public void touchOn() {
		this.selected = true;
	}

	public void touchOff() {
		this.selected = false;
	}
	
	public void select(boolean value, float xStart) {
		selected = value;
		xPickup = xStart;
	}
	
	public boolean moveX(float value, World world) {
		float delta = value - xPickup;
		if (Math.abs(delta) > 10f) {
			state = STATE_FLY;
			velocityX = (delta < 0f) ? 4f : -4f;
			velocityY = 3f;
			if ((velocityX > 0 && noob) || (velocityX < 0 && !noob)) {
				Assets.playSound(Assets.hitSound);
				World.score++;
			}else{
				Assets.playSound(Assets.missSound);
				World.lives--;
			}
			return true;
		}
		return false;
	}

	public boolean fly(float deltaTime) {
		if (Math.abs(-position.y + viewOffsetX) > 8) {
			return false;
		}
		velocityY -= 3f * deltaTime;
		viewOffsetX += velocityX * deltaTime;
		viewOffsetY += velocityY * deltaTime;
		return Math.abs(-position.y + viewOffsetX) > 8;
	}
	
	public void updateClap(float deltaTime) {
		reactTime -= deltaTime;
		if (state == STATE_REACT && !noob) {
			body = FloatMath.floor(reactTime * 16f) % 2 < 1.0 ? 1:2;
		}else{
			body = 0;
		}
	}
	
	public void react(float time) {
		reactTime = time;
	}
	
}
