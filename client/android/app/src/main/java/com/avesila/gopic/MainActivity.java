package com.avesila.gopic;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;




public class MainActivity extends AppCompatActivity {
    
    @Override
    protected void onCreate ( Bundle savedInstanceState ) {
        
        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_main );
        
        /*new LoginTask ( this, "vlad", "gopic" )
                .register ( "dudko.vlad@gmail.com" )
                .execute ();
        */
        
    }

    @Override
    protected void onResume () {

        super.onResume ();
        Intent loginActivityIntent = new Intent ( this, LoginActivity.class );

        startActivity ( loginActivityIntent );//todo remove this from here
    }
}
