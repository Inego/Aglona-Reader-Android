package ru.parallelbooks.aglonareader;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.util.ArrayList;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.content.res.Configuration;
import android.media.AudioManager;
import android.media.SoundPool;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.util.Base64;
import android.util.Base64InputStream;
import android.util.Base64OutputStream;
import android.util.Log;
import android.view.ContextMenu;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;

public class MainActivity extends Activity implements OnFileLoadingComplete, TextToSpeech.OnInitListener {
	private ParallelTextView pTC;
	private ParallelTextData pTD;
	private final static int REQUEST_CODE_SETTINGS = 1;
	private final static int REQUEST_CODE_BOOK_INFO = 2;
	private final static int REQUEST_CODE_LIBRARY = 3;
	private final static int REQUEST_CODE_NAVIGATION = 4;
	// --Commented out by Inspection (09/17/15 9:52 AM):private final static int SEEKBAR_MODE_OFF = 0;
	// --Commented out by Inspection (08/20/15 7:38 PM):final static int SEEKBAR_MODE_FONT = 1;
	// --Commented out by Inspection (08/20/15 7:39 PM):final static int SEEKBAR_MODE_BRIGHTESS = 2;
	// --Commented out by Inspection (08/20/15 7:39 PM):final static float FONT_SIZE_MIN = 8.0f;
	// --Commented out by Inspection (08/20/15 7:39 PM):final static float FONT_SIZE_STEP = .025f;
	// --Commented out by Inspection (08/20/15 7:39 PM):final static int DIALOG_OPEN_FILE = 1;
	private FileLoaderTask<MainActivity> fileLoaderTask;
	private SoundPool sp;
	protected int soundIds[];
	public static boolean DoSoundEffects;
	// Callback required for TextToSpeech:
	public static TextToSpeech speakT;
	// So we can make a Toast from ParallelTextData.java:
	private static MainActivity statMain;

	public static MainActivity getInstance() {
		return statMain;
	}

