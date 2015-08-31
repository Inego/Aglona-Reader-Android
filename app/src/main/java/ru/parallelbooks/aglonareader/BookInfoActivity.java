package ru.parallelbooks.aglonareader;

import android.app.Activity;
import android.os.Bundle;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.CompoundButton.OnCheckedChangeListener;
import android.widget.TextView;

public class BookInfoActivity extends Activity {

	private ParallelText pText;
	private ParallelTextData pTD;
//	public ParallelTextView pTV;

	private void fillFields() {
		if (pTD.reversed) {
			
			((TextView) findViewById(R.id.lang1)).setText(pText.lang2);
			((TextView) findViewById(R.id.author1)).setText(pText.author2);
			((TextView) findViewById(R.id.title1)).setText(pText.title2);
			((TextView) findViewById(R.id.info1)).setText(pText.info2);

			((TextView) findViewById(R.id.lang2)).setText(pText.lang1);
			((TextView) findViewById(R.id.author2)).setText(pText.author1);
			((TextView) findViewById(R.id.title2)).setText(pText.title1);
			((TextView) findViewById(R.id.info2)).setText(pText.info1);

		} else {
			
			((TextView) findViewById(R.id.lang1)).setText(pText.lang1);
			((TextView) findViewById(R.id.author1)).setText(pText.author1);
			((TextView) findViewById(R.id.title1)).setText(pText.title1);
			((TextView) findViewById(R.id.info1)).setText(pText.info1);

			((TextView) findViewById(R.id.lang2)).setText(pText.lang2);
			((TextView) findViewById(R.id.author2)).setText(pText.author2);
			((TextView) findViewById(R.id.title2)).setText(pText.title2);
			((TextView) findViewById(R.id.info2)).setText(pText.info2);

		}

	}

	@Override
	protected void onCreate(Bundle savedInstanceState) {

		super.onCreate(savedInstanceState);

		setContentView(R.layout.activity_book_info);

		pTD = ParallelTextData.getInstance();

		pText = pTD.pText;
		
		

		fillFields();

		((TextView) findViewById(R.id.commonInfo)).setText(pText.info);

		((TextView) findViewById(R.id.textViewNumberOfFragments))
				.setText(Integer.toString(pText.Number()));
		
		CheckBox cb = (CheckBox) findViewById(R.id.reverseCheckBox); 
		
		cb.setChecked(pTD.reversed);
		
		cb.setOnCheckedChangeListener(new OnCheckedChangeListener() {
			
			@Override
			public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
				pTD.reversed = isChecked;
				fillFields();
				pTD.SetSplitterPositionByRatio(1 - pTD.SplitterRatio);
				pTD.SetLayoutMode();
				pTD.TurnAdvancedPopupOff();
				pTD.pText.Truncate();
				pTD.PrepareScreen();
				pTD.pTV.invalidateParallelText(); // update screen

			}
		});
		
		

	}

}
