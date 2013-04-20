package ru.parallelbooks.aglonareader;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.LinearLayout;
import android.widget.TextView;

public class SelectFileActivity extends Activity {

	class FilePathData implements Comparable<FilePathData> {
		boolean isDirectory;
		String name;
		String fullPath;

		public FilePathData(boolean isDirectory, String name, String fullPath) {
			this.isDirectory = isDirectory;
			this.name = name;
			this.fullPath = fullPath;
		}

		@Override
		public int compareTo(FilePathData another) {
			if (name != null)

				return name.compareToIgnoreCase(another.name);
			else
				throw new IllegalArgumentException();
		}

	}

	private File currentDir;
	private String TopPath;

	LinearLayout linearLayout;

	List<FilePathData> dir = new ArrayList<FilePathData>();

	OnClickListener oncl = new OnClickListener() {

		@Override
		public void onClick(View v) {

			FilePathData f = dir.get(v.getId());

			if (f.isDirectory)
				fillTheList(new File(f.fullPath));
			else
			{
				Intent data = new Intent();
				data.putExtra("file_name", f.fullPath);
				setResult(0, data);
				finish();
			}
				

		}
	};

	private void fillTheList(File folder) {

		linearLayout.removeAllViews();

		File[] dirs = folder.listFiles();

		setTitle(folder.getPath());

		List<FilePathData> fls = new ArrayList<FilePathData>();

		dir.clear();

		try {
			for (File ff : dirs) {
				
				String fileName = ff.getName();

				if (ff.isDirectory()) {
					dir.add(new FilePathData(true, fileName, ff.getAbsolutePath()));
				} else {
					int dotPos = fileName.lastIndexOf('.');
					String extension = fileName.substring(dotPos + 1).toLowerCase(); 
					if (extension.equals("pbo"))
					{
						String fileNameWithoutExtension = fileName.substring(0,	dotPos);
						fls.add(new FilePathData(false, fileNameWithoutExtension, ff.getAbsolutePath()));
					}
				}

			}
		} catch (Exception e) {

		}

		Collections.sort(dir);
		Collections.sort(fls);

		dir.addAll(fls);

		if (!folder.getPath().equalsIgnoreCase(TopPath))
			dir.add(0, new FilePathData(true, "..", folder.getParent()));

		LayoutInflater inflater = getLayoutInflater();

		// Let's inflate
		for (int i = 0; i < dir.size(); i++) {

			FilePathData file = dir.get(i);

			TextView item = (TextView) inflater.inflate(
					file.isDirectory ? R.layout.select_file_folder_row
							: R.layout.select_file_file_row, linearLayout,
					false);

			item.setText(file.name);
			item.setId(i);

			item.setOnClickListener(oncl);

			linearLayout.addView(item);

		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_select_file);

		linearLayout = (LinearLayout) findViewById(R.id.select_file_linear);

		TopPath = Environment.getExternalStorageDirectory().getPath();

		currentDir = new File(TopPath);

		fillTheList(currentDir);

	}

}