	@Override
	public void onInit(int status) {
		Log.e("Main", "OnInit - Status [" + status + "]");
		if (status == TextToSpeech.ERROR) {
			pTD.SpeakText = false;
			Toast.makeText(this, R.string.tts_init_fail, Toast.LENGTH_LONG).show();
		}
	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		/* todo: on some platforms, first use of speech and switch to new language incurs delay
		of several seconds.  Possibly remedy this by starting two threads as in following:
		http://stackoverflow.com/questions/26994354/android-two-instances-of-text-to-speech-work-very-slowly
	*/
//		Log.e("Main", "creating TextToSpeech..");
		speakT = new TextToSpeech(this, this);
//		Log.e("Main", "TextToSpeech created.");
		// Hack to preload engine:
		MainActivity.speakT.speak("o", TextToSpeech.QUEUE_FLUSH, null);
//		Log.e("Main", "TextToSpeech engine startup finished.");
		statMain = this; // Needed by getInstance()
		// Remove title bar
		this.requestWindowFeature(Window.FEATURE_NO_TITLE);
		// Remove notification bar
		this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
				WindowManager.LayoutParams.FLAG_FULLSCREEN);
		setContentView(R.layout.activity_main);
		pTC = (ParallelTextView) findViewById(R.id.parallelTextView);
		registerForContextMenu(pTC);
		boolean pTDExisted = ParallelTextData.instanceExists();
		pTD = ParallelTextData.getInstance();
		pTC.pTD = pTD;
		if (!pTDExisted) {
			// Set values from default settings
			SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
			PreferenceManager.setDefaultValues(this, R.xml.preferences, false);
			pTD.HighlightFirstWords = prefs.getBoolean(
					"pref_key_highlight_first_words", false);
			pTD.HighlightFragments = prefs.getBoolean(
					"pref_key_highlight_fragments", false);
			//todo: integral TTS speed control
			pTD.SpeakText = prefs.getBoolean("pref_key_speak_text", false);
			DoSoundEffects = prefs.getBoolean("pref_key_sound_effects", false);
//			Log.e("Main", "set SoundEffects to " + Boolean.toString(DoSoundEffects));
			pTD.setBrightness(prefs.getFloat("pref_key_highlight_brightness", 0.85f));
			pTD.bookOpened = prefs.getBoolean("load_file", false);
			// pTD.SetLayoutMode();
			pTD.fontProportion = prefs.getInt("font_proportion", 200);
			if (pTD.fontRangeSet)
				pTD.setFontSize(false);
			String serializedFileList = prefs.getString("files", "");
			if (serializedFileList != null && serializedFileList.length() > 0) {
				Object fromString = stringToObject(serializedFileList);
				pTD.fileUsageInfo = (ArrayList<FileUsageInfo>) fromString;
			}
			if (pTD.fileUsageInfo == null || !pTD.bookOpened)
				startLibraryActivity();
			else
				LoadFromFile(pTD.fileUsageInfo.get(0).FileName);
		}
//		int seekBarMode = SEEKBAR_MODE_OFF;
		fileLoaderTask = (FileLoaderTask<MainActivity>) getLastNonConfigurationInstance();
		if (fileLoaderTask != null)
			fileLoaderTask.attach(this);
		pTC.updateNoBookVisibility();
		// Setup for SoundPool.play (sound effects) <crw>
		sp = new SoundPool(10, AudioManager.STREAM_SYSTEM, 0);
		soundIds = new int[10];
		soundIds[0] = sp.load(this, R.raw.pop1, 1);
		soundIds[1] = sp.load(this, R.raw.hund_hz_saw_qsec, 1);
		soundIds[2] = sp.load(this, R.raw.cwnoise3, 1);
		soundIds[3] = sp.load(this, R.raw.pageturn2, 1);
		soundIds[4] = sp.load(this, R.raw.lm_hz_sine_pt06_sec, 1);
		soundIds[5] = sp.load(this, R.raw.pluck3, 1);
		soundIds[6] = sp.load(this, R.raw.pluck4, 1);
		if (DoSoundEffects) SoundEffect(0, 1, 1, 1); // Startup sound
	}

	@Override
	protected void onResume() {
		super.onResume();
		pTD.TurnAdvancedPopupOff();
	}

	@Override
	public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
		super.onCreateContextMenu(menu, v, menuInfo);
		if (v == pTC) {
			getMenuInflater().inflate(R.menu.activity_main, menu);
		}
	}

	@Override
	public boolean onContextItemSelected(MenuItem item) {
		return onOptionsItemSelected(item);
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.activity_main, menu);
		return true;
	}

	@Override
	public Object onRetainNonConfigurationInstance() {
		if (fileLoaderTask != null)
			fileLoaderTask.detach();
		return fileLoaderTask;
	}

	@Override
	public void onConfigurationChanged(Configuration newConfig) {
		super.onConfigurationChanged(newConfig);
		pTD.TurnAdvancedPopupOff();
	}

	@Override
	protected void onPause() {
		if (pTD.fileUsageInfo.size() > 0 && pTD.bookOpened) {
			FileUsageInfo f0 = pTD.fileUsageInfo.get(0);
			f0.layoutMode = pTD.LayoutMode;
			f0.Reversed = pTD.reversed;
			f0.SplitterRatio = pTD.SplitterRatio;
			f0.TopPair = pTD.CurrentPair;
		}
		SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
		Editor edit = prefs.edit();
		String serializedList = objectToString(pTD.fileUsageInfo);
		edit.putString("files", serializedList);
		edit.putBoolean("load_file", pTD.bookOpened);
		edit.putFloat("pref_key_highlight_brightness", (float) pTD.brightness);
		edit.putInt("font_proportion", pTD.fontProportion);
		edit.commit();
		super.onPause();
	}

	@Override
	public boolean onOptionsItemSelected(MenuItem item) {
		//todo: make submenus return to parent instead of application:
		Intent intent;
		switch (item.getItemId()) {
			case R.id.menu_settings:
				intent = new Intent(this, PreferencesActivity.class);
				startActivityForResult(intent, REQUEST_CODE_SETTINGS);
				break;
			case R.id.menu_book_info:
				intent = new Intent(this, BookInfoActivity.class);
				startActivityForResult(intent, REQUEST_CODE_BOOK_INFO);
				break;
			case R.id.menu_navigation:
				intent = new Intent(this, ContentsActivity.class);
				startActivityForResult(intent, REQUEST_CODE_NAVIGATION);
				break;
			case R.id.menu_library:
				startLibraryActivity();
				break;
		}
		return super.onOptionsItemSelected(item);
	}

	private void startLibraryActivity() {
		Intent intent;
		intent = new Intent(this, LibraryActivity.class);
		startActivityForResult(intent, REQUEST_CODE_LIBRARY);
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		if (requestCode == REQUEST_CODE_LIBRARY) {
			switch (resultCode) {
				case LibraryActivity.OPEN_FILE:
					String fileName = data.getStringExtra("file_name");
					LoadFromFile(fileName);
					break;
			}
		} else if (requestCode == REQUEST_CODE_NAVIGATION) {
			switch (resultCode) {
				case ContentsActivity.GOTO_CHAPTER:
					if (data != null) {
						pTD.CurrentPair = data.getIntExtra("pair", 0);
						pTD.UpdateScreen();
					}
					break;
			}
		}
		super.onActivityResult(requestCode, resultCode, data);
	}

	private boolean RetrieveToTheTop(String fileName) {
		// Let's check whether there exists this file in the list
		for (int index = 0; index < pTD.fileUsageInfo.size(); index++) {
			if (pTD.fileUsageInfo.get(index).FileName.equals(fileName)) {
				if (index != 0) {
					FileUsageInfo toMove = pTD.fileUsageInfo.get(index);
					pTD.fileUsageInfo.remove(toMove);
					pTD.fileUsageInfo.add(0, toMove);
				}
				return true;
			}
		}
		FileUsageInfo fileUsageInfo = new FileUsageInfo();
		fileUsageInfo.FileName = fileName;
		pTD.fileUsageInfo.add(0, fileUsageInfo);
		return false;
	}

	private void LoadSettingsFromFileUsageInfo(FileUsageInfo f) {
		if (f.SplitterRatio == 0)
			f.SplitterRatio = 0.5F;
		pTD.reversed = f.Reversed;
		pTD.LayoutMode = f.layoutMode;
		setLayoutModeAsPreference();
		pTD.SetLayoutMode();
		pTD.SetSplitterPositionByRatio(f.SplitterRatio);
		if (pTD.Number() > 0) {
			if (f.TopPair >= pTD.Number())
				pTD.CurrentPair = pTD.Number() - 1;
			else
				pTD.CurrentPair = f.TopPair;
			pTD.ProcessLayoutChange(false);
		}
	}

	private void setLayoutModeAsPreference() {
		SharedPreferences prefs = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor edit = prefs.edit();
		String modeString = "0";
		switch (pTD.LayoutMode) {
			case 0:
				modeString = "0";
				break;
			case 1:
				modeString = "1";
				break;
			case 2:
				modeString = "2";
				break;
		}
		edit.putString("pref_key_reading_mode", modeString);
		edit.commit();
	}

	private void LoadFromFile(String fileName) {
		fileLoaderTask = new FileLoaderTask<MainActivity>(fileName, this);
		fileLoaderTask.execute();
	}

	@Override
	public void onFileLoadingComplete(boolean success, String fileName) {
		if (success) {
			if (RetrieveToTheTop(fileName))
				LoadSettingsFromFileUsageInfo(pTD.fileUsageInfo.get(0));
			else {
				pTD.CurrentPair = 0;
			}
			pTD.ProcessLayoutChange(false);
			pTD.bookOpened = true;
			pTC.updateNoBookVisibility();
		} else {
			pTD.clearParallelText();
			Toast.makeText(this, R.string.error_loading_file, Toast.LENGTH_LONG).show();
		}
		fileLoaderTask = null;
	}

	@Override
	public boolean onKeyDown(int keyCode, KeyEvent event) {
		if (pTC != null) {
			if (keyCode == KeyEvent.KEYCODE_VOLUME_DOWN) {
				pTC.goToNextPage();
				return true;
			} else if (keyCode == KeyEvent.KEYCODE_VOLUME_UP) {
				pTC.goToPreviousPage();
				return true;
			}
		}
		return super.onKeyDown(keyCode, event);
	}

	private static String objectToString(Serializable object) {
		ByteArrayOutputStream out = new ByteArrayOutputStream();
		try {
			new ObjectOutputStream(out).writeObject(object);
			byte[] data = out.toByteArray();
			out.close();
			out = new ByteArrayOutputStream();
			Base64OutputStream b64 = new Base64OutputStream(out, Base64.DEFAULT);
			b64.write(data);
			b64.close();
			out.close();
			return new String(out.toByteArray());
		} catch (IOException e) {
			e.printStackTrace();
		}
		return null;
	}

	private static Object stringToObject(String encodedObject) {
		try {
			return new ObjectInputStream(new Base64InputStream(
					new ByteArrayInputStream(encodedObject.getBytes()),
					Base64.DEFAULT)).readObject();
		} catch (Exception e) {
			e.printStackTrace();
		}
		return null;
	}

	public void onDestroy() {
//		Log.e("ondestroy: ", "start");
		if (speakT != null) {
			Log.e("ondestroy: ", "stopping");
			speakT.stop();
			speakT.shutdown();
			Log.e("ondestroy: ", "stopped & shut down");
		}
		super.onDestroy();
	}

	public void SoundEffect(int soundId, float lVol, float rVol, float Rate) {
//		Log.e("Main.SE", "playing " + Integer.toString(soundId));
		// play (int soundID, float leftVolume, float rightVolume, int priority, int loop, float rate)
		// Rate can be 0.5 .. 2.0:
		if ((Rate > 2) || (Rate < 0.5)) Rate = 1;
		// Volumes can be 0.0 .. 1.0:
		if ((lVol > 1) || (lVol < 0)) lVol = 1;
		if ((rVol > 1) || (rVol < 0)) lVol = 1;
//		final int play = sp.play(soundIds[soundId], lVol, rVol, 1, 0, Rate);
		sp.play(soundIds[soundId], lVol, rVol, 1, 0, Rate);
	}
}
