package com.badlogic.androidgames.exhale;

import java.util.ArrayList;
import java.util.List;

import javax.microedition.khronos.opengles.GL10;

import android.util.FloatMath;

import com.badlogic.androidgames.framework.gl.AmbientLight;
import com.badlogic.androidgames.framework.gl.DirectionalLight;
import com.badlogic.androidgames.framework.gl.LookAtCamera;
import com.badlogic.androidgames.framework.gl.SpriteBatcher;
import com.badlogic.androidgames.framework.gl.Vertices3;
import com.badlogic.androidgames.framework.impl.GLGraphics;
import com.badlogic.androidgames.framework.math.Vector3;

public class WorldRenderer {
	GLGraphics glGraphics;
	LookAtCamera camera;
	AmbientLight ambientLight;
	DirectionalLight directionalLight;
	SpriteBatcher batcher;
	float invaderAngle = 0;

	Vector3 camPosStart = new Vector3(-4f, 4f, -4f);
	Vector3 camPosEnd = new Vector3(0, 10f, -10f);
	
	float clapTimer;

	public WorldRenderer(GLGraphics glGraphics) {
		this.glGraphics = glGraphics;
		//camera = new LookAtCamera(2, glGraphics.getWidth() / (320f / 480f * glGraphics.getWidth()) /*(float) glGraphics.getHeight()*/, 10f, 300f);
		camera = new LookAtCamera(30, glGraphics.getWidth() / (320f / 480f * glGraphics.getWidth()) /*(float) glGraphics.getHeight()*/, 2f, 300f);
		camera.getPosition().set(camPosStart.x, camPosStart.y, camPosStart.z);
		camera.getLookAt().set(0, 0, 0);
		ambientLight = new AmbientLight();
		ambientLight.setColor(0.2f, 0.2f, 0.2f, 1.0f);
		directionalLight = new DirectionalLight();
		directionalLight.setDirection(-1, -0.5f, 0);
		batcher = new SpriteBatcher(glGraphics, 100);
	}

	public void render(World world, float deltaTime, int state) {
		if (world.introTimer > 0f) {
			float dx = camPosEnd.x - camPosStart.x;
			float dy = camPosEnd.y - camPosStart.y;
			float dz = camPosEnd.z - camPosStart.z;
			float factor = (World.TIME_INTRO - world.introTimer) / World.TIME_INTRO;
			factor *= factor*factor;
			dx = dx * factor;
			dy = dy * factor;
			dz = dz * factor;
			camera.getPosition().set(camPosStart.x + dx, camPosStart.y + dy, camPosStart.z + dz);
			camera.getLookAt().set(0, 0, 0);
		}
		
		GL10 gl = glGraphics.getGL();
		camera.setMatrices(gl);
		gl.glEnable(GL10.GL_TEXTURE_2D);
		gl.glEnable(GL10.GL_LIGHTING);
		gl.glEnable(GL10.GL_COLOR_MATERIAL);
		ambientLight.enable(gl);
		directionalLight.enable(gl, GL10.GL_LIGHT0);
		if (state != GameScreen.GAME_OVER) {
			
			renderRoom(gl, world, state, deltaTime);
			gl.glEnable(GL10.GL_DEPTH_TEST);
			renderEmployees(gl, world, state, deltaTime);
		}
		gl.glEnable(GL10.GL_DEPTH_TEST);
		
		renderAvatar(gl, state, deltaTime);
		gl.glDisable(GL10.GL_COLOR_MATERIAL);
		gl.glDisable(GL10.GL_LIGHTING);
		gl.glDisable(GL10.GL_DEPTH_TEST);

	}

	private void renderRoom(GL10 gl, World world, int state, float deltaTime) {
		gl.glPushMatrix();
		gl.glScalef(0.5f, 0.5f, 0.5f);
		Assets.textureRoom.bind();
		Assets.modelRoom.bind();
		Assets.modelRoom.draw(GL10.GL_TRIANGLES, 0, Assets.modelRoom.getNumVertices());
		Assets.modelRoom.unbind();
		gl.glPopMatrix();
	}

