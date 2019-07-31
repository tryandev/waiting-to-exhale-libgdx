package com.badlogic.androidgames.exhale;

import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import com.badlogic.androidgames.framework.Game;
import com.badlogic.androidgames.framework.Input.TouchEvent;
import com.badlogic.androidgames.framework.gl.Camera2D;
import com.badlogic.androidgames.framework.gl.FPSCounter;
import com.badlogic.androidgames.framework.gl.SpriteBatcher;
import com.badlogic.androidgames.framework.gl.TextureRegion;
import com.badlogic.androidgames.framework.impl.GLScreen;
import com.badlogic.androidgames.framework.math.OverlapTester;
import com.badlogic.androidgames.framework.math.Rectangle;
import com.badlogic.androidgames.framework.math.Vector2;

public class GameScreen extends GLScreen {
	static final int GAME_RUNNING = 0;
	static final int GAME_PAUSED = 1;
	static final int GAME_OVER = 2;

	static final int SNEEZE_NORMAL = 0;
	static final int SNEEZE_IN = 1;
	static final int SNEEZE_OUT = 2;
	
	int stateSneeze = 0;
	float sneezePower = 0;
	
	int state;
	Camera2D guiCam;
	Vector2 touchPoint;
	SpriteBatcher batcher;
	World world;
	WorldRenderer renderer;
	Rectangle pauseBounds;
	Rectangle resumeBounds;
	Rectangle quitBounds;
	Rectangle leftBounds;
	Rectangle rightBounds;
	Rectangle shotBounds;
	int lastScore;
	int lastLives;
	int lastWaves;
	String scoreString;
	FPSCounter fpsCounter;
	float waterSound = 0;
	float gameOverDelay = 3f;
	boolean reversedAxis;

	public GameScreen(Game game) {
		super(game);

		state = GAME_RUNNING;
		guiCam = new Camera2D(glGraphics, 480, 320);
		touchPoint = new Vector2();
		batcher = new SpriteBatcher(glGraphics, 100);
		world = new World(1);
		World.score = 0;
		World.lives = 3;
		
		renderer = new WorldRenderer(glGraphics);
		pauseBounds = new Rectangle(480 - 64, 320 - 64, 64, 64);
		resumeBounds = new Rectangle(240 - 80, 160, 160, 32);
		quitBounds = new Rectangle(240 - 80, 160 - 32, 160, 32);
		shotBounds = new Rectangle(480 - 64, 0, 64, 64);
		leftBounds = new Rectangle(0, 0, 64, 64);
		rightBounds = new Rectangle(64, 0, 64, 64);
		lastScore = 0;
		lastLives = World.lives;
		//lastWaves = world.waves;
		// scoreString = "lives:" + lastLives + " waves:" + lastWaves +
		// " score:" + lastScore;
		scoreString = "score:" + World.score + "     lives:" + lastLives;
		fpsCounter = new FPSCounter();
		reversedAxis = true;// (Math.abs(game.getInput().getAccelX()) >
							// Math.abs(game.getInput().getAccelY()) );
	}

	@Override
	public void update(float deltaTime) {
		switch (state) {
		case GAME_PAUSED:
			updatePaused();
			break;
		case GAME_RUNNING:
			updateRunning(deltaTime);
			updateTouch();
			break;
		case GAME_OVER:
			updateGameOver(deltaTime);
			break;
		}
	}

	private void updatePaused() {
		List<TouchEvent> events = game.getInput().getTouchEvents();
		int len = events.size();
		for (int i = 0; i < len; i++) {
			TouchEvent event = events.get(i);
			if (event.type != TouchEvent.TOUCH_UP)
				continue;

			guiCam.touchToWorld(touchPoint.set(event.x, event.y));
			if (OverlapTester.pointInRectangle(resumeBounds, touchPoint)) {
				Assets.playSound(Assets.clickSound);
				state = GAME_RUNNING;
			}

			if (OverlapTester.pointInRectangle(quitBounds, touchPoint)) {
				Assets.playSound(Assets.clickSound);
				game.setScreen(new MainMenuScreen(game));
			}
		}
	}

	private void updateTouch() {
		for (int i = 0; i < 2; i++) {
			if (game.getInput().isTouchDown(i)) {
				guiCam.touchToWorld(touchPoint.set(game.getInput().getTouchX(i), game.getInput().getTouchY(i)));
				world.updateTouch(touchPoint, TouchEvent.TOUCH_DOWN);
				if (world.gameState == World.STATE_BONUS) {
					world.bonusTimer = 2f;
					world.gameState = World.STATE_WRONG;
				}
				return;
			}
		}
		world.updateTouch(touchPoint, TouchEvent.TOUCH_UP);
	}

