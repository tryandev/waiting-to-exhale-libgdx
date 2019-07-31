package com.badlogic.androidgames.exhale;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

import android.util.Log;

import com.badlogic.androidgames.framework.Input.TouchEvent;
import com.badlogic.androidgames.framework.math.OverlapTester;
import com.badlogic.androidgames.framework.math.Rectangle;
import com.badlogic.androidgames.framework.math.Vector2;

public class World {
	public interface WorldListener {
		public void hit();

		public void hurt();
	}

	final static float WORLD_MIN_X = -14;
	final static float WORLD_MAX_X = 14;
	final static float WORLD_MIN_Z = -15;

	final static float TIME_INTRO = 2.5f;
	final static float TIME_SHOCK = 1.0f;

	final static int STATE_INTRO = 0;
	final static int STATE_PLAY = 1;
	final static int STATE_SHOCK = 2;
	final static int STATE_REACT = 3;
	final static int STATE_CLEAR = 4;
	final static int STATE_BONUS = 5;
	final static int STATE_WRONG = 6;
	final static int STATE_END = 7;
	
	WorldListener listener;
	//int waves = 1;
	
	public static int score = 0;
	public static int lives = 3;
	public int sneezes = 3;
	
	float speedMultiplier = 1;
	final List<Shot> shots = new ArrayList<Shot>();
	final List<Invader> invaders = new ArrayList<Invader>();
	final List<Shield> shields = new ArrayList<Shield>();
	final List<Employee> employees = new ArrayList<Employee>();
	//final Ship ship;
	long lastShotTime;
	Random random;
	int hitCount = 0;
	Employee selectedEmployee;
	int gameState = STATE_INTRO;
	
	float introTimer = TIME_INTRO;
	float shockTimer = 0f;
	float reactTimer = 0f;
	float clearTimer = 0f;
	float bonusTimer = 0f;

	public int employeesMade;
	public int employeesThrown;
	public int level;
	
	public World(int level) {
		//ship = new Ship(0, 0, 0);
		employeesMade = 0;
		employeesThrown = 0;
		sneezes = 3;
		//generateInvaders();
		generateEmployees(level);
		// generateShields();
		lastShotTime = System.nanoTime();
		random = new Random();
		this.level = level;
	}

	/*private void generateInvaders() {
		int row = 0;
		int column = 4;
		// for (int row = 0; row < 4; row++) {
		// for (int column = 0; column < 8; column++) {
		Invader invader = new Invader(-WORLD_MAX_X / 2 + column * 2f, -9, WORLD_MIN_Z + row * 2f);
		invader.bounds.radius *= 3.5;
		invaders.add(invader);
		// }
		// }
	}*/
	
	public void shockAndReact(float reactTime) {
		if (sneezes < 1) {
			return;
		}
		
		sneezes--;
		
		shockTimer = TIME_SHOCK;
		reactTimer = (float) (reactTime * Math.pow(0.8, level));
		gameState = STATE_SHOCK;

		int i;
		Employee employee;
		int max = employees.size();

		for (i = 0; i < max; i++) {
			employee = employees.get(i);
			if (Math.random() < 0.2 && employee.noob) {
				employee.dead = true;
			}
		}
	}
	
	private void generateEmployees(int level) {
		int top;
		int down;
		int left;
		int right;

		if (level == 1) {
			 top = 1;
			 down = 1;
			 left = 1;
			 right = 1;
			
		} else if (level == 2) {
			 top = 2;
			 down = 2;
			 left = 2;
			 right = 2;
			
		} else if (level == 3) {
			 top = 2;
			 down = 2;
			 left = 3;
			 right = 3;
			
		} else {
			 top = 2;
			 down = 2;
			 left = 3;
			 right = 3;
		}
		employees.clear();
		for (int x = -down; x <= top; x++) {
			for (int y = -left; y <= right; y++) {
				employees.add(new Employee(x, y, 0));
				employeesMade++;
			}
		}
	}

	public void updateTouch(Vector2 touchPoint, int state) {
		if (gameState != STATE_PLAY) return;
		int i;
		Employee employee;
		int max = employees.size();

		if (state == TouchEvent.TOUCH_UP) {
			if (selectedEmployee != null) {
				Log.i(this.toString(), "touch_up");
				selectedEmployee.select(false, 0f);
				selectedEmployee = null;
				return;
			}
		}

		if (state == TouchEvent.TOUCH_DOWN) {
			Log.i(this.toString(), "touch_down");
			if (selectedEmployee != null) {
				if (selectedEmployee.state == Employee.STATE_SIT) {
					if (selectedEmployee.moveX(touchPoint.x, this)) {
						/*if (employeesThrown >= employeesMade && lives > 0) {
							gameState = STATE_BONUS;
						}*/
					}
				}
				return;
			}

			for (i = 0; i < max; i++) {
				employee = employees.get(i);
				if (employee.state != Employee.STATE_SIT) {
					continue;
				}
				Rectangle rect = employee.hitbox;
				if (OverlapTester.pointInRectangle(rect, touchPoint)) {
					employee.select(true, touchPoint.x);
					selectedEmployee = employee;
					return;
				} else {
					employee.select(false, 0f);
				}
			}
		}
	}

	public void setWorldListener(WorldListener worldListener) {
		this.listener = worldListener;
	}

