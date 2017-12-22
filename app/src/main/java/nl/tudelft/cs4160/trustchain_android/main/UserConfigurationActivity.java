package nl.tudelft.cs4160.trustchain_android.main;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.View;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import nl.tudelft.cs4160.trustchain_android.R;
import nl.tudelft.cs4160.trustchain_android.SharedPreferences.UserNameStorage;

/**
 * Created by Boning on 12/3/2017.
 */

public class UserConfigurationActivity extends AppCompatActivity{
    Context context;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        context = this;
        if(UserNameStorage.getUserName(this) == null) {
            setContentView(R.layout.user_configuration);
            Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
            setSupportActionBar(toolbar);

            EditText userNameInput = (EditText) findViewById(R.id.username);
            userNameInput.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                @Override
                public void onFocusChange(View v, boolean hasFocus) {
                    if (!hasFocus) {
                        hideKeyboard(v);
                    }
                }
            });
            Button confirmBtn = (Button) findViewById(R.id.confirm_button);
            confirmBtn.setOnClickListener(new View.OnClickListener() {
                @SuppressLint({"ResourceAsColor", "NewApi"})
                public void onClick(View v) {
                    EditText userNameInput = (EditText) findViewById(R.id.username);
                    if(!userNameInput.getText().toString().matches("")) {
                        Intent myIntent = new Intent(UserConfigurationActivity.this, OverviewConnectionsActivity.class);
                        EditText mEdit = (EditText)findViewById(R.id.username);
                        UserNameStorage.setUserName(context,mEdit.getText().toString());
                        myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                        UserConfigurationActivity.this.startActivity(myIntent);
                    } else {
                        TextView userNot = (TextView) findViewById(R.id.user_notification);
                        userNot.setTextColor(getResources().getColor(R.color.colorStatusCantConnect, null));
                        userNot.setText("Please fill in a username first!");
                    }
                }
            });
        } else {
            Intent myIntent = new Intent(UserConfigurationActivity.this, OverviewConnectionsActivity.class);
            myIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            UserConfigurationActivity.this.startActivity(myIntent);
        }
    }

    public void hideKeyboard(View view) {
        InputMethodManager inputMethodManager =(InputMethodManager)getSystemService(Activity.INPUT_METHOD_SERVICE);
        inputMethodManager.hideSoftInputFromWindow(view.getWindowToken(), 0);
    }
}
