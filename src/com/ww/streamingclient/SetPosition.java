/*/
 * Kod u¿ywa wirtualnego czujnika Gravity, który mierzy wielkoœæ przyspieszenia grawitacyjnego w osiach x,y,z
 * i przechowuje je w tablicy event.values. Kod ma w przysz³oœci s³u¿yæ do pozycjonowania modu³u kamery.
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
		// Dostêp do us³ugi czujników
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
	//Ponowne rejestrowanie czujnika, po wznowieniu dzia³ania aplikacji.
	public void Resume()
	{
		mSensorManager.registerListener(this, mGravitySensor, SensorManager.SENSOR_DELAY_NORMAL);
		}
	// Aby nie wykorzystywaæ zasobów baterii, wyrejestrowuje siê czujnik w trakcie wstrzymania aplikacji.
	public void Pause()
	{
		mSensorManager.unregisterListener(this);
	}
// Wykonywanie dzia³añ, gdy zmienia siê stan czujnika.
	@Override
	public void onSensorChanged(SensorEvent event) {

		
		 if (event.sensor.getType() == Sensor.TYPE_GRAVITY) {
			 mGravity=Arrays.copyOf(event.values, event.values.length);
	
	


	if (mGravity[2]<-3.2 & !message.equals("Góra"))
	{
		// Tu powinny siê znaleŸæ dzia³ania w celu wys³ania odpowiedniej komendy do komputera w celu pozycjonowania
		// Aby zademonstrowaæ dzia³anie ustanawiana jest wiadomoœæ w polu tekstowym.
		message ="Góra"; 
	setMessage(message);
	}
	else if (mGravity[2]>4.8 & !message.equals("Dó³"))
	{
		// Tu powinny siê znaleŸæ dzia³ania w celu wys³ania odpowiedniej komendy do komputera w celu pozycjonowania
		// Aby zademonstrowaæ dzia³anie ustanawiana jest wiadomoœæ w polu tekstowym.
		message ="Dó³"; 
	setMessage(message);
	}

	else if(!message.equals("Oczekujê na ruch.") &mGravity[2]<=4.8 & mGravity[2]>=-3.2
			)
	{
		message ="Oczekujê na ruch."; 
		setMessage(message);
	}
	}
		 }
	@Override
	public void onAccuracyChanged(Sensor sensor, int accuracy) {
		
	}
	//Metoda ustawiaj¹ca tekst w oknie TextView
public void setMessage(String m)
{TextView textView =(TextView) this.findViewById(R.id.text);
textView.setTextSize(50);
textView.setText(m);
	}


}
