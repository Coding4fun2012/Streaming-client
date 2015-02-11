/*/ Klasa s�u�y do nawi�zywania po��czenia z serwerem przy pomocy protoko�u TCP i wysy�anie do niego informacji/*/
package com.ww.streamingclient;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.net.UnknownHostException;

import android.os.AsyncTask;

class SendingClient extends AsyncTask<String, String, String> {

	 
	  private Socket client;
	  private PrintWriter printwriter;
	  private String mes="";
	  @Override
	  
	  protected String doInBackground(String... params) {
		  
			  try {
			
			    mes = params[0];
					     client = new Socket("192.168.43.242", 5001);  //po��czenie z serwerem
					     
			     printwriter = new PrintWriter(client.getOutputStream(),true);
			     
			     printwriter.write(mes);  // zapisanie wiadomo�ci do strumienia wychodz�cego
			     
			     printwriter.flush();
			     printwriter.close();
			     client.close();// zamykanie po��czenia
					     
					     
			    } catch (UnknownHostException e) {
			     e.printStackTrace();
			    } catch (IOException e) {
			     e.printStackTrace();
			    }
			 
		  return mes;
	  }
} 