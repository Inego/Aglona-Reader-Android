package ru.parallelbooks.aglonareader;

import android.app.Activity;
import android.app.ProgressDialog;
import android.os.AsyncTask;




public class FileLoaderTask<T extends Activity & OnFileLoadingComplete> extends AsyncTask<Void, Void, Boolean> {
	
	private String fileName;
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

	public void showProgressDialog() {

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
		pText = new ParallelText();
		
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

	public void setFileName(String fileName) {
		this.fileName = fileName;
	}
	
}
