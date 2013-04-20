package ru.parallelbooks.aglonareader;

import ru.parallelbooks.aglonareader.BookContents.Chapter;
import android.app.ListActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;
import android.widget.TextView;

public class ContentsActivity extends ListActivity {

	class ContentsAdapter extends BaseAdapter {

		ParallelText pText;

		BookContents contents;

		public int side;

		@Override
		public int getViewTypeCount() {
			return 3;
		}

		@Override
		public int getItemViewType(int position) {
			return contents.chapters.get(position).level;
		}

		@Override
		public int getCount() {
			return contents.chapters.size();
		}

		@Override
		public Chapter getItem(int position) {
			return contents.chapters.get(position);
		}

		@Override
		public long getItemId(int position) {
			return 0;
		}

		@Override
		public View getView(int position, View convertView, ViewGroup parent) {

			Chapter c = getItem(position);

			if (convertView == null) {

				LayoutInflater inflater = getLayoutInflater();

				int resource_id;

				switch (c.level) {
				case 2:
					resource_id = R.layout.contents_level2;
					break;
				case 3:
					resource_id = R.layout.contents_level3;
					break;
				default:
					resource_id = R.layout.contents_level1;
					break;
				}

				convertView = inflater.inflate(resource_id, parent, false);

			}

			TextPair p = pText.get(c.pair);

			((TextView) convertView).setText(side == 1 ? p.text1 : p.text2);

			return convertView;

		}

		public ContentsAdapter(ParallelText pText2) {

			this.pText = pText2;
			this.contents = pText2.contents;

		}

		public int getChapterByCurrentPair(int currentPair) {

			int result = 0;
			int contentsSize = contents.size();

			for (int i = 0; i < contentsSize; i++)

				if (contents.get(i).pair <= currentPair)
					result = i;
				else
					break;

			return result;
		}

	}

	public static final int GOTO_CHAPTER = 0;

	private ContentsAdapter adapter;

	private int checkedSide;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_contents);

		ParallelTextData pTD = ParallelTextData.getInstance();
		ParallelText pText = pTD.pText;

		checkedSide = pText.contentsSide;

		adapter = new ContentsAdapter(pText);
		
		setAdapterSide(false);

		setListAdapter(adapter);

		// Let's find an appropriate position

		if (pTD.Number() > 0) {

			int currentChapter = adapter
					.getChapterByCurrentPair(pTD.CurrentPair);

			ListView lv = getListView();

			lv.setSelection(currentChapter);
		}

		String l1;
		String l2;

		if (pTD.reversed) {
			l1 = pText.lang2;
			l2 = pText.lang1;
		} else {
			l1 = pText.lang1;
			l2 = pText.lang2;
		}

		RadioButton r1 = (RadioButton) findViewById(R.id.contents_left);
		RadioButton r2 = (RadioButton) findViewById(R.id.contents_right);

		if (l1.length() != 0)
			r1.setText(l1);
		if (l2.length() != 0)
			r2.setText(l2);

		(pText.contentsSide == 1 ? r1 : r2).setChecked(true);

		RadioGroup rl = (RadioGroup) findViewById(R.id.contents_leftright_radiogroup);

		rl.setOnCheckedChangeListener(new OnCheckedChangeListener() {

			@Override
			public void onCheckedChanged(RadioGroup group, int checkedId) {

				checkedSide = (checkedId == R.id.contents_left ? 1 : 2);
				ParallelTextData.getInstance().pText.contentsSide = checkedSide;
				
				setAdapterSide(true);

			}
		});

	}

	private void setAdapterSide(boolean notify) {

		ParallelTextData pTD = ParallelTextData.getInstance();

		adapter.side = ((checkedSide == 1 ? 1 : -1) * (pTD.reversed ? -1 : 1) == 1 ? 1 : 2);
		
		if (notify)
			adapter.notifyDataSetChanged();

	}

	@Override
	protected void onListItemClick(ListView l, View v, int position, long id) {

		Intent i = new Intent();

		i.putExtra("pair", adapter.getItem(position).pair);

		setResult(GOTO_CHAPTER, i);

		finish();

	}

}