	private void renderEmployeesModel(GL10 gl, List<Employee> bodies, Vertices3 model, boolean isHair, World world) {
		model.bind();
		int max = bodies.size();
		Employee employee;
		float verticalOffset = -0.1f;
		for (int i = 0; i < max; i++) {
			employee = bodies.get(i);
			float levitation;
			gl.glPushMatrix();
			levitation = (employee.selected || employee.state == Employee.STATE_FLY) ?  1f: 0f;
			float tx = -employee.position.y + employee.viewOffsetX;
			float ty = levitation;
			float tz = employee.position.x + verticalOffset - levitation + employee.viewOffsetY;
			gl.glTranslatef(tx, ty, tz);
			//gl.glRotatef(employee.viewOffsetX* 50f, 0.5f, 1f, 0);
			
			gl.glTranslatef(0,0.633f,0);
			gl.glRotatef(employee.viewOffsetX* 100f, 1f, 0f, 1f);
			gl.glTranslatef(0,-0.633f,0);
			
			gl.glScalef(0.5f, 0.5f, 0.5f);
			if (isHair) {
				if (employee.dead) {
					gl.glColor4f(0.5f, 0.5f, 0.5f, 1f);
				} else if (world.gameState == World.STATE_SHOCK) {
					gl.glColor4f(1f, 1f, 1f, 1f);
				} else if (world.gameState == World.STATE_REACT && employee.noob) {
					gl.glColor4f(1f, 1f, 1f, 1f);
				} else {
					if (employee.hairColor == 0)
						gl.glColor4f(0f, 0f, 0f, 1f);
					else if (employee.hairColor == 1)
						gl.glColor4f(1f, 1f, 0.2f, 1f);
					else if (employee.hairColor == 2)
						gl.glColor4f(0.3f, 0.19f, 0.15f, 1f);
				}
			}else{
				gl.glColor4f(1f, 1f, 1f, 1f);
			}
			
			model.draw(GL10.GL_TRIANGLES, 0, model.getNumVertices());
			gl.glPopMatrix();
		}
		model.unbind();
	}
	