	private void updateRunning(float deltaTime) {
		List<TouchEvent> events = game.getInput().getTouchEvents();
		int len = events.size();
		for (int i = 0; i < len; i++) {
			TouchEvent event = events.get(i);
			if (event.type != TouchEvent.TOUCH_DOWN)
				continue;

			guiCam.touchToWorld(touchPoint.set(event.x, event.y));

			if (OverlapTester.pointInRectangle(pauseBounds, touchPoint)) {
				Assets.playSound(Assets.clickSound);
				state = GAME_PAUSED;
			}
			if (OverlapTester.pointInRectangle(shotBounds, touchPoint) && world.sneezes > 0 && world.gameState == World.STATE_PLAY) {
				if (stateSneeze == SNEEZE_NORMAL) {
					stateSneeze++;
					sneezePower = 0;
				} else if (stateSneeze == SNEEZE_IN) {	
					stateSneeze++;
					Assets.playSound(Assets.sneezeSound);
					world.shockAndReact(1f * sneezePower);				
				}
				//world.shot();
				//waterSound = 0.5f;
			}
		}

		world.update(deltaTime, calculateInputAcceleration());
		if (World.lives != lastLives || World.score != lastScore) {
			lastLives = World.lives;
			lastScore = World.score;
			//lastWaves = world.waves;
			/*
			 * scoreString = "lives:" + lastLives + " waves:" + lastWaves +
			 * " score:" + lastScore;
			 */
			scoreString = "score:" + World.score + "     lives:" + lastLives;
		}
		if (world.isGameOver()) {
			state = GAME_OVER;
			scoreString = "score:" + World.score;
		}

		if (world.gameState == World.STATE_PLAY && stateSneeze == SNEEZE_OUT) {
			stateSneeze = SNEEZE_NORMAL;
		}
		
		if (stateSneeze == SNEEZE_IN) {
			sneezePower = (sneezePower + deltaTime) % 1;
		}
	}

	private float calculateInputAcceleration() {
		float accelX = 0;
		if (Settings.touchEnabled) {
			for (int i = 0; i < 2; i++) {
				if (game.getInput().isTouchDown(i)) {

					guiCam.touchToWorld(touchPoint.set(game.getInput().getTouchX(i), game.getInput().getTouchY(i)));

					if (OverlapTester.pointInRectangle(leftBounds, touchPoint)) {
						accelX = -Ship.SHIP_VELOCITY / 10;
					}

					if (OverlapTester.pointInRectangle(rightBounds, touchPoint)) {
						accelX = Ship.SHIP_VELOCITY / 10;
					}
				}
			}
		} else {
			accelX = (reversedAxis) ? game.getInput().getAccelY() : -game.getInput().getAccelX();
		}
		return accelX;
	}

	private void updateGameOver(float deltaTime) {
		if (gameOverDelay > 0) {
			gameOverDelay -= deltaTime;
		}

		List<TouchEvent> events = game.getInput().getTouchEvents();
		int len = events.size();
		for (int i = 0; i < len; i++) {
			TouchEvent event = events.get(i);
			if (event.type == TouchEvent.TOUCH_UP) {
				//Assets.water.setVolume(0.0f);
				if (gameOverDelay > 0) {

				} else {
					Assets.playSound(Assets.clickSound);
					game.setScreen(new MainMenuScreen(game));
				}
			}
		}
	}

	@Override
	public void present(float deltaTime) {
		GL10 gl = glGraphics.getGL();
		//gl.glClear(GL10.GL_COLOR_BUFFER_BIT | GL10.GL_DEPTH_BUFFER_BIT);
		gl.glClear(GL10.GL_DEPTH_BUFFER_BIT);
		guiCam.setViewportAndMatrices();
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		if (state == GameScreen.GAME_OVER) {
			batcher.beginBatch(Assets.background3);
			batcher.drawSprite(240, 160, 480, 320, Assets.backgroundRegion3);
			batcher.endBatch();
		} /*else {
			float size = (100f - World.lives) / 100f;
			batcher.beginBatch(Assets.background);
			batcher.drawSprite(240, 160, 480, 320, Assets.backgroundRegion);
			gl.glEnable(GL10.GL_BLEND);
			batcher.drawSprite(230, 20, 195f * size, 110f * size, Assets.puddle);
		}*/

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_BLEND);

		renderer.render(world, deltaTime, state);

		switch (state) {
		case GAME_RUNNING:
			presentRunning();
			break;
		case GAME_PAUSED:
			presentPaused();
			break;
		case GAME_OVER:
			presentGameOver();
		}

		//presentHitboxes();

