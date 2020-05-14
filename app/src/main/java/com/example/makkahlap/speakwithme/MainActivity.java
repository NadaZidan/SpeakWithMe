package com.example.makkahlap.speakwithme;

import android.content.Intent;
import android.net.Uri;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.Toast;

import com.firebase.ui.auth.AuthUI;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.ChildEventListener;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends AppCompatActivity {
    public static final int DEFAULT_MSG_LENGTH_LIMIT = 1000;
    public static final String ANONYMOUS = "anonymous";
    private static final String TAG = "MainActivity";

    private adapter adapterr;
    private Message message;
    private ListView messageListView;
    private ProgressBar mProgressBar;
    private ImageButton mPhotoPickerButton;
    private EditText mMessageEditText;
    private Button mSendButton;
    private String mUsername;
    private FirebaseDatabase firebaseDatabase;
    private DatabaseReference databaseReference;
    private ChildEventListener childEventListener;
    private FirebaseAuth firebaseAuth;
    private FirebaseAuth.AuthStateListener authStateListener;
    private static final int RC_SIGN_IN = 123;
    private static final int RC_PHOTO_PICKER = 2;
    private FirebaseStorage firebaseStorage;
    private StorageReference photosStorageReference;



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate ( savedInstanceState );
        setContentView ( R.layout.activity_main );

        mUsername = ANONYMOUS;


        mProgressBar = (ProgressBar) findViewById(R.id.progressBar);
        messageListView = (ListView) findViewById(R.id.messageListView);
        mPhotoPickerButton = (ImageButton) findViewById(R.id.photoPickerButton);
        mMessageEditText = (EditText) findViewById(R.id.messageEditText);
        mSendButton = (Button) findViewById(R.id.sendButton);


        firebaseDatabase=FirebaseDatabase.getInstance ();
        firebaseStorage=FirebaseStorage.getInstance ();
        databaseReference=firebaseDatabase.getReference ().child ( "message" );
        photosStorageReference=firebaseStorage.getReference ().child ( "chat_photos" );
        firebaseAuth=FirebaseAuth.getInstance ();


        final List<Message>messages=new ArrayList <> (  );
        adapterr=new adapter (this,R.layout.item_message,messages  );
        messageListView.setAdapter ( ( ListAdapter ) adapterr );

        mProgressBar.setVisibility ( ProgressBar.INVISIBLE );



        mPhotoPickerButton.setOnClickListener ( new View.OnClickListener () {
            @Override
            public void onClick(View v) {

            }
        } );

        mMessageEditText.addTextChangedListener ( new TextWatcher () {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s.toString().trim().length() > 0) {
                    mSendButton.setEnabled(true);
                } else {
                    mSendButton.setEnabled(false);
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        } );

        mMessageEditText.setFilters ( new InputFilter[]{new InputFilter.LengthFilter ( DEFAULT_MSG_LENGTH_LIMIT )} );

        mSendButton.setOnClickListener ( new View.OnClickListener () {
            @Override
            public void onClick(View v) {
                Message friendlyMessage = new Message (mMessageEditText.getText().toString(), mUsername, null);
                databaseReference.push ().setValue ( friendlyMessage );

                mMessageEditText.setText ( " " );
            }
        } );
        childEventListener=new ChildEventListener () {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                  message=dataSnapshot.getValue ( Message.class );
                  adapterr.add ( message);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        databaseReference.addChildEventListener ( childEventListener );


        authStateListener=new FirebaseAuth.AuthStateListener () {
            @Override
            public void onAuthStateChanged(@NonNull FirebaseAuth firebaseAuth) {
                FirebaseUser user=firebaseAuth.getCurrentUser ();
                if (user!=null){
                    //user Signed in
                    OnSignedInInitialize(user.getDisplayName ());

                }else {
                    //user signed out
                    OnSignedOutCleanUp();
                    startActivityForResult(
                            AuthUI.getInstance()
                                    .createSignInIntentBuilder().setIsSmartLockEnabled (false  ).setAvailableProviders( Arrays.asList(
                            new AuthUI.IdpConfig.EmailBuilder().build(),

                            new AuthUI.IdpConfig.GoogleBuilder().build())).build(), RC_SIGN_IN);


                }


            }
        };
        // ImagePickerButton shows an image picker to upload a image for a message
        mPhotoPickerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/jpeg");
                intent.putExtra(Intent.EXTRA_LOCAL_ONLY, true);
                startActivityForResult(Intent.createChooser(intent, "Complete action using"), RC_PHOTO_PICKER);
            }
        });
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult ( requestCode, resultCode, data );
        if (requestCode==RC_SIGN_IN){
            if (resultCode==RESULT_OK){
                Toast.makeText ( this,"Signed In!" ,Toast.LENGTH_LONG).show ();
            }else {
                Toast.makeText ( this,"Signed In canceled!" ,Toast.LENGTH_LONG).show ();
                finish ();
                Uri selectImageUri=data.getData ();
                StorageReference photoRef=photosStorageReference.child ( selectImageUri.getLastPathSegment () );
                photoRef.putFile (selectImageUri  ).addOnSuccessListener ( this, new OnSuccessListener <UploadTask.TaskSnapshot> () {
                    @Override
                    public void onSuccess(UploadTask.TaskSnapshot taskSnapshot) {
                        Uri downloadUrL=taskSnapshot.getUploadSessionUri ();
                        Message message=new Message ( null,mUsername,downloadUrL.toString () );
                        databaseReference.push ().setValue ( message );

                    }
                } );

            }

        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        MenuInflater inflater=getMenuInflater ();
        inflater.inflate(R.menu.menu, menu);
        return true;

    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId ()){
            case R.id.sign_out_menu:
                AuthUI.getInstance ().signOut ( this );
                return true;
                default:
                    return super.onOptionsItemSelected ( item );

        }
    }

    @Override
    protected void onPause() {
        firebaseAuth.removeAuthStateListener ( authStateListener );
        super.onPause ();
        if (authStateListener != null){
            firebaseAuth.removeAuthStateListener ( authStateListener );
        }
        deattachDatabaseReadListener ();
        adapterr.clear ();
    }

    @Override
    protected void onResume() {
        firebaseAuth.addAuthStateListener ( authStateListener );
        super.onResume ();
    }
    private void  OnSignedInInitialize(String username){
        mUsername=username;
        attachDatabaseReadListener ();
        childEventListener=new ChildEventListener () {
            @Override
            public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                message=dataSnapshot.getValue ( Message.class );
                adapterr.add ( message);
            }
            @Override
            public void onChildChanged(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onChildRemoved(DataSnapshot dataSnapshot) {
            }

            @Override
            public void onChildMoved(DataSnapshot dataSnapshot, String s) {
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
            }
        };
        databaseReference.addChildEventListener ( childEventListener );


    }
    private void OnSignedOutCleanUp(){
        mUsername=ANONYMOUS;
        adapterr.clear ();

    }
    private void attachDatabaseReadListener(){
        if(childEventListener==null) {
            childEventListener = new ChildEventListener () {
                @Override
                public void onChildAdded(DataSnapshot dataSnapshot, String s) {
                    message = dataSnapshot.getValue ( Message.class );
                    adapterr.add ( message );
                }

                @Override
                public void onChildChanged(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onChildRemoved(DataSnapshot dataSnapshot) {
                }

                @Override
                public void onChildMoved(DataSnapshot dataSnapshot, String s) {
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            };
            databaseReference.addChildEventListener ( childEventListener );
        }
    }
    private void deattachDatabaseReadListener(){
        if(childEventListener != null){
        databaseReference.removeEventListener ( childEventListener );
        childEventListener=null;
        }
    }
}
