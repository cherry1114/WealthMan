package com.example.WealthMan;
/*
Small test to see what is needed to use API to get stock information

https://iextrading.com/developer/docs/#batch-requests
next steps:
    parse JSON
    display needed data
    create chart from JSON data
    improve input
    FIX crash when stock symbol is bad

 */

import android.app.DatePickerDialog;
import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Pair;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import com.example.WealthMan.APIInterface.*;
import com.example.WealthMan.detail.view.DetailActivity;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import org.w3c.dom.Text;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.TimeUnit;


public class HomeActivity extends AppCompatActivity {
    private ArrayList<IconBean> mIconBeenList = new ArrayList<>();
    private ListView lv;
    private String latestprice;
    private IconAdapter sa;
    DatabaseHelper db;
    private EditText p_start;
    private EditText p_end;
    private DatePickerDialog sDatePickerDialog, eDatePickerDialog;

    /*
     * Change to type CustomAutoCompleteView instead of AutoCompleteTextView
     * since we are extending to customize the view and disable filter
     * The same with the XML view, type will be CustomAutoCompleteView
     */
    CustomAutoCompleteView myAutoComplete;
    // adapter for auto-complete
    ArrayAdapter<NameSymbol> myAdapter;
    boolean dbsuccess = true;
    public static final String MY_PREFS_FILE = "wealthman_prefs";
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        Intent intent = getIntent();
        Calendar newDate = Calendar.getInstance();
        SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
        Date startDate = newDate.getTime();
        String today = sd.format(startDate);


//        final int userid = intent.getIntExtra("UserID", 1);
        SharedPreferences preference = getSharedPreferences(MY_PREFS_FILE, MODE_PRIVATE);
        final int userid = preference.getInt("UserID", 1);

        System.out.println("UserID: " + userid);
        db = new DatabaseHelper(this);

        super.onCreate(savedInstanceState);

        // ALL findViewById must be after the following line!
        final RequestQueue queue = Volley.newRequestQueue(this);

        setContentView(R.layout.activity_home);
        p_start = (EditText) findViewById(R.id.periodStart);
        p_start.setText("2016/01/01");
        p_end = (EditText) findViewById(R.id.periodEnd);
        p_end.setText(today);

        lv = (ListView)findViewById(R.id.lv);
        //为listview添加adapter
        lv.setAdapter(new IconAdapter(this,mIconBeenList));
        sa = (IconAdapter) lv.getAdapter();

        setupApp();
        setStartPeriodDate();
        setEndPeriodDate();
        String symbols = db.getWatchList(userid).trim();
        String url = "https://api.iextrading.com/1.0/stock/market/batch?symbols=" + symbols + "&types=quote,news,chart&range=1m&last=5";
        StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        if (response.equals("{}"))
                            Toast.makeText(HomeActivity.this, "No stocks being tracked", Toast.LENGTH_LONG).show();
                        else {
                            getData(response);
                            sa.notifyDataSetChanged();

                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "That didn't work! Do you have internet?", Toast.LENGTH_LONG).show();
            }
        });

        // Add the request to the RequestQueue.
        queue.add(stringRequest);

//        mButtonOk = (Button) findViewById(R.id.button);
//        mTextURI = (EditText) findViewById(R.id.url_to_fetch);
        final TextView mTextView = (TextView) findViewById(R.id.text);