	public void update(float deltaTime, float accelX) {
		//ship.update(deltaTime, accelX);
		//updateInvaders(deltaTime);
		//updateShots(deltaTime);
		updateEmployees(deltaTime);
		//checkShotCollisions();
		// checkInvaderCollisions();
		/*if (invaders.size() == 0) {
			generateInvaders();
			waves++;
			speedMultiplier += 0.5f;
		}*/
		
		if (gameState == STATE_INTRO) {
			updateIntro(deltaTime);
		}else if (gameState == STATE_SHOCK) {
			updateShock(deltaTime);
		}else if (gameState == STATE_REACT) {
			updateReact(deltaTime);
		}else if (gameState == STATE_CLEAR) {
			updateClear(deltaTime);
		}else if (gameState == STATE_BONUS) {
			updateClear(deltaTime);
		}else if (gameState == STATE_WRONG) {
			updateBonusWrong(deltaTime);
		}
	}

	private void updateBonusWrong(float deltaTime) {
		bonusTimer -= deltaTime;
		if (bonusTimer <= 0) {
			this.gameState = STATE_END;
		}
	}
	
	private void updateIntro(float deltaTime) {
		introTimer -= deltaTime;
		if (introTimer <= 0) {
			this.gameState = STATE_PLAY;
		}
	}
	
	private void updateShock(float deltaTime) {
		shockTimer -= deltaTime;
		if (shockTimer <= 0) {
			this.gameState = STATE_REACT;
			Assets.playSound(Assets.clapSound);
		}
	}
	
	private void updateReact(float deltaTime) {
		reactTimer -= deltaTime;
		if (reactTimer <= 0) {
			this.gameState = STATE_PLAY;
		}
	}
	
	private void updateClear(float deltaTime) {
		clearTimer -= deltaTime;
		if (clearTimer <= 0) {
			if (level == 1) {
				this.gameState = STATE_BONUS;
				
			}else{
				this.gameState = STATE_END;
				
			}
		}
	}
	
	public void updateEmployees(float deltaTime) {
		int len = employees.size();
		for (int i = 0; i < len; i++) {
			Employee employee = employees.get(i);
			if (employee.state == Employee.STATE_FLY) {
				if (employee.fly(deltaTime)) {
					this.employeesThrown++;
					if (employeesThrown >= employeesMade && lives > 0) {
						clearTimer = 2f;
						gameState = STATE_CLEAR;
					}
				}
			}
		}
	}

	private void updateInvaders(float deltaTime) {
		int len = invaders.size();
		for (int i = 0; i < len; i++) {
			Invader invader = invaders.get(i);
			invader.update(deltaTime, speedMultiplier);

			/*
			 * if (invader.state == Invader.INVADER_ALIVE) { if
			 * (random.nextFloat() < 0.001f) { Shot shot = new
			 * Shot(invader.position.x, invader.position.y, invader.position.z,
			 * Shot.SHOT_VELOCITY); shots.add(shot); listener.shot(); } }
			 */

			if (invader.state == Invader.INVADER_DEAD && invader.stateTime > Invader.INVADER_EXPLOSION_TIME) {
				invaders.remove(i);
				i--;
				len--;
			}
		}
	}

	private void updateShots(float deltaTime) {
		int len = shots.size();
		for (int i = 0; i < len; i++) {
			Shot shot = shots.get(i);
			shot.update(deltaTime);
			if (shot.position.z < WORLD_MIN_Z - 5 || shot.position.z > 0) {
				if (shot.position.z < WORLD_MIN_Z - 5) {
					lives--;
					Assets.playSound(Assets.missSound);
				}
				shots.remove(i);
				i--;
				len--;
			}
		}
	}

	/*
	 * private void checkInvaderCollisions() { if (ship.state ==
	 * Ship.SHIP_EXPLODING) return;
	 * 
	 * int len = invaders.size(); for (int i = 0; i < len; i++) { Invader
	 * invader = invaders.get(i); if (OverlapTester.overlapSpheres(ship.bounds,
	 * invader.bounds)) { ship.lives = 1; ship.kill(); return; } } }
	 */

	/*private void checkShotCollisions() {
		int len = shots.size();
		for (int i = 0; i < len; i++) {
			Shot shot = shots.get(i);

			int len2 = shields.size();

			if (shot.velocity.z < 0) {
				len2 = invaders.size();
				for (int j = 0; j < len2; j++) {
					Invader invader = invaders.get(j);
					if (OverlapTester.overlapSpheres(invader.bounds, shot.bounds) && invader.state == Invader.INVADER_ALIVE) {
						// invader.kill();
						Assets.playSound(Assets.hitSound);
						score++;
						hitCount++;
						if (hitCount % 40 == 0) {
							invader.bounds.radius *= 0.75f;
						}
						shots.remove(i);
						i--;
						len--;

						Shot shotBack = new Shot(shot.position.x, shot.position.y, shot.position.z, Shot.SHOT_VELOCITY);
						float xVel = (float) ((random.nextFloat() - 0.5f) * 2) * 10f;
						shotBack.velocity.x = xVel;
						shotBack.velocity.y = -shot.velocity.y;
						shotBack.velocity.z = -shot.velocity.z;
						// shotBack.bounds.radius *= 2;
						shots.add(shotBack);

						// listener.shot();
						// break;
					}
				}
			} else {
				if (OverlapTester.overlapSpheres(shot.bounds, ship.bounds) && ship.state == Ship.SHIP_ALIVE) {
					// ship.kill();
					// listener.explosion();
					lives -= (lives < 5 ? lives : 5);
					score -= 10;
					//Assets.playSound(Assets.hurtSound);
					shots.remove(i);
					i--;
					len--;
				}
			}
		}
	}*/

	public boolean isGameOver() {
		return lives <= 0;
	}

	public void shot() {
		return;
	}
}
