package com.neos.weatherservice;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ToggleButton;

import java.io.File;

public class Activity_Settings extends Activity {

    private SharedPreferences mSettings;
    private static final String APP_PREFERENCES = "settings_app";
    private static final String APP_PREFERENCES_PATH = "PathIgoAvic";
    private ToggleButton closeApp;
    private Spinner spinnerLang;
    private Spinner spinnerUnits;
    private Spinner spinnerTime;
    private Integer intLang;
    private Integer intLangWU;
    private Integer intUnit;
    private Integer intUnitWU;
    private Integer intTime;
    private Spinner spinnerPack;
    private ToggleButton SoundPlayOnOff;
    private Boolean choiceServ;
    private ToggleButton commonProfile;
    private TextView textFile;
    private static final int PICKFILE_RESULT_CODE = 1;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);
        InitSet();
    }

    private void InitSet() {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        spinnerLang = (Spinner) findViewById(R.id.spinnerLang);
        spinnerUnits = (Spinner) findViewById(R.id.spinnerUnits);
        spinnerPack = (Spinner) findViewById(R.id.spinnerPack);
        spinnerTime = (Spinner) findViewById(R.id.spinnerTimeUpd);
        closeApp = (ToggleButton) findViewById(R.id.closeApp);
        SoundPlayOnOff = (ToggleButton) findViewById(R.id.playSound);
        commonProfile = (ToggleButton) findViewById(R.id.profileButton);
        RadioButton opm = (RadioButton) findViewById(R.id.radioButtonOPM);
        RadioButton wu = (RadioButton) findViewById(R.id.radioButtonWU);
        RadioButton closeScreen = (RadioButton) findViewById(R.id.radioButton_close_screen);
        RadioButton closeIgo = (RadioButton) findViewById(R.id.radioButton_close_igo);
        textFile = (TextView) findViewById(R.id.textViewPath);
        if (mSettings.contains(APP_PREFERENCES_PATH)) {
            textFile.setText(mSettings.getString(APP_PREFERENCES_PATH, ""));
        }
        choiceServ = mSettings.getBoolean("Serv", true);
        boolean playSound = mSettings.getBoolean("PlayOnOff", false);
        boolean checkBoxValue = mSettings.getBoolean("CloseProg", false);
        boolean commonProfileValue = mSettings.getBoolean("CommonProfiles", false);
        Boolean closeService = mSettings.getBoolean("CloseService", false);

        if (playSound){
            SoundPlayOnOff.setChecked(true);
        } else {
            SoundPlayOnOff.setChecked(false);
        }
        if (checkBoxValue) {
            closeApp.setChecked(true);
        } else {
            closeApp.setChecked(false);
        }
        if (choiceServ) {
            opm.setChecked(true);
            wu.setChecked(false);
        } else {
            opm.setChecked(false);
            wu.setChecked(true);
        }
        if (commonProfileValue) {
            commonProfile.setChecked(true);
        } else {
            commonProfile.setChecked(false);
        }
        if (closeService) {
            closeScreen.setChecked(true);
            closeIgo.setChecked(false);
        } else {
            closeScreen.setChecked(false);
            closeIgo.setChecked(true);
        }
        intLang = mSettings.getInt("Lang", 0);
        intLangWU = mSettings.getInt("LangWU", 0);
        intUnit = mSettings.getInt("Unit", 0);
        intUnitWU = mSettings.getInt("UnitWU", 0);
        intTime = mSettings.getInt("TimeSpPos", 4);
        spinerSelected();
        seinerSelectedPack();
    }

    public void onClickPath(View view) {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("file/*");
        startActivityForResult(intent,PICKFILE_RESULT_CODE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch(requestCode){
            case PICKFILE_RESULT_CODE:
                if(resultCode == RESULT_OK){
                    String FilePath = data.getData().getPath();
                    String FileName = data.getData().getLastPathSegment();
                    int lastPos = FilePath.length() - FileName.length();
                    String Folder = FilePath.substring(0, lastPos);

                    textFile.setText(Folder);
                    File file = new File(Folder + "/save");
                    if (file.exists() && file.isDirectory()) {
                        Toast toast = Toast.makeText(getApplicationContext(),
                                R.string.text_toast_ok,
                                Toast.LENGTH_SHORT);
                        toast.show();
                        savePrefString(APP_PREFERENCES_PATH, Folder);
                    } else {
                        Toast toast_no = Toast.makeText(getApplicationContext(),
                                R.string.text_toast_no,
                                Toast.LENGTH_SHORT);
                        toast_no.show();
                    }
                }
                break;
        }
    }

    private void spinerSelected() {
// Настраиваем адаптер
        ArrayAdapter<?> adapterLang;
        ArrayAdapter<?> adapterUnits;
        if (choiceServ) {
            adapterLang =
                    ArrayAdapter.createFromResource(this, R.array.lang, android.R.layout.simple_spinner_item);
            adapterUnits =
                    ArrayAdapter.createFromResource(this, R.array.units, android.R.layout.simple_spinner_item);
        } else {
            adapterLang =
                    ArrayAdapter.createFromResource(this, R.array.langW, android.R.layout.simple_spinner_item);
            adapterUnits =
                    ArrayAdapter.createFromResource(this, R.array.unitsWU, android.R.layout.simple_spinner_item);
        }
        adapterLang.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        adapterUnits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        ArrayAdapter<?> adapterTime =
                ArrayAdapter.createFromResource(this, R.array.time, android.R.layout.simple_spinner_item);
        adapterUnits.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

// Вызываем адаптер
        spinnerLang.setAdapter(adapterLang);
        spinnerUnits.setAdapter(adapterUnits);
        spinnerTime.setAdapter(adapterTime);

        if (choiceServ) {
            spinnerLang.setSelection(intLang);
            spinnerUnits.setSelection(intUnit);
        } else {
            spinnerLang.setSelection(intLangWU);
            spinnerUnits.setSelection(intUnitWU);
        }
        spinnerTime.setSelection(intTime);


        spinnerLang.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (choiceServ) {
                    savePrefInt("Lang", position);
                    intLang = position;
                    retSpinPref();
                } else {
                    savePrefInt("LangWU", position);
                    intLangWU = position;
                    retSpinPrefWU();
                }

            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinnerUnits.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (choiceServ) {
                    savePrefInt("Unit", position);
                    intUnit = position;
                    retSpinUnit();
                } else {
                    savePrefInt("UnitWU", position);
                    intUnitWU = position;
                    retSpinUnitWU();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });

        spinnerTime.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                savePrefInt("TimeSpPos", position);
                intTime = position;
                retSpinTime();
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    public void onClickCloseApp(View view) {
        savePreferences("CloseProg", closeApp.isChecked());
    }
    public void onClickSoundPlay(View view) { savePreferences("PlayOnOff", SoundPlayOnOff.isChecked()); }
    public void onClickServOPM(View view) {
        savePreferences("Serv", true);
        InitSet();
    }

    public void onClickServWU(View view) {
        savePreferences("Serv", false);
        InitSet();
    }

    public void onClickCommonProfile(View view) {
        savePreferences("CommonProfiles", commonProfile.isChecked());
        InitSet();
    }

    public void onClickCloseScreen(View view) {
        savePreferences("CloseService", true);
        InitSet();
    }

    public void onClickCloseIgo(View view) {
        savePreferences("CloseService", false);
        InitSet();
    }

    private void savePreferences(String key, Boolean value) {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putBoolean(key, value);
        editor.apply();
    }

    private void savePrefInt(String key, Integer value) {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putInt(key, value);
        editor.apply();
    }

    private void savePrefString(String key, String value) {
        mSettings = getSharedPreferences(APP_PREFERENCES, Context.MODE_PRIVATE);
        SharedPreferences.Editor editor = mSettings.edit();
        editor.putString(key, value);
        editor.apply();
    }

    private void retSpinPref() {
        if (intLang == 0) {
            savePrefString("LangName", "en");
        } else if (intLang == 1){
            savePrefString("LangName", "ru");
        } else if (intLang == 2) {
            savePrefString("LangName", "it");
        } else if (intLang == 3) {
            savePrefString("LangName", "es");
        } else if (intLang == 4) {
            savePrefString("LangName", "uk");
        } else if (intLang == 5) {
            savePrefString("LangName", "de");
        } else if (intLang == 6) {
            savePrefString("LangName", "pt");
        } else if (intLang == 7) {
            savePrefString("LangName", "ro");
        } else if (intLang == 8) {
            savePrefString("LangName", "pl");
        } else if (intLang == 9) {
            savePrefString("LangName", "fi");
        } else if (intLang == 10) {
            savePrefString("LangName", "nl");
        } else if (intLang == 11) {
            savePrefString("LangName", "fr");
        } else if (intLang == 12) {
            savePrefString("LangName", "bg");
        } else if (intLang == 13) {
            savePrefString("LangName", "sv");
        } else if (intLang == 14) {
            savePrefString("LangName", "tr");
        } else if (intLang == 15) {
            savePrefString("LangName", "hr");
        } else if (intLang == 16) {
            savePrefString("LangName", "ca");
        }
    }

    private void retSpinPrefWU() {
        if (intLangWU == 0) {
            savePrefString("LangNameWU", "BY");
        } else if (intLangWU == 1){
            savePrefString("LangNameWU", "BU");
        } else if (intLangWU == 2) {
            savePrefString("LangNameWU", "LI");
        } else if (intLangWU == 3) {
            savePrefString("LangNameWU", "CR");
        } else if (intLangWU == 4) {
            savePrefString("LangNameWU", "CZ");
        } else if (intLangWU == 5) {
            savePrefString("LangNameWU", "EN");
        } else if (intLangWU == 6) {
            savePrefString("LangNameWU", "ET");
        } else if (intLangWU == 7) {
            savePrefString("LangNameWU", "FI");
        } else if (intLangWU == 8) {
            savePrefString("LangNameWU", "FR");
        } else if (intLangWU == 9) {
            savePrefString("LangNameWU", "DL");
        } else if (intLangWU == 10) {
            savePrefString("LangNameWU", "IL");
        } else if (intLangWU == 11) {
            savePrefString("LangNameWU", "HU");
        } else if (intLangWU == 12) {
            savePrefString("LangNameWU", "IT");
        } else if (intLangWU == 13) {
            savePrefString("LangNameWU", "LV");
        } else if (intLangWU == 14) {
            savePrefString("LangNameWU", "LT");
        } else if (intLangWU == 15) {
            savePrefString("LangNameWU", "PL");
        } else if (intLangWU == 16) {
            savePrefString("LangNameWU", "RO");
        } else if (intLangWU == 17) {
            savePrefString("LangNameWU", "RU");
        } else if (intLangWU == 18) {
            savePrefString("LangNameWU", "SP");
        }
    }

    private void retSpinUnit() {
        if (intUnit == 0) {
                savePrefString("UnitName", "standart");
        } else if (intUnit == 1) {
            savePrefString("UnitName", "metric");
        } else if (intUnit == 2) {
            savePrefString("UnitName", "imperial");
        }
    }

    private void retSpinUnitWU() {
        if (intUnit == 0) {
            savePrefString("UnitNameWU", "metric");
        } else if (intUnit == 1) {
            savePrefString("UnitNameWU", "imperial");
        }
    }

    private void retSpinTime() {
        if (intTime == 0) {
            savePrefInt("TimeUpdate", 600000); //10
        } else if (intTime == 1) {
            savePrefInt("TimeUpdate", 900000); //15
        } else if (intTime == 2) {
            savePrefInt("TimeUpdate", 1200000); //20
        } else if (intTime == 3) {
            savePrefInt("TimeUpdate", 1500000); //25
        } else if (intTime == 4) {
            savePrefInt("TimeUpdate", 1800000); //30
        } else if (intTime == 5) {
            savePrefInt("TimeUpdate", 3600000); //60
        } else if (intTime == 6) {
            savePrefInt("TimeUpdate", 5400000); //90
        } else if (intTime == 7) {
            savePrefInt("TimeUpdate", 7200000); //120
        } else if (intTime == 8) {
            savePrefInt("TimeUpdate", 9000000); //150
        } else if (intTime == 9) {
            savePrefInt("TimeUpdate", 10800000); //180
        }
    }

    private void seinerSelectedPack() {
        ArrayAdapter<?> adapterPack =
                ArrayAdapter.createFromResource(this, R.array.packageName, android.R.layout.simple_spinner_item);
        adapterPack.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spinnerPack.setAdapter(adapterPack);

        spinnerPack.setSelection(mSettings.getInt("SpinPos", 0));

        spinnerPack.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {

            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                String PackName = spinnerPack.getSelectedItem().toString();
                savePrefInt("SpinPos", position);
                savePrefString("PackName", PackName);
                inspectionApp(PackName);
            }

            @Override
            public void onNothingSelected(AdapterView<?> arg0) {
            }
        });
    }

    private void inspectionApp(String appNameInsp) {
        Intent LaunchIntent = getPackageManager().getLaunchIntentForPackage(appNameInsp);
        if (LaunchIntent == null) {
            Toast.makeText(getApplicationContext(),
                    R.string.text_toast_no_app,
                    Toast.LENGTH_SHORT).show();
        }
    }

}
