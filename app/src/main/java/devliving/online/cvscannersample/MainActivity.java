package devliving.online.cvscannersample;

import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.getbase.floatingactionbutton.FloatingActionButton;

import java.io.File;

import devliving.online.cvscanner.CVScanner;
import devliving.online.cvscanner.util.Util;

import static devliving.online.cvscanner.DocumentData.V_FILTER_TYPE_COLOR;

public class MainActivity extends AppCompatActivity {

    private final int REQ_SCAN = 11;

    RecyclerView mList;
    ImageAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        mList = findViewById(R.id.image_list);
        mList.setLayoutManager(new LinearLayoutManager(this));

        mAdapter = new ImageAdapter();
        mList.setAdapter(mAdapter);

        FloatingActionButton fabScan = findViewById(R.id.action_scan);
        fabScan.setOnClickListener(view ->
                CVScanner.startScanner(this, false, true, false, V_FILTER_TYPE_COLOR, false, REQ_SCAN));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_step_by_step) {
            new AlertDialog.Builder(this)
                    .setTitle("Choose one")
                    .setItems(new String[]{"Document (ID, license etc.)" ,
                            "Passport - HoughLines algorithm",
                            "Passport - MRZ based algorithm",
                        "Passport - MRZ based retrial"}, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            Intent intent = new Intent(MainActivity.this, StepByStepTestActivity.class);
                            intent.putExtra(StepByStepTestActivity.EXTRA_SCAN_TYPE, StepByStepTestActivity.CVCommand.values()[which]);
                            startActivity(intent);
                        }
                    })
                    .setCancelable(true)
                    .show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("MAIN", "got activity result, code: " + requestCode + ", result: " + (resultCode == RESULT_OK));

        if (resultCode == RESULT_OK && requestCode == REQ_SCAN) {
            Log.d("MAIN", "got intent data");
            if (data != null && data.getExtras() != null) {
                String[] pathList = data.getStringArrayExtra(CVScanner.RESULT_IMAGES_PATH);
                for (String s : pathList) {
                    File file = new File(s);
                    Uri imageUri = Util.getUriForFile(this, file);
                    if (imageUri != null)
                        mAdapter.add(imageUri);
                    Log.d("MAIN", "added: " + imageUri);
                }
            }
        }
    }
}