		fpsCounter.logFrame();
	}

	private void presentPaused() {
		GL10 gl = glGraphics.getGL();
		guiCam.setViewportAndMatrices();
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		batcher.beginBatch(Assets.items);
		Assets.font.drawText(batcher, scoreString, 10, 320 - 20);
		batcher.drawSprite(240, 160, 160, 64, Assets.pauseRegion);
		batcher.endBatch();

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_BLEND);
		//Assets.water.setVolume(0.0f);
	}

	private void presentRunning() {
		GL10 gl = glGraphics.getGL();
		guiCam.setViewportAndMatrices();
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		gl.glDisable(GL10.GL_TEXTURE_2D);
		
		batcher.beginBatch(Assets.items);
		batcher.drawSprite(480/2, 16, 480, 32, Assets.fireRegion);
		gl.glColor4f(0, 0, 0, 1f);
		batcher.endBatch();

		batcher.beginBatch(Assets.items);
		batcher.drawSprite(480/2 + 30, 16, 480 - 225, 8, Assets.fireRegion);
		gl.glColor4f(0.5f, 0.5f, 0.5f, 1f);
		batcher.endBatch();
		
		if (stateSneeze > SNEEZE_NORMAL) {
			batcher.beginBatch(Assets.items);
			batcher.drawSprite(480/2 + 30, 16, (480 - 225) * sneezePower, 8, Assets.fireRegion);
			gl.glColor4f(0.0f, 1.0f, 0.0f, 1f);
			batcher.endBatch();
		}
		
		gl.glColor4f(1f, 1f, 1f, 1f);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		
		batcher.beginBatch(Assets.mchen);
		TextureRegion mchenRegion;
		if (stateSneeze == SNEEZE_NORMAL) {
			mchenRegion = Assets.mchenNormal;
		}else if (stateSneeze == SNEEZE_IN) {
			mchenRegion = Assets.mchenInhale;
		}else{
			mchenRegion = Assets.mchenExhale;
		}
		batcher.drawSprite(48, 48, 96, 96, mchenRegion);
		batcher.endBatch();

		batcher.beginBatch(Assets.items);
		batcher.drawSprite(480 - 32, 320 - 32, 64, 64, Assets.pauseButtonRegion);
		Assets.font.drawText(batcher, scoreString /*+ " " + world.employeesThrown + "/" + world.employeesMade*/, 10, 320 - 20);
		Assets.font.drawText(batcher, "x" + world.sneezes, 96 + 8, 16);
		// Assets.font.drawText(batcher, (int) this.touchPoint.x + " " + (int)
		// this.touchPoint.y, 10, 320 - 20);
		if (Settings.touchEnabled) {
			batcher.drawSprite(32, 32, 64, 64, Assets.leftRegion);
			batcher.drawSprite(96, 32, 64, 64, Assets.rightRegion);
		}
		batcher.drawSprite(480 - 40, 32, 64, 64, Assets.fireRegion);

		if (world.gameState == World.STATE_CLEAR) {

			batcher.drawSprite(480/2, 320/2, 256*1.5f, 64*1.5f, Assets.clearRegion);
		}
		batcher.endBatch();

		if (world.gameState >= World.STATE_BONUS) {
			batcher.beginBatch(Assets.bonus);
			if (world.gameState == World.STATE_BONUS) {
				batcher.drawSprite(480 / 2, 320 / 2+20, 256, 290, Assets.bonus1);
			}else {
				batcher.drawSprite(480 / 2, 320 / 2+20, 256, 290, Assets.bonus2);
			}
			batcher.endBatch();
		}
		
		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_BLEND);
		// gl.glDisable(GL10.GL_TEXTURE_2D);
		
		if (world.gameState == World.STATE_END) {
			world = new World(world.level + 1);
		}
	}

	private void presentHitboxes() {
		GL10 gl = glGraphics.getGL();
		int i;
		Employee employee;
		int max = world.employees.size();
		batcher.beginBatch(Assets.items);
		for (i = 0; i < max; i++) {
			employee = world.employees.get(i);
			Rectangle rect = employee.hitbox;
			batcher.drawSprite(rect.lowerLeft.x + rect.width / 2f, rect.lowerLeft.y + rect.height / 2f, rect.width, rect.height, Assets.accelRegion);
		}
		gl.glEnable(GL10.GL_BLEND);
		gl.glColor4f(1f, 0f, 0f, 0.25f);
		batcher.endBatch();
		gl.glColor4f(1f, 1f, 1f, 1f);
	}

	private void presentGameOver() {
		//Assets.water.setVolume(0.0f);
		GL10 gl = glGraphics.getGL();
		guiCam.setViewportAndMatrices();
		gl.glEnable(GL10.GL_BLEND);
		gl.glBlendFunc(GL10.GL_SRC_ALPHA, GL10.GL_ONE_MINUS_SRC_ALPHA);
		gl.glEnable(GL10.GL_TEXTURE_2D);

		batcher.beginBatch(Assets.items);
		batcher.drawSprite(128 / 2 + 30, 240, 128, 64, Assets.gameOverRegion);
		batcher.drawSprite(480 / 2, 45, 194, 64, Assets.quote);
		Assets.font.drawText(batcher, scoreString, 10, 320 - 20);
		batcher.endBatch();

		gl.glDisable(GL10.GL_TEXTURE_2D);
		gl.glDisable(GL10.GL_BLEND);
	}

	@Override
	public void pause() {
		state = GAME_PAUSED;
	}

	@Override
	public void resume() {

	}

	@Override
	public void dispose() {

	}
}
