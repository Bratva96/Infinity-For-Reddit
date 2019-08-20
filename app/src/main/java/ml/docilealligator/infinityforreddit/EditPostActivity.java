package ml.docilealligator.infinityforreddit;

import android.content.Intent;
import android.content.res.Configuration;
import android.os.Build;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import androidx.core.content.ContextCompat;

import com.google.android.material.snackbar.Snackbar;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;

import java.util.HashMap;
import java.util.Map;

import javax.inject.Inject;
import javax.inject.Named;

import butterknife.BindView;
import butterknife.ButterKnife;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;

public class EditPostActivity extends AppCompatActivity {

    static final String EXTRA_TITLE = "ET";
    static final String EXTRA_CONTENT = "EC";
    static final String EXTRA_FULLNAME = "EF";
    static final String EXTRA_ACCESS_TOKEN = "EAT";

    @BindView(R.id.coordinator_layout_edit_post_activity) CoordinatorLayout coordinatorLayout;
    @BindView(R.id.toolbar_edit_post_activity) Toolbar toolbar;
    @BindView(R.id.post_title_text_view_edit_post_activity) TextView titleTextView;
    @BindView(R.id.post_text_content_edit_text_edit_post_activity) EditText contentEditText;

    private String mFullName;
    private String mAccessToken;
    private boolean isSubmitting = false;

    @Inject
    @Named("oauth")
    Retrofit mOauthRetrofit;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_post);

        ButterKnife.bind(this);

        ((Infinity) getApplication()).getAppComponent().inject(this);

        EventBus.getDefault().register(this);

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Window window = getWindow();
            if((getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) != Configuration.UI_MODE_NIGHT_YES) {
                window.getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_NAVIGATION_BAR);
            }
            window.setNavigationBarColor(ContextCompat.getColor(this, R.color.navBarColor));
        }

        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mFullName = getIntent().getExtras().getString(EXTRA_FULLNAME);
        mAccessToken = getIntent().getExtras().getString(EXTRA_ACCESS_TOKEN);
        titleTextView.setText(getIntent().getExtras().getString(EXTRA_TITLE));
        contentEditText.setText(getIntent().getExtras().getString(EXTRA_CONTENT));

        contentEditText.requestFocus();
        getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.edit_post_activity, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if(item.getItemId() == R.id.action_send_edit_post_activity) {
            if(!isSubmitting) {
                isSubmitting = true;

                Snackbar.make(coordinatorLayout, R.string.posting, Snackbar.LENGTH_SHORT).show();

                Map<String, String> params = new HashMap<>();
                params.put(RedditUtils.THING_ID_KEY, mFullName);
                params.put(RedditUtils.TEXT_KEY, contentEditText.getText().toString());

                mOauthRetrofit.create(RedditAPI.class)
                        .editPostOrComment(RedditUtils.getOAuthHeader(mAccessToken), params)
                        .enqueue(new Callback<String>() {
                            @Override
                            public void onResponse(@NonNull Call<String> call, @NonNull Response<String> response) {
                                isSubmitting = false;
                                Toast.makeText(EditPostActivity.this, R.string.edit_success, Toast.LENGTH_SHORT).show();
                                Intent returnIntent = new Intent();
                                setResult(RESULT_OK, returnIntent);
                                finish();
                            }

                            @Override
                            public void onFailure(@NonNull Call<String> call, @NonNull Throwable t) {
                                isSubmitting = false;
                                Snackbar.make(coordinatorLayout, R.string.post_failed, Snackbar.LENGTH_SHORT).show();
                            }
                        });

            }
            return true;
        } else if(item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return false;
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        EventBus.getDefault().unregister(this);
    }

    @Subscribe
    public void onAccountSwitchEvent(SwitchAccountEvent event) {
        finish();
    }
}
