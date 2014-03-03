package com.moodilabs.pass;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;

import nl.matshofman.saxrssreader.RssFeed;
import nl.matshofman.saxrssreader.RssItem;
import nl.matshofman.saxrssreader.RssReader;

import org.xml.sax.SAXException;

import android.app.Activity;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.Intent;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.view.Menu;
import android.view.View;
import android.view.View.OnClickListener;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.google.common.io.Files;
import com.simonguest.btxfr.ClientThread;
import com.simonguest.btxfr.MessageType;
import com.simonguest.btxfr.ProgressData;
import com.simonguest.btxfr.ServerThread;

public class MainActivity extends Activity {

	private static final String loggerTag = "MainActivityLogger";
	private static final int REQUEST_ENABLE_BT_DURATION = 300;
	private boolean dataUpdated = false;
	private RssFeed feed;
	boolean btEnabled = true;

	private String sampleURL1 = "http://en.balatarin.com/feed";
	private String sampleURL2 = "http://www.engadget.com/rss.xml";
	private String selectedURL = sampleURL2;

	private Spinner deviceSpinner;
	private ProgressDialog progressDialog;
	private static String filePath = Environment.getExternalStorageDirectory()
			.getAbsolutePath() + "/TestDir/downloadedFile.xml";;
	private static File rssFile;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		setContentView(R.layout.activity_main);

		Button source = (Button) findViewById(R.id.sourceButton);
		source.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				if (selectedURL.equals(sampleURL1))
					selectedURL = sampleURL2;
				else
					selectedURL = sampleURL1;

