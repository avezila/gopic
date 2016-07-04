package com.avesila.gopic;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.Toast;

import auth.AuthGrpc;
import auth.AuthOuterClass;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;

/**
 * Created by vlad on 05.07.16.
 */
public class NetworkConnection extends AsyncTask<Void, Void, String> {

    private ManagedChannel channel;
    private AuthGrpc.AuthBlockingStub blockingStub;
    private Context context;
    
    public NetworkConnection (Context context)
    {
        this.context = context;
    }

    @Override
    protected String doInBackground ( Void... voids ) {

        channel = ManagedChannelBuilder.forAddress ( "gopic.ru", 5353 )
                                       .usePlaintext ( true )
                                       .build ();
        
        AuthOuterClass.RegisterReq registerReq = AuthOuterClass.RegisterReq
                .newBuilder ()
                .setLogin ( "vlad" )
                .setPassword ( "gopic" )
                .setEmail ( "dudko.vlad@gmail.com" )
                .build ();
                

        AuthOuterClass.LoginReq loginReq = AuthOuterClass.LoginReq
                .newBuilder ()
                .setLogin ( "vlad" )
                .setPassword ( "gopic" )
                .build ();

        blockingStub = AuthGrpc.newBlockingStub ( channel );
        //AuthOuterClass.RegisterRes registerRes = blockingStub.register ( registerReq );
        AuthOuterClass.LoginRes  loginRes = blockingStub.login ( loginReq );

        return loginRes.getSession ();
    }

    @Override
    protected void onPostExecute ( String s ) {
        super.onPostExecute ( s );
        Toast.makeText ( context, s, Toast.LENGTH_LONG ).show ();
        
    }
}
