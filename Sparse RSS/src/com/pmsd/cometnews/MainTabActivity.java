/**
 * Sparse rss
 *
 * Copyright (c) 2010-2012 Stefan Handschuh
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 *
 */

package com.pmsd.cometnews;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.TabActivity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnKeyListener;
import android.content.Intent;
import android.content.SharedPreferences.Editor;
import android.os.Build;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.util.Linkify;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ScrollView;
import android.widget.TabHost;
import android.widget.TabHost.OnTabChangeListener;
import android.widget.TextView;
import com.pmsd.cometnews.provider.FeedData;

@SuppressWarnings("deprecation")
public class MainTabActivity extends TabActivity {
	private static final int DIALOG_LICENSEAGREEMENT = 0;
	
	private boolean tabsAdded;
	
	private static final String TAG_NORMAL = "normal";
	
	private static final String TAG_ALL = "all";
	
	private static final String TAG_FAVORITE = "favorite";
	
	public static MainTabActivity INSTANCE;
	
	public static final boolean POSTGINGERBREAD = !Build.VERSION.RELEASE.startsWith("1") &&
		!Build.VERSION.RELEASE.startsWith("2"); // this way around is future save
	
	
	private static Boolean LIGHTTHEME;
	
	public static boolean isLightTheme(Context context) {
		if (LIGHTTHEME == null) {
			LIGHTTHEME = PreferenceManager.getDefaultSharedPreferences(context).getBoolean(Strings.SETTINGS_LIGHTTHEME, false);
		}
		return LIGHTTHEME;
	}
	
	private Menu menu;
	
	private boolean hasContent;
	
	public void onCreate(Bundle savedInstanceState) {
		if (isLightTheme(this)) {
	    	setTheme(R.style.Theme_Light);
	    }
	    super.onCreate(savedInstanceState);
	    setContentView(R.layout.tabs);
	    INSTANCE = this;
	    hasContent = false;
        if (getPreferences(MODE_PRIVATE).getBoolean(Strings.PREFERENCE_LICENSEACCEPTED, false)) {
        	setContent();
        } else {
        	/* Workaround for android issue 4499 on 1.5 devices */
        	getTabHost().addTab(getTabHost().newTabSpec(Strings.EMPTY).setIndicator(Strings.EMPTY).setContent(new Intent(this, EmptyActivity.class)));
        	
        	showDialog(DIALOG_LICENSEAGREEMENT);
        }
	}
	
	@Override
	protected Dialog onCreateDialog(int id) {
		AlertDialog.Builder builder = new AlertDialog.Builder(this);
		
		builder.setIcon(android.R.drawable.ic_dialog_alert);
		builder.setTitle(R.string.dialog_licenseagreement);
		builder.setNegativeButton(R.string.button_decline, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.cancel();
				finish();
			}
		});
		builder.setPositiveButton(R.string.button_accept, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
				dialog.dismiss();
				
				Editor editor = getPreferences(MODE_PRIVATE).edit();
				
				editor.putBoolean(Strings.PREFERENCE_LICENSEACCEPTED, true);
				editor.commit();
				
				/* Part of workaround for android issue 4499 on 1.5 devices */
				getTabHost().clearAllTabs();
				
				/* we only want to invoke actions if the license is accepted */
				setContent();
			}
		});
		setupLicenseText(builder);
		builder.setOnKeyListener(new OnKeyListener() {
			public boolean onKey(DialogInterface dialog, int keyCode, KeyEvent event) {
				if (keyCode == KeyEvent.KEYCODE_BACK) {
					dialog.cancel();
					finish();
				}
				return true;
			}
		});
		return builder.create();
	}
	
	@Override
	public boolean onCreateOptionsMenu(Menu menu) {
		this.menu = menu;

		Activity activity = getCurrentActivity();
		
		if (hasContent && activity != null) {
			return activity.onCreateOptionsMenu(menu);
		} else {
			menu.add(Strings.EMPTY); // to let the menu be available
			return true;
		}
	}
	
	@Override
	public boolean onMenuItemSelected(int featureId, MenuItem item) {
		Activity activity = getCurrentActivity();
		
		if (hasContent && activity != null) {
			return activity.onMenuItemSelected(featureId, item);
		} else {
			return super.onMenuItemSelected(featureId, item);
		}
	}
	
	@Override
	public boolean onPrepareOptionsMenu(Menu menu) {
		Activity activity = getCurrentActivity();
		
		if (hasContent && activity != null) {
			return activity.onPrepareOptionsMenu(menu);
		} else {
			return super.onPrepareOptionsMenu(menu);
		}
	}
	
	private void setContent() {
	    TabHost tabHost = getTabHost();
	    
	    tabHost.addTab(tabHost.newTabSpec(TAG_NORMAL).setIndicator(getString(R.string.overview)).setContent(new Intent().setClass(this, RSSOverview.class)));
	    hasContent = true;
	    if (PreferenceManager.getDefaultSharedPreferences(this).getBoolean(Strings.SETTINGS_SHOWTABS, false)) {
	    	tabHost.addTab(tabHost.newTabSpec(TAG_ALL).setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
	    	
		    tabHost.addTab(tabHost.newTabSpec(TAG_FAVORITE).setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true).putExtra(EntriesListActivity.EXTRA_AUTORELOAD, true)));
		    tabsAdded = true;
		    getTabWidget().setVisibility(View.VISIBLE);
	    }
	    if (POSTGINGERBREAD) {
		    /* Change the menu also on ICS when tab is changed */
		    tabHost.setOnTabChangedListener(new OnTabChangeListener() {
				public void onTabChanged(String tabId) {
					if (menu != null) {
						menu.clear();
						onCreateOptionsMenu(menu);
					}
				}
		    });
		    if (menu != null) {
				menu.clear();
				onCreateOptionsMenu(menu);
			}
	    }
	}

	public void setTabWidgetVisible(boolean visible) {
		if (visible) {
			if (!tabsAdded) {
				TabHost tabHost = getTabHost();
				
				tabHost.addTab(tabHost.newTabSpec(TAG_ALL).setIndicator(getString(R.string.all)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
			    tabHost.addTab(tabHost.newTabSpec(TAG_FAVORITE).setIndicator(getString(R.string.favorites), getResources().getDrawable(android.R.drawable.star_big_on)).setContent(new Intent(Intent.ACTION_VIEW, FeedData.EntryColumns.FAVORITES_CONTENT_URI).putExtra(EntriesListActivity.EXTRA_SHOWFEEDINFO, true)));
				tabsAdded = true;
			}
			getTabWidget().setVisibility(View.VISIBLE);
		} else {
			getTabWidget().setVisibility(View.GONE);
		}
		
	}
	
	void setupLicenseText(AlertDialog.Builder builder) {
		ScrollView scrollView = new ScrollView(this);
		
		TextView textView = new TextView(this);
		
		scrollView.addView(textView);
		scrollView.setPadding(0, 0, 2, 0);
		
		textView.setTextColor(textView.getTextColors().getDefaultColor()); // disables color change on selection
		textView.setPadding(5, 0, 5, 0);
		textView.setTextSize(15);
		textView.setAutoLinkMask(Linkify.EMAIL_ADDRESSES | Linkify.WEB_URLS);
		textView.setText(new StringBuilder(getString(R.string.license_intro)).append(Strings.THREENEWLINES).append(getString(R.string.license)));
		builder.setView(scrollView);
	}

}
