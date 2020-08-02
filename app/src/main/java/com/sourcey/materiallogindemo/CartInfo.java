package com.sourcey.materiallogindemo;

import android.content.DialogInterface;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Handler;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.AuthFailureError;
import com.android.volley.NetworkResponse;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.VolleyLog;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.gson.JsonObject;
import com.koushikdutta.async.future.FutureCallback;
import com.koushikdutta.ion.Ion;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.DataOutputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;

public class CartInfo extends AppCompatActivity implements SensorEventListener, StepListener{

    boolean cartAssigned = false;
    int cartID = 1;
    String email;
    private StepDetector simpleStepDetector;
    private SensorManager sensorManager;
    private Sensor accel;
    private Handler mHandler = new Handler();
    private static final String TEXT_NUM_STEPS = "Number of Steps: ";
    private int numSteps = 0;
    String direction = "Straight";
    private Runnable mToastRunnable;
    TextView active_cart_text;
    Button btn_order_cart,btn_start_cart,btn_stop_cart,btn_left_cart,btn_right_cart,btn_emergency_cart;
    AlertDialog alert,alert2;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_cart_info);
        email= getIntent().getExtras().getString("EMAIL");
        btn_order_cart = (Button) findViewById(R.id.btn_order_cart);
        btn_start_cart = (Button) findViewById(R.id.btn_start_cart);
        btn_stop_cart = (Button) findViewById(R.id.btn_stop_cart);
        btn_left_cart = (Button) findViewById(R.id.btn_left_cart);
        btn_right_cart = (Button) findViewById(R.id.btn_right_cart);
        btn_emergency_cart = (Button) findViewById(R.id.btn_emergency_cart);
        active_cart_text = (TextView) findViewById(R.id.active_cart_text);

        // Get an instance of the SensorManager
        sensorManager = (SensorManager) getSystemService(SENSOR_SERVICE);
        accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        simpleStepDetector = new StepDetector();
        simpleStepDetector.registerListener(this);

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Do you want to order a new cart?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        orderCart();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alert = builder.create();

        AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
        builder2.setMessage("Do you want to apply emergency stop?")
                .setCancelable(false)
                .setPositiveButton("Yes", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        emergency_stop();
                    }
                })
                .setNegativeButton("No", new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });

        alert2 = builder2.create();

        btn_order_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert.show();
            }
        });

        btn_start_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //mHandler.postDelayed(mToastRunnable,5000);
                numSteps = 0;
                sensorManager.registerListener(CartInfo.this, accel, SensorManager.SENSOR_DELAY_FASTEST);
                mToastRunnable.run();
            }
        });

        btn_stop_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                sensorManager.unregisterListener(CartInfo.this);
                mHandler.removeCallbacks(mToastRunnable);
            }
        });

        btn_left_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(CartInfo.this, "Left Button pressed", Toast.LENGTH_SHORT).show();
                direction = "Left";
            }
        });


        btn_right_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //Toast.makeText(CartInfo.this, "Right button pressed", Toast.LENGTH_SHORT).show();
                direction = "Right";
            }
        });

        btn_emergency_cart.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                alert2.show();
            }
        });
        mToastRunnable = new Runnable() {
            @Override
            public void run() {
                getDistance();
                numSteps = 0;
                direction = "Straight";
                mHandler.postDelayed(mToastRunnable,5000);
            }
        };


    }

    public void emergency_stop(){
        try {
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String URL = "http://139.59.15.209:5000/stopCart";
            JSONObject jsonBody = new JSONObject();
            jsonBody.put("cartID", cartID);
            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY", response);
                    Toast.makeText(CartInfo.this, "Cart Stopped.!!", Toast.LENGTH_SHORT).show();
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        sensorManager.unregisterListener(CartInfo.this);
        mHandler.removeCallbacks(mToastRunnable);
    }
    public void orderCart() {
        if (cartAssigned){
            direction = "Reverse";
            Log.d("Direction",direction);
            cartID = 2;
            active_cart_text.setText("Active cart: "+Integer.toString(cartID));
            Log.d("CartID",Integer.toString(cartID));
            try {
                RequestQueue requestQueue = Volley.newRequestQueue(this);
                String URL = "http://139.59.15.209:5000/startCart";
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("cartID", cartID);
                jsonBody.put("prevCartID", cartID - 1);
                jsonBody.put("email", email);
                final String requestBody = jsonBody.toString();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("VOLLEY", response);
                        Toast.makeText(CartInfo.this, "New cart ordered.!!", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", error.toString());
                    }
                }) {
                    @Override
                    public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        try {
                            return requestBody == null ? null : requestBody.getBytes("utf-8");
                        } catch (UnsupportedEncodingException uee) {
                            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                            return null;
                        }
                    }

                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        String responseString = "";
                        if (response != null) {
                            responseString = String.valueOf(response.statusCode);
                            // can get more details such as response.headers
                        }
                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                    }
            };

            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }
    else{
            active_cart_text.setText("Active cart: "+Integer.toString(cartID));
            cartAssigned = true;
            Log.d("CartID",Integer.toString(cartID));
            try {
                RequestQueue requestQueue = Volley.newRequestQueue(this);
                String URL = "http://139.59.15.209:5000/startCart";
                JSONObject jsonBody = new JSONObject();
                jsonBody.put("cartID", cartID);
                jsonBody.put("prevCartID", cartID - 1);
                jsonBody.put("email", email);
                final String requestBody = jsonBody.toString();

                StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
                        Log.i("VOLLEY", response);
                        Toast.makeText(CartInfo.this, "New cart ordered.!!", Toast.LENGTH_SHORT).show();
                    }
                }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e("VOLLEY", error.toString());
                    }
                }) {
                    @Override
                    public String getBodyContentType() {
                        return "application/json; charset=utf-8";
                    }

                    @Override
                    public byte[] getBody() throws AuthFailureError {
                        try {
                            return requestBody == null ? null : requestBody.getBytes("utf-8");
                        } catch (UnsupportedEncodingException uee) {
                            VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                            return null;
                        }
                    }

                    @Override
                    protected Response<String> parseNetworkResponse(NetworkResponse response) {
                        String responseString = "";
                        if (response != null) {
                            responseString = String.valueOf(response.statusCode);
                            // can get more details such as response.headers
                        }
                        return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                    }
                };

                requestQueue.add(stringRequest);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }

    public void getDistance(){
        try {
            if (direction.equals("Reverse")){
                cartID = cartID - 1;
                numSteps = 1;
            }
            RequestQueue requestQueue = Volley.newRequestQueue(this);
            String URL = "http://139.59.15.209:5000/sendDirection";
            Toast.makeText(CartInfo.this, direction+": "+numSteps, Toast.LENGTH_SHORT).show();
            JSONObject jsonBody = new JSONObject();
            Log.d("dicrection",direction);
            Log.d("distance",String.valueOf(Math.round(numSteps*76.2)));
            Log.d("cartID",String.valueOf(cartID));
            jsonBody.put("direction", String.valueOf(direction));
            jsonBody.put("distance", String.valueOf(Math.round(numSteps*76.2)));
            jsonBody.put("cartID", cartID);
            final String requestBody = jsonBody.toString();

            StringRequest stringRequest = new StringRequest(Request.Method.POST, URL, new Response.Listener<String>() {
                @Override
                public void onResponse(String response) {
                    Log.i("VOLLEY", response);
                }
            }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError error) {
                    Log.e("VOLLEY", error.toString());
                }
            }) {
                @Override
                public String getBodyContentType() {
                    return "application/json; charset=utf-8";
                }

                @Override
                public byte[] getBody() throws AuthFailureError {
                    try {
                        return requestBody == null ? null : requestBody.getBytes("utf-8");
                    } catch (UnsupportedEncodingException uee) {
                        VolleyLog.wtf("Unsupported Encoding while trying to get the bytes of %s using %s", requestBody, "utf-8");
                        return null;
                    }
                }

                @Override
                protected Response<String> parseNetworkResponse(NetworkResponse response) {
                    String responseString = "";
                    if (response != null) {
                        responseString = String.valueOf(response.statusCode);
                        // can get more details such as response.headers
                    }
                    return Response.success(responseString, HttpHeaderParser.parseCacheHeaders(response));
                }
            };

            requestQueue.add(stringRequest);
        } catch (JSONException e) {
            e.printStackTrace();
    }
        if (direction.equals("Reverse")){
            Log.d("LOG",direction);
            sensorManager.unregisterListener(CartInfo.this);
            mHandler.removeCallbacks(mToastRunnable);
        }
    }




    @Override
    public void onSensorChanged(SensorEvent event) {
        if (event.sensor.getType() == Sensor.TYPE_ACCELEROMETER) {
            simpleStepDetector.updateAccel(
                    event.timestamp, event.values[0], event.values[1], event.values[2]);
        }
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    @Override
    public void step(long timeNs) {
        numSteps++;
    }
}