				fetchRSS();

			}

		});
		Button refresh = (Button) findViewById(R.id.refreshButton);

		refresh.setOnClickListener(new OnClickListener() {

			@Override
			public void onClick(View v) {

				onResume();
			}
		});

		BluetoothAdapter mBluetoothAdapter = BluetoothAdapter
				.getDefaultAdapter();
		if (mBluetoothAdapter == null) {
			// Device does not support Bluetooth

			Log.e(loggerTag, "NO BLUETOOTH HARDWARE!");
			btEnabled = false;
		}

		if (!mBluetoothAdapter.isEnabled()) {
			Intent discoverableIntent = new Intent(
					BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
			discoverableIntent.putExtra(
					BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION,
					REQUEST_ENABLE_BT_DURATION);
			startActivity(discoverableIntent);
		}

		if (!btEnabled)
			finish();

		feed = fetchRSS();

		while (!dataUpdated) {
			try {
				Thread.sleep(100);
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
		}

		MainApplication.clientHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
				case MessageType.READY_FOR_DATA: {
					// TODO

					byte[] fileBytes;
					try {

						fileBytes = Files.toByteArray(rssFile);

						Message message2 = new Message();
						message2.obj = fileBytes;
						MainApplication.clientThread.incomingHandler
								.sendMessage(message2);

					} catch (IOException e) {
						// TODO Auto-generated catch block
						Log.e(loggerTag, "File not found");
						e.printStackTrace();
					}

					// Invoke client thread to send

					break;
				}

				case MessageType.COULD_NOT_CONNECT: {
					Toast.makeText(MainActivity.this,
							"Could not connect to the paired device",
							Toast.LENGTH_SHORT).show();
					break;
				}

				case MessageType.SENDING_DATA: {
					progressDialog = new ProgressDialog(MainActivity.this);
					progressDialog.setMessage("Sending data...");
					progressDialog
							.setProgressStyle(ProgressDialog.STYLE_SPINNER);
					progressDialog.show();
					break;
				}

				case MessageType.DATA_SENT_OK: {
					if (progressDialog != null) {
						progressDialog.dismiss();
						progressDialog = null;
					}
					Toast.makeText(MainActivity.this,
							"Data was sent successfully", Toast.LENGTH_SHORT)
							.show();
					break;
				}

				case MessageType.DIGEST_DID_NOT_MATCH: {
					Toast.makeText(MainActivity.this,
							"Data was sent, but didn't go through correctly",
							Toast.LENGTH_SHORT).show();
					break;
				}
				}
			}
		};

		MainApplication.serverHandler = new Handler() {
			@Override
			public void handleMessage(Message message) {
				switch (message.what) {
				case MessageType.DATA_RECEIVED: {
					if (progressDialog != null) {
						progressDialog.dismiss();
						progressDialog = null;
					}
					File SDCardRoot = new File(
							Environment.getExternalStorageDirectory(),
							"TestDir");
					SDCardRoot.mkdirs();
					String filename = "downloadedFile.xml";
					Log.i("Local filename:", "" + filename);
					File file = new File(SDCardRoot, filename);
					try {
						if (file.createNewFile()) {
							file.createNewFile();
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					// TODO
					try {
						Files.write((byte[]) message.obj, rssFile);
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						Log.d(loggerTag, "Write failed");
					}
					rssFile = file;
					onResume();

					/*
					 * FileOutputStream fileOutput = new FileOutputStream(
					 * file); int downloadedSize = 0; byte[] buffer = (byte[])
					 * message.obj; new byte[1024]; int bufferLength = 0; while
					 * ((bufferLength = inputStream.read(buffer)) > 0) {
					 * fileOutput.write(buffer, 0, buffer.length);
					 * downloadedSize += bufferLength; Log.i("Progress:",
					 * "downloadedSize:" + downloadedSize + "totalSize:" +
					 * totalSize); } fileOutput.close();
					 * 
					 * rssFile = file; if (downloadedSize == totalSize) filepath
					 * = file.getPath();
					 */break;
				}

				case MessageType.DIGEST_DID_NOT_MATCH: {
					Toast.makeText(
							MainActivity.this,
							"Data was received, but didn't come through correctly",
							Toast.LENGTH_SHORT).show();
					break;
				}

				case MessageType.DATA_PROGRESS_UPDATE: {
					// some kind of update
					MainApplication.progressData = (ProgressData) message.obj;
					double pctRemaining = 100 - (((double) MainApplication.progressData.remainingSize / MainApplication.progressData.totalSize) * 100);
					if (progressDialog == null) {
						progressDialog = new ProgressDialog(MainActivity.this);
						progressDialog.setMessage("Receiving data...");
						progressDialog
								.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
						progressDialog.setProgress(0);
						progressDialog.setMax(100);
						progressDialog.show();
					}
					progressDialog.setProgress((int) Math.floor(pctRemaining));
					break;
				}

				case MessageType.INVALID_HEADER: {
					Toast.makeText(
							MainActivity.this,
							"Data was sent, but the header was formatted incorrectly",
							Toast.LENGTH_SHORT).show();
					break;
				}
				}
			}
		};

		if (MainApplication.pairedDevices != null) {
			if (MainApplication.serverThread == null) {
				Log.v(loggerTag,
						"Starting server thread.  Able to accept photos.");
				MainApplication.serverThread = new ServerThread(
						MainApplication.adapter, MainApplication.serverHandler);
				MainApplication.serverThread.start();
			}
		}

		if (MainApplication.pairedDevices != null) {
			ArrayList<DeviceData> deviceDataList = new ArrayList<DeviceData>();
			for (BluetoothDevice device : MainApplication.pairedDevices) {
				deviceDataList.add(new DeviceData(device.getName(), device
						.getAddress()));
			}

			ArrayAdapter<DeviceData> deviceArrayAdapter = new ArrayAdapter<DeviceData>(
					this, android.R.layout.simple_spinner_item, deviceDataList);
			deviceArrayAdapter
					.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
			deviceSpinner = (Spinner) findViewById(R.id.deviceSpinner);
			deviceSpinner.setAdapter(deviceArrayAdapter);

			Button clientButton = (Button) findViewById(R.id.clientButton);

			clientButton.setOnLongClickListener(new View.OnLongClickListener() {

				@Override
				public boolean onLongClick(View v) {

					feed = fetchRSS();
					onResume();
					return false;
				}
			});

			clientButton.setOnClickListener(new View.OnClickListener() {
				@Override
				public void onClick(View view) {
					DeviceData deviceData = (DeviceData) deviceSpinner
							.getSelectedItem();
					for (BluetoothDevice device : MainApplication.adapter
							.getBondedDevices()) {
						if (device.getAddress().contains(deviceData.getValue())) {
							Log.v(loggerTag, "Starting client thread");
							if (MainApplication.clientThread != null) {
								MainApplication.clientThread.cancel();
							}
							MainApplication.clientThread = new ClientThread(
									device, MainApplication.clientHandler);
							MainApplication.clientThread.start();
						}
					}
				}
			});
		} else {
			Toast.makeText(this,
					"Bluetooth is not enabled or supported on this device",
					Toast.LENGTH_LONG).show();
		}

	}

	@Override
	protected void onResume() {
		// TODO Auto-generated method stub
		super.onResume();
		TextView tv = (TextView) findViewById(R.id.textView1);
		ArrayList<String> rssItemsTitle = new ArrayList<String>();
		updateFeed();
		ListView lv = (ListView) findViewById(R.id.listView1);

		
		if (dataUpdated) {
			lv.setVisibility(View.VISIBLE);
			tv.setVisibility(View.INVISIBLE);
				
			
			final ArrayList<RssItem> rssItems = feed.getRssItems();
			for (RssItem rssItem : rssItems) {
				rssItemsTitle.add(rssItem.getTitle());

			}

		
			lv.setAdapter(new ArrayAdapter<String>(this,
					android.R.layout.simple_list_item_1, rssItemsTitle));

			OnItemClickListener listClickListener = new OnItemClickListener() {

				@Override
				public void onItemClick(AdapterView<?> arg0, View v,
						int position, long arg3) {

					Intent detailIntent = new Intent(getBaseContext(),
							DetailActivity.class);
					detailIntent.putExtra("content", rssItems.get(position)
							.getContent());

					startActivity(detailIntent);
				}

			};
			lv.setOnItemClickListener(listClickListener);
		}
		
		else{
			lv.setVisibility(View.INVISIBLE);
			tv.setVisibility(View.VISIBLE);
			
		}
	}

	private RssFeed fetchRSS() {

		dataUpdated = false;
		Thread mThread = new Thread(new Runnable() {
			@Override
			public void run() {
				try {

					new Thread(new Runnable() {

						@Override
						public void run() {

							String filepath = Environment
									.getExternalStorageDirectory()
									.getAbsolutePath();
							try {
								URL url = new URL(selectedURL);
								HttpURLConnection urlConnection = (HttpURLConnection) url
										.openConnection();
								urlConnection.setRequestMethod("GET");
								urlConnection.setDoOutput(true);
								urlConnection.connect();

								File SDCardRoot = new File(Environment
										.getExternalStorageDirectory(),
										"TestDir");
								SDCardRoot.mkdirs();
								String filename = "downloadedFile.xml";
								Log.i("Local filename:", "" + filename);
								File file = new File(SDCardRoot, filename);
								if (file.createNewFile()) {
									file.createNewFile();
								}
								FileOutputStream fileOutput = new FileOutputStream(
										file);
								InputStream inputStream = urlConnection
										.getInputStream();
								int totalSize = urlConnection
										.getContentLength();
								int downloadedSize = 0;
								byte[] buffer = new byte[1024];
								int bufferLength = 0;
								while ((bufferLength = inputStream.read(buffer)) > 0) {
									fileOutput.write(buffer, 0, bufferLength);
									downloadedSize += bufferLength;
									Log.i("Progress:", "downloadedSize:"
											+ downloadedSize + "totalSize:"
											+ totalSize);
								}
								fileOutput.close();

								rssFile = file;
								if (downloadedSize == totalSize)
									filepath = file.getPath();

							} catch (MalformedURLException e) {
								e.printStackTrace();
							} catch (IOException e) {
								filepath = null;
								e.printStackTrace();
							}
						}
					}).start();
				}

				finally {

					updateFeed();

					dataUpdated = true;

				}
			}
		});
		mThread.start();
		return feed;
	}

	protected void updateFeed() {
		try {
			feed = RssReader.read(new URL("file://" + filePath));
		} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (SAXException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
		super.onActivityResult(requestCode, resultCode, data);

		if (requestCode == REQUEST_ENABLE_BT_DURATION) {
			if (resultCode == RESULT_OK) {
				btEnabled = true;
			} else
				btEnabled = false;
		}
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		// Inflate the menu; this adds items to the action bar if it is present.
		getMenuInflater().inflate(R.menu.main, menu);
		return true;
	}

	class DeviceData {
		public DeviceData(String spinnerText, String value) {
			this.spinnerText = spinnerText;
			this.value = value;
		}

		public String getValue() {
			return value;
		}

		public String toString() {
			return spinnerText;
		}

		String spinnerText;
		String value;
	}

}
