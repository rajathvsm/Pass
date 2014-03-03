package com.moodilabs.pass;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.os.Bundle;
import android.text.Html;
import android.view.Menu;
import android.webkit.WebView;
import android.widget.TextView;

public class DetailActivity extends Activity {

	@SuppressLint("NewApi")
	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.showdetail);
//		TextView tv = (TextView) findViewById(R.id.textView1);
				
				String content = (String) getIntent().getExtras().getCharSequence("content", "Data not fetched");

				//content =(Html.fromHtml(content)).toString();
		
				 WebView webview = (WebView)this.findViewById(R.id.webView1);
				 webview.getSettings().setJavaScriptEnabled(true);
				 webview.loadDataWithBaseURL("", content, "text/html", "UTF-8", "");
				
				
				/*		WebView wv = (WebView) findViewById(R.id.textView1);
		wv.set
		wv.setText(getIntent().getExtras().getCharSequence("content", "Data not fetched"));
*/
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.detail, menu);
		return true;
	}

}
