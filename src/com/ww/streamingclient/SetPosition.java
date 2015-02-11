/*/
 * Kod u�ywa wirtualnego czujnika Gravity, kt�ry mierzy wielko�� przyspieszenia grawitacyjnego w osiach x,y,z
 * i przechowuje je w tablicy event.values. Kod ma w przysz�o�ci s�u�y� do pozycjonowania modu�u kamery.
 */
package com.ww.streamingclient;

import java.util.Arrays;

import android.annotation.TargetApi;
import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.NavUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

public class SetPosition extends Activity implements SensorEventListener{
	private SensorManager mSensorManager;
	private Sensor mGravitySensor;
	private float [] mGravity = new float [3];
	private String message="";


	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_set_position);
		// Dost�p do us�ugi czujnik�w
		mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
		// Wybranie czujnika typu Gravity
	    mGravitySensor = mSensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY);
	    // Rejestracja czujnika
	    mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		// Show the Up button in the action bar.
		setupActionBar();
	}

	/**
	 * Set up the {@link android.app.ActionBar}, if the API is available.
	 */
	@TargetApi(Build.VERSION_CODES.HONEYCOMB)
	private void setupActionBar() {
		if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.HONEYCOMB) {
			getActionBar().setDisplayHomeAsUpEnabled(true);
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.set_position, menu);
		return true;
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		switch (item.getItemId()) {
		case android.R.id.home:
		
			NavUtils.navigateUpFromSameTask(this);
			return true;
		}
		return super.onOptionsItemSelected(item);
	}
	//Ponowne rejestrowanie czujnika, po wznowieniu dzia�ania aplikacji.
	public void Resume()
	{
		mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	// Aby nie wykorzystywa� zasob�w baterii, wyrejestrowuje si� czujnik w trakcie wstrzymania aplikacji.
	public void Pause()
	{
		mSensorManager.unregisterListener(this);
	}
// Wykonywanie dzia�a�, gdy zmienia si� stan czujnika.
	@Override
	public void onSensorChanged(SensorEvent event) {

		
		 if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			 mGravity=Arrays.copyOf(event.values, event.values.length);
	
	


	if (mGravity[2]<-3.2 & !message.equals("G�ra"))
	{
		// Tu powinny si� znale�� dzia�ania w celu wys�ania odpowiedniej komendy do komputera w celu pozycjonowania
		// Aby zademonstrowa� dzia�anie ustanawiana jest wiadomo�� w polu tekstowym.
		message ="G�ra"; 
	setMessage(message);
	}
	else if (mGravity[2]>4.8 & !message.equals("D�"))
	{
		// Tu powinny si� znale�� dzia�ania w celu wys�ania odpowiedniej komendy do komputera w celu pozycjonowania
		// Aby zademonstrowa� dzia�anie ustanawiana jest wiadomo�� w polu tekstowym.
		message ="D�"; 
	setMessage(message);
	}

	else if(!message.equals("Oczekuj� na ruch.") &mGravity[2]<=4.8 & mGravity[2]>=-3.2
			)
	{
		message ="Oczekuj� na ruch."; 
		setMessage(message);
	}
	}
		 }
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	//Metoda ustawiaj�ca tekst w oknie TextView
public void setMessage(String m)
{TextView textView =(TextView) this.findViewById(R.id.text);
textView.setTextSize(50);
textView.setText(m);
	}


}
