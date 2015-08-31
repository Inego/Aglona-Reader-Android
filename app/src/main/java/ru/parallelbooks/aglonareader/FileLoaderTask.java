package ru.parallelbooks.aglonareader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;




class FileLoaderTask<T extends Activity & OnFileLoadingComplete> extends AsyncTask<Void, Void, Boolean> {
	
	private final String fileName;
	private T activity;
	private ProgressDialog dialog;

	public FileLoaderTask(String fileName, T activity) {
		this.fileName = fileName;
		this.activity = activity;
	}

	@Override
	protected void onPreExecute() {

		showProgressDialog();

		super.onPreExecute();
	}

	private void showProgressDialog() {

		dialog = new ProgressDialog(activity);

		dialog.setMessage(activity.getResources().getString(
				R.string.loading_please_wait));
		dialog.setIndeterminate(true);
		dialog.setCancelable(false);

		dialog.show();

	}
	
	public void detach() {
		activity = null;
	}
	
	
	public void attach(T activity) {
		this.activity = activity;
		showProgressDialog();
	}

	@Override
	protected Boolean doInBackground(Void... unused) {
		
		ParallelText pText = new ParallelText();
		
		boolean result = pText.Load(fileName);
		
		ParallelTextData pTD = ParallelTextData.getInstance();
		
		if (result)
			pTD.pText = pText;
		
		return result;
	}

	@Override
	protected void onPostExecute(Boolean result) {
		
		if (dialog != null) {
			dialog.dismiss();
			dialog = null;
		}
		
		if (activity != null) {
			activity.onFileLoadingComplete(result, fileName);
			activity = null;
		}

		super.onPostExecute(result);
	}

// --Commented out by Inspection START (08/20/15 7:44 PM):
//	public void setFileName(String fileName) {
//		this.fileName = fileName;
//	}
// --Commented out by Inspection STOP (08/20/15 7:44 PM)

}
