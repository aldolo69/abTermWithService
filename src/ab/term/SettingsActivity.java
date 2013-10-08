package ab.term;

import android.content.Intent;
import android.os.Bundle;
import android.os.Message;
import android.os.Messenger;
import android.preference.PreferenceActivity;
import android.util.Log;

 public class SettingsActivity extends PreferenceActivity {
private int id;
	 
	 @Override
	 protected void onCreate(Bundle savedInstanceState) {
		 Intent intent = getIntent();
		id=intent.getIntExtra("id",-1);
	    super.onCreate(savedInstanceState);
	    addPreferencesFromResource(R.xml.preferences);
	   
	 }
	 
	    @Override
	    protected void onDestroy() {
	    	OutputService.show(this, OutputService.class, id); 
	        super.onDestroy();
	        }
	 }
