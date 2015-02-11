/*/ Kod powsta³ na podstawie maetria³ów dostêpnych na stronie http://docs.gstreamer.com/display/GstSDK/Android+tutorial+3%3A+Video
 * S³u¿y wyœwietlaniu obrazu otrzymanego przy u¿yciu zewnêtrznych bibliotek libgstreamer_android.so oraz libStreamingC.so 
 * na powierzchni elementu SurfaceView. Dziêki aplikacji mo¿na równie¿ wstrzymywaæ odtwarzanie, wznawiaæ je oraz przejœæ do aktywnoœci
 * umo¿iwaj¹cej pozycjonowanie modu³u kamery.
 */
package com.ww.streamingclient;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.SurfaceHolder;
import android.view.SurfaceView;
import android.widget.Toast;

import com.gstreamer.GStreamer;

public class StreamingClient extends Activity implements SurfaceHolder.Callback {
	//Inicjalizacja kodu macierzystego, budowa potoku
	  private native void nInit();     
	  // Niszczenie potoku, zamykanie kodu macierzystego
	    private native void nFinalize(); 
	    // Ustawienie potoku na odtwarzanie
	    private native void nPlay();     
	    // Ustawienie potoku na wstrzymanie odtwarzania
	    private native void nStop();    
	    // Inicjalizacja klasy macierzystej 
	    private static native boolean nClassInit(); 
	    // Macierzysta metoda odpowiadaj¹ca za rysowanie na powierzchni SurfaceView
	    private native void nSurfaceInit(Object surface);
	    // Finalizacja rysowania na powierzchni
	    private native void nSurfaceFinalize();
	    // Pole s³u¿¹ce do przechowywania danych kodu macierzystego
	    private long native_custom_data;      
	    // Pole to s³u¿y do przechowywania stanu odtwarzania, w celu przywrócenia go po wznowieniu aktywnoœci.
	    private boolean playing_state;   
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		 // Inicjalizacja GStreamera
		try {
	            GStreamer.init(this);
	        } catch (Exception e) {
	            Toast.makeText(this, e.getMessage(), Toast.LENGTH_LONG).show();
	            finish(); 
	            return;
	        }
			setContentView(R.layout.activity_streaming_client);
		 SurfaceView sv = (SurfaceView) this.findViewById(R.id.streamingView);
		 SurfaceHolder sh = sv.getHolder();
		 sh.addCallback(this);
	
 // Przywracanie stanu odtwarzania w jakim nast¹pi³o wstrzymanie aplikacji.
        if (savedInstanceState != null) {
            playing_state = savedInstanceState.getBoolean("playing");
        } else {
            playing_state = false;

        }
 
 // Inicjalizacja kodu macierzystego.
        nInit();
    }
 // Zapisywanie stanu odtwarzania w celu przywrócenia go, po wznowieniu dzia³ania aplikacji.
    protected void onSaveInstanceState (Bundle outState) {
        outState.putBoolean("playing", playing_state);
    }
 
    protected void onDestroy() {
        nFinalize();
        super.onDestroy();
    }
 
    // Metoda ta jest wywo³ywana przez kod macierzysty w momencie poprawnego zainicjowania GStreamera.
    private void onGStreamerInitialized () {
              // Po inicjalizacji nastêpuje ustawienie stanu odtwarzania
        if (playing_state) {
            nPlay();
        } else {
            nStop();
        }
 
    
    }
 
  
// Dodanie listy opcji do menu.
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.streaming_client, menu);
		return true;
	}
	// Dzia³ania po wybraniu opcji menu. Mo¿liwe jest rozpoczêcie odtwarzania, wstrzymanie, przejœcie do trybu pozycjonowania.
	@Override
    public boolean onOptionsItemSelected(MenuItem item) {
 
 
        switch (item.getItemId()) {
 
        case R.id.action_play:
        	playing_state = true;
            nPlay();
            return true;
        case R.id.action_stop:
        	playing_state = false;
            nStop();
            return true;
        case R.id.set_position:
        	startActivity(new Intent(this, SetPosition.class));
            return true;
        default:
        	return super.onOptionsItemSelected(item);
 
        }
 

    }

// Statyczne wczytanie bibliotek u¿ywanych w projekcie oraz macierzystej klasy nClassInit().
	  static {
	        System.loadLibrary("gstreamer_android");
	        System.loadLibrary("StreamingC");
	        nClassInit();
	    }
	  
	  public void surfaceChanged(SurfaceHolder holder, int format, int width,
	            int height) {
	       	        nSurfaceInit (holder.getSurface());
	    }
	 
	    public void surfaceCreated(SurfaceHolder holder) {
	      
	    }
	 
	    public void surfaceDestroyed(SurfaceHolder holder) {
	        nSurfaceFinalize ();
	    }
}
