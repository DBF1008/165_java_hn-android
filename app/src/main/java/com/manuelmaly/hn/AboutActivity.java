package com.manuelmaly.hn;

import android.graphics.Typeface;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

import com.manuelmaly.hn.util.FontHelper;

public class AboutActivity extends AppCompatActivity {

    TextView mHNView;
    TextView mByView;
    TextView mURLView;
    TextView mGithubView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.about);

        mHNView = findViewById(R.id.about_hn);
        mByView = findViewById(R.id.about_by);
        mURLView = findViewById(R.id.about_url);
        mGithubView = findViewById(R.id.about_github);

        setupViews();
    }

    private void setupViews() {
        Typeface tf = FontHelper.getComfortaa(this, true);
        mHNView.setTypeface(tf);

        mURLView.setMovementMethod(LinkMovementMethod.getInstance());
        mURLView.setText(Html.fromHtml("<a href=\"http://www.creativepragmatics.com\">creativepragmatics.com</a>"));

        mGithubView.setMovementMethod(LinkMovementMethod.getInstance());
        mGithubView.setText(Html.fromHtml("<a href=\"https://github.com/manmal/hn-android/\">Fork this at Github</a>"));
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
