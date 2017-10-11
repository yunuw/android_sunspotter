package edu.uw.yw239.sunspotter;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.util.LruCache;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
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

import java.util.ArrayList;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MainActivity";

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

                sunSpotterSearch(zipCode);
            }
        });
    }

    private void sunSpotterSearch(String searchTerm){
        String urlString = "http://api.openweathermap.org/data/2.5/forecast?zip=" + searchTerm
                + "&format=json" + "&units=imperial" + "&appid=bb264d6c8d3a55daf186ca534f121625";

        Request request = new JsonObjectRequest(Request.Method.GET, urlString, null,
                new Response.Listener<JSONObject>() {
                    public void onResponse(JSONObject response) {
                        ArrayList<WeatherInfo> weatherInfos = new ArrayList<>();

                        try {
                            JSONArray results = response.getJSONArray("list");

                            for(int i = 0; i < results.length(); i++){
                                WeatherInfo info = new WeatherInfo();
                                info.icon = results.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("icon");
                                info.weather = results.getJSONObject(i).getJSONArray("weather").getJSONObject(0).getString("main");
                                info.date = results.getJSONObject(i).getString("dt_txt");
                                info.temp = results.getJSONObject(i).getJSONObject("main").getDouble("temp");

                                weatherInfos.add(info);
                            }
                        } catch (JSONException e){
                            e.getStackTrace();
                        }

//                        adapter.clear();
//                        for(String media : mediaTitles) {
//                            adapter.add(media);
//                        }
                    }
                },
                new Response.ErrorListener() {

                    public void onErrorResponse(VolleyError error) {
                        String errorMsg = new String(error.networkResponse.data);
                        Toast.makeText(MainActivity.this.getApplicationContext(), errorMsg, Toast.LENGTH_LONG).show();
                    }
            });
        RequestSingleton.getInstance(this).add(request);
    }

    protected static class WeatherInfo{
        String icon;
        String weather;
        String date;
        Double temp;
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