	private void renderEmployees(GL10 gl, World world, int state, float deltaTime) {
		Assets.textureNormal.bind();

		int max = world.employees.size();
		int i;
		float verticalOffset = -0.1f;
		Employee employee;

		List<Employee> bodies1 = new ArrayList<Employee>();
		List<Employee> bodies2 = new ArrayList<Employee>();
		List<Employee> bodies3 = new ArrayList<Employee>();
		
		List<Employee> hairs1 = new ArrayList<Employee>();
		List<Employee> hairs2 = new ArrayList<Employee>();
		List<Employee> hairs3 = new ArrayList<Employee>();

		List<Employee> noobs = new ArrayList<Employee>();
		List<Employee> vets = new ArrayList<Employee>();
		

		List<Employee> vetHairs1 = new ArrayList<Employee>();
		List<Employee> vetHairs2 = new ArrayList<Employee>();
		List<Employee> vetHairs3 = new ArrayList<Employee>();

		List<Employee> dead = new ArrayList<Employee>();
		List<Employee> alive = new ArrayList<Employee>();
		
		for (i = 0; i < max; i++) {
			employee = world.employees.get(i);
			if (employee.dead) {
				dead.add(employee);
			}else{
				alive.add(employee);
				if (employee.body == 0) bodies1.add(employee);
				if (employee.body == 1) bodies2.add(employee);
				if (employee.body == 2) bodies3.add(employee);
				if (employee.hairModel == 0) hairs1.add(employee);
				if (employee.hairModel == 1) hairs2.add(employee);
				if (employee.hairModel == 2) hairs3.add(employee);
				if (employee.noob) { 
					noobs.add(employee); 
				}else{
					vets.add(employee);
					if (employee.hairModel == 0) vetHairs1.add(employee);
					if (employee.hairModel == 1) vetHairs2.add(employee);
					if (employee.hairModel == 2) vetHairs3.add(employee);
				}
			}
		}
		
		if (world.gameState == World.STATE_SHOCK) {
			Assets.textureShocked.bind();
			gl.glEnable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, alive, Assets.modelHuman1, false, world);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, alive, Assets.modelHair1, true, world);
		}else if (world.gameState == World.STATE_REACT) {

			clapTimer += deltaTime * 16f;
			boolean clapToggle = FloatMath.floor(clapTimer * 16f) % 2 < 1.0;
			
			Assets.textureShocked.bind();
			gl.glEnable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, noobs, Assets.modelHuman1, false, world);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, noobs, Assets.modelHair1, true, world);
			
			Assets.textureNormal.bind();
			gl.glEnable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, vets, clapToggle ? Assets.modelHuman2:Assets.modelHuman3, false, world);
			gl.glDisable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, vetHairs2, Assets.modelHair2, true, world);
			renderEmployeesModel(gl, vetHairs3, Assets.modelHair3, true, world);
			
		}else{
			Assets.textureNormal.bind();
			gl.glEnable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, bodies1, Assets.modelHuman1, false, world);
			renderEmployeesModel(gl, bodies2, Assets.modelHuman2, false, world);
			renderEmployeesModel(gl, bodies3, Assets.modelHuman3, false, world);

			gl.glDisable(GL10.GL_TEXTURE_2D);
			renderEmployeesModel(gl, hairs1, Assets.modelHair1, true, world);
			renderEmployeesModel(gl, hairs2, Assets.modelHair2, true, world);
			renderEmployeesModel(gl, hairs3, Assets.modelHair3, true, world);
			gl.glEnable(GL10.GL_TEXTURE_2D);
			
		}

		gl.glEnable(GL10.GL_TEXTURE_2D);
		Assets.textureDead.bind();
		renderEmployeesModel(gl, dead, Assets.modelHuman1, false, world);
		gl.glDisable(GL10.GL_TEXTURE_2D);
		renderEmployeesModel(gl, dead, Assets.modelHair1, true, world);

		Assets.modelDesk.bind();
		gl.glEnable(GL10.GL_TEXTURE_2D);
		Assets.textureDesk.bind();
		gl.glColor4f(1, 1, 1, 1);
		for (i = 0; i < max; i++) {
			employee = world.employees.get(i);
			gl.glPushMatrix();
			gl.glTranslatef(-employee.position.y, 0, employee.position.x + verticalOffset);
			gl.glScalef(0.5f, 0.5f, 0.5f);
			Assets.modelDesk.draw(GL10.GL_TRIANGLES, 0, Assets.modelDesk.getNumVertices());
			gl.glPopMatrix();
		}
		Assets.modelDesk.unbind();
	}

	private void renderAvatar(GL10 gl, int state, float deltaTime) {
		Assets.textureShocked.bind();
		gl.glPushMatrix();

		if (state != GameScreen.GAME_OVER) {
			/*gl.glTranslatef(ship.position.x, ship.position.y, ship.position.z - 0.5f);
			gl.glRotatef(ship.velocity.x / Ship.SHIP_VELOCITY * 135, 0, 0, -1);
			gl.glEnable(GL10.GL_LIGHTING);
			Assets.modelHuman1.bind();
			Assets.modelHuman1.draw(GL10.GL_TRIANGLES, 0, Assets.modelHuman1.getNumVertices());
			Assets.modelHuman1.unbind();*/
		} else {
			invaderAngle += deltaTime;
			camera.setMatrices(gl, 45f);
			gl.glTranslatef(2f, 0, -1f);
			gl.glRotatef(FloatMath.sin(invaderAngle * 50f) * 4 + 45, 0, 1, 0);
			gl.glScalef(0.75f,0.75f,0.75f);
			camera.getPosition().set(0, 3f, -6f);
			camera.getLookAt().set(0, 1.2f, 0);
			Assets.modelHuman1.bind();
			Assets.modelHuman1.draw(GL10.GL_TRIANGLES, 0, Assets.modelHuman1.getNumVertices());
			Assets.modelHuman1.unbind();
			Assets.modelHair1.bind();
			Assets.modelHair1.draw(GL10.GL_TRIANGLES, 0, Assets.modelHair1.getNumVertices());
			Assets.modelHair1.unbind();
			gl.glEnable(GL10.GL_LIGHTING);
		}
		gl.glPopMatrix();
	}

	/*private void renderInvaders(GL10 gl, List<Invader> invaders, float deltaTime) {
		invaderAngle += 45 * deltaTime;

		Assets.invaderTexture.bind();
		Assets.invaderModel.bind();
		int len = invaders.size();
		for (int i = 0; i < len; i++) {
			Invader invader = invaders.get(i);
			if (invader.state == Invader.INVADER_DEAD) {
			} else {
				float boundRadius = invader.bounds.radius / Invader.INVADER_RADIUS;
				gl.glPushMatrix();
				gl.glTranslatef(invader.position.x, invader.position.y, invader.position.z);
				gl.glRotatef(invaderAngle / invaderAngle, 0, 1, 0);
				gl.glScalef(boundRadius, boundRadius, boundRadius);
				Assets.invaderModel.draw(GL10.GL_TRIANGLES, 0, Assets.invaderModel.getNumVertices());
				gl.glPopMatrix();
			}
		}
		Assets.invaderModel.unbind();
	}*/

	/*private void renderUrinal(GL10 gl, List<Invader> invaders, float deltaTime) {
		Assets.invaderTexture.bind();
		Assets.urinalModel.bind();
		Invader invader = invaders.get(0);
		float boundRadius = invader.bounds.radius / Invader.INVADER_RADIUS;
		gl.glPushMatrix();
		gl.glTranslatef(invader.position.x, invader.position.y, invader.position.z);
		gl.glScalef(boundRadius, boundRadius, boundRadius);
		gl.glRotatef(90, 0, 1, 0);
		Assets.urinalModel.draw(GL10.GL_TRIANGLES, 0, Assets.urinalModel.getNumVertices());
		gl.glPopMatrix();
		Assets.invaderModel.unbind();
	}*/

	/*private void renderShots(GL10 gl, List<Shot> shots) {
		gl.glColor4f(1, 1, 0, 1);
		Assets.shotModel.bind();
		int len = shots.size();
		for (int i = 0; i < len; i++) {
			Shot shot = shots.get(i);
			gl.glPushMatrix();
			gl.glTranslatef(shot.position.x, shot.position.y, shot.position.z);
			gl.glRotatef((-180f / (float) Math.PI) * (float) Math.atan(shot.velocity.y / shot.velocity.z), 1f, 0f, 0f);
			// float boundRadius = shot.bounds.radius / Shot.SHOT_RADIUS;
			gl.glScalef(1f, 1f, 5f);
			Assets.shotModel.draw(GL10.GL_TRIANGLES, 0, Assets.shotModel.getNumVertices());
			gl.glPopMatrix();
		}
		Assets.shotModel.unbind();
		gl.glColor4f(1, 1, 1, 1);
	}*/
}