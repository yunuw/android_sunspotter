package edu.uw.yw239.sunspotter;

import android.content.Context;
import android.graphics.Bitmap;
import android.nfc.Tag;
import android.support.v4.graphics.drawable.DrawableWrapper;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";
    private WeatherInfoAdapter weatherInfoAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button button = (Button)findViewById(R.id.btnFind);

        button.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                TextView input = (TextView)findViewById(R.id.txtZipCode);
                String zipCode = input.getText().toString();

                //Dismiss a virtual keyboard
                InputMethodManager imm = (InputMethodManager)getSystemService(Context.INPUT_METHOD_SERVICE);
                imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);

                sunSpotterSearch(zipCode);
            }
        });


        // Construct the data source
        ArrayList<WeatherInfo> arrayOfWeatherInfo = new ArrayList<WeatherInfo>();
        // Create the adapter to convert the array to views
        weatherInfoAdapter = new WeatherInfoAdapter(this, arrayOfWeatherInfo);
        // Attach the adapter to a ListView
        AdapterView listView = (AdapterView) findViewById(R.id.rollerale_view);
        listView.setAdapter(weatherInfoAdapter);
    }

    private void sunSpotterSearch(String searchTerm){
        String urlString = "http://api.openweathermap.org/data/2.5/forecast?q=" + searchTerm
                + "&format=json" + "&units=imperial" + "&appid=bb264d6c8d3a55daf186ca534f121625";

        Request request = new JsonObjectRequest(Request.Method.GET, urlString, null,
                new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        ArrayList<WeatherInfo> weatherInfos = new ArrayList<>();

                        try {
                            JSONArray results = response.getJSONArray("list");

                            for(int i = 0; i < results.length(); i++){
                                String icon = results.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("icon");
                                String weather = results.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("main");
                                String date = results.getJSONObject(i).getString("dt_txt");
                                Double temp = results.getJSONObject(i).getJSONObject("main").getDouble("temp");

                                WeatherInfo info = new WeatherInfo(icon, weather, date, temp);
                                weatherInfos.add(info);
                            }
                        } catch (JSONException e){
                            e.getStackTrace();
                        }

                        boolean foundSun = false;
                        String date = "Make your own sunshine:)";

                        weatherInfoAdapter.clear();
                        for(WeatherInfo weatherInfo : weatherInfos) {
                            weatherInfoAdapter.add(weatherInfo);
                            if(((weatherInfo.weather).equals("Clear") || (weatherInfo.icon).equals("few clouds")) && foundSun == false){
                                foundSun = true;
                                String dateString = weatherInfo.date;
                                SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
                                SimpleDateFormat finalDateFormat = new SimpleDateFormat("E HH:mm a");
                                Date convertedDate = new Date();
                                try {
                                    convertedDate = dateFormat.parse(dateString);
                                } catch (ParseException e) {
                                    e.printStackTrace();
                                }
                                date = "At " + finalDateFormat.format(convertedDate);
                            }
                        }

                        TextView resultView = (TextView)findViewById(R.id.findResultTxt);
                        TextView dateView = (TextView)findViewById(R.id.dateTime);
                        ImageView findResultImage = (ImageView)findViewById(R.id.findResultImg);

                        String result = foundSun == true ? "There will be sun!" : "Sorry! No sun.";
                        String resultImage = foundSun == true ? "yes" : "no";
                        int id = getResources().getIdentifier(resultImage, "drawable", getPackageName());

                        resultView.setText(result);
                        dateView.setText(date);
                        findResultImage.setImageResource(id);
                    }
                },
                new Response.ErrorListener() {

                    public void onErrorResponse(VolleyError error) {
                        String errorMsg = new String(error.networkResponse.data);
                        //// TODO: 10/12/17
                        Toast.makeText(MainActivity.this.getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
            });
        RequestSingleton.getInstance(this).add(request);
    }

    public class WeatherInfoAdapter extends ArrayAdapter<WeatherInfo> {
        // View lookup cache
        private class ViewHolder {
            ImageView icon;
            TextView weather;
            TextView date;
            TextView temp;
        }

        public WeatherInfoAdapter(Context context, ArrayList<WeatherInfo> users) {
            super(context, R.layout.item_weatherinfo, users);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            // Get the data item for this position
            WeatherInfo weatherInfo = getItem(position);
            // Check if an existing view is being reused, otherwise inflate the view
            ViewHolder viewHolder; // view lookup cache stored in tag
            if (convertView == null) {
                // If there's no view to re-use, inflate a brand new view for row
                viewHolder = new ViewHolder();
                LayoutInflater inflater = LayoutInflater.from(getContext());
                convertView = inflater.inflate(R.layout.item_weatherinfo, parent, false);
                viewHolder.icon = (ImageView) convertView.findViewById(R.id.ivIcon);
                viewHolder.weather = (TextView) convertView.findViewById(R.id.tvWeather);
                viewHolder.date = (TextView) convertView.findViewById(R.id.tvDate);
                viewHolder.temp = (TextView) convertView.findViewById(R.id.tvTemp);
                // Cache the viewHolder object inside the fresh view
                convertView.setTag(viewHolder);
            } else {
                // View is being recycled, retrieve the viewHolder object from tag
                viewHolder = (ViewHolder) convertView.getTag();
            }
            // Populate the data from the data object via the viewHolder object
            // into the template view.
            int id = getResources().getIdentifier("icon" + weatherInfo.icon, "drawable", getPackageName());

            String dateString = weatherInfo.date;
            SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            SimpleDateFormat finalDateFormat = new SimpleDateFormat("E HH:mm a");
            Date convertedDate = new Date();
            try {
                convertedDate = dateFormat.parse(dateString);
            } catch (ParseException e) {
                e.printStackTrace();
            }

            viewHolder.icon.setImageDrawable(getDrawable(id));
            viewHolder.weather.setText(weatherInfo.weather);
            viewHolder.date.setText(finalDateFormat.format(convertedDate) + " ");
            viewHolder.temp.setText("(" + weatherInfo.temp.toString() + "°)");
            // Return the completed view to render on screen
            return convertView;
        }
    }

    protected static class WeatherInfo{
        String icon;
        String weather;
        String date;
        Double temp;

        public WeatherInfo(){}

        public WeatherInfo(String i, String w, String d, Double t){
            icon = i;
            weather = w;
            date = d;
            temp = t;
        }
    }

    protected static class RequestSingleton {
        //the single instance of this singleton
        private static RequestSingleton instance;

        private RequestQueue requestQueue = null; //the singleton's RequestQueue
        private ImageLoader imageLoader = null;

        //private constructor; cannot instantiate directly
        private RequestSingleton(Context ctx){
            //create the requestQueue
            this.requestQueue = Volley.newRequestQueue(ctx.getApplicationContext());

            //create the imageLoader
            imageLoader = new ImageLoader(requestQueue,
                    new ImageLoader.ImageCache() {  //define an anonymous Cache object
                        //the cache instance variable
                        private final LruCache<String, Bitmap> cache = new LruCache<String, Bitmap>(20);

                        //method for accessing the cache
                        @Override
                        public Bitmap getBitmap(String url) {
                            return cache.get(url);
                        }

                        //method for storing to the cache
                        @Override
                        public void putBitmap(String url, Bitmap bitmap) {
                            cache.put(url, bitmap);
                        }
                    });
        }

        //call this "factory" method to access the Singleton
        public static RequestSingleton getInstance(Context ctx) {
            //only create the singleton if it doesn't exist yet
            if(instance == null){
                instance = new RequestSingleton(ctx);
            }

            return instance; //return the singleton object
        }

        //get queue from singleton for direct action
        public RequestQueue getRequestQueue() {
            return this.requestQueue;
        }

        //convenience wrapper method
        public <T> void add(Request<T> req) {
            requestQueue.add(req);
        }

        public ImageLoader getImageLoader() {
            return this.imageLoader;
        }
    }
}
