package com.badlogic.androidgames.exhale;

import com.badlogic.androidgames.framework.DynamicGameObject3D;

public class Shot extends DynamicGameObject3D {
	static float SHOT_VELOCITY = 10f;
	static float SHOT_RADIUS = 0.1f;
	
	public Shot(float x, float y, float z, float velocityZ) {
		super(x, y, z, SHOT_RADIUS);
		velocity.z = velocityZ;
		velocity.y = 12f;
	}

	public void update(float deltaTime) {
		velocity.y -= 25f * deltaTime;
		position.x += velocity.x * deltaTime;
		position.y += velocity.y * deltaTime;
		position.z += velocity.z * deltaTime;
		bounds.center.set(position);
	}
}
