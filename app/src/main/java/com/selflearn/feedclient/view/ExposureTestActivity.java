package com.selflearn.feedclient.view;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.selflearn.feedclient.R;
import com.selflearn.feedclient.utils.ExposureLogManager;

public class ExposureTestActivity extends AppCompatActivity {

    private TextView logTextView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.layout_exposure_test);

        setTitle("曝光测试工具");

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        logTextView = findViewById(R.id.tv_exposure_log);
        Button btnClear = findViewById(R.id.btn_clear_logs);
        Button btnCopy = findViewById(R.id.btn_copy_logs);
        Button btnTest = findViewById(R.id.btn_test_exposure);

        updateLogDisplay();

        btnClear.setOnClickListener(v -> {
            ExposureLogManager.getInstance().clear();
            updateLogDisplay();
            Toast.makeText(this, "日志已清空", Toast.LENGTH_SHORT).show();
        });

        btnCopy.setOnClickListener(v -> {
            String logs = ExposureLogManager.getInstance().getLogsAsText();
            if (logs.trim().isEmpty()) {
                Toast.makeText(this, "没有日志可复制", Toast.LENGTH_SHORT).show();
                return;
            }

            ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData clip = ClipData.newPlainText("曝光日志", logs);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "日志已复制", Toast.LENGTH_SHORT).show();
        });

        btnTest.setOnClickListener(v -> {
            // 添加测试日志
            ExposureLogManager.getInstance().log("test_item_1", "appear", 1.0f);
            ExposureLogManager.getInstance().log("test_item_2", "appear_50", 0.5f);
            ExposureLogManager.getInstance().log("test_item_3", "disappear", 0.0f);

            updateLogDisplay();
            Toast.makeText(this, "测试日志已添加", Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateLogDisplay();
    }

    private void updateLogDisplay() {
        if (logTextView != null) {
            logTextView.setText(ExposureLogManager.getInstance().getLogsAsText());
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}