//        mTextURI.append("");
        try {
            // autocompletetextview is in activity_home.xml
            myAutoComplete = (CustomAutoCompleteView) findViewById(R.id.myautocomplete);

            myAutoComplete.setOnItemClickListener(new AdapterView.OnItemClickListener() {

                @Override
                public void onItemClick(AdapterView<?> parent, View arg1, int pos, long id) {
                    RelativeLayout rl = (RelativeLayout) arg1;
                    LinearLayout Lin1 = (LinearLayout) rl.getChildAt(0);
                    TextView tv = (TextView) Lin1.getChildAt(0);
                    TextView sym = (TextView) Lin1.getChildAt(1);
    //                String companyToSend = tv.getText().toString();
                    String stringToSend = sym.getText().toString();
                    myAutoComplete.setText(tv.getText().toString());
                    Log.e("MAIN", stringToSend);
                    nextActivity(stringToSend, userid);
                }

            });
            // add the listener so it will tries to suggest while the user types
            myAutoComplete.addTextChangedListener(new CustomAutoCompleteTextChangedListener(this));
            // ObjectItemData has no value at first
            NameSymbol[] ObjectItemData = new NameSymbol[0];
            // set the custom ArrayAdapter
            myAdapter = new AutocompleteCustomArrayAdapter(this, R.layout.list_view_row_item, ObjectItemData);
            myAutoComplete.setAdapter(myAdapter);
        } catch (NullPointerException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        lv.setOnItemClickListener(new AdapterView.OnItemClickListener(){
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id){
                IconBean stock = mIconBeenList.get(position);
                nextActivity(stock.symbol, userid);
            }
        });
        p_start.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                sDatePickerDialog.show();
                return false;
            }
        });
        p_end.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                eDatePickerDialog.show();
                return false;
            }
        });
    }
    public void nextActivity(String symbol, Integer ID){
        Intent intent = new Intent(HomeActivity.this,com.example.WealthMan.detail.view.DetailActivity.class);
//        Intent intent = new Intent(HomeActivity.this,TransactionLogActivity.class);
        intent.putExtra("Symbol",symbol);
        intent.putExtra("UserID",ID);
        System.out.println("UserID: " + ID);
        startActivity(intent);
    }

    public void setupApp(){
        SharedPreferences preference = getSharedPreferences(MY_PREFS_FILE, MODE_PRIVATE);
        SharedPreferences.Editor editor = preference.edit();
        long now = System.currentTimeMillis();
        long updateDue = preference.getLong("updateDue", 0);
//        if (false) {  // Calc next time to update
        System.out.println("After Read Prefs: Now = " + now + " , Due = " + updateDue);
        if (updateDue < now) {  // Calc next time to update
            System.out.println("Symbol Update due");
            updateDue = now + TimeUnit.MILLISECONDS.convert(30, TimeUnit.DAYS);
            if (updateSymbols()) {
                editor.putLong("updateDue", updateDue);   // Store new time to update
                editor.commit();
                System.out.println("At commit: Now = " + now + " , Due = " + updateDue);
//                System.out.println("Update was saved");
            } else
                Toast.makeText(HomeActivity.this, "Error updating Stock Symbols", Toast.LENGTH_LONG).show();
        } else
            System.out.println("No Symbol Update is due...");
        if (!preference.getBoolean("setupDone", false)) {
            long res = db.createWatchlist();  //special one time add
            if (res > 0) {
                editor.putBoolean("setupDone", true);
                editor.commit();
            } else
                Toast.makeText(HomeActivity.this, "Database Error creating Watch List", Toast.LENGTH_LONG).show();
        }
    }
    public boolean updateSymbols() {
        final RequestQueue queue = Volley.newRequestQueue(this);
        System.out.println("Updating Symbols now");
        String symbolUrl = "https://api.iextrading.com/1.0/ref-data/symbols";
        final GsonBuilder gsonSymbols = new GsonBuilder();
        dbsuccess = false;
        StringRequest symbolRequest = new StringRequest(Request.Method.GET, symbolUrl,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        stockSym[] mySyms = gsonSymbols.create().fromJson(response, stockSym[].class);
                        List<Pair> SymbolList = new ArrayList<Pair>();
                        System.out.println("Length = " + mySyms.length);
                        long val = 0;
                        for (int i = 0; i < mySyms.length; i++) {
                            Pair temp = new Pair(mySyms[i].name, mySyms[i].symbol);
                            SymbolList.add(temp);
                        }
                        db.populateSymbols(SymbolList);
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError error) {
                Toast.makeText(HomeActivity.this, "That didn't work! Do you have an internet connection?", Toast.LENGTH_LONG).show();
            }
        });
        // Add the request to the RequestQueue.
        queue.add(symbolRequest);
        return true;
    }

    public void getData(String jsonData) {
        GsonBuilder gsonBuilder = new GsonBuilder();
        gsonBuilder.registerTypeAdapter(Batches.class, new CompanyListDeserializer());
        Batches watchList = gsonBuilder.create().fromJson(jsonData, Batches.class);
//        Gson gsonPretty = new GsonBuilder().setPrettyPrinting().create();
//        System.out.println(watchList);
//        String json = gsonPretty.toJson(watchList);
//        System.out.println("JSON = " + json);
        for (int index = 0; index < watchList.batches.size(); index++) {
//            String coSym = watchList.batches.get(index).coSym;
            ArrayList<Pair> chartData = new ArrayList<>();
            System.out.println(index
//                    + "\t" + coSym
                    + "\t" + watchList.batches.get(index).quote.symbol
                    + "\t" + watchList.batches.get(index).quote.latestPrice
                    + "\t" + watchList.batches.get(index).quote.change);
            for (int i = 0 ; i < watchList.batches.get(index).chart.size(); i++){
//                float xValue = Float.valueOf(watchList.batches.get(index).chart.get(i).date.replace("-",""));
                float xValue = Float.valueOf(i+1);
                float yValue = (float)watchList.batches.get(index).chart.get(i).close;
                Pair data = new Pair(xValue, yValue);
                chartData.add(data);
            }
                IconBean symbol = new IconBean(
                        watchList.batches.get(index).quote.symbol,
                        watchList.batches.get(index).quote.companyName,
                        watchList.batches.get(index).quote.latestPrice,
                        watchList.batches.get(index).quote.change,
                        chartData
                );
                mIconBeenList.add(symbol); }
    }
    private void setStartPeriodDate() {

        Calendar newCalendar = Calendar.getInstance();
        sDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date startDate = newDate.getTime();
                String fdate = sd.format(startDate);

                p_start.setText(fdate);

            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
//        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

    }
    private void setEndPeriodDate() {

        Calendar newCalendar = Calendar.getInstance();
        eDatePickerDialog = new DatePickerDialog(this, new DatePickerDialog.OnDateSetListener() {

            public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                Calendar newDate = Calendar.getInstance();
                newDate.set(year, monthOfYear, dayOfMonth);
                SimpleDateFormat sd = new SimpleDateFormat("yyyy-MM-dd", Locale.ENGLISH);
                Date startDate = newDate.getTime();
                String fdate = sd.format(startDate);

                p_end.setText(fdate);

            }
        }, newCalendar.get(Calendar.YEAR), newCalendar.get(Calendar.MONTH), newCalendar.get(Calendar.DAY_OF_MONTH));
//        mDatePickerDialog.getDatePicker().setMaxDate(System.currentTimeMillis());

    }
}
