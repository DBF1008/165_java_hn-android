package com.manuelmaly.hn.login;

import android.app.Activity;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import com.manuelmaly.hn.R;
import com.manuelmaly.hn.data.network.HNApiClient;
import com.manuelmaly.hn.data.storage.AppSettings;
import com.manuelmaly.hn.server.HNCredentials;
import com.manuelmaly.hn.task.HNLoginTask;
import com.manuelmaly.hn.task.ITaskFinishedHandler;

import dagger.hilt.android.AndroidEntryPoint;
import javax.inject.Inject;

@AndroidEntryPoint
public class LoginActivity extends Activity implements ITaskFinishedHandler<Boolean> {

    private static final int TASKCODE_LOGIN = 10;

    @Inject HNApiClient apiClient;
    @Inject AppSettings appSettings;

    Button mSaveButton;
    Button mCancelButton;
    EditText mUsernameText;
    EditText mPasswordText;

    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login_dialog);

        mSaveButton = findViewById(R.id.user_settings_dialog_save_button);
        mCancelButton = findViewById(R.id.user_settings_dialog_cancel_button);
        mUsernameText = findViewById(R.id.user_settings_dialog_username);
        mPasswordText = findViewById(R.id.user_settings_dialog_password);

        mCancelButton.setOnClickListener(v -> exit());
        mSaveButton.setOnClickListener(v -> saveCredentials());
    }

    void exit() {
        setResult(RESULT_CANCELED);
        finish();
    }

    void saveCredentials() {
        mSaveButton.setText(R.string.checking);
        HNLoginTask.start(mUsernameText.getText().toString(), mPasswordText.getText().toString(),
                this, this, TASKCODE_LOGIN, apiClient, appSettings);
    }

    @Override
    public void onTaskFinished(int taskCode,
            com.manuelmaly.hn.task.ITaskFinishedHandler.TaskResultCode code,
            Boolean result, Object tag) {
        if (result != null && result) {
            setResult(RESULT_OK);
            HNCredentials.invalidate();
            finish();
        } else {
            int messageId;
            if (result != null && !result)
                messageId = R.string.error_login_failed;
            else
                messageId = code.equals(TaskResultCode.NoNetworkConnection) ? R.string.error_login_device_offline
                    : R.string.error_unknown_error;
            Toast.makeText(this, getString(messageId), Toast.LENGTH_LONG).show();
            mSaveButton.setText(getString(R.string.check_and_save));
        }
    }
}
