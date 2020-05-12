package com.example.notekeeper;

import android.content.Intent;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.lifecycle.ViewModelProvider;

import android.os.PersistableBundle;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;

import java.util.List;

public class NoteActivity extends AppCompatActivity {
    public static final String NOTE_INFO = "com.example.notekeeper.NOTE_INFO";
    public static final String NOTE_INFO_POSITION = "com.example.notekeeper.NOTE_INFO_POSITION";
    public static final int POSITION_NOT_SET = -1;
    private NoteInfo note;
    private NoteInfo mNote;
    private Boolean mIsNewNote;
    private Spinner mSpinnerCourses;
    private EditText mTextNoteTitle;
    private EditText mTextNoteText;
    private int mNotePosition;
    private boolean mIsCacelling;
    private NoteActivityViewModel mViewModel;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_note);
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // instantiate the view model by first getting the view model provider
        ViewModelProvider viewModelProvider = new ViewModelProvider(getViewModelStore(), ViewModelProvider.AndroidViewModelFactory.getInstance(getApplication()));
        mViewModel =  viewModelProvider.get(NoteActivityViewModel.class);

        // if the activity is newly created, we don't restore our state
        if (savedInstanceState != null && mViewModel.mIsNewlyCreated) {
            mViewModel.restoreState(savedInstanceState);
        }
        mViewModel.mIsNewlyCreated = false;

        mSpinnerCourses = findViewById(R.id.spinner_courses);

        // retrieves the list of courses
        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        // create a new adapter with the context set to this, a spinner layout and the data for the courses
        ArrayAdapter<CourseInfo> adapterCourses = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, courses);
        // set the drop down view resource to be the default android layout for it
        adapterCourses.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // set the spinner courses Spinner adapter to the adapter we just made
        mSpinnerCourses.setAdapter(adapterCourses);

        // read the note info that was passed from the note list activity
        readDisplayStateValues();

        saveOriginalNoteValues();

        mTextNoteTitle = findViewById(R.id.text_note_title);
        mTextNoteText = findViewById(R.id.text_note_text);

        // display the passed values if there is a note
        if (!mIsNewNote) {
            displayNote(mSpinnerCourses, mTextNoteTitle, mTextNoteText);
        }

//        FloatingActionButton fab = findViewById(R.id.fab);
//        fab.setOnClickListener(new View.OnClickListener() {
//            @Override
//            public void onClick(View view) {
//                Snackbar.make(view, "Replace with your own action", Snackbar.LENGTH_LONG)
//                        .setAction("Action", null).show();
//            }
//        });
    }

    // instance state management
    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        // verify that the bundle is not null
        if (outState != null) {
            mViewModel.saveState(outState);
        }
    }

    /* Not necessary
    @Override
    protected void onRestoreInstanceState(@NonNull Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        if (savedInstanceState != null) {
            mViewModel.restoreState(savedInstanceState);
        }
    } */

    private void saveOriginalNoteValues() {
        if (mIsNewNote) {
            return;
        }

        mViewModel.mOriginalNoteCourseId = mNote.getCourse().getCourseId();
        mViewModel.mOriginalNoteTitle = mNote.getTitle();
        mViewModel.mOriginalNoteText = mNote.getText();
    }

    private void displayNote(Spinner spinnerCourses, EditText textNoteTitle, EditText textNoteText) {
        List<CourseInfo> courses = DataManager.getInstance().getCourses();
        int courseIndex = courses.indexOf(mNote.getCourse());

        spinnerCourses.setSelection(courseIndex);
        textNoteTitle.setText(mNote.getTitle());
        textNoteText.setText(mNote.getText());
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (mIsCacelling) {
            if (mIsNewNote) {
                // removes the note if it is a new note
                DataManager.getInstance().removeNote(mNotePosition);
            }
            else {
                storePreviousNoteValues();
            }
        } else {
            // Save the note if not cancelled
            saveNote();
        }
    }

    private void storePreviousNoteValues() {
        CourseInfo course = DataManager.getInstance().getCourse(mViewModel.mOriginalNoteCourseId);

        // sets back the values
        mNote.setCourse(course);
        mNote.setTitle(mViewModel.mOriginalNoteTitle);
        mNote.setText(mViewModel.mOriginalNoteText);
    }

    private void saveNote() {
        mNote.setCourse((CourseInfo) mSpinnerCourses.getSelectedItem());
        mNote.setTitle(mTextNoteTitle.getText().toString());
        mNote.setText(mTextNoteText.getText().toString());
    }

    private void readDisplayStateValues() {
        Intent intent = getIntent();
        // if we're using parcelable we use NOTE_INFO
//        mNote = intent.getParcelableExtra(NOTE_INFO);
//
//        mIsNewNote = mNote == null;

        // we need to give it a default value because only reference types return null, hence we
        // need to set it to position not set which has a value of -1
        int position = intent.getIntExtra(NOTE_INFO_POSITION, POSITION_NOT_SET);
        mIsNewNote = position == POSITION_NOT_SET;

        if (mIsNewNote) {
            createNewNote();
        } else {
            mNote = DataManager.getInstance().getNotes().get(position);
        }
    }

    private void createNewNote() {
        DataManager dataManager = DataManager.getInstance();
        // the position of the newly created note
        mNotePosition = dataManager.createNewNote();
        mNote = dataManager.getNotes().get(mNotePosition);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_note, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_send_mail) {
            sendEmail();
            return true;
        }
        else if (id == R.id.action_cancel) {
            mIsCacelling = true;
            finish();
        }

        return super.onOptionsItemSelected(item);
    }

    private void sendEmail() {
        CourseInfo course = (CourseInfo) mSpinnerCourses.getSelectedItem();
        String subject = mTextNoteTitle.getText().toString();
        String body = "Checkout what I learned in the Pluralsight course \"" + course.getTitle() + "\n" +  mTextNoteText.getText().toString();

        // initialize an intent and set it to the action for sending an email
        Intent intent = new Intent(Intent.ACTION_SEND);
        // we need multiple characteristics, so we need to set the type
        intent.setType("message/rfc2822"); // this is a mime-type for email

        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        startActivity(intent);
    }
}
