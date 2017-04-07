package cachalote.blogspot.myapplication;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.provider.CalendarContract;
import android.support.annotation.RequiresApi;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.util.Log;
import android.view.View;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.DecimalFormat;
import java.util.Calendar;
import java.util.UUID;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    private TextView waterLevel;
    private Switch powerDevice;
    private ImageView waterTankImage;
    private TextView dataGet;

    Handler bluetoothIn;
    final int handlerState = 0;//Identifica l mensaje enviado por Bluetooth
    private BluetoothAdapter btAdapter = null;//Adapatador de bluetooth para la aplicación
    private BluetoothSocket btSocket = null;//
    private StringBuilder recDataString = new StringBuilder();
    private float quantity = 0;


    Calendar calendar = Calendar.getInstance();
    int dia,mes,anio;

    private ConnectedThread mConnectedThread;


    // SPP UUID service - Para tener la compatibilidad con la mayoria de dispositivos
    private static final UUID BTMODULEUUID = UUID.fromString("00001101-0000-1000-8000-00805F9B34FB");

    private static String address = "";

    /**
     * Metodo que evalua el swich grafico para encender o apagar el dispositivo
     */
    public CompoundButton.OnCheckedChangeListener onCheckedChangeListener = new CompoundButton.OnCheckedChangeListener() {
        @Override
        public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
            if(buttonView == powerDevice && isChecked){
                mConnectedThread.write("1");
                messageToast("Device On");
            }else if(buttonView == powerDevice){
                mConnectedThread.write("0");
                messageToast("Device Off");
            }
        }
    };



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                goToSearchDevice();
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        /**
         * Instanciar los elementos llamados de la parte grafica
         */
        powerDevice = (Switch)findViewById(R.id.swich_power_device);
        powerDevice.setOnCheckedChangeListener(onCheckedChangeListener);
        waterTankImage = (ImageView)findViewById(R.id.water_tank_imagen);
        waterLevel = (TextView)findViewById(R.id.water_level);
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        dataGet = (TextView)findViewById(R.id.data_get);

        /**
         * Evalua si existe alguna direccion eviada por al activity searchDevice
         */
        if(getIntent().hasExtra("address")){
            address = getIntent().getExtras().getString("address");
        }
        /**
         * Si la direccion MAC esta limpia envia al usuario al activity searchDevice
         * para seleccionar un dispositivo
         */
        if(address.equals("")){
            goToSearchDevice();
        }

        Animation rise = (Animation) AnimationUtils.loadAnimation(this, R.anim.water_level);
        waterLevel.setAnimation(rise);

        /**
         * Metodo que recibe e interpreta los datos, por bluetooth
         */
        bluetoothIn = new Handler() {
            public void handleMessage(android.os.Message msg) {
                if (msg.what == handlerState) {
                    String readMessage = (String) msg.obj;
                    recDataString.append(readMessage);
                    int endOfLineIndex = recDataString.indexOf("~");//Determina el final del string recibido
                    if (endOfLineIndex > 0) {//Depura el string recibido
                        String dataInPrint = recDataString.substring(1, endOfLineIndex);
                        dataGet.setText(recDataString);
                        waterLevel.setText(""+dataInPrint+"%");
                        DecimalFormat df = new DecimalFormat("#.00");//Evita que se muestren mas de dos decimales
                        try {
                            quantity = (Float.parseFloat(dataInPrint));//Convierte el dato recibido a un dato float
                        }catch (Exception e){
                            Log.d("Arduino nano",e.getMessage());
                            quantity = 0;
                        }

                        /**
                         * Evalua el nivel del agua, le da una imagen y muestra el nivel con numeros
                         * de manera grafica
                         */
                        if(quantity >=75){
                            waterTankImage.setImageResource(R.drawable.water_100);
                            waterLevel.setText(""+df.format(quantity)+"%");
                        }else if(quantity >= 50 && quantity <= 74){
                            waterTankImage.setImageResource(R.drawable.water_50);
                            waterLevel.setText(""+df.format(quantity)+"%");
                        }else if(quantity >= 25 && quantity <= 49){
                            waterTankImage.setImageResource(R.drawable.water_25);
                            waterLevel.setText(""+df.format(quantity)+"%");
                        }else if(quantity <= 24){
                            waterTankImage.setImageResource(R.drawable.water_0);
                            waterLevel.setText(""+df.format(quantity)+"%");
                        }

                        recDataString.delete(0, recDataString.length());//Limpia la variable que recibe los datos
                        dataInPrint = " ";
                    }
                }
            }
        };

        btAdapter = BluetoothAdapter.getDefaultAdapter();//Adapta el bluetooth
        checkBTState();
    }

    private BluetoothSocket createBluetoothSocket(BluetoothDevice device) throws IOException {
        return  device.createRfcommSocketToServiceRecord(BTMODULEUUID);
    }


    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement


        return super.onOptionsItemSelected(item);
    }

    @RequiresApi(api = Build.VERSION_CODES.N)
    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_home) {
            goToMainActivity();
        } else if (id == R.id.nav_calendar) {
            addEvent();
        } else if (id == R.id.nav_share) {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, "http://chaacweb.herokuapp.com/");
            startActivity(Intent.createChooser(intent, "Share with"));
        } else if (id == R.id.nav_info) {
            goToAboutActivity();
        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    private void goToAboutActivity() {
        Intent intent = new Intent(this, AboutActivity.class);
        startActivity(intent);
    }

    public void addEvent() {
        try {
            dia = calendar.get(Calendar.DAY_OF_MONTH);
            mes = calendar.get(Calendar.MONTH);
            anio = calendar.get(Calendar.YEAR);
            Calendar beginTime = Calendar.getInstance();
            beginTime.set(anio, mes,dia, 7, 30);
            Calendar endTime = Calendar.getInstance();
            endTime.set(anio, mes, dia, 7, 30);
            Intent intent = new Intent(Intent.ACTION_INSERT)
                    .setData(CalendarContract.Events.CONTENT_URI)
                    .addCategory(Intent.CATEGORY_DEFAULT)
                    .putExtra(CalendarContract.EXTRA_EVENT_BEGIN_TIME, beginTime.getTimeInMillis())
                    .putExtra(CalendarContract.EXTRA_EVENT_END_TIME, endTime.getTimeInMillis())
                    .putExtra(CalendarContract.Events.TITLE, "Chaac")
                    .putExtra(CalendarContract.Events.DESCRIPTION, "(Editar )Es necesaria una acción con el dispositivo.")
                    .putExtra(CalendarContract.Events.EVENT_LOCATION, "Casa")
                    .putExtra(CalendarContract.Events.AVAILABILITY, CalendarContract.Events.AVAILABILITY_BUSY)
                    .putExtra(CalendarContract.Events.CUSTOM_APP_PACKAGE, getPackageName())
                    .putExtra(CalendarContract.Events.CUSTOM_APP_URI, "myapplication");
            startActivity(intent);
        }catch (Exception e){
            Log.i("NO funcionó", e.getMessage());
        }
    }

    /**
     * Envia al layout principal
     */
    private void  goToMainActivity(){
        Intent intent = new Intent(this,MainActivity.class);
        startActivity(intent);
        finish();
    }

    /**
     * Envia al layout para seleccionar dispositivos
     */
    public void goToSearchDevice(){
        Intent intent = new Intent(this, searchDevice.class);
        startActivity(intent);
        finish();
    }

    /**
     * Muestra mensajes de tipo toast
     * @param message
     */
    public void messageToast(String message){
        Toast.makeText(MainActivity.this, message, Toast.LENGTH_LONG).show();
    }

    @Override
    public void onResume() {
        super.onResume();
        //Crea un dispositivo con la direccion MAC seleccionada
        BluetoothDevice device = btAdapter.getRemoteDevice(address);

        try {
            btSocket = createBluetoothSocket(device);
        } catch (IOException e) {
            //Toast.makeText(getBaseContext(), "Socket creation failed", Toast.LENGTH_LONG).show();
        }
        // Establece la conexion
        try
        {
            btSocket.connect();
        } catch (IOException e) {
            try
            {
                btSocket.close();
            } catch (IOException e2)
            {
                Log.d("Arduino nano","Imposible conectar");
            }
        }

        mConnectedThread = new ConnectedThread(btSocket);
        mConnectedThread.start();

        /**
         * Envia caracter para validar la conexion
         */
        mConnectedThread.write("x");
    }

    @Override
    public void onPause()
    {
        super.onPause();
        try
        {
            //Borra la conexion bluetooth cuando la aplicación de pausa, para eviata sobrecargar el dispositivo
            btSocket.close();
        } catch (IOException e2) {
            Log.d("Arduino nano","No se puede desconectar el dispositivo");
        }
    }

    //Revisa si el bluetooth aun esta activo al ponerse en pausa
    private void checkBTState() {
        if(btAdapter==null) {
            Toast.makeText(getBaseContext(), "Device does not support bluetooth", Toast.LENGTH_LONG).show();
        } else {
            if (btAdapter.isEnabled()) {
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                startActivityForResult(enableBtIntent, 1);
            }
        }
    }

    /**
     * Metodo que permite la transmision y recepcion de datos
     */
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        //Creacion del tread para conexion
        public ConnectedThread(BluetoothSocket socket) {
            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                //Crea parametro de entrada y salida para la aplicacion
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) { }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            //Se mantiene buscando datos a recibir
            while (true) {
                try {
                    bytes = mmInStream.read(buffer);//Lee los bytes recibidos
                    String readMessage = new String(buffer, 0, bytes);
                    //Enivia lo obtenido a la interface de usuario via handler
                    bluetoothIn.obtainMessage(handlerState, bytes, -1, readMessage).sendToTarget();
                } catch (IOException e) {
                    break;
                }
            }
        }
        //write method
        public void write(String input) {
            byte[] msgBuffer = input.getBytes();//Transforama los que se desea enviar a bytes
            try {
                mmOutStream.write(msgBuffer);//Envia los datos por bluetooth
            } catch (IOException e) {
                //Si no se puede enviar te envia a seleccionar un nuevo dispositivo
                messageToast(getString(R.string.lost_connection));
                goToSearchDevice();
            }
        }
    }
}
