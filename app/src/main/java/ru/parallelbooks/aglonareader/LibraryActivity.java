package ru.parallelbooks.aglonareader;

import java.util.ArrayList;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ListActivity;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

public class LibraryActivity extends ListActivity {
	
	private ParallelTextData pTD;

	private class LibraryAdapter extends BaseAdapter {

		private final ArrayList<FileUsageInfo> mData;

		public LibraryAdapter() {
			mData = pTD.fileUsageInfo;
		}

		@Override
		public int getCount() {
			return mData.size();
		}

		@Override
		public Object getItem(int position) {
			return mData.get(position);
		}

		@Override
		public long getItemId(int position) {
			return position;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			if (convertView == null) {
				convertView = getLayoutInflater().inflate(R.layout.library_row,
						parent, false);
			}

			FileUsageInfo f = mData.get(position);

			int lastSlash = f.FileName.lastIndexOf('/') + 1;

			String path = f.FileName.substring(0, lastSlash);
			String fileName = f.FileName.substring(lastSlash);

			int lastDot = fileName.lastIndexOf('.');

			fileName = fileName.substring(0, lastDot);

			((TextView) convertView.findViewById(R.id.library_row_filename))
					.setText(fileName);
			((TextView) convertView.findViewById(R.id.library_row_path))
					.setText(path);

			return convertView;

		}
		
		public void removeAt(int position) {
			mData.remove(position);
			notifyDataSetChanged();
		}
		
		public void clear() {
			mData.clear();
			notifyDataSetChanged();
		}

	}

	public static final int OPEN_FILE = 1;

	private LibraryAdapter mAdapter;
	
	private int lineNumber;

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_library);

		ListView lv = getListView();
		lv.setLongClickable(true);
		lv.setOnItemLongClickListener(new OnItemLongClickListener() {

			@Override
			public boolean onItemLongClick(AdapterView<?> parent, View view,
					int position, long id) {
				
				lineNumber = position;

				showDialog(0);

				return false;
			}

		});
		
		pTD = ParallelTextData.getInstance();

		mAdapter = new LibraryAdapter();

		setListAdapter(mAdapter);

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		FileUsageInfo f = (FileUsageInfo) mAdapter.getItem(position);

		openFile(f.FileName);

		super.onListItemClick(l, v, position, id);
	}

	private void openFile(String fileName) {

		Intent intent = new Intent();

		intent.putExtra("file_name", fileName);

		setResult(OPEN_FILE, intent);

		finish();
	}

 	public void onAddFromSDCard(View v) {

		Intent intent = new Intent(this, SelectFileActivity.class);
		startActivityForResult(intent, 0);

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {

		if (resultCode == 0 && data != null)
			openFile(data.getStringExtra("file_name"));

		super.onActivityResult(requestCode, resultCode, data);
	}

	@Override
	protected Dialog onCreateDialog(int id) {

		AlertDialog.Builder builder = new AlertDialog.Builder(
				LibraryActivity.this);

		switch (id) {

		case 0:

			builder.setTitle(R.string.removing_item);
			builder.setMessage(R.string.remove_item_library_q);

			builder.setPositiveButton(R.string.yes, new OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					
					if (lineNumber == 0 && pTD.bookOpened)
						pTD.clearParallelText();
					
					mAdapter.removeAt(lineNumber);

				}
			});

			builder.setNeutralButton(R.string.remove_all,
					new OnClickListener() {

						@Override
						public void onClick(DialogInterface dialog, int which) {
							
							showDialog(1);

						}
					});

			builder.setNegativeButton(R.string.cancel, new OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					// TODO Auto-generated method stub

				}
			});

			break;
			
		case 1:
			
			builder.setTitle(R.string.confirm);
			builder.setMessage(R.string.remove_all_library_q);

			builder.setPositiveButton(R.string.yes, new OnClickListener() {

				public void onClick(DialogInterface dialog, int which) {
					
					if (pTD.bookOpened)
						pTD.clearParallelText();
					
					mAdapter.clear();

				}
			});

			builder.setNegativeButton(R.string.no,
					new OnClickListener() {
						@Override
						public void onClick(DialogInterface dialog, int which) {
							// Do nothing
						}
					});

			break;

		}

		return builder.create();

	}

}
